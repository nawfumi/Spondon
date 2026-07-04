package com.spondon.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Column
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.spondon.app.core.data.local.PreferencesManager
import com.spondon.app.core.ui.i18n.LocalAppLanguage
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.SpondonTheme
import com.spondon.app.core.util.BiometricHelper
import com.spondon.app.feature.auth.AuthViewModel
import com.spondon.app.navigation.SpondonNavGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.compose.material3.AlertDialog
import com.spondon.app.feature.update.UpdateManager
import com.spondon.app.feature.update.UpdateViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.spondon.app.core.util.NetworkConnectivityObserver

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    // ── Firebase Auth ──────────────────────────────────────────
    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var preferencesManager: PreferencesManager

    // Activity-scoped ViewModel shared with all screens via hiltViewModel()
    private val authViewModel: AuthViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    private lateinit var updateManager: UpdateManager


    @Inject lateinit var networkObserver: NetworkConnectivityObserver

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

    // ── Credential Manager (modern Google Sign-In replacement) ─
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        credentialManager = CredentialManager.create(this)
        updateManager = UpdateManager(this)


        updateViewModel.checkForUpdates(BuildConfig.VERSION_NAME)

        // Read initial prefs synchronously (DataStore is fast for read-first)
        val initialDarkMode = runBlocking { preferencesManager.isDarkMode.first() }
        val initialLanguage = runBlocking { preferencesManager.language.first() }
        val initialBiometric = runBlocking { preferencesManager.isBiometricEnabled.first() }

        setContent {
            // Observe language & dark mode reactively
            val darkMode by preferencesManager.isDarkMode.collectAsState(initial = initialDarkMode)
            val language by preferencesManager.language.collectAsState(initial = initialLanguage)
            val biometricEnabled by preferencesManager.isBiometricEnabled.collectAsState(initial = initialBiometric)

            // Biometric gate
            var biometricPassed by remember {
                mutableStateOf(
                    !initialBiometric || !BiometricHelper.canAuthenticate(this@MainActivity)
                )
            }
            var biometricError by remember { mutableStateOf<String?>(null) }

            // Show biometric prompt on launch if enabled
            LaunchedEffect(biometricEnabled) {
                if (biometricEnabled && BiometricHelper.canAuthenticate(this@MainActivity) && !biometricPassed) {
                    BiometricHelper.showBiometricPrompt(
                        activity = this@MainActivity,
                        title = "স্পন্দন — Spondon",
                        subtitle = if (language == "bn") "আপনার পরিচয় যাচাই করুন"
                        else "Verify your identity to continue",
                        negativeButtonText = if (language == "bn") "বাতিল" else "Cancel",
                        onSuccess = {
                            biometricPassed = true
                            biometricError = null
                        },
                        onError = { msg ->
                            biometricError = if (msg == "CANCELLED") {
                                // Allow user to retry
                                if (language == "bn") "আবার চেষ্টা করুন" else "Try again"
                            } else {
                                msg
                            }
                        },
                    )
                }
            }

            CompositionLocalProvider(LocalAppLanguage provides language) {
                SpondonTheme(darkTheme = darkMode) {
                    val updateInfo by updateViewModel.updateInfo.collectAsState()
                    val isCheckingUpdate by updateViewModel.isChecking.collectAsState()
                    val isUpToDate by updateViewModel.isUpToDate.collectAsState()

                    // Startup auto-check dialog — only shown once on launch
                    var startupDialogShown by remember { mutableStateOf(false) }
                    if (!startupDialogShown && updateInfo != null) {
                        val info = updateInfo!!
                        AlertDialog(
                            onDismissRequest = {
                                startupDialogShown = true
                                updateViewModel.dismissUpdate()
                            },
                            title = { Text(if (language == "bn") "আপডেট উপলব্ধ" else "Update Available") },
                            text = {
                                Text(
                                    if (language == "bn") "ভার্সন ${info.version} ডাউনলোডের জন্য প্রস্তুত। আপনি কি আপডেট করতে চান?"
                                    else "Version ${info.version} is available. Would you like to update?"
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    updateManager.downloadUpdate(info.downloadUrl)
                                    startupDialogShown = true
                                    updateViewModel.dismissUpdate()
                                }) {
                                    Text(if (language == "bn") "ডাউনলোড" else "Download", color = BloodRed)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    startupDialogShown = true
                                    updateViewModel.dismissUpdate()
                                }) {
                                    Text(if (language == "bn") "পরে" else "Later")
                                }
                            }
                        )
                    }

                    if (!biometricPassed && biometricEnabled && BiometricHelper.canAuthenticate(this@MainActivity)) {
                        // Biometric lock screen
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Fingerprint,
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                        tint = BloodRed.copy(alpha = 0.6f),
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        if (language == "bn") "বায়োমেট্রিক প্রমাণীকরণ প্রয়োজন"
                                        else "Biometric authentication required",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    if (biometricError != null) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = biometricError!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BloodRed,
                                        )
                                    }
                                    Spacer(Modifier.height(24.dp))
                                    TextButton(onClick = {
                                        BiometricHelper.showBiometricPrompt(
                                            activity = this@MainActivity,
                                            title = "স্পন্দন — Spondon",
                                            subtitle = if (language == "bn") "আপনার পরিচয় যাচাই করুন"
                                            else "Verify your identity to continue",
                                            negativeButtonText = if (language == "bn") "বাতিল" else "Cancel",
                                            onSuccess = {
                                                biometricPassed = true
                                                biometricError = null
                                            },
                                            onError = { msg ->
                                                biometricError = if (msg == "CANCELLED") {
                                                    if (language == "bn") "আবার চেষ্টা করুন" else "Try again"
                                                } else msg
                                            },
                                        )
                                    }) {
                                        Text(
                                            if (language == "bn") "আনলক করুন" else "Unlock",
                                            color = BloodRed,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Main app - pass network state to NavGraph
                        val isConnected by networkObserver.isConnected
                            .collectAsState(initial = true)

                        SpondonNavGraph(
                            authViewModel = authViewModel,
                            onGoogleSignIn = { launchGoogleSignIn() },
                            onSendOtp = { phone -> sendOtp(phone) },
                            updateAvailable = updateInfo,
                            isCheckingUpdate = isCheckingUpdate,
                            isUpToDate = isUpToDate,
                            onCheckForUpdate = {
                                updateViewModel.checkForUpdates(BuildConfig.VERSION_NAME)
                            },
                            onDownloadUpdate = { url ->
                                updateManager.downloadUpdate(url)
                                updateViewModel.dismissUpdate()
                            },
                            onDismissUpdate = { updateViewModel.dismissUpdate() },
                            onClearUpToDate = { updateViewModel.clearUpToDateFlag() },
                            isConnected = isConnected,
                        )
                    }
                }
            }
        }

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Ensure FCM token is saved in Firestore on every launch.
        // onNewToken() doesn't fire every time, so we proactively fetch
        // and store the current token so the Cloud Function can reach this device.
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val uid = firebaseAuth.currentUser?.uid ?: return@addOnSuccessListener
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("fcmToken", token)
        }
        
        // Subscribe to global announcements topic for Spondon posts & SuperAdmin broadcasts
        FirebaseMessaging.getInstance().subscribeToTopic("global_announcements")
    }

    // ── Google Sign-In via Credential Manager ──────────────────

    private fun launchGoogleSignIn() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Step 1: Try GetGoogleIdOption (fast, reuses existing credential)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity,
                )
                handleGoogleSignInResult(result)

            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                // Step 2: Fallback — use GetSignInWithGoogleOption (full bottom sheet)
                Log.w("GoogleSignIn", "No saved credential, falling back to Sign-In button flow")
                try {
                    val signInOption = GetSignInWithGoogleOption.Builder(
                        getString(R.string.default_web_client_id)
                    ).build()

                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(signInOption)
                        .build()

                    val result = credentialManager.getCredential(
                        request = fallbackRequest,
                        context = this@MainActivity,
                    )
                    handleGoogleSignInResult(result)

                } catch (fallbackEx: GetCredentialException) {
                    Log.e("GoogleSignIn", "Fallback also failed", fallbackEx)
                    authViewModel.setError(
                        "Google Sign-In failed. Please make sure you have a Google account " +
                                "added to your device and try again."
                    )
                }
            } catch (e: GetCredentialException) {
                Log.e("GoogleSignIn", "Credential request failed", e)
                val userMessage = when {
                    e.message?.contains("cancel", ignoreCase = true) == true ->
                        "Google Sign-In was cancelled."
                    else ->
                        "Google Sign-In failed. Please try again."
                }
                authViewModel.setError(userMessage)
            }
        }
    }

    private fun handleGoogleSignInResult(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        authViewModel.signInWithGoogle(idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("GoogleSignIn", "Invalid Google ID token", e)
                        authViewModel.setError("Google Sign-In failed: invalid token")
                    }
                } else {
                    Log.e("GoogleSignIn", "Unexpected credential type: ${credential.type}")
                    authViewModel.setError("Google Sign-In failed: unexpected credential type")
                }
            }
            else -> {
                Log.e("GoogleSignIn", "Unexpected credential class: ${credential.javaClass}")
                authViewModel.setError("Google Sign-In failed: unexpected credential")
            }
        }
    }

    // ── Phone (OTP) Auth ───────────────────────────────────────

    /**
     * Sends an OTP to [phoneNumber] using Firebase PhoneAuthProvider.
     * The verification ID is stored in AuthViewModel for later verifyOtp() use.
     */
    private fun sendOtp(phoneNumber: String) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification (instant verify on device) — sign in directly
                authViewModel.signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("PhoneAuth", "onVerificationFailed", e)
                authViewModel.setError(e.message ?: "OTP send failed")
                authViewModel.setOtpLoading(false)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                authViewModel.setVerificationId(verificationId)
                authViewModel.setOtpPhone(phoneNumber)
                authViewModel.setOtpLoading(false)
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        authViewModel.setOtpLoading(true)
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
