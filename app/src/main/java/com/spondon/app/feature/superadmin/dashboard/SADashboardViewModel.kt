package com.spondon.app.feature.superadmin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.feature.superadmin.data.SARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SADashboardState(
    val totalUsers: Int = 0,
    val totalCommunities: Int = 0,
    val activeRequests: Int = 0,
    val totalDonations: Int = 0,
    val bannedUsers: Int = 0,
    val pendingFeedback: Int = 0,
    val newUsersToday: Int = 0,
    val isMaintenanceOn: Boolean = false,
    val isPrivacyOn: Boolean = false,
    val forceUpdateVersion: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class SADashboardViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SADashboardState())
    val state: StateFlow<SADashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val totalUsers = saRepository.getTotalUserCount()
                val totalCommunities = saRepository.getTotalCommunityCount()
                val activeRequests = saRepository.getActiveRequestCount()
                val totalDonations = saRepository.getTotalDonationCount()
                val bannedUsers = saRepository.getBannedUserCount()
                val pendingFeedback = saRepository.getPendingFeedbackCount()
                val newUsersToday = saRepository.getNewUsersTodayCount()
                val maintenanceStatus = saRepository.getMaintenanceStatus()
                val forceUpdateVersion = saRepository.getForceUpdateVersion()
                val privacyStatus = saRepository.isPrivacyEnabled()

                _state.update {
                    it.copy(
                        totalUsers = totalUsers,
                        totalCommunities = totalCommunities,
                        activeRequests = activeRequests,
                        totalDonations = totalDonations,
                        bannedUsers = bannedUsers,
                        pendingFeedback = pendingFeedback,
                        newUsersToday = newUsersToday,
                        isMaintenanceOn = maintenanceStatus,
                        isPrivacyOn = privacyStatus,
                        forceUpdateVersion = forceUpdateVersion,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load dashboard")
                }
            }
        }
    }
}
