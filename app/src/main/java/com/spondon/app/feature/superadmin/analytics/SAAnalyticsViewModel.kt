package com.spondon.app.feature.superadmin.analytics

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

// ─── State ───────────────────────────────────────────────────

data class SAAnalyticsState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Signups per day (last 7 days)
    val signupsPerDay: Map<String, Int> = emptyMap(),

    // Request urgency breakdown
    val requestsByUrgency: Map<String, Int> = emptyMap(),

    // Blood group distribution
    val requestsByBloodGroup: Map<String, Int> = emptyMap(),

    // Fulfillment rate
    val fulfilledCount: Int = 0,
    val totalRequestCount: Int = 0,

    // Top districts
    val topDistricts: List<Pair<String, Int>> = emptyList(),

    // Top donors
    val topDonors: List<Pair<String, Int>> = emptyList(),

    // Critical alerts
    val criticalAlerts: List<Map<String, String>> = emptyList(),

    // Community stats
    val totalCommunities: Int = 0,
    val avgMembersPerCommunity: Double = 0.0,
    val largestCommunity: String = "—",
)

// ─── ViewModel ───────────────────────────────────────────────

@HiltViewModel
class SAAnalyticsViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SAAnalyticsState())
    val state: StateFlow<SAAnalyticsState> = _state.asStateFlow()

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val signups = saRepository.getSignupsPerDay(7)
                val urgency = saRepository.getRequestsByUrgency()
                val bloodGroup = saRepository.getRequestsByBloodGroup()
                val (fulfilled, total) = saRepository.getDonationFulfillmentRate()
                val districts = saRepository.getTopDistricts(5)
                val donors = saRepository.getTopDonorsThisMonth(5)
                val alerts = saRepository.getCriticalUnfulfilledRequests()
                val (communityTotal, avgMembers, largest) = saRepository.getCommunityGrowthStats()

                _state.update {
                    it.copy(
                        isLoading = false,
                        signupsPerDay = signups,
                        requestsByUrgency = urgency,
                        requestsByBloodGroup = bloodGroup,
                        fulfilledCount = fulfilled,
                        totalRequestCount = total,
                        topDistricts = districts,
                        topDonors = donors,
                        criticalAlerts = alerts,
                        totalCommunities = communityTotal,
                        avgMembersPerCommunity = avgMembers,
                        largestCommunity = largest,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load analytics") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
