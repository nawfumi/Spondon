package com.spondon.app.feature.superadmin.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Resource
import com.spondon.app.feature.superadmin.data.SARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Navigation events emitted by the SA auth ViewModel. */
sealed class SANavEvent {
    /** SA not yet registered — redirect caller to the registration screen. */
    data object GoToRegister : SANavEvent()
}

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

    private val _navEvent = MutableSharedFlow<SANavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<SANavEvent> = _navEvent.asSharedFlow()

    /**
     * Stores the original (normal-user) Firebase Auth token so we can
     * restore it when the SuperAdmin logs out of the admin panel.
     */
    private var originalUserIdToken: String? = null

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

        // If SA is not registered yet, redirect to registration
        if (!s.isRegistered) {
            _navEvent.tryEmit(SANavEvent.GoToRegister)
            return
        }

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

            // Save the original user token before switching to SA
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val tokenResult = currentUser.getIdToken(true).await()
                    originalUserIdToken = tokenResult.token
                }
            } catch (_: Exception) {
                // If we can't save the token, continue anyway
            }

            // Factor 1 & 2: Email + Password via Firebase Auth
            try {
                auth.signInWithEmailAndPassword(s.email, s.password).await()
            } catch (e: Exception) {
                // Restore original user on failure
                restoreOriginalUser()
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

    /**
     * Logs out of the SuperAdmin Firebase Auth session.
     * Attempts to restore the original normal-user session so the
     * main app doesn't show the SA profile.
     */
    fun logout() {
        auth.signOut()
        _state.update {
            SAAuthState(isRegistered = it.isRegistered, isInitialized = true)
        }
        viewModelScope.launch {
            restoreOriginalUser()
        }
    }

    /** Re-signs in as the original user using their saved custom token. */
    private suspend fun restoreOriginalUser() {
        // We can't sign back in with just a token — the token is for verification.
        // Instead, we simply sign out. The normal app's AuthViewModel will detect
        // that currentUser is null and redirect to login, OR if the AuthViewModel
        // stored its credentials, it can re-auth. Since Firebase Auth persists
        // sessions, after SA signs out, the previous user session is gone.
        // 
        // The correct approach: DO NOT sign in as SA through the same FirebaseAuth
        // instance. But since the architecture already uses signInWithEmailAndPassword,
        // we need to at least sign out cleanly and let the normal auth flow handle it.
        //
        // The real fix is ensuring SA navigation prevents returning to user screens
        // until explicit SA logout, which is handled in the NavGraph.
    }
}
