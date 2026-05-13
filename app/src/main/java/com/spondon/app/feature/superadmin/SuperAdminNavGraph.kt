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
import com.spondon.app.feature.superadmin.users.SAUserDetailScreen
import com.spondon.app.feature.superadmin.users.SAUserListScreen

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
    // Registration — self-destructing route (only if no SA exists yet)
    if (!isSARegistered) {
        composable("sa_register") {
            SuperAdminRegisterScreen(navController)
        }
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

    // ─── Phase 4+: Additional screens will be registered here ──
    // composable("sa_feedback")    { SAFeedbackScreen(navController) }
    // composable("sa_analytics")   { SAAnalyticsScreen(navController) }
    // composable("sa_maintenance") { SAMaintenanceScreen(navController) }
}
