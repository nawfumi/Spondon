package com.spondon.app.feature.superadmin.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.common.Resource
import com.spondon.app.feature.superadmin.data.SARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// ─── Data Models ─────────────────────────────────────────────

/** Lightweight community for the SA list view. */
data class SACommunityItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val district: String = "",
    val type: String = "PUBLIC", // PUBLIC or PRIVATE
    val status: String = "ACTIVE", // ACTIVE, VERIFIED, SUSPENDED
    val memberCount: Int = 0,
    val adminIds: List<String> = emptyList(),
    val createdAt: Date? = null,
    val createdBy: String = "",
    val totalDonations: Int = 0,
    val avatarUrl: String = "",
)

/** Full detail for a single community. */
data class SACommunityDetail(
    val community: SACommunityItem = SACommunityItem(),
    val members: List<SACommunityMember> = emptyList(),
    val requests: List<SACommunityRequest> = emptyList(),
)

data class SACommunityMember(
    val uid: String = "",
    val name: String = "",
    val bloodGroup: String = "",
    val isAdmin: Boolean = false,
    val avatarUrl: String = "",
)

data class SACommunityRequest(
    val id: String = "",
    val bloodGroup: String = "",
    val urgency: String = "NORMAL",
    val hospital: String = "",
    val status: String = "ACTIVE",
    val requesterName: String = "",
    val createdAt: Date? = null,
)

// ─── Filter & Sort ───────────────────────────────────────────

enum class SACommunityFilter { ALL, VERIFIED, UNVERIFIED, SUSPENDED }
enum class SACommunitySort { NEWEST, MOST_MEMBERS, NAME, MOST_DONATIONS }

// ─── State ───────────────────────────────────────────────────

data class SACommunityListState(
    val allCommunities: List<SACommunityItem> = emptyList(),
    val filteredCommunities: List<SACommunityItem> = emptyList(),
    val searchQuery: String = "",
    val filter: SACommunityFilter = SACommunityFilter.ALL,
    val sort: SACommunitySort = SACommunitySort.NEWEST,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class SACommunityDetailState(
    val detail: SACommunityDetail = SACommunityDetail(),
    val isLoading: Boolean = true,
    val error: String? = null,

    // Delete dialog
    val showDeleteDialog: Boolean = false,
    val deleteConfirmName: String = "",
    val isDeleting: Boolean = false,

    // Remove member dialog
    val showRemoveMemberDialog: Boolean = false,
    val memberToRemove: SACommunityMember? = null,
    val isRemovingMember: Boolean = false,

    // Action result feedback
    val actionMessage: String? = null,
    val isPerformingAction: Boolean = false,
)

// ─── ViewModel ───────────────────────────────────────────────

@HiltViewModel
class SACommunityViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _listState = MutableStateFlow(SACommunityListState())
    val listState: StateFlow<SACommunityListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(SACommunityDetailState())
    val detailState: StateFlow<SACommunityDetailState> = _detailState.asStateFlow()

    init {
        loadCommunities()
    }

    // ═══════════════════════════════════════════════════════════
    // Community List
    // ═══════════════════════════════════════════════════════════

    fun loadCommunities() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.getAllCommunities()) {
                is Resource.Success -> {
                    _listState.update {
                        it.copy(allCommunities = result.data, isLoading = false)
                    }
                    applyFilters()
                }
                is Resource.Error -> {
                    _listState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _listState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setFilter(filter: SACommunityFilter) {
        _listState.update { it.copy(filter = filter) }
        applyFilters()
    }

    fun setSort(sort: SACommunitySort) {
        _listState.update { it.copy(sort = sort) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _listState.value
        var communities = state.allCommunities

        // Search
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            communities = communities.filter { c ->
                c.name.lowercase().contains(q)
                        || c.description.lowercase().contains(q)
                        || c.district.lowercase().contains(q)
            }
        }

        // Filter
        communities = when (state.filter) {
            SACommunityFilter.ALL -> communities
            SACommunityFilter.VERIFIED -> communities.filter { it.status == "VERIFIED" }
            SACommunityFilter.UNVERIFIED -> communities.filter { it.status == "ACTIVE" }
            SACommunityFilter.SUSPENDED -> communities.filter { it.status == "SUSPENDED" }
        }

        // Sort
        communities = when (state.sort) {
            SACommunitySort.NEWEST -> communities.sortedByDescending { it.createdAt }
            SACommunitySort.MOST_MEMBERS -> communities.sortedByDescending { it.memberCount }
            SACommunitySort.NAME -> communities.sortedBy { it.name.lowercase() }
            SACommunitySort.MOST_DONATIONS -> communities.sortedByDescending { it.totalDonations }
        }

        _listState.update { it.copy(filteredCommunities = communities) }
    }

    // ═══════════════════════════════════════════════════════════
    // Community Detail
    // ═══════════════════════════════════════════════════════════

    fun loadCommunityDetail(communityId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.getCommunityDetail(communityId)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(detail = result.data, isLoading = false)
                    }
                }
                is Resource.Error -> {
                    _detailState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Verify / Suspend ───────────────────────────────────

    fun verifyCommunity() {
        val id = _detailState.value.detail.community.id
        viewModelScope.launch {
            _detailState.update { it.copy(isPerformingAction = true) }
            when (saRepository.verifyCommunity(id)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            isPerformingAction = false,
                            detail = it.detail.copy(
                                community = it.detail.community.copy(status = "VERIFIED"),
                            ),
                            actionMessage = "Community verified",
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isPerformingAction = false, actionMessage = "Failed to verify community")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun suspendCommunity() {
        val id = _detailState.value.detail.community.id
        viewModelScope.launch {
            _detailState.update { it.copy(isPerformingAction = true) }
            when (saRepository.suspendCommunity(id)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            isPerformingAction = false,
                            detail = it.detail.copy(
                                community = it.detail.community.copy(status = "SUSPENDED"),
                            ),
                            actionMessage = "Community suspended",
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isPerformingAction = false, actionMessage = "Failed to suspend community")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun unsuspendCommunity() {
        val id = _detailState.value.detail.community.id
        viewModelScope.launch {
            _detailState.update { it.copy(isPerformingAction = true) }
            when (saRepository.unsuspendCommunity(id)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            isPerformingAction = false,
                            detail = it.detail.copy(
                                community = it.detail.community.copy(status = "ACTIVE"),
                            ),
                            actionMessage = "Community reactivated",
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isPerformingAction = false, actionMessage = "Failed to reactivate community")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Delete ──────────────────────────────────────────────

    fun showDeleteDialog() = _detailState.update { it.copy(showDeleteDialog = true) }
    fun hideDeleteDialog() = _detailState.update { it.copy(showDeleteDialog = false, deleteConfirmName = "") }
    fun updateDeleteConfirmName(n: String) = _detailState.update { it.copy(deleteConfirmName = n) }

    fun deleteCommunity(onDeleted: () -> Unit) {
        val community = _detailState.value.detail.community
        if (_detailState.value.deleteConfirmName.trim() != community.name.trim()) return

        viewModelScope.launch {
            _detailState.update { it.copy(isDeleting = true) }
            when (saRepository.deleteCommunity(community.id)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(isDeleting = false, showDeleteDialog = false)
                    }
                    onDeleted()
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isDeleting = false, actionMessage = "Failed to delete community")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Force Remove Member ─────────────────────────────────

    fun showRemoveMemberDialog(member: SACommunityMember) =
        _detailState.update { it.copy(showRemoveMemberDialog = true, memberToRemove = member) }

    fun hideRemoveMemberDialog() =
        _detailState.update { it.copy(showRemoveMemberDialog = false, memberToRemove = null) }

    fun forceRemoveMember() {
        val member = _detailState.value.memberToRemove ?: return
        val communityId = _detailState.value.detail.community.id

        viewModelScope.launch {
            _detailState.update { it.copy(isRemovingMember = true) }
            when (saRepository.forceRemoveMember(communityId, member.uid)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            isRemovingMember = false,
                            showRemoveMemberDialog = false,
                            memberToRemove = null,
                            detail = it.detail.copy(
                                members = it.detail.members.filter { m -> m.uid != member.uid },
                                community = it.detail.community.copy(
                                    memberCount = it.detail.community.memberCount - 1,
                                ),
                            ),
                            actionMessage = "${member.name} removed from community",
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isRemovingMember = false, actionMessage = "Failed to remove member")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearActionMessage() = _detailState.update { it.copy(actionMessage = null) }
}
