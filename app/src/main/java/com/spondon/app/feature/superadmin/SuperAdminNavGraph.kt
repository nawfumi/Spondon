package com.spondon.app.feature.superadmin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.spondon.app.feature.superadmin.auth.SuperAdminLoginScreen
import com.spondon.app.feature.superadmin.auth.SuperAdminRegisterScreen
import com.spondon.app.feature.superadmin.broadcast.SABroadcastScreen
import com.spondon.app.feature.superadmin.community.SACommunityDetailScreen
import com.spondon.app.feature.superadmin.community.SACommunityListScreen
import com.spondon.app.feature.superadmin.dashboard.SADashboardScreen
import com.spondon.app.feature.superadmin.feedback.SAFeedbackScreen
import com.spondon.app.feature.superadmin.forceupdate.SAForceUpdateScreen
import com.spondon.app.feature.superadmin.maintenance.SAMaintenanceScreen
import com.spondon.app.feature.superadmin.users.SAUserDetailScreen
import com.spondon.app.feature.superadmin.users.SAUserListScreen
import com.spondon.app.feature.superadmin.analytics.SAAnalyticsScreen

/**
 * SuperAdmin navigation graph. Plugged into SpondonNavGraph conditionally.
 *
 * - Registration route only exists if no SA has been registered yet (self-destructs).
 * - Login is accessible via hidden deep link only.
 * - All other screens are guarded by SA role check.
 */
fun NavGraphBuilder.superAdminGraph(
    navController: NavController,
    isSARegistered: Boolean,
) {
    // Registration — available at the route level always;
    // the LoginScreen redirects here only when Firestore says no SA exists.
    composable("sa_register") {
        SuperAdminRegisterScreen(navController)
    }

    // Login — accessible via hidden deep link, not from any UI
    composable("sa_login") {
        SuperAdminLoginScreen(navController)
    }

    // Dashboard — command center
    composable("sa_dashboard") {
        SADashboardScreen(navController)
    }

    // ─── Phase 2: User Management ────────────────────────────
    composable("sa_users") {
        SAUserListScreen(navController)
    }

    composable(
        route = "sa_user_detail/{uid}",
        arguments = listOf(navArgument("uid") { type = NavType.StringType }),
    ) { backStackEntry ->
        val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
        SAUserDetailScreen(navController = navController, uid = uid)
    }

    // ─── Phase 3: Community Management ───────────────────────
    composable("sa_communities") {
        SACommunityListScreen(navController)
    }

    composable(
        route = "sa_community_detail/{communityId}",
        arguments = listOf(navArgument("communityId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val communityId = backStackEntry.arguments?.getString("communityId") ?: return@composable
        SACommunityDetailScreen(navController = navController, communityId = communityId)
    }

    // ─── Phase 3: Broadcast ──────────────────────────────────
    composable("sa_broadcast") {
        SABroadcastScreen(navController)
    }

    // ─── Phase 4: Feedback ───────────────────────────────────
    composable("sa_feedback") {
        SAFeedbackScreen(navController)
    }

    // ─── Phase 4: Maintenance Mode ──────────────────────────
    composable("sa_maintenance") {
        SAMaintenanceScreen(navController)
    }

    // ─── Phase 4: Force Update ───────────────────────────────
    composable("sa_force_update") {
        SAForceUpdateScreen(navController)
    }

    // ─── Phase 5: Analytics ──────────────────────────────────
    composable("sa_analytics") {
        SAAnalyticsScreen(navController)
    }
}
