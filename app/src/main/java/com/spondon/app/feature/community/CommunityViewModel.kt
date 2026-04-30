package com.spondon.app.feature.community

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import com.spondon.app.core.common.daysSince
import com.spondon.app.core.data.repository.CommunityRepositoryImpl
import com.spondon.app.core.data.repository.NotificationRepository
import com.spondon.app.core.domain.model.*
import com.spondon.app.core.domain.usecase.community.CreateCommunityUseCase
import com.spondon.app.core.domain.usecase.community.GetCommunitiesUseCase
import com.spondon.app.core.domain.usecase.community.ManageMembersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// ─── UI State classes ─────────────────────────────────────────────

data class CommunityListState(
    val myCommunities: List<Community> = emptyList(),
    val discoverCommunities: List<Community> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0, // 0 = My Communities, 1 = Discover
    val searchQuery: String = "",
)

data class CommunityDetailState(
    val community: Community? = null,
    val members: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val membershipStatus: MembershipStatus = MembershipStatus.NONE,
    val currentUserRole: CommunityRole? = null,
    val selectedTab: Int = 0, // 0 = Feed, 1 = Members, 2 = About
)

data class CreateCommunityState(
    val name: String = "",
    val description: String = "",
    val coverUri: Uri? = null,
    val coverUrl: String = "",
    val type: CommunityType = CommunityType.PUBLIC,
    val district: String = "",
    val upazila: String = "",
    val selectedBloodGroups: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreated: Boolean = false,
)

data class JoinRequestState(
    val community: Community? = null,
    val message: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false,
    val isPending: Boolean = false,
)

data class AdminDashboardState(
    val community: Community? = null,
    val pendingRequests: List<JoinRequest> = emptyList(),
    val members: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccess: String? = null,
    val pendingCount: Int = 0,
    val activeMembers: Int = 0,
    val monthlyDonations: Int = 0,
    val broadcastMessage: String = "",
    val isBroadcasting: Boolean = false,
    val broadcastSuccess: Boolean = false,
)

// ─── Events ────────────────────────────────────────────────────

sealed class CommunityEvent {
    data class ShowSnackbar(val message: String) : CommunityEvent()
    data class NavigateToCommunity(val communityId: String) : CommunityEvent()
    data object NavigateBack : CommunityEvent()
}

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val getCommunitiesUseCase: GetCommunitiesUseCase,
    private val createCommunityUseCase: CreateCommunityUseCase,
    private val manageMembersUseCase: ManageMembersUseCase,
    private val communityRepository: CommunityRepositoryImpl,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    // ─── Community List ──────────────────────────────────────

    private val _listState = MutableStateFlow(CommunityListState())
    val listState: StateFlow<CommunityListState> = _listState.asStateFlow()

    // ─── Community Detail ────────────────────────────────────

    private val _detailState = MutableStateFlow(CommunityDetailState())
    val detailState: StateFlow<CommunityDetailState> = _detailState.asStateFlow()

    // ─── Create Community ────────────────────────────────────

    private val _createState = MutableStateFlow(CreateCommunityState())
    val createState: StateFlow<CreateCommunityState> = _createState.asStateFlow()

    // ─── Join Request ────────────────────────────────────────

    private val _joinState = MutableStateFlow(JoinRequestState())
    val joinState: StateFlow<JoinRequestState> = _joinState.asStateFlow()

    // ─── Admin Dashboard ─────────────────────────────────────

    private val _adminState = MutableStateFlow(AdminDashboardState())
    val adminState: StateFlow<AdminDashboardState> = _adminState.asStateFlow()

    // ─── One-shot events ─────────────────────────────────────

    private val _events = MutableSharedFlow<CommunityEvent>()
    val events: SharedFlow<CommunityEvent> = _events.asSharedFlow()

    // ═══════════════════════════════════════════════════════════
    // Community List Actions
    // ═══════════════════════════════════════════════════════════

    fun loadCommunities() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, error = null) }

            // Load My Communities
            val myResult = getCommunitiesUseCase.getMyCommunities(currentUserId)
            val myCommunities = when (myResult) {
                is Resource.Success -> myResult.data
                is Resource.Error -> {
                    _listState.update { it.copy(error = myResult.message) }
                    emptyList()
                }
                is Resource.Loading -> emptyList()
            }

            // Load All Communities (for Discover)
            val allResult = getCommunitiesUseCase.getAllCommunities()
            val allCommunities = when (allResult) {
                is Resource.Success -> allResult.data
                is Resource.Error -> {
                    _listState.update { it.copy(error = allResult.message) }
                    emptyList()
                }
                is Resource.Loading -> emptyList()
            }

            // Discover = all communities (show all available communities)
            val discover = allCommunities

            _listState.update {
                it.copy(
                    myCommunities = myCommunities,
                    discoverCommunities = discover,
                    isLoading = false,
                )
            }
        }
    }

    fun setListTab(tab: Int) {
        _listState.update { it.copy(selectedTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _listState.update { it.copy(searchQuery = query) }
    }

    fun getFilteredDiscoverCommunities(): List<Community> {
        val state = _listState.value
        val query = state.searchQuery.lowercase()
        return if (query.isBlank()) {
            state.discoverCommunities
        } else {
            state.discoverCommunities.filter {
                it.name.lowercase().contains(query) ||
                it.district.lowercase().contains(query) ||
                it.upazila.lowercase().contains(query)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Community Detail Actions
    // ═══════════════════════════════════════════════════════════

    fun loadCommunityDetail(communityId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }

            when (val result = getCommunitiesUseCase.getCommunity(communityId)) {
                is Resource.Success -> {
                    val community = result.data
                    val membershipStatus = when {
                        community.memberIds.contains(currentUserId) -> MembershipStatus.JOINED
                        community.pendingIds.contains(currentUserId) -> MembershipStatus.PENDING
                        else -> MembershipStatus.NONE
                    }
                    val role = when {
                        community.adminIds.contains(currentUserId) -> CommunityRole.ADMIN
                        community.moderatorIds.contains(currentUserId) -> CommunityRole.MODERATOR
                        community.memberIds.contains(currentUserId) -> CommunityRole.MEMBER
                        else -> null
                    }

                    _detailState.update {
                        it.copy(
                            community = community,
                            membershipStatus = membershipStatus,
                            currentUserRole = role,
                            isLoading = false,
                        )
                    }

                    // Load members
                    if (community.memberIds.isNotEmpty()) {
                        loadMembers(community.memberIds)
                    }
                }
                is Resource.Error -> {
                    _detailState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadMembers(memberIds: List<String>) {
        viewModelScope.launch {
            when (val result = communityRepository.getCommunityMembers(memberIds)) {
                is Resource.Success -> {
                    _detailState.update { it.copy(members = result.data) }
                }
                is Resource.Error -> {
                    // Silently fail; members list just stays empty
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun setDetailTab(tab: Int) {
        _detailState.update { it.copy(selectedTab = tab) }
    }

    fun joinPublicCommunity(communityId: String) {
        viewModelScope.launch {
            when (val result = manageMembersUseCase.joinCommunity(communityId, currentUserId)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Joined successfully!"))
                    loadCommunityDetail(communityId)
                    loadCommunities()
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun leaveCommunity(communityId: String) {
        viewModelScope.launch {
            when (val result = communityRepository.leaveCommunity(communityId, currentUserId)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Left community"))
                    loadCommunityDetail(communityId)
                    loadCommunities()
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Create Community Actions
    // ═══════════════════════════════════════════════════════════

    fun updateCreateName(name: String) { _createState.update { it.copy(name = name) } }
    fun updateCreateDescription(desc: String) { _createState.update { it.copy(description = desc) } }
    fun updateCreateType(type: CommunityType) { _createState.update { it.copy(type = type) } }
    fun updateCreateDistrict(district: String) { _createState.update { it.copy(district = district) } }
    fun updateCreateUpazila(upazila: String) { _createState.update { it.copy(upazila = upazila) } }
    fun updateCreateCoverUri(uri: Uri?) { _createState.update { it.copy(coverUri = uri) } }

    fun toggleBloodGroup(group: String) {
        _createState.update {
            val current = it.selectedBloodGroups.toMutableSet()
            if (current.contains(group)) current.remove(group) else current.add(group)
            it.copy(selectedBloodGroups = current)
        }
    }

    fun createCommunity() {
        viewModelScope.launch {
            val state = _createState.value
            if (state.name.isBlank()) {
                _createState.update { it.copy(error = "Community name is required") }
                return@launch
            }

            _createState.update { it.copy(isLoading = true, error = null) }

            // Upload cover image if provided
            var coverUrl = ""
            if (state.coverUri != null) {
                val tempId = System.currentTimeMillis().toString()
                when (val uploadResult = communityRepository.uploadCoverImage(tempId, state.coverUri)) {
                    is Resource.Success -> coverUrl = uploadResult.data
                    is Resource.Error -> {
                        _createState.update { it.copy(error = "Cover upload failed", isLoading = false) }
                        return@launch
                    }
                    is Resource.Loading -> {}
                }
            }

            val community = Community(
                name = state.name,
                description = state.description,
                coverUrl = coverUrl,
                type = state.type,
                adminIds = listOf(currentUserId),
                moderatorIds = emptyList(),
                memberIds = listOf(currentUserId),
                pendingIds = emptyList(),
                district = state.district,
                upazila = state.upazila,
                bloodGroups = state.selectedBloodGroups.toList(),
                memberCount = 1,
                donationCount = 0,
                isVerified = false,
                createdAt = Date(),
            )

            when (val result = createCommunityUseCase(community)) {
                is Resource.Success -> {
                    _createState.update { it.copy(isLoading = false, isCreated = true) }
                    _events.emit(CommunityEvent.ShowSnackbar("Community created!"))
                    _events.emit(CommunityEvent.NavigateToCommunity(result.data))
                    loadCommunities()
                }
                is Resource.Error -> {
                    _createState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateCommunityState()
    }

    // ═══════════════════════════════════════════════════════════
    // Join Request Actions
    // ═══════════════════════════════════════════════════════════

    fun loadJoinRequestScreen(communityId: String) {
        viewModelScope.launch {
            _joinState.update { it.copy(isLoading = true) }
            when (val result = getCommunitiesUseCase.getCommunity(communityId)) {
                is Resource.Success -> {
                    val community = result.data
                    val isPending = community.pendingIds.contains(currentUserId)
                    _joinState.update {
                        it.copy(
                            community = community,
                            isLoading = false,
                            isPending = isPending,
                        )
                    }
                }
                is Resource.Error -> {
                    _joinState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateJoinMessage(message: String) {
        _joinState.update { it.copy(message = message) }
    }

    fun submitJoinRequest(communityId: String) {
        viewModelScope.launch {
            _joinState.update { it.copy(isLoading = true, error = null) }
            val result = manageMembersUseCase.requestToJoin(
                communityId, currentUserId, _joinState.value.message,
            )
            when (result) {
                is Resource.Success -> {
                    _joinState.update { it.copy(isLoading = false, isSubmitted = true, isPending = true) }
                    _events.emit(CommunityEvent.ShowSnackbar("Join request sent!"))
                }
                is Resource.Error -> {
                    _joinState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun cancelJoinRequest(communityId: String) {
        viewModelScope.launch {
            _joinState.update { it.copy(isLoading = true) }
            when (val result = communityRepository.cancelJoinRequest(communityId, currentUserId)) {
                is Resource.Success -> {
                    _joinState.update { it.copy(isLoading = false, isPending = false, isSubmitted = false) }
                    _events.emit(CommunityEvent.ShowSnackbar("Request cancelled"))
                }
                is Resource.Error -> {
                    _joinState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Admin Dashboard Actions
    // ═══════════════════════════════════════════════════════════

    fun loadAdminDashboard(communityId: String) {
        viewModelScope.launch {
            _adminState.update { it.copy(isLoading = true, error = null) }

            // Load community
            when (val result = getCommunitiesUseCase.getCommunity(communityId)) {
                is Resource.Success -> {
                    val community = result.data
                    _adminState.update {
                        it.copy(
                            community = community,
                            activeMembers = community.memberCount,
                            monthlyDonations = community.donationCount,
                        )
                    }

                    // Load pending join requests
                    loadPendingRequests(communityId)

                    // Load members
                    if (community.memberIds.isNotEmpty()) {
                        loadAdminMembers(community.memberIds)
                    }
                }
                is Resource.Error -> {
                    _adminState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadPendingRequests(communityId: String) {
        viewModelScope.launch {
            when (val result = communityRepository.getPendingJoinRequests(communityId)) {
                is Resource.Success -> {
                    _adminState.update {
                        it.copy(
                            pendingRequests = result.data,
                            pendingCount = result.data.size,
                            isLoading = false,
                        )
                    }
                }
                is Resource.Error -> {
                    _adminState.update { it.copy(isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadAdminMembers(memberIds: List<String>) {
        viewModelScope.launch {
            when (val result = communityRepository.getCommunityMembers(memberIds)) {
                is Resource.Success -> {
                    _adminState.update { it.copy(members = result.data) }
                }
                is Resource.Error -> {} // Silently handle
                is Resource.Loading -> {}
            }
        }
    }

    fun approveJoinRequest(communityId: String, requestId: String, userId: String) {
        viewModelScope.launch {
            when (communityRepository.approveJoinRequest(communityId, requestId, userId)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Member approved!"))
                    loadAdminDashboard(communityId)
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to approve"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun rejectJoinRequest(communityId: String, requestId: String, userId: String, note: String?) {
        viewModelScope.launch {
            when (communityRepository.rejectJoinRequest(communityId, requestId, userId, note)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Request rejected"))
                    loadAdminDashboard(communityId)
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to reject"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun removeMember(communityId: String, userId: String) {
        viewModelScope.launch {
            when (manageMembersUseCase.removeMember(communityId, userId)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Member removed"))
                    loadAdminDashboard(communityId)
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to remove member"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun promoteMember(communityId: String, userId: String, role: CommunityRole) {
        viewModelScope.launch {
            when (manageMembersUseCase.promoteMember(communityId, userId, role)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Member promoted to ${role.name}"))
                    loadAdminDashboard(communityId)
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to promote member"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateMemberDonationStatus(userId: String, date: Date, totalDonations: Int) {
        viewModelScope.launch {
            when (communityRepository.updateMemberDonationStatus(userId, date, totalDonations)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Donation status updated"))
                    // Reload members
                    _adminState.value.community?.let { loadAdminMembers(it.memberIds) }
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to update donation status"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun overrideMemberAvailability(userId: String) {
        viewModelScope.launch {
            when (communityRepository.overrideMemberAvailability(userId)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Availability overridden"))
                    _adminState.value.community?.let { loadAdminMembers(it.memberIds) }
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to override availability"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Admin Broadcast Notifications ────────────────────────

    fun updateBroadcastMessage(message: String) {
        _adminState.update { it.copy(broadcastMessage = message) }
    }

    fun sendBroadcastNotification(communityId: String) {
        viewModelScope.launch {
            val state = _adminState.value
            val community = state.community ?: return@launch
            val message = state.broadcastMessage.trim()
            if (message.isBlank()) {
                _events.emit(CommunityEvent.ShowSnackbar("Please enter a message"))
                return@launch
            }

            _adminState.update { it.copy(isBroadcasting = true) }

            // Collect all unique member IDs
            val allMemberIds = mutableSetOf<String>()
            allMemberIds.addAll(community.memberIds)
            allMemberIds.addAll(community.adminIds)
            allMemberIds.addAll(community.moderatorIds)
            allMemberIds.remove(currentUserId) // Don't notify self

            if (allMemberIds.isEmpty()) {
                _adminState.update { it.copy(isBroadcasting = false) }
                _events.emit(CommunityEvent.ShowSnackbar("No members to notify"))
                return@launch
            }

            when (notificationRepository.sendNotificationToUsers(
                userIds = allMemberIds.toList(),
                type = NotificationType.ADMIN,
                title = "📢 ${community.name}",
                body = message,
                deepLink = "community_detail/$communityId",
            )) {
                is Resource.Success -> {
                    _adminState.update {
                        it.copy(
                            isBroadcasting = false,
                            broadcastMessage = "",
                            broadcastSuccess = true,
                        )
                    }
                    _events.emit(CommunityEvent.ShowSnackbar("Notification sent to ${allMemberIds.size} members"))
                }
                is Resource.Error -> {
                    _adminState.update { it.copy(isBroadcasting = false) }
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to send notifications"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetBroadcastSuccess() {
        _adminState.update { it.copy(broadcastSuccess = false) }
    }

    // ─── Helpers ─────────────────────────────────────────────

    fun isCurrentUserAdmin(community: Community): Boolean {
        return community.adminIds.contains(currentUserId)
    }

    fun isCurrentUserModOrAdmin(community: Community): Boolean {
        return community.adminIds.contains(currentUserId) ||
               community.moderatorIds.contains(currentUserId)
    }

    fun getDaysUntilAvailable(user: User): Int {
        val lastDonation = user.lastDonationDate ?: return 0
        val daysSince = lastDonation.daysSince().toInt()
        val interval = user.donationInterval
        return if (user.availabilityOverride || daysSince >= interval) 0
               else interval - daysSince
    }

    fun isUserAvailable(user: User): Boolean {
        return getDaysUntilAvailable(user) == 0
    }

    fun canOverrideAvailability(user: User): Boolean {
        val lastDonation = user.lastDonationDate ?: return false
        val daysSince = lastDonation.daysSince().toInt()
        return daysSince >= Constants.MIN_OVERRIDE_DAYS && daysSince < user.donationInterval
    }
}