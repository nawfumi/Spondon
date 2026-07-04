package com.spondon.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.firestore.FirebaseFirestore
import com.spondon.app.core.ui.components.SpondonBottomNav
import com.spondon.app.core.ui.components.bottomNavItems
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.feature.auth.AuthViewModel
import com.spondon.app.feature.auth.DonorProfileSetupScreen
import com.spondon.app.feature.auth.ForgotPasswordScreen
import com.spondon.app.feature.auth.LocationSetupScreen
import com.spondon.app.feature.auth.LoginScreen
import com.spondon.app.feature.auth.OnboardingScreen
import com.spondon.app.feature.auth.OtpScreen
import com.spondon.app.feature.auth.PermissionsScreen
import com.spondon.app.feature.auth.PhoneLoginScreen
import com.spondon.app.feature.auth.PrivacyPolicyScreen
import com.spondon.app.feature.auth.SignUpScreen
import com.spondon.app.feature.auth.SplashScreen
import com.spondon.app.feature.auth.TermsOfServiceScreen
import com.spondon.app.feature.community.AdminDashboardScreen
import com.spondon.app.feature.community.CommunityDetailScreen
import com.spondon.app.feature.community.CommunityInfoScreen
import com.spondon.app.feature.community.CommunityListScreen
import com.spondon.app.feature.community.CreateCommunityScreen
import com.spondon.app.feature.community.CreateSpondonPostScreen
import com.spondon.app.feature.community.EditCommunityScreen
import com.spondon.app.feature.community.JoinRequestScreen
import com.spondon.app.feature.community.SpondonCommunityScreen
import com.spondon.app.feature.community.SpondonInfoScreen
import com.spondon.app.feature.donor.AchievementsScreen
import com.spondon.app.feature.donor.DonationHistoryScreen
import com.spondon.app.feature.donor.DonorProfileScreen
import com.spondon.app.feature.donor.FindDonorScreen
import com.spondon.app.feature.donor.TipsLibraryScreen
import com.spondon.app.feature.feedback.SendFeedbackScreen
import com.spondon.app.feature.notification.NotificationDetailScreen
import com.spondon.app.feature.notification.NotificationScreen
import com.spondon.app.feature.onboarding.EligibilityQuizScreen
import com.spondon.app.feature.onboarding.InitialSetupScreen
import com.spondon.app.feature.onboarding.OnboardingCompleteScreen
import com.spondon.app.feature.onboarding.TipsPreviewScreen
import com.spondon.app.feature.onboarding.WelcomeScreen
import com.spondon.app.feature.profile.EditProfileScreen
import com.spondon.app.feature.profile.ProfileScreen
import com.spondon.app.feature.request.CreateRequestScreen
import com.spondon.app.feature.request.HomeScreen
import com.spondon.app.feature.request.RequestDetailScreen
import com.spondon.app.feature.request.RequestFeedScreen
import com.spondon.app.feature.settings.AboutScreen
import com.spondon.app.feature.settings.SecuritySettingsScreen
import com.spondon.app.feature.settings.SettingsScreen
import com.spondon.app.feature.superadmin.auth.BannedScreen
import com.spondon.app.feature.superadmin.maintenance.MaintenanceGateScreen
import com.spondon.app.feature.superadmin.superAdminGraph
import com.spondon.app.feature.support.SupportScreen
import com.spondon.app.feature.update.UpdateInfo
import kotlinx.coroutines.tasks.await

private val safeEnter: EnterTransition =
    fadeIn(animationSpec = tween(300, easing = LinearEasing))

private val safeExit: ExitTransition =
    fadeOut(animationSpec = tween(250, easing = LinearEasing))

@Composable
fun SpondonNavGraph(
    authViewModel: AuthViewModel,
    onGoogleSignIn: () -> Unit = {},
    onSendOtp: (String) -> Unit = {},
    // Update-related
    updateAvailable: UpdateInfo? = null,
    isCheckingUpdate: Boolean = false,
    isUpToDate: Boolean? = null,
    onCheckForUpdate: () -> Unit = {},
    onDownloadUpdate: (String) -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onClearUpToDate: () -> Unit = {},
    // Network connectivity
    isConnected: Boolean = true,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ─── SuperAdmin registration status (checked once) ─────
    var isSARegistered by remember { mutableStateOf(true) } // default true = hide registration
    LaunchedEffect(Unit) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .document("config/superadmin").get().await()
            isSARegistered = doc.exists() && doc.getBoolean("registered") == true
        } catch (_: Exception) {
            isSARegistered = true // fail-safe: don't expose registration
        }
    }

    val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()
    val showBottomBar = currentRoute in bottomNavRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            bottomBar = {
                if (showBottomBar) {
                    SpondonBottomNav(
                        currentRoute = currentRoute ?: "",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "auth_flow",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                enterTransition = { safeEnter },
                exitTransition = { safeExit },
                popEnterTransition = { safeEnter },
                popExitTransition = { safeExit },
            ) {
            // ─── Auth Flow (nested graph) ────────────────────
            navigation(
                startDestination = Routes.Splash.route,
                route = "auth_flow",
                enterTransition = { safeEnter },
                exitTransition = { safeExit },
                popEnterTransition = { safeEnter },
                popExitTransition = { safeExit },
            ) {
                composable(Routes.Splash.route) {
                    SplashScreen(navController, authViewModel)
                }
                composable(Routes.Onboarding.route) {
                    OnboardingScreen(navController, authViewModel)
                }
                composable(Routes.Login.route) {
                    LoginScreen(
                        navController = navController,
                        onGoogleSignIn = onGoogleSignIn,
                        onSendOtp = onSendOtp,
                        viewModel = authViewModel,
                    )
                }
                composable(Routes.SignUp.route) {
                    SignUpScreen(
                        navController = navController,
                        onGoogleSignIn = onGoogleSignIn,
                        viewModel = authViewModel,
                    )
                }
                composable(Routes.DonorProfileSetup.route) {
                    DonorProfileSetupScreen(navController, authViewModel)
                }
                composable(Routes.LocationSetup.route) {
                    LocationSetupScreen(navController, authViewModel)
                }
                composable(Routes.PhoneLogin.route) {
                    PhoneLoginScreen(
                        navController = navController,
                        onSendOtp = onSendOtp,
                        viewModel = authViewModel,
                    )
                }
                composable(Routes.Otp.route) { entry ->
                    val phone = entry.arguments?.getString("phone") ?: ""
                    if (phone.isNotEmpty()) {
                        authViewModel.setOtpPhone(phone)
                    }
                    OtpScreen(
                        navController = navController,
                        onSendOtp = onSendOtp,
                        viewModel = authViewModel,
                    )
                }
                composable(Routes.ForgotPassword.route) {
                    ForgotPasswordScreen(navController, authViewModel)
                }
                composable(Routes.Permissions.route) {
                    PermissionsScreen(navController)
                }
                composable(Routes.TermsOfService.route) {
                    TermsOfServiceScreen(navController)
                }
                composable(Routes.PrivacyPolicy.route) {
                    PrivacyPolicyScreen(navController)
                }

                // ─── New Onboarding Flow ─────────────────────
                composable(Routes.InitialSetup.route) {
                    InitialSetupScreen(navController)
                }
                composable(Routes.OnboardingWelcome.route) {
                    WelcomeScreen(navController)
                }
                composable(Routes.OnboardingQuiz.route) {
                    EligibilityQuizScreen(navController)
                }
                composable(Routes.OnboardingTipsPreview.route) {
                    TipsPreviewScreen(navController)
                }
                composable(Routes.OnboardingComplete.route) {
                    OnboardingCompleteScreen(navController)
                }
            }

            // ─── Main (Bottom Nav Destinations) ──────────────
            composable(Routes.Home.route) { HomeScreen(navController) }
            composable(Routes.CommunityList.route) { CommunityListScreen(navController) }
            composable(Routes.CreateRequest.route) { CreateRequestScreen(navController) }
            composable(Routes.FindDonor.route) { FindDonorScreen(navController) }
            composable(Routes.Profile.route) { ProfileScreen(navController) }

            // ─── Community Sub-screens ────────────────────────
            composable(
                route = Routes.CommunityDetail.route,
                arguments = listOf(navArgument("communityId") { type = NavType.StringType }),
            ) {
                CommunityDetailScreen(navController)
            }

            composable(Routes.CreateCommunity.route) {
                CreateCommunityScreen(navController)
            }

            composable(
                route = Routes.EditCommunity.route,
                arguments = listOf(navArgument("communityId") { type = NavType.StringType }),
            ) {
                EditCommunityScreen(navController)
            }

            composable(
                route = Routes.JoinRequest.route,
                arguments = listOf(navArgument("communityId") { type = NavType.StringType }),
            ) {
                JoinRequestScreen(navController)
            }

            composable(
                route = Routes.AdminDashboard.route,
                arguments = listOf(navArgument("communityId") { type = NavType.StringType }),
            ) {
                AdminDashboardScreen(navController)
            }

            // ─── Spondon Community ────────────────────────────
            composable(Routes.SpondonCommunity.route) {
                SpondonCommunityScreen(navController)
            }
            composable(Routes.CreateSpondonPost.route) {
                CreateSpondonPostScreen(navController)
            }

            // ─── Info Screens (Members/About/Manage) ────────────
            composable(
                route = Routes.CommunityInfo.route,
                arguments = listOf(navArgument("communityId") { type = NavType.StringType }),
            ) {
                CommunityInfoScreen(navController)
            }
            composable(Routes.SpondonInfo.route) {
                SpondonInfoScreen(navController)
            }

            // ─── Blood Request Sub-screens ───────────────────
            composable(
                route = Routes.RequestDetail.route,
                arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
            ) {
                RequestDetailScreen(navController)
            }
            composable(Routes.RequestFeed.route) { RequestFeedScreen(navController) }

            // ─── Donor Sub-screens ───────────────────────────
            composable(
                route = Routes.DonorProfile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) {
                DonorProfileScreen(navController)
            }
            composable(Routes.DonationHistory.route) { DonationHistoryScreen(navController) }
            composable(Routes.Achievements.route) { AchievementsScreen(navController) }

            // ─── Tips ────────────────────────────────────────
            composable(Routes.TipsLibrary.route) { TipsLibraryScreen(navController) }

            // ─── Profile Sub-screens ─────────────────────────
            composable(Routes.EditProfile.route) { EditProfileScreen(navController) }

            // ─── Settings & Notifications ────────────────────
            composable(Routes.Settings.route) { SettingsScreen(navController) }
            composable(Routes.SecuritySettings.route) { SecuritySettingsScreen(navController) }
            composable(Routes.Notifications.route) { NotificationScreen(navController) }
            composable(
                route = Routes.NotificationDetail.route,
                arguments = listOf(navArgument("notificationId") { type = NavType.StringType }),
            ) {
                NotificationDetailScreen(navController)
            }
            composable(Routes.Support.route) { SupportScreen(navController) }
            composable(Routes.SendFeedback.route) { SendFeedbackScreen(navController) }
            composable(Routes.About.route) {
                AboutScreen(
                    navController = navController,
                    updateAvailable = updateAvailable,
                    isCheckingUpdate = isCheckingUpdate,
                    isUpToDate = isUpToDate,
                    onCheckForUpdate = onCheckForUpdate,
                    onDownloadUpdate = onDownloadUpdate,
                    onDismissUpdate = onDismissUpdate,
                    onClearUpToDate = onClearUpToDate,
                )
            }

            // ─── Maintenance Gate ─────────────────────────────
            composable("maintenance_gate") {
                val authState by authViewModel.state.collectAsState()
                MaintenanceGateScreen(
                    title = authState.maintenanceTitle,
                    message = authState.maintenanceMessage,
                )
            }

            // ─── Ban Gate ─────────────────────────────────────
            composable("banned/{reason}") { entry ->
                val reason = entry.arguments?.getString("reason")
                BannedScreen(
                    banReason = if (reason == "none") null else reason,
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Routes.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            // ─── SuperAdmin (gitignored in production) ───────
            superAdminGraph(navController, isSARegistered)
        }
        }

        // ─── No Internet Blocking Overlay ─────────────────────
        if (!isConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ) // Block all interactions
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = BloodRed,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Internet Connection",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please check your internet connection and try again",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
