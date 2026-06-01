package com.spondon.app.feature.donor

import android.content.Context

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.CommunityRepository
import com.spondon.app.core.data.repository.DonorRepository
import com.spondon.app.core.data.repository.UserRepository
import com.spondon.app.core.domain.model.Community
import com.spondon.app.core.domain.model.Donation
import com.spondon.app.core.domain.model.DonationStatus
import com.spondon.app.core.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════
// UI States
// ═══════════════════════════════════════════════════════════════

data class FindDonorState(
    val donors: List<User> = emptyList(),
    val communities: List<Community> = emptyList(),
    val searchQuery: String = "",
    val selectedBloodGroups: List<String> = emptyList(),
    val selectedCommunityId: String? = null,
    val selectedDistrict: String? = null,
    val availableOnly: Boolean = false,
    val sortBy: DonorSortOption = DonorSortOption.MOST_DONATIONS,
    val isLoading: Boolean = false,
    val isFilterSheetVisible: Boolean = false,
    val error: String? = null,
)

enum class DonorSortOption(val label: String) {
    MOST_DONATIONS("Most Donations"),
    RECENTLY_ACTIVE("Recently Active"),
    NAME("Name"),
}

data class DonorProfileState(
    val donor: User? = null,
    val currentUser: User? = null,
    val sharedCommunities: List<Community> = emptyList(),
    val donationHistory: List<Donation> = emptyList(),
    val isAvailable: Boolean = false,
    val cooldownDaysRemaining: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class DonationHistoryState(
    val donations: List<Donation> = emptyList(),
    val totalDonations: Int = 0,
    val nextEligibleDays: Int = 0,
    val isEligibleNow: Boolean = false,
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val certificateMessage: String? = null,
)

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String, // emoji or icon key
    val criteria: Int, // number of donations required
    val earnedDate: Date? = null,
)

data class AchievementsState(
    val badges: List<Badge> = emptyList(),
    val totalDonations: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DonorViewModel @Inject constructor(
    private val donorRepository: DonorRepository,
    private val userRepository: UserRepository,
    private val communityRepository: CommunityRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val currentUserId get() = auth.currentUser?.uid ?: ""

    /** Tracks badge IDs last written to Firestore to avoid re-triggering the snapshot listener. */
    private var lastWrittenBadgeIds: Set<String> = emptySet()

    /** True once the real-time observer has delivered at least one emission. */
    private var observerHasEmitted = false

    // ─── Find Donor State ────────────────────────────────────
    private val _findState = MutableStateFlow(FindDonorState())
    val findState: StateFlow<FindDonorState> = _findState.asStateFlow()

    // ─── Donor Profile State ─────────────────────────────────
    private val _profileState = MutableStateFlow(DonorProfileState())
    val profileState: StateFlow<DonorProfileState> = _profileState.asStateFlow()

    // ─── Donation History State ──────────────────────────────
    private val _historyState = MutableStateFlow(DonationHistoryState())
    val historyState: StateFlow<DonationHistoryState> = _historyState.asStateFlow()

    // ─── Achievements State ──────────────────────────────────
    private val _achievementsState = MutableStateFlow(AchievementsState())
    val achievementsState: StateFlow<AchievementsState> = _achievementsState.asStateFlow()

    init {
        observeCurrentUser()
    }

    /**
     * Listens to the current user’s Firestore document in real-time.
     *
     * When [totalDonations] or [lastDonationDate] changes (i.e. after a
     * donation is confirmed by a requester), this automatically:
     *   • refreshes the donation history list from Firestore
     *   • recalculates the 120-day eligibility countdown
     *   • recalculates earned badges
     *
     * This solves the stale-data problem where DonationHistoryScreen and
     * AchievementsScreen stay open in the backstack while another screen
     * (RequestDetailScreen) confirms a donation.
     */
    private fun observeCurrentUser() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            var previousDonationCount = -1
            // Use distinctUntilChanged keyed on fields that matter for the UI
            // to prevent redundant recompositions when unrelated fields change.
            userRepository.observeUser(currentUserId)
                .distinctUntilChanged { old, new ->
                    old.totalDonations == new.totalDonations &&
                            old.lastDonationDate == new.lastDonationDate &&
                            old.badges == new.badges &&
                            old.availabilityOverride == new.availabilityOverride &&
                            old.donationInterval == new.donationInterval
                }
                .collect { user ->
                    observerHasEmitted = true
                    val (isEligible, cooldownDays) = checkAvailability(user)

                    // ── Update history state ─────────────────────────────────
                    val countChanged = previousDonationCount != -1 &&
                            user.totalDonations != previousDonationCount
                    previousDonationCount = user.totalDonations

                    // Reload donations list whenever count changes OR first emission
                    val donations = if (countChanged || _historyState.value.donations.isEmpty()) {
                        (donorRepository.getDonationHistory(currentUserId) as? Resource.Success)?.data
                            ?: _historyState.value.donations
                    } else {
                        _historyState.value.donations
                    }

                    // Only update state if something actually changed to avoid
                    // redundant recompositions that cause flickering.
                    val currentHistory = _historyState.value
                    if (currentHistory.totalDonations != user.totalDonations ||
                        currentHistory.isEligibleNow != isEligible ||
                        currentHistory.nextEligibleDays != cooldownDays ||
                        currentHistory.donations !== donations ||
                        currentHistory.user?.badges != user.badges ||
                        currentHistory.isLoading
                    ) {
                        _historyState.update {
                            it.copy(
                                user = user,
                                donations = donations,
                                totalDonations = user.totalDonations,
                                nextEligibleDays = cooldownDays,
                                isEligibleNow = isEligible,
                                isLoading = false,
                            )
                        }
                    }

                    // ── Update achievements state ─────────────────────────────
                    recalculateBadges(user)
                }
        }
    }

    /** Recalculates badge list from [user.totalDonations] and persists newly earned ones. */
    private fun recalculateBadges(user: com.spondon.app.core.domain.model.User) {
        val totalDonations = user.totalDonations
        val earnedBadges = user.badges

        val allBadges = listOf(
            Badge("first_drop", "First Drop", "Complete your first blood donation", "🩸", 1),
            Badge("life_saver", "Life Saver", "Complete 5 blood donations", "💉", 5),
            Badge("hero_donor", "Hero Donor", "Complete 10 blood donations", "🦸", 10),
            Badge("legend", "Donation Legend", "Complete 25 blood donations", "🏆", 25),
            Badge("champion", "Community Champion", "Complete 50 blood donations", "👑", 50),
            Badge("century", "Century Donor", "Complete 100 blood donations", "💯", 100),
        ).map { badge ->
            when {
                // Badge already persisted in Firestore
                totalDonations >= badge.criteria && earnedBadges.contains(badge.id) ->
                    badge.copy(earnedDate = user.createdAt ?: Date())

                // Badge newly earned (not yet persisted)
                totalDonations >= badge.criteria ->
                    badge.copy(earnedDate = Date())

                else -> badge
            }
        }

        // Persist newly earned badges using a targeted field-level update
        // instead of a full-document write to avoid re-triggering the snapshot
        // listener with all user fields (which caused infinite flickering).
        //
        // We additionally track `lastWrittenBadgeIds` in memory so that the
        // Firestore write only fires for *genuinely new* badges, not on the
        // second observer emission caused by the write itself.
        val newBadgeIdSet = allBadges.filter { it.earnedDate != null }.map { it.id }.toSet()
        val needsPersist = newBadgeIdSet != earnedBadges.toSet() && newBadgeIdSet != lastWrittenBadgeIds
        if (needsPersist) {
            lastWrittenBadgeIds = newBadgeIdSet
            // Update the achievements state FIRST (before Firestore write) so
            // the UI shows the correct badges immediately. The Firestore write
            // will trigger another observer emission, but since the state will
            // already match, the change-detection guard below will skip it.
            _achievementsState.update {
                it.copy(
                    badges = allBadges,
                    totalDonations = totalDonations,
                    isLoading = false,
                )
            }
            viewModelScope.launch {
                try {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    firestore.collection(com.spondon.app.core.common.Constants.USERS_COLLECTION)
                        .document(user.uid)
                        .update("badges", newBadgeIdSet.toList())
                        .await()
                } catch (_: Exception) { }
            }
        } else {
            // Only update achievements state if something actually changed to
            // avoid redundant recompositions that cause flickering.
            val currentAchievements = _achievementsState.value
            val earnedIds = allBadges.filter { it.earnedDate != null }.map { it.id }.toSet()
            val currentEarnedIds = currentAchievements.badges.filter { it.earnedDate != null }.map { it.id }.toSet()
            if (currentAchievements.totalDonations != totalDonations ||
                currentEarnedIds != earnedIds ||
                currentAchievements.isLoading
            ) {
                _achievementsState.update {
                    it.copy(
                        badges = allBadges,
                        totalDonations = totalDonations,
                        isLoading = false,
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Find Donor
    // ═══════════════════════════════════════════════════════════

    fun loadFindDonor() {
        viewModelScope.launch {
            _findState.update { it.copy(isLoading = true, error = null) }

            // Load user's communities for the filter
            val commResult = communityRepository.getMyCommunities(currentUserId)
            val communities = (commResult as? Resource.Success)?.data ?: emptyList()

            _findState.update { it.copy(communities = communities) }

            // Initial search with no filters
            searchDonors()
        }
    }

    fun searchDonors() {
        viewModelScope.launch {
            _findState.update { it.copy(isLoading = true, error = null) }
            val state = _findState.value

            // Use first selected blood group for Firestore query
            val bloodGroup = state.selectedBloodGroups.firstOrNull()

            val result = donorRepository.searchDonors(
                bloodGroup = bloodGroup,
                communityId = state.selectedCommunityId,
                district = state.selectedDistrict,
                availableOnly = state.availableOnly,
            )

            when (result) {
                is Resource.Success -> {
                    var donors = result.data.filter { it.uid != currentUserId }

                    // Filter by multiple blood groups if more than one selected
                    if (state.selectedBloodGroups.size > 1) {
                        donors = donors.filter { it.bloodGroup in state.selectedBloodGroups }
                    }

                    // Apply search query
                    if (state.searchQuery.isNotBlank()) {
                        val q = state.searchQuery.lowercase()
                        donors = donors.filter {
                            it.name.lowercase().contains(q) ||
                                    it.bloodGroup.lowercase().contains(q) ||
                                    it.district.lowercase().contains(q)
                        }
                    }

                    // Sort
                    donors = when (state.sortBy) {
                        DonorSortOption.MOST_DONATIONS -> donors.sortedByDescending { it.totalDonations }
                        DonorSortOption.RECENTLY_ACTIVE -> donors.sortedByDescending { it.lastDonationDate?.time ?: 0L }
                        DonorSortOption.NAME -> donors.sortedBy { it.name }
                    }

                    _findState.update { it.copy(donors = donors, isLoading = false) }
                }

                is Resource.Error -> {
                    _findState.update { it.copy(isLoading = false, error = result.message) }
                }

                is Resource.Loading -> {}
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _findState.update { it.copy(searchQuery = query) }
        searchDonors()
    }

    fun toggleBloodGroupFilter(bg: String) {
        _findState.update { state ->
            val current = state.selectedBloodGroups.toMutableList()
            if (current.contains(bg)) current.remove(bg) else current.add(bg)
            state.copy(selectedBloodGroups = current)
        }
        searchDonors()
    }

    fun setCommunityFilter(communityId: String?) {
        _findState.update { it.copy(selectedCommunityId = communityId) }
        searchDonors()
    }

    fun setDistrictFilter(district: String?) {
        _findState.update { it.copy(selectedDistrict = district) }
        searchDonors()
    }

    fun toggleAvailableOnly() {
        _findState.update { it.copy(availableOnly = !it.availableOnly) }
        searchDonors()
    }

    fun setSortOption(sort: DonorSortOption) {
        _findState.update { it.copy(sortBy = sort) }
        searchDonors()
    }

    fun toggleFilterSheet() {
        _findState.update { it.copy(isFilterSheetVisible = !it.isFilterSheetVisible) }
    }

    // ═══════════════════════════════════════════════════════════
    // Donor Public Profile
    // ═══════════════════════════════════════════════════════════

    fun loadDonorProfile(userId: String) {
        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }

            val donorResult = donorRepository.getDonorProfile(userId)
            val currentUserResult = userRepository.getUser(currentUserId)

            val donor = (donorResult as? Resource.Success)?.data
            val currentUser = (currentUserResult as? Resource.Success)?.data

            if (donor == null) {
                _profileState.update { it.copy(isLoading = false, error = "Donor not found") }
                return@launch
            }

            // Find shared communities
            val sharedIds = donor.communityIds.filter { it in (currentUser?.communityIds ?: emptyList()) }
            val sharedCommunities = mutableListOf<Community>()
            for (id in sharedIds.take(10)) {
                val comm = communityRepository.getCommunity(id)
                if (comm is Resource.Success) sharedCommunities.add(comm.data)
            }

            // Calculate availability
            val (isAvailable, cooldownDays) = checkAvailability(donor)

            // Load public donation history
            val historyResult = donorRepository.getDonationHistory(userId)
            val history = (historyResult as? Resource.Success)?.data
                ?.filter { it.status == DonationStatus.CONFIRMED }
                ?: emptyList()

            _profileState.update {
                it.copy(
                    donor = donor,
                    currentUser = currentUser,
                    sharedCommunities = sharedCommunities,
                    donationHistory = history.take(5), // Only public summary
                    isAvailable = isAvailable,
                    cooldownDaysRemaining = cooldownDays,
                    isLoading = false,
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // My Donation History
    // ═══════════════════════════════════════════════════════════

    fun loadDonationHistory() {
        // If the observer has already delivered data, skip the explicit
        // load to avoid a loading→loaded→loading→loaded flicker cycle.
        if (observerHasEmitted && !_historyState.value.isLoading) return
        viewModelScope.launch {
            if (!observerHasEmitted) {
                _historyState.update { it.copy(isLoading = true, error = null) }
            }

            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data

            val donationsResult = donorRepository.getDonationHistory(currentUserId)
            val donations = (donationsResult as? Resource.Success)?.data ?: emptyList()

            val (isEligible, cooldownDays) = checkAvailability(user)

            _historyState.update {
                it.copy(
                    donations = donations,
                    totalDonations = user?.totalDonations ?: donations.size,
                    nextEligibleDays = cooldownDays,
                    isEligibleNow = isEligible,
                    user = user,
                    isLoading = false,
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Achievements & Badges
    // ═══════════════════════════════════════════════════════════

    fun loadAchievements() {
        // The real-time observer in observeCurrentUser() already drives
        // _achievementsState.  This explicit call is only needed if the
        // observer hasn't emitted yet (e.g. slow network on first open).
        // If the observer has already delivered data, skip the one-shot
        // fetch to avoid racing with the observer and showing stale data.
        if (observerHasEmitted && !_achievementsState.value.isLoading) return
        viewModelScope.launch {
            // Don't reset isLoading if the observer already set it to false
            // — that would cause a flicker (loading → loaded → loading → loaded).
            if (!observerHasEmitted) {
                _achievementsState.update { it.copy(isLoading = true) }
            }
            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data ?: return@launch
            recalculateBadges(user)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private fun checkAvailability(user: User?): Pair<Boolean, Int> {
        if (user == null) return false to 0
        if (!user.isDonor) return false to 0

        val lastDonation = user.lastDonationDate ?: return true to 0

        val daysSince = TimeUnit.MILLISECONDS.toDays(
            Date().time - lastDonation.time,
        ).toInt()

        val requiredDays = if (user.availabilityOverride) {
            Constants.MIN_OVERRIDE_DAYS
        } else {
            user.donationInterval
        }

        return if (daysSince >= requiredDays) {
            true to 0
        } else {
            false to (requiredDays - daysSince)
        }
    }

    companion object {
        val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    }

    // ═══════════════════════════════════════════════════════════
    // Certificate Generation
    // ═══════════════════════════════════════════════════════════

    fun generateCertificate(context: Context) {
        val state = _historyState.value
        val user = state.user ?: return

        if (state.totalDonations <= 0) {
            _historyState.update { it.copy(certificateMessage = "You need at least one donation to get a certificate.") }
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val certificateData = com.spondon.app.core.util.CertificateGenerator.CertificateData(
                    donorName = user.name.ifBlank { "Donor" },
                    bloodGroup = user.bloodGroup.ifBlank { "Unknown" },
                    totalDonations = user.totalDonations,
                    lastDonationDate = user.lastDonationDate,
                )

                val result = com.spondon.app.core.util.CertificateGenerator.generateCertificate(
                    context = context,
                    data = certificateData,
                )

                if (result != null) {
                    _historyState.update { it.copy(certificateMessage = "Certificate saved to Downloads!") }
                } else {
                    _historyState.update { it.copy(certificateMessage = "Failed to generate certificate.") }
                }
            } catch (e: Exception) {
                _historyState.update { it.copy(certificateMessage = "Failed to generate certificate: ${e.localizedMessage}") }
            }
        }
    }

    fun clearCertificateMessage() {
        _historyState.update { it.copy(certificateMessage = null) }
    }
}
