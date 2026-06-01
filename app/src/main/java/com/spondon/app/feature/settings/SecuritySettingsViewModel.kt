package com.spondon.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecuritySettingsState(
    val isBiometricEnabled: Boolean = false,
    val isLoading: Boolean = true,
    /** Set to a description when the user wants to change a sensitive setting.
     *  The UI layer reads this, triggers the biometric prompt, and calls
     *  [onAuthResult] with the outcome. */
    val pendingAuthAction: PendingAuthAction? = null,
)

/** Describes a sensitive change that requires biometric auth before applying. */
sealed class PendingAuthAction(val promptTitle: String) {
    /** Toggle the biometric lock (enable ↔ disable). */
    data class ToggleBiometric(val newValue: Boolean) :
        PendingAuthAction(if (newValue) "Enable Biometric Lock" else "Disable Biometric Lock")
}

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SecuritySettingsState())
    val state: StateFlow<SecuritySettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val biometric = preferencesManager.isBiometricEnabled.first()

            _state.update {
                it.copy(
                    isBiometricEnabled = biometric,
                    isLoading = false,
                )
            }
        }
    }

    // ─── Biometric Lock Toggle ─────────────────────────────────

    /**
     * Called by the UI when the user taps the biometric lock toggle.
     * Does NOT apply the change — instead sets [pendingAuthAction] so the
     * Composable can trigger the biometric prompt.
     */
    fun requestToggleBiometric() {
        val newValue = !_state.value.isBiometricEnabled
        _state.update { it.copy(pendingAuthAction = PendingAuthAction.ToggleBiometric(newValue)) }
    }

    // ─── Auth Result ───────────────────────────────────────────

    /**
     * Called by the UI after the biometric prompt completes.
     * @param success true if authentication succeeded.
     */
    fun onAuthResult(success: Boolean) {
        val action = _state.value.pendingAuthAction ?: return
        if (success) {
            viewModelScope.launch {
                when (action) {
                    is PendingAuthAction.ToggleBiometric -> {
                        preferencesManager.setBiometricEnabled(action.newValue)
                        _state.update {
                            it.copy(
                                isBiometricEnabled = action.newValue,
                                pendingAuthAction = null,
                            )
                        }
                    }
                }
            }
        } else {
            // Auth failed / cancelled — just clear the pending action
            _state.update { it.copy(pendingAuthAction = null) }
        }
    }
}
