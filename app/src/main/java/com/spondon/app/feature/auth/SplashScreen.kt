package com.spondon.app.feature.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.BloodDropLoader
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsState()

    // Local guard: ensures we only navigate ONCE per SplashScreen composition.
    // This prevents re-navigation when the ViewModel's isInitialized is already
    // true from a prior session (e.g. process-kept Activity ViewModel).
    var hasNavigated by remember { mutableStateOf(false) }

    // Entrance animation values
    val logoAlpha = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.6f) }

    // Use LaunchedEffect(Unit) so it runs exactly once per composition.
    // Inside, we wait until isInitialized is true, then navigate.
    LaunchedEffect(Unit) {
        // Animate logo regardless of init state
        logoAlpha.animateTo(1f, animationSpec = tween(600))
        logoScale.animateTo(1f, animationSpec = tween(500, easing = EaseOutCubic))
        nameAlpha.animateTo(1f, animationSpec = tween(500))
        taglineAlpha.animateTo(1f, animationSpec = tween(500))
        delay(600)

        // Poll until ViewModel has finished checking auth + Firestore.
        // If it was already initialized (ViewModel survived), this is instant.
        while (!viewModel.state.value.isInitialized) {
            delay(100)
        }

        if (hasNavigated) return@LaunchedEffect
        hasNavigated = true

        val currentState = viewModel.state.value
        val destination = when {
            currentState.isMaintenanceMode -> "maintenance_gate"
            currentState.isBanned -> "banned/${java.net.URLEncoder.encode(currentState.banReason ?: "none", "UTF-8")}"
            !currentState.isOnboardingComplete -> Routes.Onboarding.route
            currentState.isLoggedIn && !currentState.needsProfileSetup -> Routes.Home.route
            currentState.isLoggedIn && currentState.needsProfileSetup -> Routes.DonorProfileSetup.route
            else -> Routes.Login.route
        }

        // Home, Banned, and Maintenance gate are OUTSIDE auth_flow → pop the entire auth graph.
        // All other destinations are INSIDE auth_flow → just pop the splash screen.
        if (destination == Routes.Home.route || destination.startsWith("banned/") || destination == "maintenance_gate") {
            navController.navigate(destination) {
                popUpTo("auth_flow") { inclusive = true }
            }
        } else {
            navController.navigate(destination) {
                popUpTo(Routes.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        BloodRed.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background,
                    ),
                    radius = 800f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // Animated blood drop logo
            BloodDropLoader(
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value),
                size = 120.dp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App name in Bangla — keep on a single line, prevent word wrapping
            Text(
                text = "স্পন্দন",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp,
                ),
                color = BloodRed,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .alpha(nameAlpha.value)
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // App name in English
            Text(
                text = "Spondon",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 4.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(nameAlpha.value)
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline
            Text(
                text = "রক্ত দিন, জীবন বাঁচান",
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .fillMaxWidth(),
            )
        }
    }
}