package com.spondon.app.feature.superadmin.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Resource
import com.spondon.app.feature.superadmin.data.SARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SAAuthState(
    val email: String = "",
    val password: String = "",
    val passphrase: String = "",
    val passwordVisible: Boolean = false,
    val passphraseVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Registration
    val isRegistered: Boolean = false,
    val registrationComplete: Boolean = false,

    // Login
    val isLoginComplete: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutEndTime: Long = 0L,
    val isSuperAdmin: Boolean = false,

    // Init check
    val isInitialized: Boolean = false,
)

@HiltViewModel
class SuperAdminAuthViewModel @Inject constructor(
    private val saRepository: SARepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _state = MutableStateFlow(SAAuthState())
    val state: StateFlow<SAAuthState> = _state.asStateFlow()

    init {
        checkRegistrationStatus()
    }

    private fun checkRegistrationStatus() {
        viewModelScope.launch {
            val registered = saRepository.isSuperAdminRegistered()
            _state.update {
                it.copy(isRegistered = registered, isInitialized = true)
            }
        }
    }

    // ─── Field Updates ─────────────────────────────────────────

    fun updateEmail(email: String) = _state.update { it.copy(email = email, error = null) }
    fun updatePassword(pw: String) = _state.update { it.copy(password = pw, error = null) }
    fun updatePassphrase(pp: String) = _state.update { it.copy(passphrase = pp, error = null) }
    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun togglePassphraseVisibility() = _state.update { it.copy(passphraseVisible = !it.passphraseVisible) }
    fun clearError() = _state.update { it.copy(error = null) }

    // ─── Registration ──────────────────────────────────────────

    fun register() {
        val s = _state.value
        if (s.email.isBlank() || !s.email.contains("@")) {
            _state.update { it.copy(error = "Enter a valid email") }
            return
        }
        if (s.password.length < 8) {
            _state.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        if (s.passphrase.length < 6) {
            _state.update { it.copy(error = "Secret passphrase must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.registerSuperAdmin(s.email, s.password, s.passphrase)) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            registrationComplete = true,
                            isRegistered = true,
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Login (3-factor: Email + Password + Passphrase) ──────

    fun login() {
        val s = _state.value

        // Check lockout
        if (s.lockoutEndTime > System.currentTimeMillis()) {
            val remaining = ((s.lockoutEndTime - System.currentTimeMillis()) / 1000).toInt()
            _state.update { it.copy(error = "Too many attempts. Try again in ${remaining}s") }
            return
        }

        if (s.email.isBlank() || s.password.isBlank() || s.passphrase.isBlank()) {
            _state.update { it.copy(error = "All fields are required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Factor 1 & 2: Email + Password via Firebase Auth
            try {
                auth.signInWithEmailAndPassword(s.email, s.password).await()
            } catch (e: Exception) {
                handleFailedAttempt("Invalid email or password")
                return@launch
            }

            // Factor 3: Verify passphrase against Firestore hash
            when (val result = saRepository.verifyPassphrase(s.passphrase)) {
                is Resource.Success -> {
                    // Verify this user is actually the SA
                    if (saRepository.isCurrentUserSuperAdmin()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isLoginComplete = true,
                                isSuperAdmin = true,
                                failedAttempts = 0,
                            )
                        }
                    } else {
                        auth.signOut()
                        handleFailedAttempt("Not authorized as SuperAdmin")
                    }
                }
                is Resource.Error -> {
                    auth.signOut()
                    handleFailedAttempt(result.message ?: "Invalid passphrase")
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun handleFailedAttempt(message: String) {
        val attempts = _state.value.failedAttempts + 1
        val lockout = if (attempts >= 3) {
            System.currentTimeMillis() + 10 * 60 * 1000L // 10-minute lockout
        } else {
            0L
        }
        _state.update {
            it.copy(
                isLoading = false,
                error = if (attempts >= 3) "Too many failed attempts. Locked for 10 minutes." else message,
                failedAttempts = attempts,
                lockoutEndTime = lockout,
            )
        }
    }
}
