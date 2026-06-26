package com.spondon.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.local.PreferencesManager
import com.spondon.app.core.data.remote.FirestoreService
import com.spondon.app.core.data.repository.UserRepository
import com.spondon.app.core.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val isDarkMode: Boolean = true,
    val language: String = "bn",
    // Notification prefs (stored locally)
    val notifyNewRequests: Boolean = true,
    val notifyJoinApprovals: Boolean = true,
    val notifyDonationReminders: Boolean = true,
    val notifyAdminAlerts: Boolean = true,
    // Privacy
    val showPhoneNumber: Boolean = true,
    val showInDonorSearch: Boolean = true,
    // Security
    val isBiometricEnabled: Boolean = false,
    // Account
    val showDeleteDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository,
    private val firestoreService: FirestoreService,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val darkMode = preferencesManager.isDarkMode.first()
            val lang = preferencesManager.language.first()
            val biometric = preferencesManager.isBiometricEnabled.first()

            // Load persisted notification preferences
            val notifyReqs = preferencesManager.notifyNewRequests.first()
            val notifyJoin = preferencesManager.notifyJoinApprovals.first()
            val notifyDonation = preferencesManager.notifyDonationReminders.first()
            val notifyAdmin = preferencesManager.notifyAdminAlerts.first()

            val uid = auth.currentUser?.uid ?: ""
            var showPhone = true
            var isDonor = true
            if (uid.isNotBlank()) {
                val userResult = userRepository.getUser(uid)
                if (userResult is Resource.Success) {
                    showPhone = userResult.data.isPhoneVisible
                    isDonor = userResult.data.isDonor
                }
            }

            _state.update {
                it.copy(
                    isDarkMode = darkMode,
                    language = lang,
                    isBiometricEnabled = biometric,
                    notifyNewRequests = notifyReqs,
                    notifyJoinApprovals = notifyJoin,
                    notifyDonationReminders = notifyDonation,
                    notifyAdminAlerts = notifyAdmin,
                    showPhoneNumber = showPhone,
                    showInDonorSearch = isDonor,
                    isLoading = false,
                )
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newVal = !_state.value.isDarkMode
            preferencesManager.setDarkMode(newVal)
            _state.update { it.copy(isDarkMode = newVal) }
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(lang)
            _state.update { it.copy(language = lang) }
        }
    }

    fun toggleNotifyNewRequests() {
        viewModelScope.launch {
            val newVal = !_state.value.notifyNewRequests
            preferencesManager.setNotifyNewRequests(newVal)
            _state.update { it.copy(notifyNewRequests = newVal) }
        }
    }
    fun toggleNotifyJoinApprovals() {
        viewModelScope.launch {
            val newVal = !_state.value.notifyJoinApprovals
            preferencesManager.setNotifyJoinApprovals(newVal)
            _state.update { it.copy(notifyJoinApprovals = newVal) }
        }
    }
    fun toggleNotifyDonationReminders() {
        viewModelScope.launch {
            val newVal = !_state.value.notifyDonationReminders
            preferencesManager.setNotifyDonationReminders(newVal)
            _state.update { it.copy(notifyDonationReminders = newVal) }
        }
    }
    fun toggleNotifyAdminAlerts() {
        viewModelScope.launch {
            val newVal = !_state.value.notifyAdminAlerts
            preferencesManager.setNotifyAdminAlerts(newVal)
            _state.update { it.copy(notifyAdminAlerts = newVal) }
        }
    }

    fun togglePhoneVisibility() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val newVal = !_state.value.showPhoneNumber
            val userResult = userRepository.getUser(uid)
            if (userResult is Resource.Success) {
                userRepository.updateUser(userResult.data.copy(isPhoneVisible = newVal))
            }
            _state.update { it.copy(showPhoneNumber = newVal) }
        }
    }

    fun toggleDonorSearchVisibility() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val newVal = !_state.value.showInDonorSearch
            val userResult = userRepository.getUser(uid)
            if (userResult is Resource.Success) {
                userRepository.updateUser(userResult.data.copy(isDonor = newVal))
            }
            _state.update { it.copy(showInDonorSearch = newVal) }
        }
    }

    fun toggleBiometric() {
        viewModelScope.launch {
            val newVal = !_state.value.isBiometricEnabled
            preferencesManager.setBiometricEnabled(newVal)
            _state.update { it.copy(isBiometricEnabled = newVal) }
        }
    }

    fun showDeleteDialog() = _state.update { it.copy(showDeleteDialog = true) }
    fun hideDeleteDialog() = _state.update { it.copy(showDeleteDialog = false) }
    fun showLogoutDialog() = _state.update { it.copy(showLogoutDialog = true) }
    fun hideLogoutDialog() = _state.update { it.copy(showLogoutDialog = false) }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, error = null) }
            val uid = auth.currentUser?.uid
            if (uid != null) {
                firestoreService.deleteUserAccount(uid)
            }
            // Clear all local data before signing out
            preferencesManager.clearUserData()
            try {
                auth.currentUser?.delete()
            } catch (_: Exception) {}
            auth.signOut()
            _state.update { it.copy(isDeleting = false, showDeleteDialog = false) }
            onComplete()
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            // Clear user-specific prefs (preserves dark mode, language)
            preferencesManager.clearUserData()
            auth.signOut()
            onComplete()
        }
    }
}