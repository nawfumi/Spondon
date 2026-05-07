package com.spondon.app.feature.superadmin.users

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

/** Lightweight user representation for the SA list view. */
data class SAUserItem(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarUrl: String = "",
    val bloodGroup: String = "",
    val district: String = "",
    val isDonor: Boolean = false,
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val totalDonations: Int = 0,
    val communityIds: List<String> = emptyList(),
    val createdAt: Date? = null,
    val fcmToken: String = "",
    val lastDonationDate: Date? = null,
    val role: String = "USER",
)

/** Full detail data for a single user, including their requests. */
data class SAUserDetail(
    val user: SAUserItem = SAUserItem(),
    val communities: List<SAUserCommunity> = emptyList(),
    val requests: List<SAUserRequest> = emptyList(),
)

data class SAUserCommunity(
    val id: String = "",
    val name: String = "",
    val memberCount: Int = 0,
)

data class SAUserRequest(
    val id: String = "",
    val bloodGroup: String = "",
    val urgency: String = "NORMAL",
    val hospital: String = "",
    val status: String = "ACTIVE",
    val createdAt: Date? = null,
)

// ─── Filter & Sort ───────────────────────────────────────────

enum class SAUserFilter { ALL, ACTIVE, BANNED, DONORS }
enum class SAUserSort { NEWEST, MOST_DONATIONS, NAME }

// ─── State ───────────────────────────────────────────────────

data class SAUserListState(
    val allUsers: List<SAUserItem> = emptyList(),
    val filteredUsers: List<SAUserItem> = emptyList(),
    val searchQuery: String = "",
    val filter: SAUserFilter = SAUserFilter.ALL,
    val sort: SAUserSort = SAUserSort.NEWEST,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class SAUserDetailState(
    val detail: SAUserDetail = SAUserDetail(),
    val isLoading: Boolean = true,
    val error: String? = null,

    // Ban dialog
    val showBanDialog: Boolean = false,
    val banReason: String = "",
    val isBanning: Boolean = false,

    // Delete dialog
    val showDeleteDialog: Boolean = false,
    val deleteConfirmName: String = "",
    val isDeleting: Boolean = false,

    // Send notification dialog
    val showNotifyDialog: Boolean = false,
    val notifyTitle: String = "",
    val notifyBody: String = "",
    val notifyType: String = "INFO",
    val isSendingNotification: Boolean = false,

    // Action result feedback
    val actionMessage: String? = null,
)

// ─── ViewModel ───────────────────────────────────────────────

@HiltViewModel
class SAUserViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _listState = MutableStateFlow(SAUserListState())
    val listState: StateFlow<SAUserListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(SAUserDetailState())
    val detailState: StateFlow<SAUserDetailState> = _detailState.asStateFlow()

    init {
        loadUsers()
    }

    // ═══════════════════════════════════════════════════════════
    // User List
    // ═══════════════════════════════════════════════════════════

    fun loadUsers() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.getAllUsers()) {
                is Resource.Success -> {
                    _listState.update {
                        it.copy(
                            allUsers = result.data,
                            isLoading = false,
                        )
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

    fun setFilter(filter: SAUserFilter) {
        _listState.update { it.copy(filter = filter) }
        applyFilters()
    }

    fun setSort(sort: SAUserSort) {
        _listState.update { it.copy(sort = sort) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _listState.value
        var users = state.allUsers

        // Filter by search query
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            users = users.filter { u ->
                u.name.lowercase().contains(q)
                        || u.email.lowercase().contains(q)
                        || u.phone.contains(q)
                        || u.bloodGroup.lowercase().contains(q)
                        || u.district.lowercase().contains(q)
            }
        }

        // Filter by status
        users = when (state.filter) {
            SAUserFilter.ALL -> users
            SAUserFilter.ACTIVE -> users.filter { !it.isBanned }
            SAUserFilter.BANNED -> users.filter { it.isBanned }
            SAUserFilter.DONORS -> users.filter { it.isDonor }
        }

        // Sort
        users = when (state.sort) {
            SAUserSort.NEWEST -> users.sortedByDescending { it.createdAt }
            SAUserSort.MOST_DONATIONS -> users.sortedByDescending { it.totalDonations }
            SAUserSort.NAME -> users.sortedBy { it.name.lowercase() }
        }

        _listState.update { it.copy(filteredUsers = users) }
    }

    // ═══════════════════════════════════════════════════════════
    // User Detail
    // ═══════════════════════════════════════════════════════════

    fun loadUserDetail(uid: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.getUserDetail(uid)) {
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

    // ─── Ban / Unban ─────────────────────────────────────────

    fun showBanDialog() = _detailState.update { it.copy(showBanDialog = true) }
    fun hideBanDialog() = _detailState.update { it.copy(showBanDialog = false, banReason = "") }
    fun updateBanReason(r: String) = _detailState.update { it.copy(banReason = r) }

    fun banUser() {
        val uid = _detailState.value.detail.user.uid
        val reason = _detailState.value.banReason

        if (reason.isBlank()) return

        viewModelScope.launch {
            _detailState.update { it.copy(isBanning = true) }
            when (saRepository.banUser(uid, reason)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            isBanning = false,
                            showBanDialog = false,
                            banReason = "",
                            detail = it.detail.copy(
                                user = it.detail.user.copy(isBanned = true, banReason = reason),
                            ),
                            actionMessage = "User banned successfully",
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isBanning = false, actionMessage = "Failed to ban user")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun unbanUser() {
        val uid = _detailState.value.detail.user.uid
        viewModelScope.launch {
            _detailState.update { it.copy(isBanning = true) }
            when (saRepository.unbanUser(uid)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            isBanning = false,
                            detail = it.detail.copy(
                                user = it.detail.user.copy(isBanned = false, banReason = null),
                            ),
                            actionMessage = "User unbanned successfully",
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isBanning = false, actionMessage = "Failed to unban user")
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

    fun deleteUser(onDeleted: () -> Unit) {
        val user = _detailState.value.detail.user
        if (_detailState.value.deleteConfirmName.trim() != user.name.trim()) return

        viewModelScope.launch {
            _detailState.update { it.copy(isDeleting = true) }
            when (saRepository.deleteUser(user.uid)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(isDeleting = false, showDeleteDialog = false)
                    }
                    onDeleted()
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(isDeleting = false, actionMessage = "Failed to delete user")
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Send Notification ───────────────────────────────────

    fun showNotifyDialog() = _detailState.update { it.copy(showNotifyDialog = true) }
    fun hideNotifyDialog() = _detailState.update {
        it.copy(showNotifyDialog = false, notifyTitle = "", notifyBody = "", notifyType = "INFO")
    }
    fun updateNotifyTitle(t: String) = _detailState.update { it.copy(notifyTitle = t) }
    fun updateNotifyBody(b: String) = _detailState.update { it.copy(notifyBody = b) }
    fun updateNotifyType(t: String) = _detailState.update { it.copy(notifyType = t) }

    fun sendNotification() {
        val state = _detailState.value
        val uid = state.detail.user.uid

        if (state.notifyTitle.isBlank() || state.notifyBody.isBlank()) return

        viewModelScope.launch {
            _detailState.update { it.copy(isSendingNotification = true) }
            when (saRepository.sendNotificationToUser(
                uid = uid,
                title = state.notifyTitle,
                body = state.notifyBody,
                type = state.notifyType,
            )) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            isSendingNotification = false,
                            showNotifyDialog = false,
                            notifyTitle = "",
                            notifyBody = "",
                            actionMessage = "Notification sent",
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(
                            isSendingNotification = false,
                            actionMessage = "Failed to send notification",
                        )
                    }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearActionMessage() = _detailState.update { it.copy(actionMessage = null) }
}
