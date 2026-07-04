package com.spondon.app.feature.request


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.remote.FirestoreService
import com.spondon.app.core.data.repository.CommunityRepository
import com.spondon.app.core.data.repository.DonorRepository
import com.spondon.app.core.data.repository.NotificationRepository
import com.spondon.app.core.data.repository.RequestRepository
import com.spondon.app.core.data.repository.UserRepository
import com.spondon.app.core.domain.model.*
import com.spondon.app.core.util.BloodGroupUtils
import com.spondon.app.core.util.EligibilityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ─── UI State ────────────────────────────────────────────────
data class HomeState(
    val userName: String = "",
    val user: User? = null,
    val communities: List<Community> = emptyList(),
    val selectedCommunityFilter: String? = null, // null = "All"
    val requests: List<BloodRequest> = emptyList(),
    val urgentRequests: List<BloodRequest> = emptyList(),
    val totalDonors: Int = 0,
    val fulfilledRequests: Int = 0,
    val pendingRequests: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class CreateRequestState(
    val bloodGroup: String = "",
    val urgency: Urgency = Urgency.NORMAL,
    val unitsNeeded: Int = 1,
    val patientName: String = "",
    val hospital: String = "",
    val address: String = "",
    val donationDate: Date? = null,
    val contactNumber: String = "",
    val patientCondition: String = "",
    val selectedCommunityIds: List<String> = emptyList(),
    val availableCommunities: List<Community> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)

data class RequestDetailState(
    val request: BloodRequest? = null,
    val requesterName: String = "",
    val requesterPhone: String = "",
    val isCurrentUserRequester: Boolean = false,
    val isCurrentUserAdminOrMod: Boolean = false,
    val canDonate: Boolean = false,
    val bloodGroupMatch: Boolean = true,
    val cooldownDaysRemaining: Int = 0,
    val hasResponded: Boolean = false,
    val respondentProfiles: Map<String, User> = emptyMap(),
    val confirmedDonorIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isResponding: Boolean = false,
    val isConfirming: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    /** Editable only when the requester owns it, it's active, and no donors yet. */
    val isEditable: Boolean
        get() = isCurrentUserRequester &&
                request?.status == RequestStatus.ACTIVE &&
                request.respondents.isEmpty()
}

data class FeedState(
    val selectedTab: Int = 0, // 0 = Feed, 1 = My Requests
    val feedRequests: List<BloodRequest> = emptyList(),
    val myRequests: List<BloodRequest> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class RequestViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    private val donorRepository: DonorRepository,
    private val notificationRepository: NotificationRepository,
    private val firestoreService: FirestoreService,
    private val auth: FirebaseAuth,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val currentUserId get() = auth.currentUser?.uid ?: ""

    // ─── Home State ──────────────────────────────────────────
    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    // ─── Create Request State ────────────────────────────────
    private val _createState = MutableStateFlow(CreateRequestState())
    val createState: StateFlow<CreateRequestState> = _createState.asStateFlow()

    // ─── Request Detail State ────────────────────────────────
    private val _detailState = MutableStateFlow(RequestDetailState())
    val detailState: StateFlow<RequestDetailState> = _detailState.asStateFlow()

    // ─── Feed State ──────────────────────────────────────────
    private val _feedState = MutableStateFlow(FeedState())
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    init {
        loadHome()
    }

    // ═══════════════════════════════════════════════════════════
    // Home Dashboard
    // ═══════════════════════════════════════════════════════════

    fun loadHome() {
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true, error = null) }

            // Load user
            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data

            // Load communities (includes admin/moderator communities)
            val commResult = communityRepository.getMyCommunities(currentUserId)
            val communities = (commResult as? Resource.Success)?.data ?: emptyList()
            // Use community IDs from the actual query result, not user.communityIds
            val communityIds = communities.map { it.id }

            // Load requests for all communities the user belongs to
            val reqResult = if (communityIds.isNotEmpty()) {
                requestRepository.getRequestsForCommunities(communityIds)
            } else {
                Resource.Success(emptyList())
            }
            val allRequests = (reqResult as? Resource.Success)?.data ?: emptyList()
            val activeRequests = allRequests.filter { it.status == RequestStatus.ACTIVE }
            val urgent = activeRequests
                .sortedWith(
                    compareByDescending<BloodRequest> {
                        when (it.urgency) {
                            Urgency.CRITICAL -> 2
                            Urgency.MODERATE -> 1
                            Urgency.NORMAL -> 0
                        }
                    }.thenByDescending { it.createdAt }
                )

            _homeState.update {
                it.copy(
                    userName = user?.name?.split(" ")?.firstOrNull() ?: "User",
                    user = user,
                    communities = communities,
                    requests = activeRequests,
                    urgentRequests = urgent,
                    totalDonors = communities.flatMap { c ->
                        c.memberIds + c.adminIds + c.moderatorIds
                    }.toSet().size,
                    fulfilledRequests = allRequests.count { r -> r.status == RequestStatus.FULFILLED },
                    pendingRequests = activeRequests.size,
                    isLoading = false,
                )
            }
        }
    }

    fun filterByCommunity(communityId: String?) {
        _homeState.update { state ->
            val filtered = if (communityId == null) {
                state.requests
            } else {
                state.requests.filter { it.communityIds.contains(communityId) }
            }
            state.copy(
                selectedCommunityFilter = communityId,
                urgentRequests = filtered.sortedWith(
                    compareByDescending<BloodRequest> {
                        when (it.urgency) {
                            Urgency.CRITICAL -> 2
                            Urgency.MODERATE -> 1
                            Urgency.NORMAL -> 0
                        }
                    }.thenByDescending { it.createdAt }
                ),
            )
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Create Request
    // ═══════════════════════════════════════════════════════════

    fun loadCreateForm() {
        viewModelScope.launch {
            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data

            val commResult = communityRepository.getMyCommunities(currentUserId)
            val communities = (commResult as? Resource.Success)?.data
                ?.filter { !it.isSpondon } ?: emptyList()

            _createState.update {
                it.copy(
                    contactNumber = user?.phone ?: "",
                    availableCommunities = communities,
                    selectedCommunityIds = communities.map { c -> c.id },
                )
            }
        }
    }

    fun updateBloodGroup(bg: String) = _createState.update { it.copy(bloodGroup = bg) }
    fun updateUrgency(u: Urgency) = _createState.update { it.copy(urgency = u) }
    fun updateUnits(n: Int) = _createState.update { it.copy(unitsNeeded = n.coerceIn(1, 20)) }
    fun updatePatientName(n: String) = _createState.update { it.copy(patientName = n) }
    fun updateHospital(h: String) = _createState.update { it.copy(hospital = h) }
    fun updateAddress(a: String) = _createState.update { it.copy(address = a) }
    fun updateDonationDate(d: Date?) = _createState.update { it.copy(donationDate = d) }
    fun updateContactNumber(n: String) = _createState.update { it.copy(contactNumber = n) }
    fun updatePatientCondition(c: String) = _createState.update { it.copy(patientCondition = c) }

    fun toggleCommunity(id: String) {
        _createState.update { state ->
            val current = state.selectedCommunityIds.toMutableList()
            if (current.contains(id)) current.remove(id) else current.add(id)
            state.copy(selectedCommunityIds = current)
        }
    }

    fun submitRequest() {
        val state = _createState.value
        if (state.bloodGroup.isBlank()) {
            _createState.update { it.copy(error = "Please select a blood group") }
            return
        }
        if (state.patientName.isBlank()) {
            _createState.update { it.copy(error = "Patient name is required") }
            return
        }
        if (state.hospital.isBlank()) {
            _createState.update { it.copy(error = "Hospital name is required") }
            return
        }
        if (state.address.isBlank()) {
            _createState.update { it.copy(error = "Address is required") }
            return
        }
        if (state.selectedCommunityIds.isEmpty()) {
            _createState.update { it.copy(error = "Please select at least one community") }
            return
        }

        viewModelScope.launch {
            _createState.update { it.copy(isSubmitting = true, error = null) }

            // Calculate expiry: donation date + 24 hours, or 7 days from now
            val expiry = state.donationDate?.let {
                val cal = Calendar.getInstance()
                cal.time = it
                cal.add(Calendar.HOUR, 24)
                cal.time
            } ?: run {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 7)
                cal.time
            }

            // Fetch requester name and community name
            val userResult = userRepository.getUser(currentUserId)
            val requesterName = (userResult as? Resource.Success)?.data?.name ?: "Unknown"

            val firstCommunityId = state.selectedCommunityIds.firstOrNull()
            val communityName = state.availableCommunities.find { it.id == firstCommunityId }?.name ?: ""

            val request = BloodRequest(
                communityIds = state.selectedCommunityIds,
                requesterId = currentUserId,
                bloodGroup = state.bloodGroup,
                urgency = state.urgency,
                unitsNeeded = state.unitsNeeded,
                patientName = state.patientName,
                requesterName = requesterName,
                communityName = communityName,
                address = state.address,
                hospital = state.hospital,
                donationDateTime = state.donationDate,
                contactNumber = state.contactNumber,
                patientCondition = state.patientCondition,
                status = RequestStatus.ACTIVE,
                expiresAt = expiry,
            )

            when (val result = requestRepository.createRequest(request)) {
                is Resource.Success -> {
                    // !! Send notifications BEFORE setting isSuccess.
                    // Setting isSuccess=true triggers popBackStack() which destroys
                    // this ViewModel and cancels viewModelScope — any coroutine
                    // launched AFTER that point is silently killed.
                    sendBloodGroupNotifications(
                        bloodGroup = state.bloodGroup,
                        communityIds = state.selectedCommunityIds,
                        hospital = state.hospital,
                        requestId = result.data,
                    )
                    _createState.update { it.copy(isSubmitting = false, isSuccess = true) }
                }

                is Resource.Error -> {
                    _createState.update { it.copy(isSubmitting = false, error = result.message) }
                }

                is Resource.Loading -> {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Request Detail
    // ═══════════════════════════════════════════════════════════

    fun loadRequestDetail(requestId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }

            when (val result = requestRepository.getRequest(requestId)) {
                is Resource.Success -> {
                    val request = result.data

                    // Load requester info
                    val requesterResult = userRepository.getUser(request.requesterId)
                    val requester = (requesterResult as? Resource.Success)?.data

                    // Check current user eligibility
                    val currentUserResult = userRepository.getUser(currentUserId)
                    val currentUser = (currentUserResult as? Resource.Success)?.data

                    val (canDonate, cooldownDays) = EligibilityUtils.checkAvailability(currentUser)

                    // Check if user's blood group matches the request's blood group
                    val userBloodGroup = currentUser?.bloodGroup ?: ""
                    val requestBloodGroup = request.bloodGroup
                    val bloodGroupMatches = BloodGroupUtils.canDonate(userBloodGroup, requestBloodGroup)

                    // Check if user is admin/mod for any of the request's communities
                    var isAdminOrMod = false
                    for (cId in request.communityIds) {
                        val commResult = communityRepository.getCommunity(cId)
                        if (commResult is Resource.Success) {
                            val comm = commResult.data
                            if (comm.adminIds.contains(currentUserId) || comm.moderatorIds.contains(currentUserId)) {
                                isAdminOrMod = true
                                break
                            }
                        }
                    }

                    // Load respondent profiles
                    val respondentProfiles = mutableMapOf<String, User>()
                    request.respondents.forEach { respondentId ->
                        val profileResult = userRepository.getUser(respondentId)
                        if (profileResult is Resource.Success) {
                            respondentProfiles[respondentId] = profileResult.data
                        }
                    }

                    _detailState.update {
                        it.copy(
                            request = request,
                            requesterName = requester?.name ?: "Unknown",
                            requesterPhone = requester?.phone ?: "",
                            isCurrentUserRequester = request.requesterId == currentUserId,
                            isCurrentUserAdminOrMod = isAdminOrMod,
                            canDonate = canDonate && bloodGroupMatches,
                            bloodGroupMatch = bloodGroupMatches,
                            cooldownDaysRemaining = cooldownDays,
                            hasResponded = request.respondents.contains(currentUserId),
                            respondentProfiles = respondentProfiles,
                            confirmedDonorIds = request.confirmedDonors.toSet(),
                            isLoading = false,
                        )
                    }
                }

                is Resource.Error -> {
                    _detailState.update { it.copy(isLoading = false, error = result.message) }
                }

                is Resource.Loading -> {}
            }
        }
    }

    fun respondToRequest() {
        val request = _detailState.value.request ?: return
        val requestId = request.id
        viewModelScope.launch {
            _detailState.update { it.copy(isResponding = true) }
            when (val result = requestRepository.respondToRequest(requestId, currentUserId)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            isResponding = false,
                            hasResponded = true,
                            request = it.request?.copy(
                                respondents = it.request.respondents + currentUserId,
                            ),
                        )
                    }

                    // ── Notify the requester that a donor accepted ──
                    if (request.requesterId != currentUserId) {
                        val donorResult = userRepository.getUser(currentUserId)
                        val donor = (donorResult as? Resource.Success)?.data
                        val donorName = donor?.name ?: "A donor"
                        val donorPhone = donor?.phone ?: ""
                        val contactInfo = if (donorPhone.isNotEmpty()) " (📞 $donorPhone)" else ""

                        notificationRepository.sendNotificationToUsers(
                            userIds = listOf(request.requesterId),
                            type = NotificationType.DONATION,
                            title = "🩸 Someone Accepted Your Request!",
                            body = "$donorName has responded to your ${request.bloodGroup} blood request at ${request.hospital}$contactInfo. Tap to view.",
                            deepLink = "request_detail/$requestId",
                        )
                    }
                }

                is Resource.Error -> {
                    _detailState.update { it.copy(isResponding = false, error = result.message) }
                }

                is Resource.Loading -> {}
            }
        }
    }

    fun updateStatus(status: RequestStatus) {
        val requestId = _detailState.value.request?.id ?: return
        viewModelScope.launch {
            when (requestRepository.updateRequestStatus(requestId, status)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(request = it.request?.copy(status = status))
                    }
                }

                else -> {}
            }
        }
    }

    /** Toggle edit mode on the request detail screen. */
    fun toggleEditing() {
        _detailState.update { it.copy(isEditing = !it.isEditing) }
    }

    /** Update editable fields on an existing request (only if no respondents yet). */
    fun updateRequest(edited: BloodRequest) {
        viewModelScope.launch {
            _detailState.update { it.copy(isSaving = true) }
            when (requestRepository.updateRequest(edited)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            request = edited,
                            isEditing = false,
                            isSaving = false,
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update { it.copy(isSaving = false, error = "Failed to update request") }
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Confirm that selected respondents have successfully donated.
     * Updates each donor's totalDonations + lastDonationDate, adds them to confirmedDonors,
     * and auto-fulfills the request when confirmedDonors.size >= unitsNeeded.
     * Can be called by the requester, community moderator, or admin.
     */
    fun confirmMultipleDonations(donorUserIds: List<String>) {
        val request = _detailState.value.request ?: return
        if (donorUserIds.isEmpty()) return

        viewModelScope.launch {
            _detailState.update { it.copy(isConfirming = true) }

            // Filter out already-confirmed donors
            val alreadyConfirmed = request.confirmedDonors.toSet()
            val newDonors = donorUserIds.filter { it !in alreadyConfirmed }
            if (newDonors.isEmpty()) {
                _detailState.update { it.copy(isConfirming = false) }
                return@launch
            }

            // 1. Update each donor's profile and record donation
            for (donorUserId in newDonors) {
                try {
                    // Fetch donor and update profile via injected service
                    val donorResult = userRepository.getUser(donorUserId)
                    if (donorResult is Resource.Success) {
                        val donor = donorResult.data
                        firestoreService.updateMemberDonationStatus(
                            userId = donorUserId,
                            lastDonationDate = com.google.firebase.Timestamp(Date()),
                            totalDonations = donor.totalDonations + 1,
                        )
                    }
                } catch (_: Exception) {
                    // Continue with other donors even if one fails
                }

                // Record donation
                donorRepository.recordDonation(
                    Donation(
                        requestId = request.id,
                        donorId = donorUserId,
                        hospital = request.hospital,
                        bloodGroup = request.bloodGroup,
                        date = Date(),
                        status = DonationStatus.CONFIRMED,
                        confirmedBy = currentUserId,
                    )
                )

                // Notify donor (non-critical)
                try {
                    notificationRepository.sendNotificationToUsers(
                        userIds = listOf(donorUserId),
                        type = NotificationType.DONATION,
                        title = "\uD83C\uDF89 Donation Confirmed!",
                        body = "Your blood donation for ${request.bloodGroup} at ${request.hospital} has been confirmed. Thank you for saving a life!",
                        deepLink = "request_detail/${request.id}",
                    )
                } catch (_: Exception) { }
            }

            // 2. Add to confirmedDonors on the request document
            requestRepository.confirmDonors(request.id, newDonors)

            // 3. Calculate new total confirmed count
            val totalConfirmed = alreadyConfirmed.size + newDonors.size

            // 4. Auto-fulfill if confirmed count >= units needed
            if (totalConfirmed >= request.unitsNeeded) {
                requestRepository.updateRequestStatus(request.id, RequestStatus.FULFILLED)
            }

            // 5. Reload detail to get fresh state
            _detailState.update { it.copy(isConfirming = false) }
            loadRequestDetail(request.id)
        }
    }

    /**
     * Updates the status of a request by ID (used from card menu).
     * After updating, refreshes the home feed.
     */
    fun updateRequestStatusById(requestId: String, status: RequestStatus) {
        viewModelScope.launch {
            when (requestRepository.updateRequestStatus(requestId, status)) {
                is Resource.Success -> {
                    loadHome()
                    loadFeed()
                }
                else -> {}
            }
        }
    }

    /**
     * Deletes a request permanently (used when user cancels from card menu).
     * After deleting, refreshes the home feed.
     */
    fun deleteRequest(requestId: String) {
        viewModelScope.launch {
            when (requestRepository.deleteRequest(requestId)) {
                is Resource.Success -> {
                    loadHome()
                    loadFeed()
                }
                else -> {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Feed
    // ═══════════════════════════════════════════════════════════

    fun loadFeed() {
        viewModelScope.launch {
            _feedState.update { it.copy(isLoading = true, error = null) }

            // Get community IDs from the actual query (includes admin/moderator)
            val commResult = communityRepository.getMyCommunities(currentUserId)
            val communities = (commResult as? Resource.Success)?.data ?: emptyList()
            val communityIds = communities.map { it.id }

            val feedResult = if (communityIds.isNotEmpty()) {
                requestRepository.getRequestsForCommunities(communityIds)
            } else {
                Resource.Success(emptyList())
            }
            val feedRequests = (feedResult as? Resource.Success)?.data ?: emptyList()
            // Filter out fulfilled/cancelled requests from the feed
            val activeFeedRequests = feedRequests.filter {
                it.status == RequestStatus.ACTIVE
            }

            val myResult = requestRepository.getMyRequests(currentUserId)
            val myRequests = (myResult as? Resource.Success)?.data ?: emptyList()

            _feedState.update {
                it.copy(
                    feedRequests = activeFeedRequests.sortedWith(
                        compareByDescending<BloodRequest> { r ->
                            when (r.urgency) {
                                Urgency.CRITICAL -> 2
                                Urgency.MODERATE -> 1
                                Urgency.NORMAL -> 0
                            }
                        }.thenByDescending { r -> r.createdAt },
                    ),
                    myRequests = myRequests,
                    isLoading = false,
                )
            }
        }
    }

    fun setFeedTab(tab: Int) = _feedState.update { it.copy(selectedTab = tab) }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    /**
     * Sends notifications to every community member whose stored blood group
     * matches the requested blood group.
     *
     * IMPORTANT: this is a suspend fun — it must be called directly inside the
     * submitRequest() coroutine, BEFORE isSuccess is set to true.  Launching
     * it in a separate viewModelScope.launch{} after isSuccess=true means the
     * ViewModel is destroyed (by navigation) before the coroutine completes.
     *
     * Uses a single batch Firestore read (getUsers) instead of one read per
     * member, and applies BloodGroupUtils.normalize() so that Unicode minus
     * variants stored by older app versions are still matched correctly.
     */
    private suspend fun sendBloodGroupNotifications(
        bloodGroup: String,
        communityIds: List<String>,
        hospital: String,
        requestId: String,
    ) {
        try {
            val uid = auth.currentUser?.uid ?: return
            val normalizedTarget = BloodGroupUtils.normalize(bloodGroup)

            // ── Step 1: collect every unique member ID across all selected communities ──
            val allMemberIds = mutableSetOf<String>()
            communityIds.forEach { communityId ->
                when (val res = communityRepository.getCommunity(communityId)) {
                    is Resource.Success -> {
                        allMemberIds.addAll(res.data.memberIds)
                        allMemberIds.addAll(res.data.adminIds)
                        allMemberIds.addAll(res.data.moderatorIds)
                    }

                    else -> {}
                }
            }
            allMemberIds.remove(uid) // requester never notifies themselves
            if (allMemberIds.isEmpty()) return

            // ── Step 2: batch-fetch all profiles in one Firestore call ──────────────
            val usersResult = userRepository.getUsers(allMemberIds.toList())
            val members = (usersResult as? Resource.Success)?.data ?: return

            // ── Step 3: keep only users whose blood group matches (normalized) ──────
            // Exclude users in cooldown period (recent donation), but DO notify paused users
            val matchingIds = members
                .filter { user ->
                    if (user.uid.isBlank() || BloodGroupUtils.normalize(user.bloodGroup) != normalizedTarget) {
                        return@filter false
                    }
                    
                    // Check cooldown explicitly (ignoring user.isDonor flag for notifications)
                    val lastDonation = user.lastDonationDate
                    if (lastDonation != null) {
                        val daysSince = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
                            java.util.Date().time - lastDonation.time
                        ).toInt()
                        val requiredDays = if (user.availabilityOverride) com.spondon.app.core.common.Constants.MIN_OVERRIDE_DAYS else user.donationInterval
                        if (daysSince < requiredDays) {
                            return@filter false // User is in cooldown period
                        }
                    }
                    true
                }
                .map { it.uid }

            if (matchingIds.isEmpty()) return

            // ── Step 4: write notification docs for each matched user ───────────────
            notificationRepository.sendNotificationToUsers(
                userIds = matchingIds,
                type = NotificationType.REQUEST,
                title = "\uD83E\uDE78 $bloodGroup Blood Needed!",
                body = "A $bloodGroup blood request has been posted at $hospital. Your blood group matches — tap to respond!",
                deepLink = "request_detail/$requestId",
                extraData = mapOf("requestId" to requestId),
            )
        } catch (_: Exception) {
            // Silently fail — request creation must never be blocked by a
            // notification dispatch error.
        }
    }

    /** Utility: relative time display */
    companion object {
        fun getRelativeTime(date: Date?): String {
            if (date == null) return ""
            val diff = Date().time - date.time
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> "${days / 7}w ago"
            }
        }
    }
}
