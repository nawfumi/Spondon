package com.spondon.app.feature.superadmin.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.common.Resource
import com.spondon.app.feature.superadmin.community.SACommunityItem
import com.spondon.app.feature.superadmin.data.SARepository
import com.spondon.app.feature.superadmin.users.SAUserItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SAPrivacyState(
    /** IDs of users whose data is currently protected. */
    val protectedUserIds: Set<String> = emptySet(),
    /** Resolved user items for the protected users list. */
    val protectedUsers: List<SAUserItem> = emptyList(),
    /** Authorized admin IDs that can bypass privacy. */
    val authorizedAdminIds: List<String> = emptyList(),
    /** Resolved user items for authorized admins. */
    val authorizedAdmins: List<SAUserItem> = emptyList(),
    /** All users — used for search dialogs. */
    val allUsers: List<SAUserItem> = emptyList(),
    /** All communities — used for community-level protection. */
    val communities: List<SACommunityItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isToggling: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class SAPrivacyViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SAPrivacyState())
    val state: StateFlow<SAPrivacyState> = _state.asStateFlow()

    init {
        loadPrivacyConfig()
    }

    fun loadPrivacyConfig() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val protectedIds = saRepository.getProtectedUserIds().toSet()
                val authorizedIds = saRepository.getPrivacyAuthorizedAdmins()

                // Load all users for both protected list and search
                val allUsersResult = saRepository.getAllUsers()
                val allUsers = when (allUsersResult) {
                    is Resource.Success -> allUsersResult.data
                    else -> emptyList()
                }

                // Load communities for community-level protection
                val communitiesResult = saRepository.getAllCommunities()
                val communities = when (communitiesResult) {
                    is Resource.Success -> communitiesResult.data
                    else -> emptyList()
                }

                // Resolve protected users and authorized admins from all users
                val protectedUsers = allUsers.filter { it.uid in protectedIds }
                val authorizedAdmins = allUsers.filter { it.uid in authorizedIds }

                _state.update {
                    it.copy(
                        protectedUserIds = protectedIds,
                        protectedUsers = protectedUsers,
                        authorizedAdminIds = authorizedIds,
                        authorizedAdmins = authorizedAdmins,
                        allUsers = allUsers,
                        communities = communities,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load privacy config")
                }
            }
        }
    }

    // ─── Per-User Protection ─────────────────────────────────

    fun addProtectedUser(uid: String) {
        viewModelScope.launch {
            when (saRepository.addProtectedUser(uid)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(successMessage = "User added to protected list")
                    }
                    loadPrivacyConfig()
                }
                is Resource.Error -> {
                    _state.update { it.copy(error = "Failed to protect user") }
                }
                else -> {}
            }
        }
    }

    fun removeProtectedUser(uid: String) {
        viewModelScope.launch {
            when (saRepository.removeProtectedUser(uid)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(successMessage = "User removed from protected list")
                    }
                    loadPrivacyConfig()
                }
                is Resource.Error -> {
                    _state.update { it.copy(error = "Failed to unprotect user") }
                }
                else -> {}
            }
        }
    }

    // ─── Community-Level Protection ──────────────────────────

    fun protectCommunity(communityId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isToggling = true) }
            when (val result = saRepository.protectCommunityMembers(communityId)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isToggling = false,
                            successMessage = "${result.data} members added to protected list",
                        )
                    }
                    loadPrivacyConfig()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isToggling = false, error = "Failed to protect community") }
                }
                else -> {}
            }
        }
    }

    fun unprotectCommunity(communityId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isToggling = true) }
            when (val result = saRepository.unprotectCommunityMembers(communityId)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isToggling = false,
                            successMessage = "${result.data} members removed from protected list",
                        )
                    }
                    loadPrivacyConfig()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isToggling = false, error = "Failed to unprotect community") }
                }
                else -> {}
            }
        }
    }

    fun clearAllProtected() {
        viewModelScope.launch {
            _state.update { it.copy(isToggling = true) }
            when (saRepository.clearAllProtectedUsers()) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isToggling = false,
                            successMessage = "All protected users cleared",
                        )
                    }
                    loadPrivacyConfig()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isToggling = false, error = "Failed to clear") }
                }
                else -> {}
            }
        }
    }

    // ─── Authorized Admins ───────────────────────────────────

    fun grantAccess(uid: String) {
        viewModelScope.launch {
            when (saRepository.grantPrivacyAccess(uid)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(successMessage = "Access granted")
                    }
                    loadPrivacyConfig()
                }
                is Resource.Error -> {
                    _state.update { it.copy(error = "Failed to grant access") }
                }
                else -> {}
            }
        }
    }

    fun revokeAccess(uid: String) {
        viewModelScope.launch {
            when (saRepository.revokePrivacyAccess(uid)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(successMessage = "Access revoked")
                    }
                    loadPrivacyConfig()
                }
                is Resource.Error -> {
                    _state.update { it.copy(error = "Failed to revoke access") }
                }
                else -> {}
            }
        }
    }

    // ─── Search ──────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    /** Users not yet protected — for the "Add user" dialog. */
    fun getFilteredUsersForProtection(): List<SAUserItem> {
        val state = _state.value
        val query = state.searchQuery.lowercase()
        return state.allUsers
            .filter { it.uid !in state.protectedUserIds }
            .filter {
                query.isBlank() ||
                it.name.lowercase().contains(query) ||
                it.email.lowercase().contains(query) ||
                it.phone.contains(query)
            }
    }

    /** Users not yet authorized — for the "Add admin" dialog. */
    fun getFilteredUsersForAuthorization(): List<SAUserItem> {
        val state = _state.value
        val query = state.searchQuery.lowercase()
        val authorizedIds = state.authorizedAdminIds.toSet()
        return state.allUsers
            .filter { it.uid !in authorizedIds }
            .filter {
                query.isBlank() ||
                it.name.lowercase().contains(query) ||
                it.email.lowercase().contains(query) ||
                it.phone.contains(query)
            }
    }
}
