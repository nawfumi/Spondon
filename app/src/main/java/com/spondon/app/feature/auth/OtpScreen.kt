package com.spondon.app.feature.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.AuthOverlayState
import com.spondon.app.core.ui.components.AuthStateOverlay
import com.spondon.app.core.ui.components.BloodDropLoader
import com.spondon.app.core.ui.components.SpondonButton
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OtpScreen(
    navController: NavController,
    onSendOtp: (String) -> Unit = {},
    viewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // ── Entrance animations ──
    val headerAlpha = remember { Animatable(0f) }
    val boxesAlpha = remember { Animatable(0f) }
    val boxesScale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(400))
        boxesAlpha.animateTo(1f, tween(400))
        boxesScale.animateTo(1f, tween(400, easing = EaseOutCubic))
    }

    // ── Timer ──
    var timerSeconds by remember { mutableIntStateOf(60) }
    var timerActive by remember { mutableStateOf(true) }

    LaunchedEffect(timerActive) {
        if (timerActive) {
            while (timerSeconds > 0) {
                delay(1000.milliseconds)
                timerSeconds--
            }
            timerActive = false
        }
    }

    // ── Auth overlay state ──
    var overlayState by remember { mutableStateOf(AuthOverlayState.HIDDEN) }
    var overlayMessage by remember { mutableStateOf("") }

    LaunchedEffect(state.isLoading) {
        if (state.isLoading) overlayState = AuthOverlayState.LOADING
    }

    LaunchedEffect(state.error) {
        if (state.error != null && !state.isLoading) {
            overlayMessage = state.error ?: "Verification failed"
            overlayState = AuthOverlayState.ERROR
        }
    }

    // ── One-shot navigation events ──
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToHome -> {
                    overlayState = AuthOverlayState.SUCCESS
                    delay(1200.milliseconds)
                    navController.navigate(Routes.Home.route) {
                        popUpTo("auth_flow") { inclusive = true }
                    }
                }
                is AuthNavigationEvent.NavigateToProfileSetup -> {
                    overlayState = AuthOverlayState.SUCCESS
                    delay(800.milliseconds)
                    navController.navigate(Routes.DonorProfileSetup.route) {
                        // DonorProfileSetup is INSIDE auth_flow, so just pop current screen
                        popUpTo(Routes.Otp.route) { inclusive = true }
                    }
                }
                else -> {}
            }
        }
    }

    // Error shake animation for OTP boxes
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state.error) {
        if (state.error != null) {
            repeat(3) {
                shakeOffset.animateTo(10f, tween(50))
                shakeOffset.animateTo(-10f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        BloodRed.copy(alpha = 0.02f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Header ──
            Column(
                modifier = Modifier.alpha(headerAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BloodDropLoader(size = 60.dp)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Verify Your Number",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(12.dp))

                val maskedPhone = if (state.otpPhone.length >= 4) {
                    "+880 ×××× ×${state.otpPhone.takeLast(4)}"
                } else {
                    "your phone number"
                }
                Text(
                    text = "Code sent to $maskedPhone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── 6 OTP digit boxes with bounce-in ──
            Row(
                modifier = Modifier
                    .alpha(boxesAlpha.value)
                    .offset(x = shakeOffset.value.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                state.otpDigits.forEachIndexed { index, digit ->
                    BasicTextField(
                        value = digit,
                        onValueChange = { newValue ->
                            if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                                viewModel.updateOtpDigit(index, newValue)
                                if (newValue.isNotEmpty() && index < 5) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                                if (index == 5 && newValue.isNotEmpty()) {
                                    focusManager.clearFocus()
                                    viewModel.verifyOtp()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (state.error != null) {
                                    Modifier.border(2.dp, UrgencyCritical, RoundedCornerShape(14.dp))
                                } else if (digit.isNotEmpty()) {
                                    Modifier.border(2.dp, BloodRed, RoundedCornerShape(14.dp))
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(14.dp),
                                    )
                                },
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .focusRequester(focusRequesters[index]),
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        cursorBrush = SolidColor(BloodRed),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                innerTextField()
                            }
                        },
                    )
                }
            }

            // Inline error
            if (state.error != null && overlayState == AuthOverlayState.HIDDEN) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.error!!,
                    color = UrgencyCritical,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Timer & Resend ──
            if (timerActive && timerSeconds > 0) {
                // Animated timer
                Text(
                    text = "Resend code in ${timerSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            } else {
                TextButton(
                    onClick = {
                        timerSeconds = 60
                        timerActive = true
                        viewModel.clearOtpDigits()
                        if (state.otpPhone.isNotBlank()) {
                            onSendOtp(state.otpPhone)
                        }
                    },
                ) {
                    Text("Resend Code", color = BloodRed, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Verify button ──
            SpondonButton(
                text = "Verify",
                onClick = { viewModel.verifyOtp() },
                enabled = state.otpDigits.all { it.isNotEmpty() } && !state.isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Blood-themed auth overlay ──
        AuthStateOverlay(
            state = overlayState,
            loadingText = "Verifying...",
            successText = "Verified! 🩸",
            errorText = overlayMessage,
        )

        if (overlayState == AuthOverlayState.ERROR) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        overlayState = AuthOverlayState.HIDDEN
                        viewModel.clearError()
                    },
            )
        }
    }
}