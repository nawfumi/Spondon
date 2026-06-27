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
import com.spondon.app.core.data.repository.PrivacyConfigRepository
import com.spondon.app.core.data.repository.RequestRepository
import com.spondon.app.core.domain.model.*
import com.spondon.app.core.util.BloodGroupUtils
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
    val currentUserBloodGroup: String = "",
)

data class CommunityDetailState(
    val community: Community? = null,
    val members: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val membershipStatus: MembershipStatus = MembershipStatus.NONE,
    val currentUserRole: CommunityRole? = null,
    val selectedTab: Int = 0, // 0 = Feed, 1 = Members, 2 = About
    val requests: List<BloodRequest> = emptyList(),
    val isRequestsLoading: Boolean = false,
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

data class EditCommunityState(
    val communityId: String = "",
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
    val isUpdated: Boolean = false,
)

data class JoinRequestState(
    val community: Community? = null,
    val message: String = "",
    val serialInput: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false,
    val isPending: Boolean = false,
)

data class AdminDashboardState(
    val community: Community? = null,
    val pendingRequests: List<JoinRequest> = emptyList(),
    val members: List<User> = emptyList(),
    val memberSerials: Map<String, String> = emptyMap(),
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
    data class SharePdf(val file: java.io.File) : CommunityEvent()
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
    private val requestRepository: RequestRepository,
    private val privacyConfigRepository: PrivacyConfigRepository,
) : ViewModel() {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    /** Expose current user ID for UI pending-status checks */
    fun fetchCurrentUserId(): String = currentUserId

    // ─── Privacy State ───────────────────────────────────────
    private val _hideSensitiveData = MutableStateFlow(false)
    val hideSensitiveData: StateFlow<Boolean> = _hideSensitiveData.asStateFlow()

    /** Set of user IDs whose data should be hidden (per-user privacy). */
    private val _protectedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val protectedUserIds: StateFlow<Set<String>> = _protectedUserIds.asStateFlow()

    /** Whether the current viewer is authorized to see protected data. */
    private val _isAuthorizedViewer = MutableStateFlow(false)
    val isAuthorizedViewer: StateFlow<Boolean> = _isAuthorizedViewer.asStateFlow()

    init {
        viewModelScope.launch {
            val protectedIds = privacyConfigRepository.loadProtectedUserIds()
            _protectedUserIds.value = protectedIds
            val authorized = privacyConfigRepository.isCurrentUserAuthorized()
            _isAuthorizedViewer.value = authorized
            // Backward-compat: global flag is true if any protected users exist and viewer is not authorized
            _hideSensitiveData.value = protectedIds.isNotEmpty() && !authorized
        }
    }

    /** Check if a specific user's sensitive data should be hidden. */
    fun shouldHideForUser(userId: String): Boolean {
        return userId in _protectedUserIds.value && !_isAuthorizedViewer.value
    }

    // ─── Community List ──────────────────────────────────────

    private val _listState = MutableStateFlow(CommunityListState())
    val listState: StateFlow<CommunityListState> = _listState.asStateFlow()

    // ─── Community Detail ────────────────────────────────────

    private val _detailState = MutableStateFlow(CommunityDetailState())
    val detailState: StateFlow<CommunityDetailState> = _detailState.asStateFlow()

    // ─── Create Community ────────────────────────────────────

    private val _createState = MutableStateFlow(CreateCommunityState())
    val createState: StateFlow<CreateCommunityState> = _createState.asStateFlow()

    // ─── Edit Community ──────────────────────────────────────

    private val _editState = MutableStateFlow(EditCommunityState())
    val editState: StateFlow<EditCommunityState> = _editState.asStateFlow()

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

            // Ensure Spondon community exists and user is enrolled
            // This must happen before fetching community lists so Spondon appears
            try {
                val spondonId = communityRepository.getSpondonCommunityId()
                if (spondonId != null && currentUserId.isNotEmpty()) {
                    communityRepository.ensureUserInSpondonCommunity(currentUserId)
                }
            } catch (_: Exception) {
                // Non-critical — continue loading communities even if Spondon setup fails
            }

            // Load current user's blood group for eligibility checks
            val userBloodGroup = getCurrentUserBloodGroup()

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
            val allCommunities = when (val allResult = getCommunitiesUseCase.getAllCommunities()) {
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
                    currentUserBloodGroup = userBloodGroup,
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

            // Ensure the user's blood group is loaded for eligibility checks
            if (_listState.value.currentUserBloodGroup.isBlank()) {
                val bg = getCurrentUserBloodGroup()
                _listState.update { it.copy(currentUserBloodGroup = bg) }
            }
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

                    // Load blood requests for this community if it's public or the user is a member
                    if (community.type == CommunityType.PUBLIC || membershipStatus == MembershipStatus.JOINED) {
                        loadCommunityRequests(communityId)
                    }
                }
                is Resource.Error -> {
                    _detailState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadCommunityRequests(communityId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isRequestsLoading = true) }
            when (val result = requestRepository.getRequestsForCommunities(listOf(communityId))) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            requests = result.data
                                .filter { r -> r.status == RequestStatus.ACTIVE }
                                .sortedWith(
                                    compareByDescending<BloodRequest> { r ->
                                        when (r.urgency) {
                                            Urgency.CRITICAL -> 2
                                            Urgency.MODERATE -> 1
                                            Urgency.NORMAL   -> 0
                                        }
                                    }.thenByDescending { r -> r.createdAt }
                                ),
                            isRequestsLoading = false,
                        )
                    }
                }
                is Resource.Error -> _detailState.update { it.copy(isRequestsLoading = false) }
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
            // Look up community from listState first, then detailState
            val community = _listState.value.discoverCommunities.find { it.id == communityId }
                ?: _detailState.value.community

            if (community != null && community.bloodGroups.isNotEmpty()) {
                val currentUserBloodGroup = _listState.value.currentUserBloodGroup.ifBlank { getCurrentUserBloodGroup() }
                if (currentUserBloodGroup.isNotBlank() &&
                    !community.bloodGroups.any { BloodGroupUtils.normalize(it) == BloodGroupUtils.normalize(currentUserBloodGroup) }
                ) {
                    _events.emit(CommunityEvent.ShowSnackbar(
                        "Your blood group ($currentUserBloodGroup) is not supported by this community. Supported: ${community.bloodGroups.joinToString(", ")}"
                    ))
                    return@launch
                }
            }

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
    // Edit Community Actions
    // ═══════════════════════════════════════════════════════════

    fun loadCommunityForEdit(communityId: String) {
        viewModelScope.launch {
            _editState.update { it.copy(isLoading = true, error = null, isUpdated = false) }
            when (val result = getCommunitiesUseCase.getCommunity(communityId)) {
                is Resource.Success -> {
                    val community = result.data
                    _editState.update {
                        it.copy(
                            communityId = community.id,
                            name = community.name,
                            description = community.description,
                            coverUrl = community.coverUrl,
                            type = community.type,
                            district = community.district,
                            upazila = community.upazila,
                            selectedBloodGroups = community.bloodGroups.toSet(),
                            isLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _editState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateEditName(name: String) { _editState.update { it.copy(name = name) } }
    fun updateEditDescription(desc: String) { _editState.update { it.copy(description = desc) } }
    fun updateEditType(type: CommunityType) { _editState.update { it.copy(type = type) } }
    fun updateEditDistrict(district: String) { _editState.update { it.copy(district = district) } }
    fun updateEditUpazila(upazila: String) { _editState.update { it.copy(upazila = upazila) } }
    fun updateEditCoverUri(uri: Uri?) { _editState.update { it.copy(coverUri = uri) } }

    fun toggleEditBloodGroup(group: String) {
        _editState.update {
            val current = it.selectedBloodGroups.toMutableSet()
            if (current.contains(group)) current.remove(group) else current.add(group)
            it.copy(selectedBloodGroups = current)
        }
    }

    fun updateCommunity() {
        viewModelScope.launch {
            val state = _editState.value
            if (state.name.isBlank()) {
                _editState.update { it.copy(error = "Community name is required") }
                return@launch
            }

            _editState.update { it.copy(isLoading = true, error = null) }

            // Fetch the existing community to retain fields we are not updating from the UI directly
            val existingResult = getCommunitiesUseCase.getCommunity(state.communityId)
            if (existingResult !is Resource.Success) {
                _editState.update { it.copy(error = "Failed to load existing community", isLoading = false) }
                return@launch
            }
            val existingCommunity = existingResult.data

            // Upload new cover image if provided
            var finalCoverUrl = state.coverUrl
            if (state.coverUri != null) {
                val tempId = System.currentTimeMillis().toString()
                when (val uploadResult = communityRepository.uploadCoverImage(tempId, state.coverUri)) {
                    is Resource.Success -> finalCoverUrl = uploadResult.data
                    is Resource.Error -> {
                        _editState.update { it.copy(error = "Cover upload failed", isLoading = false) }
                        return@launch
                    }
                    is Resource.Loading -> {}
                }
            }

            val updatedCommunity = existingCommunity.copy(
                name = state.name,
                description = state.description,
                coverUrl = finalCoverUrl,
                type = state.type,
                district = state.district,
                upazila = state.upazila,
                bloodGroups = state.selectedBloodGroups.toList(),
            )

            when (val result = communityRepository.updateCommunity(updatedCommunity)) {
                is Resource.Success -> {
                    _editState.update { it.copy(isLoading = false, isUpdated = true) }
                    _events.emit(CommunityEvent.ShowSnackbar("Community updated!"))
                    loadCommunities()
                }
                is Resource.Error -> {
                    _editState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetEditState() {
        _editState.value = EditCommunityState()
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

    fun updateJoinSerialInput(serial: String) {
        _joinState.update { it.copy(serialInput = serial) }
    }

    fun submitJoinRequest(communityId: String) {
        viewModelScope.launch {
            _joinState.update { it.copy(isLoading = true, error = null) }

            // Check blood group eligibility before submitting join request
            val community = _joinState.value.community
            if (community != null && community.bloodGroups.isNotEmpty()) {
                val currentUserBloodGroup = getCurrentUserBloodGroup()
                if (currentUserBloodGroup.isNotBlank() &&
                    !community.bloodGroups.any { BloodGroupUtils.normalize(it) == BloodGroupUtils.normalize(currentUserBloodGroup) }
                ) {
                    _joinState.update {
                        it.copy(
                            isLoading = false,
                            error = "Your blood group ($currentUserBloodGroup) is not supported by this community. Supported: ${community.bloodGroups.joinToString(", ")}"
                        )
                    }
                    return@launch
                }
            }

            val result = manageMembersUseCase.requestToJoin(
                communityId, currentUserId, _joinState.value.message,
                serialId = _joinState.value.serialInput.trim().ifBlank { null },
            )
            when (result) {
                is Resource.Success -> {
                    _joinState.update { it.copy(isLoading = false, isSubmitted = true, isPending = true) }
                    _events.emit(CommunityEvent.ShowSnackbar("Join request sent!"))

                    // Notify community admins about the new join request
                    if (community != null) {
                        try {
                            notificationRepository.sendNotificationToUsers(
                                userIds = community.adminIds,
                                type = NotificationType.COMMUNITY_JOIN_REQUEST,
                                title = "New Join Request",
                                body = "Someone wants to join ${community.name}",
                                deepLink = "community_detail/$communityId",
                                extraData = mapOf(
                                    "communityId" to communityId,
                                    "requesterId" to currentUserId,
                                ),
                            )
                        } catch (_: Exception) { /* non-critical */ }
                    }
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

                    // Load member serials if serial is enabled
                    if (community.isSerialEnabled) {
                        loadMemberSerials(communityId)
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
                    _adminState.update { it.copy(error = "Failed to load pending requests: ${result.message}", isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadAdminMembers(memberIds: List<String>) {
        viewModelScope.launch {
            when (val result = communityRepository.getCommunityMembers(memberIds)) {
                is Resource.Success -> {
                    val members = result.data
                    val activeCount = members.count { isUserAvailable(it) }
                    val totalDonations = members.sumOf { it.totalDonations }
                    _adminState.update {
                        it.copy(
                            members = members,
                            activeMembers = activeCount,
                            monthlyDonations = totalDonations,
                        )
                    }
                }
                is Resource.Error -> {} // Silently handle
                is Resource.Loading -> {}
            }
        }
    }

    fun approveJoinRequest(communityId: String, requestId: String, userId: String, serialId: String? = null) {
        viewModelScope.launch {
            when (val result = communityRepository.approveJoinRequest(communityId, requestId, userId, serialId)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Member approved!"))
                    loadAdminDashboard(communityId)

                    // Notify the approved user
                    val communityName = _adminState.value.community?.name ?: "a community"
                    try {
                        notificationRepository.sendNotificationToUsers(
                            userIds = listOf(userId),
                            type = NotificationType.JOIN_REQUEST_ACCEPTED,
                            title = "Request Approved!",
                            body = "You've been added to $communityName.",
                            deepLink = "community_detail/$communityId",
                        )
                    } catch (_: Exception) { /* non-critical */ }
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to approve: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun rejectJoinRequest(communityId: String, requestId: String, userId: String, note: String?) {
        viewModelScope.launch {
            when (val result = communityRepository.rejectJoinRequest(communityId, requestId, userId, note)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Request rejected"))
                    loadAdminDashboard(communityId)

                    // Notify the rejected user
                    val communityName = _adminState.value.community?.name ?: "a community"
                    try {
                        notificationRepository.sendNotificationToUsers(
                            userIds = listOf(userId),
                            type = NotificationType.JOIN_REQUEST_REJECTED,
                            title = "Request Rejected",
                            body = "Your join request for $communityName was not approved.",
                            deepLink = "community_detail/$communityId",
                        )
                    } catch (_: Exception) { /* non-critical */ }
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to reject: ${result.message}"))
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

    // ─── Member Serial Assignment ────────────────────────────

    private fun loadMemberSerials(communityId: String) {
        viewModelScope.launch {
            val serials = communityRepository.getMemberSerials(communityId)
            _adminState.update { it.copy(memberSerials = serials) }
        }
    }

    fun assignSerialId(communityId: String, userId: String, serialId: String) {
        viewModelScope.launch {
            when (val result = communityRepository.assignSerialId(communityId, userId, serialId)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Serial ID assigned: $serialId"))
                    loadMemberSerials(communityId)
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to assign serial: ${result.message}"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── PDF Export ──────────────────────────────────────────

    fun exportMembersPdf(
        context: android.content.Context,
        communityId: String,
        sortOption: PdfSortOption = PdfSortOption.ALPHABETICAL,
    ) {
        viewModelScope.launch {
            val state = _adminState.value
            val community = state.community ?: return@launch
            val members = state.members
            if (members.isEmpty()) {
                _events.emit(CommunityEvent.ShowSnackbar("No members to export"))
                return@launch
            }

            try {
                val serials = if (community.isSerialEnabled) state.memberSerials else emptyMap()
                val generator = CommunityPdfGenerator()
                val file = generator.generate(
                    context = context,
                    communityName = community.name,
                    members = members,
                    community = community,
                    serials = serials,
                    sortOption = sortOption,
                )
                _events.emit(CommunityEvent.SharePdf(file))
            } catch (e: Exception) {
                _events.emit(CommunityEvent.ShowSnackbar("PDF export failed: ${e.message}"))
            }
        }
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

    /** Fetches the current user's blood group from Firestore. */
    private suspend fun getCurrentUserBloodGroup(): String {
        return try {
            val result = communityRepository.getCommunityMembers(listOf(currentUserId))
            (result as? Resource.Success)?.data?.firstOrNull()?.bloodGroup ?: ""
        } catch (_: Exception) {
            ""
        }
    }



    /**
     * Check if the current user's blood group is eligible to join a community.
     * Returns true if the community has no blood group restriction, or the user's group matches.
     */
    fun isBloodGroupEligible(community: Community): Boolean {
        if (community.bloodGroups.isEmpty()) return true
        val userBg = _listState.value.currentUserBloodGroup
        if (userBg.isBlank()) return true // can't determine, allow
        return community.bloodGroups.any { BloodGroupUtils.normalize(it) == BloodGroupUtils.normalize(userBg) }
    }

    // ═══════════════════════════════════════════════════════════
    // Spondon Global Community
    // ═══════════════════════════════════════════════════════════

    private val _spondonState = MutableStateFlow(SpondonCommunityState())
    val spondonState: StateFlow<SpondonCommunityState> = _spondonState.asStateFlow()

    private val _createPostState = MutableStateFlow(CreatePostState())
    val createPostState: StateFlow<CreatePostState> = _createPostState.asStateFlow()

    /**
     * Loads the Spondon community data and its posts.
     * Also ensures the current user is a member.
     */
    fun loadSpondonCommunity() {
        viewModelScope.launch {
            _spondonState.update { it.copy(isLoading = true, error = null) }

            // Get the Spondon community ID
            val spondonId = communityRepository.getSpondonCommunityId()
            if (spondonId == null) {
                _spondonState.update {
                    it.copy(
                        isLoading = false,
                        error = "Spondon community not set up yet. Contact admin.",
                    )
                }
                return@launch
            }

            // Ensure user is a member
            communityRepository.ensureUserInSpondonCommunity(currentUserId)

            // Fetch the current user's platform-wide role (SUPER_ADMIN / USER) and profile
            var platformRole = UserRole.USER
            var currentUser: User? = null
            try {
                val userResult = communityRepository.getCommunityMembers(listOf(currentUserId))
                if (userResult is Resource.Success && userResult.data.isNotEmpty()) {
                    currentUser = userResult.data.first()
                    platformRole = currentUser.role
                }
            } catch (_: Exception) { /* use defaults */ }

            // Load community details
            when (val result = getCommunitiesUseCase.getCommunity(spondonId)) {
                is Resource.Success -> {
                    val community = result.data
                    val role = when {
                        community.adminIds.contains(currentUserId) -> CommunityRole.ADMIN
                        community.moderatorIds.contains(currentUserId) -> CommunityRole.MODERATOR
                        else -> CommunityRole.MEMBER
                    }
                    _spondonState.update {
                        it.copy(
                            community = community,
                            currentUserRole = role,
                            currentUserPlatformRole = platformRole,
                            currentUser = currentUser,
                            isLoading = false,
                        )
                    }
                    // Load posts
                    loadSpondonPosts(spondonId)
                    // Load members
                    if (community.memberIds.isNotEmpty()) {
                        loadSpondonMembers(community.memberIds)
                    }
                }
                is Resource.Error -> {
                    _spondonState.update { it.copy(error = result.message, isLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadSpondonPosts(communityId: String) {
        viewModelScope.launch {
            _spondonState.update { it.copy(isPostsLoading = true) }
            when (val result = communityRepository.getCommunityPosts(communityId)) {
                is Resource.Success -> {
                    _spondonState.update {
                        it.copy(posts = result.data, isPostsLoading = false)
                    }
                }
                is Resource.Error -> {
                    _spondonState.update { it.copy(isPostsLoading = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun loadSpondonMembers(memberIds: List<String>) {
        viewModelScope.launch {
            when (val result = communityRepository.getCommunityMembers(memberIds)) {
                is Resource.Success -> {
                    _spondonState.update { it.copy(members = result.data) }
                }
                is Resource.Error -> {}
                is Resource.Loading -> {}
            }
        }
    }

    fun setSpondonTab(tab: Int) {
        _spondonState.update { it.copy(selectedTab = tab) }
    }

    fun updateSpondonMemberSearchQuery(query: String) {
        _spondonState.update { it.copy(memberSearchQuery = query) }
    }

    // ─── Create Post ──────────────────────────────────────────

    fun updatePostContent(content: String) {
        _createPostState.update { it.copy(content = content) }
    }

    fun updatePostImageUri(uri: Uri?) {
        _createPostState.update { it.copy(imageUri = uri) }
    }

    fun createPost() {
        viewModelScope.launch {
            val state = _createPostState.value
            if (state.content.isBlank()) {
                _createPostState.update { it.copy(error = "Post content cannot be empty") }
                return@launch
            }

            // Use the cached community ID, or fetch it from repo if state not loaded
            val spondonId = _spondonState.value.community?.id
                ?: communityRepository.getSpondonCommunityId()
            if (spondonId == null) {
                _createPostState.update { it.copy(error = "Spondon community not found", isLoading = false) }
                return@launch
            }

            _createPostState.update { it.copy(isLoading = true, error = null) }

            // Fetch author info
            val authorName: String
            val authorAvatarUrl: String
            val userResult = communityRepository.getCommunityMembers(listOf(currentUserId))
            if (userResult is Resource.Success && userResult.data.isNotEmpty()) {
                val user = userResult.data.first()
                authorName = user.name
                authorAvatarUrl = user.avatarUrl
            } else {
                authorName = "Admin"
                authorAvatarUrl = ""
            }

            when (val result = communityRepository.createCommunityPost(
                communityId = spondonId,
                authorId = currentUserId,
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                content = state.content,
                imageUri = state.imageUri,
            )) {
                is Resource.Success -> {
                    _createPostState.update {
                        it.copy(isLoading = false, isCreated = true)
                    }
                    _events.emit(CommunityEvent.ShowSnackbar("Post published!"))
                    // Refresh posts
                    loadSpondonPosts(spondonId)
                }
                is Resource.Error -> {
                    _createPostState.update {
                        it.copy(error = result.message, isLoading = false)
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetCreatePostState() {
        _createPostState.value = CreatePostState()
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            when (communityRepository.deleteCommunityPost(postId)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Post deleted"))
                    _spondonState.value.community?.id?.let { loadSpondonPosts(it) }
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to delete post"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Promotes a member within the Spondon community to the given role
     * (MODERATOR = sub-admin, ADMIN = full admin).
     */
    fun promoteSpondonMember(userId: String, role: CommunityRole) {
        viewModelScope.launch {
            val communityId = _spondonState.value.community?.id ?: return@launch
            when (manageMembersUseCase.promoteMember(communityId, userId, role)) {
                is Resource.Success -> {
                    val label = if (role == CommunityRole.MODERATOR) "Sub-Admin" else "Admin"
                    _events.emit(CommunityEvent.ShowSnackbar("Member promoted to $label"))
                    loadSpondonCommunity() // refresh community + members
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to promote member"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Demotes a moderator/admin back to a regular member in the Spondon community.
     */
    fun demoteSpondonMember(userId: String) {
        viewModelScope.launch {
            val communityId = _spondonState.value.community?.id ?: return@launch
            when (communityRepository.demoteMember(communityId, userId)) {
                is Resource.Success -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Member role removed"))
                    loadSpondonCommunity() // refresh community + members
                }
                is Resource.Error -> {
                    _events.emit(CommunityEvent.ShowSnackbar("Failed to demote member"))
                }
                is Resource.Loading -> {}
            }
        }
    }
}

// ─── Spondon UI State ─────────────────────────────────────────────

data class SpondonCommunityState(
    val community: Community? = null,
    val posts: List<CommunityPost> = emptyList(),
    val members: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isPostsLoading: Boolean = false,
    val error: String? = null,
    val currentUserRole: CommunityRole? = null,
    val currentUserPlatformRole: UserRole = UserRole.USER,
    val currentUser: User? = null,
    val memberSearchQuery: String = "",
    val selectedTab: Int = 0, // 0 = Feed, 1 = Members, 2 = About, 3 = Manage
)

data class CreatePostState(
    val content: String = "",
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreated: Boolean = false,
)
