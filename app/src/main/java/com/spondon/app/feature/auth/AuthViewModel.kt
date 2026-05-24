package com.spondon.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.FirebaseFirestore
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.local.PreferencesManager
import com.spondon.app.core.data.repository.AuthRepository
import com.spondon.app.core.data.repository.UserRepository
import com.spondon.app.core.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * One-shot navigation events emitted by the ViewModel.
 * Using a Channel ensures each event is consumed exactly once,
 * preventing the crash-on-reopen bug caused by stale state flags
 * re-triggering navigation after the auth_flow graph is destroyed.
 */
sealed class AuthNavigationEvent {
    data object NavigateToHome : AuthNavigationEvent()
    data object NavigateToProfileSetup : AuthNavigationEvent()
    data object NavigateToLogin : AuthNavigationEvent()
    data object NavigateToOnboarding : AuthNavigationEvent()
    data object PasswordResetSuccess : AuthNavigationEvent()
    data class NavigateToBanned(val reason: String?) : AuthNavigationEvent()
}

data class AuthState(
    // ── Sign Up Step 1: Basic Info ───
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val agreedToTerms: Boolean = false,
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,

    // ── Sign Up Step 2: Health Profile ───
    val selectedBloodGroup: String = "",
    val dateOfBirth: Long? = null,
    val weight: String = "",
    val lastDonationDate: Long? = null,
    val wantsToBeDonor: Boolean = true,

    // ── Sign Up Step 3: Location ───
    val selectedDistrict: String = "",
    val selectedUpazila: String = "",

    // ── Login ───
    val loginEmail: String = "",
    val loginPassword: String = "",
    val loginPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,

    // ── OTP ───
    val otpDigits: List<String> = List(6) { "" },
    val verificationId: String = "",
    val otpTimerSeconds: Int = 60,
    val otpPhone: String = "",
    val otpSent: Boolean = false,

    // ── Forgot Password ───
    val forgotPasswordStep: Int = 0,
    val resetEmail: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val newPasswordVisible: Boolean = false,

    // ── Phone Login ───
    val phoneLoginNumber: String = "",

    // ── General State ───
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignUpComplete: Boolean = false,
    val isLoginComplete: Boolean = false,
    val isPasswordResetSuccess: Boolean = false,
    val isOnboardingComplete: Boolean = false,
    val isLoggedIn: Boolean = false,
    val needsProfileSetup: Boolean = false,
    val isInitialized: Boolean = false,

    // ── Splash navigation decision (consumed once) ───
    val splashDestination: String? = null,

    // ── Ban state ───
    val isBanned: Boolean = false,
    val banReason: String? = null,

    // ── Maintenance gate ───
    val isMaintenanceMode: Boolean = false,
    val maintenanceTitle: String = "",
    val maintenanceMessage: String = "",
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager,
    private val firestore: FirebaseFirestore,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * One-shot navigation events via Channel.
     * Each event is consumed exactly once — this prevents the reopen crash
     * caused by stale boolean flags (isLoginComplete, needsProfileSetup)
     * re-firing LaunchedEffects after the nav graph is rebuilt.
     */
    private val _navigationEvent = Channel<AuthNavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<AuthNavigationEvent> = _navigationEvent.receiveAsFlow()

    init {
        checkInitialState()
    }

    private fun fetchUserDataAndCompleteLogin() {
        viewModelScope.launch {
            try {
                val uid = authRepository.getCurrentUserId()
                if (uid != null) {
                    when (val result = userRepository.getUser(uid)) {
                        is Resource.Success -> {
                            val userData = result.data

                            // ── Ban check ──
                            if (userData.isBanned) {
                                authRepository.signOut()
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        isBanned = true,
                                        banReason = userData.banReason,
                                    )
                                }
                                _navigationEvent.send(
                                    AuthNavigationEvent.NavigateToBanned(userData.banReason),
                                )
                                return@launch
                            }

                            val needsSetup = userData.bloodGroup.isEmpty()
                            _state.update {
                                it.copy(
                                    fullName = userData.name,
                                    email = userData.email,
                                    phone = userData.phone,
                                    selectedBloodGroup = userData.bloodGroup,
                                    dateOfBirth = userData.dob?.time,
                                    weight = if (userData.weight > 0) userData.weight.toString() else "",
                                    selectedDistrict = userData.district,
                                    selectedUpazila = userData.upazila,
                                    wantsToBeDonor = userData.isDonor,
                                    isLoading = false,
                                    isLoginComplete = !needsSetup,
                                    isLoggedIn = true,
                                    needsProfileSetup = needsSetup,
                                )
                            }
                            // Emit one-shot navigation event
                            if (needsSetup) {
                                _navigationEvent.send(AuthNavigationEvent.NavigateToProfileSetup)
                            } else {
                                _navigationEvent.send(AuthNavigationEvent.NavigateToHome)
                            }
                        }
                        is Resource.Error -> {
                            // User authenticated but no Firestore doc — create a stub profile
                            // This happens for phone OTP users who just signed up
                            val currentState = _state.value
                            val phoneNumber = currentState.otpPhone.ifEmpty { currentState.phoneLoginNumber }
                            val stubUser = User(
                                uid = uid,
                                phone = phoneNumber,
                                name = currentState.fullName,
                                email = currentState.email,
                            )
                            // Create the stub in Firestore so profile setup can update it later
                            userRepository.updateUser(stubUser)
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    needsProfileSetup = true,
                                    phone = phoneNumber,
                                )
                            }
                            _navigationEvent.send(AuthNavigationEvent.NavigateToProfileSetup)
                        }
                        else -> {
                            // Firestore has no data but user is authenticated — needs profile setup
                            val currentState = _state.value
                            val phoneNumber = currentState.otpPhone.ifEmpty { currentState.phoneLoginNumber }
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    needsProfileSetup = true,
                                    phone = phoneNumber,
                                )
                            }
                            _navigationEvent.send(AuthNavigationEvent.NavigateToProfileSetup)
                        }
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Authentication failed") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun checkInitialState() {
        viewModelScope.launch {
            try {
                // ── Maintenance mode check (before auth) ──
                var isMaintenanceOn = false
                var maintenanceTitle = ""
                var maintenanceMessage = ""
                try {
                    val mDoc = firestore.document("config/maintenance").get().await()
                    isMaintenanceOn = mDoc.getBoolean("isEnabled") ?: false
                    if (isMaintenanceOn) {
                        maintenanceTitle = mDoc.getString("title") ?: ""
                        maintenanceMessage = mDoc.getString("message") ?: ""
                    }
                } catch (_: Exception) { /* fail-open: don't block users if Firestore is down */ }

                val isOnboarded = preferencesManager.isOnboardingComplete.first()
                val isLoggedIn = authRepository.isLoggedIn()
                var needsSetup = false
                var userData: User? = null
                var isBanned = false
                var banReason: String? = null
                var isSuperAdmin = false

                if (isLoggedIn) {
                    val uid = authRepository.getCurrentUserId()
                    if (uid != null) {
                        // Check if this user is the SuperAdmin (exempt from maintenance)
                        if (isMaintenanceOn) {
                            try {
                                val saDoc = firestore.document("config/superadmin").get().await()
                                isSuperAdmin = saDoc.getString("uid") == uid
                            } catch (_: Exception) { /* not SA */ }
                        }

                        when (val result = userRepository.getUser(uid)) {
                            is Resource.Success -> {
                                userData = result.data
                                // Ban check at splash
                                if (userData.isBanned) {
                                    isBanned = true
                                    banReason = userData.banReason
                                    authRepository.signOut()
                                }
                                needsSetup = userData.bloodGroup.isEmpty()
                            }
                            else -> {
                                // User exists in Firebase Auth but not in Firestore — needs setup
                                needsSetup = true
                            }
                        }
                    }
                }

                // Maintenance blocks everyone except SuperAdmin
                val showMaintenanceGate = isMaintenanceOn && !isSuperAdmin

                _state.update {
                    it.copy(
                        fullName = userData?.name ?: it.fullName,
                        email = userData?.email ?: it.email,
                        phone = userData?.phone ?: it.phone,
                        selectedBloodGroup = userData?.bloodGroup ?: it.selectedBloodGroup,
                        dateOfBirth = userData?.dob?.time ?: it.dateOfBirth,
                        weight = if (userData?.weight != null && userData.weight > 0) userData.weight.toString() else it.weight,
                        selectedDistrict = userData?.district ?: it.selectedDistrict,
                        selectedUpazila = userData?.upazila ?: it.selectedUpazila,
                        wantsToBeDonor = userData?.isDonor ?: it.wantsToBeDonor,
                        isOnboardingComplete = isOnboarded,
                        isLoggedIn = isLoggedIn && !isBanned,
                        needsProfileSetup = needsSetup,
                        isInitialized = true,
                        isBanned = isBanned,
                        banReason = banReason,
                        isMaintenanceMode = showMaintenanceGate,
                        maintenanceTitle = maintenanceTitle,
                        maintenanceMessage = maintenanceMessage,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isInitialized = true, error = e.message) }
            }
        }
    }

    // ─── Onboarding ──────────────────────────────────────────

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingComplete(true)
            _state.update { it.copy(isOnboardingComplete = true) }
        }
    }

    // ─── Sign Up Step 1 ──────────────────────────────────────

    fun updateFullName(name: String) = _state.update { it.copy(fullName = name, error = null) }
    fun updatePhone(phone: String) = _state.update { it.copy(phone = phone, error = null) }
    fun updateEmail(email: String) = _state.update { it.copy(email = email, error = null) }
    fun updatePassword(pw: String) = _state.update { it.copy(password = pw, error = null) }
    fun updateConfirmPassword(pw: String) = _state.update { it.copy(confirmPassword = pw, error = null) }
    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun toggleConfirmPasswordVisibility() = _state.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    fun toggleTermsAgreement() = _state.update { it.copy(agreedToTerms = !it.agreedToTerms) }

    fun isStep1Valid(): Boolean {
        val s = _state.value
        return s.fullName.isNotBlank() &&
            s.phone.length >= 11 &&
            s.email.contains("@") &&
            s.password.length >= 6 &&
            s.password == s.confirmPassword &&
            s.agreedToTerms
    }

    fun getPasswordStrength(): PasswordStrength {
        val pw = _state.value.password
        if (pw.length < 6) return PasswordStrength.WEAK
        val hasUpper = pw.any { it.isUpperCase() }
        val hasDigit = pw.any { it.isDigit() }
        val hasSpecial = pw.any { !it.isLetterOrDigit() }
        val score = listOf(pw.length >= 8, hasUpper, hasDigit, hasSpecial).count { it }
        return when {
            score >= 3 -> PasswordStrength.STRONG
            score >= 2 -> PasswordStrength.FAIR
            else -> PasswordStrength.WEAK
        }
    }

    // ─── Sign Up Step 2 ──────────────────────────────────────

    fun selectBloodGroup(group: String) = _state.update { it.copy(selectedBloodGroup = group, error = null) }
    fun updateDateOfBirth(millis: Long) = _state.update { it.copy(dateOfBirth = millis, error = null) }
    fun updateWeight(w: String) = _state.update { it.copy(weight = w, error = null) }
    fun updateLastDonationDate(millis: Long?) = _state.update { it.copy(lastDonationDate = millis, error = null) }
    fun toggleDonorWillingness() = _state.update { it.copy(wantsToBeDonor = !it.wantsToBeDonor) }

    fun isStep2Valid(): Boolean {
        val s = _state.value
        return s.selectedBloodGroup.isNotBlank() && s.dateOfBirth != null
    }

    fun getAge(): Int? {
        val dob = _state.value.dateOfBirth ?: return null
        val diff = System.currentTimeMillis() - dob
        return (diff / (365.25 * 24 * 60 * 60 * 1000)).toInt()
    }

    // ─── Sign Up Step 3 ──────────────────────────────────────

    fun selectDistrict(district: String) = _state.update { it.copy(selectedDistrict = district, selectedUpazila = "", error = null) }
    fun selectUpazila(upazila: String) = _state.update { it.copy(selectedUpazila = upazila, error = null) }

    fun isStep3Valid(): Boolean {
        val s = _state.value
        return s.selectedDistrict.isNotBlank() && s.selectedUpazila.isNotBlank()
    }

    fun completeSignUp() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val s = _state.value
            val user = User(
                name = s.fullName,
                phone = s.phone,
                email = s.email,
                bloodGroup = s.selectedBloodGroup,
                dob = s.dateOfBirth?.let { Date(it) },
                weight = s.weight.toFloatOrNull() ?: 0f,
                isDonor = s.wantsToBeDonor,
                lastDonationDate = s.lastDonationDate?.let { Date(it) },
                district = s.selectedDistrict,
                upazila = s.selectedUpazila,
            )

            // If user is already logged in (Google sign-in flow), update profile instead
            val existingUid = authRepository.getCurrentUserId()
            if (existingUid != null && s.isLoggedIn) {
                val updatedUser = user.copy(uid = existingUid)
                when (val result = userRepository.updateUser(updatedUser)) {
                    is Resource.Success -> {
                        _state.update { it.copy(isLoading = false, isSignUpComplete = true, isLoggedIn = true, needsProfileSetup = false) }
                        _navigationEvent.send(AuthNavigationEvent.NavigateToHome)
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                    }
                    is Resource.Loading -> {}
                }
            } else {
                when (val result = authRepository.signUpWithEmail(s.email, s.password, user)) {
                    is Resource.Success -> {
                        _state.update { it.copy(isLoading = false, isSignUpComplete = true, isLoggedIn = true) }
                        _navigationEvent.send(AuthNavigationEvent.NavigateToHome)
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    // ─── Login ───────────────────────────────────────────────

    fun updateLoginEmail(email: String) = _state.update { it.copy(loginEmail = email, error = null) }
    fun updateLoginPassword(pw: String) = _state.update { it.copy(loginPassword = pw, error = null) }
    fun toggleLoginPasswordVisibility() = _state.update { it.copy(loginPasswordVisible = !it.loginPasswordVisible) }
    fun toggleRememberMe() = _state.update { it.copy(rememberMe = !it.rememberMe) }

    fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val s = _state.value
            when (val result = authRepository.signInWithEmail(s.loginEmail, s.loginPassword)) {
                is Resource.Success -> {
                    if (s.rememberMe) {
                        preferencesManager.setRememberMe(true)
                        preferencesManager.setSavedEmail(s.loginEmail)
                    }
                    fetchUserDataAndCompleteLogin()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    fetchUserDataAndCompleteLogin()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── OTP ─────────────────────────────────────────────────

    fun updateOtpDigit(index: Int, digit: String) {
        val digits = _state.value.otpDigits.toMutableList()
        digits[index] = digit.take(1)
        _state.update { it.copy(otpDigits = digits, error = null) }
    }

    fun setVerificationId(id: String) = _state.update { it.copy(verificationId = id, otpSent = true) }
    fun setOtpPhone(phone: String) = _state.update { it.copy(otpPhone = phone) }
    fun updateOtpTimer(seconds: Int) = _state.update { it.copy(otpTimerSeconds = seconds) }

    fun updatePhoneLoginNumber(phone: String) = _state.update { it.copy(phoneLoginNumber = phone, error = null) }

    /** Clear OTP digits (used when resending or on error) */
    fun clearOtpDigits() = _state.update { it.copy(otpDigits = List(6) { "" }, error = null) }

    fun verifyOtp() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val code = _state.value.otpDigits.joinToString("")
            if (code.length != 6) {
                _state.update { it.copy(isLoading = false, error = "Please enter a valid 6-digit code") }
                return@launch
            }
            when (val result = authRepository.verifyOtp(_state.value.verificationId, code)) {
                is Resource.Success -> {
                    fetchUserDataAndCompleteLogin()
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Forgot Password ─────────────────────────────────────

    fun updateResetEmail(email: String) = _state.update { it.copy(resetEmail = email, error = null) }
    fun updateNewPassword(pw: String) = _state.update { it.copy(newPassword = pw, error = null) }
    fun updateConfirmNewPassword(pw: String) = _state.update { it.copy(confirmNewPassword = pw, error = null) }
    fun toggleNewPasswordVisibility() = _state.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }

    fun sendPasswordResetEmail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.resetPassword(_state.value.resetEmail)) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, isPasswordResetSuccess = true) }
                    _navigationEvent.send(AuthNavigationEvent.PasswordResetSuccess)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun advanceForgotPasswordStep() = _state.update { it.copy(forgotPasswordStep = it.forgotPasswordStep + 1) }

    /** Reset forgot password state when leaving the screen */
    fun resetForgotPasswordState() = _state.update {
        it.copy(
            resetEmail = "",
            newPassword = "",
            confirmNewPassword = "",
            newPasswordVisible = false,
            forgotPasswordStep = 0,
            isPasswordResetSuccess = false,
        )
    }

    // ─── Sign Out ────────────────────────────────────────────

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            preferencesManager.setRememberMe(false)
            preferencesManager.setSavedEmail("")
            _state.update { AuthState(isOnboardingComplete = true, isInitialized = true) }
            _navigationEvent.send(AuthNavigationEvent.NavigateToLogin)
        }
    }

    // ─── Utility ─────────────────────────────────────────────

    fun clearError() = _state.update { it.copy(error = null) }

    /**
     * Resets transient auth state flags to prevent stale navigation.
     * Should be called when a screen is entered.
     */
    fun resetAuthFlags() = _state.update {
        it.copy(
            isLoginComplete = false,
            isSignUpComplete = false,
            needsProfileSetup = false,
            isPasswordResetSuccess = false,
        )
    }

    // ─── Activity-bridge helpers ─────────────────────────────

    /** Called by MainActivity when Google Sign-In fails before we get an id token. */
    fun setError(msg: String) = _state.update { it.copy(error = msg, isLoading = false) }

    /** Shows the loading spinner while OTP is being sent (Activity layer). */
    fun setOtpLoading(loading: Boolean) = _state.update { it.copy(isLoading = loading) }

    /** Allows MainActivity to run a suspend block on the ViewModel's scope. */
    fun launchOnScope(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    /**
     * Signs in using a [PhoneAuthCredential] obtained from auto-verification
     * (called from MainActivity's PhoneAuthProvider callbacks).
     */
    fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Delegate to the repository's verifyOtp by reusing the credential directly
                when (val result = authRepository.signInWithPhoneCredential(credential)) {
                    is Resource.Success -> fetchUserDataAndCompleteLogin()
                    is Resource.Error   -> _state.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Sign in failed") }
            }
        }
    }
}

enum class PasswordStrength { WEAK, FAIR, STRONG }