package com.spondon.app.feature.superadmin.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.feature.superadmin.auth.SuperAdminAuthViewModel

private val SAGold = Color(0xFFFFD700)
private val SADark = Color(0xFF0D0D0D)
private val SADarkCard = Color(0xFF1A1A2E)
private val SAGreen = Color(0xFF4CAF50)
private val SARed = Color(0xFFEF5350)
private val SABlue = Color(0xFF42A5F5)
private val SAOrange = Color(0xFFFFA726)
private val SAPurple = Color(0xFFAB47BC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SADashboardScreen(
    navController: NavController,
    viewModel: SADashboardViewModel = hiltViewModel(),
    authViewModel: SuperAdminAuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Block system back — must logout explicitly
    BackHandler { showLogoutDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = SAGold,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SuperAdmin",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Outlined.Logout, contentDescription = "Logout", tint = SARed)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SADark,
                ),
            )
        },
        containerColor = SADark,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ─── Welcome Header ──────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Card(
                                shape = RoundedCornerShape(50),
                                colors = CardDefaults.cardColors(
                                    containerColor = SAGold.copy(alpha = 0.1f),
                                ),
                            ) {
                                Icon(
                                    Icons.Outlined.AdminPanelSettings,
                                    null,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(28.dp),
                                    tint = SAGold,
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Command Center",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color.White,
                                )
                                Text(
                                    "Full platform control • All actions logged",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SAGold.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }

                // ─── Real-Time Stats Grid ────────────────────
                item {
                    Text(
                        "PLATFORM STATS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                        ),
                        color = SAGold.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                // Stats row 1
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SAStatCard(
                            icon = Icons.Outlined.People,
                            label = "Total Users",
                            value = "${state.totalUsers}",
                            color = SABlue,
                            modifier = Modifier.weight(1f),
                        )
                        SAStatCard(
                            icon = Icons.Outlined.Groups,
                            label = "Communities",
                            value = "${state.totalCommunities}",
                            color = SAPurple,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Stats row 2
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SAStatCard(
                            icon = Icons.Outlined.Bloodtype,
                            label = "Active Requests",
                            value = "${state.activeRequests}",
                            color = SARed,
                            modifier = Modifier.weight(1f),
                        )
                        SAStatCard(
                            icon = Icons.Outlined.VolunteerActivism,
                            label = "Donations",
                            value = "${state.totalDonations}",
                            color = SAGreen,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Stats row 3
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SAStatCard(
                            icon = Icons.Outlined.Block,
                            label = "Banned Users",
                            value = "${state.bannedUsers}",
                            color = SAOrange,
                            modifier = Modifier.weight(1f),
                        )
                        SAStatCard(
                            icon = Icons.Outlined.PersonAdd,
                            label = "New Today",
                            value = "${state.newUsersToday}",
                            color = SAGold,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ─── Management Quick Actions ────────────────
                item {
                    Text(
                        "MANAGEMENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                        ),
                        color = SAGold.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SAQuickAction(
                            icon = Icons.Outlined.People,
                            label = "Users",
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("sa_users") },
                        )
                        SAQuickAction(
                            icon = Icons.Outlined.Groups,
                            label = "Communities",
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("sa_communities") },
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SAQuickAction(
                            icon = Icons.Outlined.Campaign,
                            label = "Broadcast",
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("sa_broadcast") },
                        )
                        SAQuickAction(
                            icon = Icons.Outlined.Feedback,
                            label = "Feedback",
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("sa_feedback") },
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SAQuickAction(
                            icon = Icons.Outlined.Build,
                            label = "Maintenance",
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("sa_maintenance") },
                        )
                        SAQuickAction(
                            icon = Icons.Outlined.SystemUpdate,
                            label = "Force Update",
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("sa_force_update") },
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SAQuickAction(
                            icon = Icons.Outlined.Analytics,
                            label = "Analytics",
                            modifier = Modifier.weight(1f),
                            onClick = { /* Phase 5 */ },
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }

                // ─── System Status ───────────────────────────
                item {
                    Text(
                        "SYSTEM STATUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                        ),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SAStatusRow(
                                "Maintenance Mode",
                                if (state.isMaintenanceOn) "ON" else "OFF",
                                if (state.isMaintenanceOn) SARed else SAGreen,
                            )
                            Spacer(Modifier.height(10.dp))
                            SAStatusRow(
                                "Force Update",
                                state.forceUpdateVersion ?: "Not Set",
                                if (state.forceUpdateVersion != null) SAOrange else Color(0xFF9E9E9E),
                            )
                            Spacer(Modifier.height(10.dp))
                            SAStatusRow(
                                "Pending Feedback",
                                "${state.pendingFeedback}",
                                if (state.pendingFeedback > 0) SAOrange else SAGreen,
                            )
                            Spacer(Modifier.height(10.dp))
                            SAStatusRow(
                                "System Health",
                                "Operational",
                                SAGreen,
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // ─── Logout confirmation dialog ──────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = SADarkCard,
            title = {
                Text("Logout from SuperAdmin?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    "You will be signed out of the admin panel and returned to the normal user experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                        // Navigate back to about screen, clear SA backstack
                        navController.navigate("about") {
                            popUpTo("sa_dashboard") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SARed),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            },
        )
    }
}

@Composable
private fun SAStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SADarkCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = color.copy(alpha = 0.1f),
                    ),
                ) {
                    Icon(
                        icon,
                        null,
                        tint = color,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun SAQuickAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SADarkCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = SAGold, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun SAStatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.15f),
            ),
        ) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = color,
            )
        }
    }
}
