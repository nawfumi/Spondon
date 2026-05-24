package com.spondon.app.feature.superadmin.broadcast

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

// ─── Data Model ──────────────────────────────────────────────

data class SABroadcastItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val target: String = "",
    val sentAt: Date? = null,
    val status: String = "SENT",
)

// ─── State ───────────────────────────────────────────────────

data class SABroadcastState(
    // Composer
    val title: String = "",
    val body: String = "",
    val type: String = "Announcement",
    val target: String = "All Users",
    val targetDistrict: String = "",
    val targetBloodGroup: String = "",
    val targetCommunityId: String = "",
    val targetCommunityName: String = "",
    val communities: List<Pair<String, String>> = emptyList(), // id to name
    val isSending: Boolean = false,
    val sendSuccess: Boolean = false,
    val error: String? = null,

    // History
    val history: List<SABroadcastItem> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val historyError: String? = null,

    // UI mode
    val showHistory: Boolean = false,
)

// ─── ViewModel ───────────────────────────────────────────────

@HiltViewModel
class SABroadcastViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SABroadcastState())
    val state: StateFlow<SABroadcastState> = _state.asStateFlow()

    init {
        loadCommunities()
    }

    private fun loadCommunities() {
        viewModelScope.launch {
            val result = saRepository.getAllCommunities()
            if (result is Resource.Success) {
                _state.update { it.copy(communities = result.data.map { c -> c.id to c.name }) }
            }
        }
    }

    // ─── Composer ──────────────────────────────────────────

    fun updateTitle(v: String) = _state.update { it.copy(title = v.take(65), error = null) }
    fun updateBody(v: String) = _state.update { it.copy(body = v.take(240), error = null) }
    fun updateType(v: String) = _state.update { it.copy(type = v) }
    fun updateTarget(v: String) = _state.update { it.copy(target = v) }
    fun updateTargetDistrict(v: String) = _state.update { it.copy(targetDistrict = v) }
    fun updateTargetBloodGroup(v: String) = _state.update { it.copy(targetBloodGroup = v) }
    fun updateTargetCommunity(id: String, name: String) = _state.update { it.copy(targetCommunityId = id, targetCommunityName = name) }

    fun sendBroadcast() {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "Title is required") }
            return
        }
        if (s.body.isBlank()) {
            _state.update { it.copy(error = "Body is required") }
            return
        }
        if (s.target == "Specific District" && s.targetDistrict.isBlank()) {
            _state.update { it.copy(error = "Please select a district") }
            return
        }
        if (s.target == "Specific Blood Group" && s.targetBloodGroup.isBlank()) {
            _state.update { it.copy(error = "Please select a blood group") }
            return
        }
        if (s.target == "Specific Community" && s.targetCommunityId.isBlank()) {
            _state.update { it.copy(error = "Please select a community") }
            return
        }

        val targetValue = when (s.target) {
            "Specific District" -> s.targetDistrict
            "Specific Blood Group" -> s.targetBloodGroup
            "Specific Community" -> s.targetCommunityName
            else -> ""
        }

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            when (saRepository.sendBroadcast(
                title = s.title,
                body = s.body,
                type = s.type,
                target = s.target,
                targetValue = targetValue,
            )) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isSending = false,
                            sendSuccess = true,
                            title = "",
                            body = "",
                            targetDistrict = "",
                            targetBloodGroup = "",
                            targetCommunityId = "",
                            targetCommunityName = "",
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSending = false, error = "Broadcast failed") }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearSendSuccess() = _state.update { it.copy(sendSuccess = false) }

    // ─── History ───────────────────────────────────────────

    fun toggleHistory() {
        val showing = !_state.value.showHistory
        _state.update { it.copy(showHistory = showing) }
        if (showing) loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingHistory = true, historyError = null) }
            when (val result = saRepository.getBroadcastHistory()) {
                is Resource.Success -> {
                    _state.update { it.copy(history = result.data, isLoadingHistory = false) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoadingHistory = false, historyError = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }
}
