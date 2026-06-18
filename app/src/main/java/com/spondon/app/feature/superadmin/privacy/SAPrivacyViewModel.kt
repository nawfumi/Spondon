package com.spondon.app.feature.superadmin.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.common.Resource
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
    val isPrivacyEnabled: Boolean = false,
    val authorizedAdminIds: List<String> = emptyList(),
    val authorizedAdmins: List<SAUserItem> = emptyList(),
    val allUsers: List<SAUserItem> = emptyList(),
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
                val isEnabled = saRepository.isPrivacyEnabled()
                val authorizedIds = saRepository.getPrivacyAuthorizedAdmins()

                // Load all users for both authorized list and search
                val allUsersResult = saRepository.getAllUsers()
                val allUsers = when (allUsersResult) {
                    is Resource.Success -> allUsersResult.data
                    else -> emptyList()
                }

                // Filter authorized admins from all users
                val authorizedAdmins = allUsers.filter { it.uid in authorizedIds }

                _state.update {
                    it.copy(
                        isPrivacyEnabled = isEnabled,
                        authorizedAdminIds = authorizedIds,
                        authorizedAdmins = authorizedAdmins,
                        allUsers = allUsers,
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

    fun togglePrivacy() {
        viewModelScope.launch {
            val currentState = _state.value
            _state.update { it.copy(isToggling = true) }

            when (saRepository.setPrivacyEnabled(!currentState.isPrivacyEnabled)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isPrivacyEnabled = !currentState.isPrivacyEnabled,
                            isToggling = false,
                            successMessage = if (!currentState.isPrivacyEnabled)
                                "Privacy mode enabled — member data is now hidden"
                            else
                                "Privacy mode disabled — member data is now visible to all",
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update {
                        it.copy(isToggling = false, error = "Failed to toggle privacy mode")
                    }
                }
                else -> {}
            }
        }
    }

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

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    fun getFilteredUsers(): List<SAUserItem> {
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
