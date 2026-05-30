package com.spondon.app.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController

/**
 * Legacy onboarding entry point.
 * Redirects to the new multi-screen onboarding flow.
 */
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: AuthViewModel,
) {
    LaunchedEffect(Unit) {
        navController.navigate("initial_setup") {
            popUpTo("onboarding") { inclusive = true }
        }
    }
}