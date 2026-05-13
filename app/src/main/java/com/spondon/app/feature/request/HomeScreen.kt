package com.spondon.app.feature.request

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.BloodRequest
import com.spondon.app.core.domain.model.Urgency
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes
import com.spondon.app.feature.notification.NotificationViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: RequestViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
) {
    val state by viewModel.homeState.collectAsState()
    val notificationState by notificationViewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadHome() }

    val s = S.strings

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // ─── Top Bar ─────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Welcome, ${state.userName}!",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = s.homeTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }

                IconButton(
                    onClick = { navController.navigate(Routes.Notifications.route) },
                ) {
                    BadgedBox(
                        badge = {
                            if (notificationState.unreadCount > 0) {
                                Badge(containerColor = BloodRed) {
                                    Text("${notificationState.unreadCount}", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = s.notifications,
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }

        // ─── Hero Banner ─────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BloodRed, DarkRose, BloodRed.copy(alpha = 0.9f)),
                            ),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(24.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "রক্ত দিন,জীবন বাঁচান",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 34.sp,
                                ),
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (s == com.spondon.app.core.ui.i18n.Bn) "প্রতিটি ফোঁটা গুরুত্বপূর্ণ।" else "Every drop counts. Be someone's hero today.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            Icons.Filled.Bloodtype,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ─── Community Filter Chips ──────────────────────────
        if (state.communities.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.selectedCommunityFilter == null,
                        onClick = { viewModel.filterByCommunity(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BloodRed,
                            selectedLabelColor = Color.White,
                        ),
                    )
                    state.communities.forEach { community ->
                        FilterChip(
                            selected = state.selectedCommunityFilter == community.id,
                            onClick = { viewModel.filterByCommunity(community.id) },
                            label = {
                                Text(
                                    community.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BloodRed,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ─── Stats Row ───────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    icon = Icons.Outlined.Groups,
                    label = s.nearbyDonors,
                    value = "${state.totalDonors}",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                StatCard(
                    icon = Icons.Outlined.CheckCircle,
                    label = s.fulfilled,
                    value = "${state.fulfilledRequests}",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                StatCard(
                    icon = Icons.Outlined.Pending,
                    label = s.pending,
                    value = "${state.pendingRequests}",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // ─── Quick Actions Grid ──────────────────────────────
        item {
            Text(
                text = s.quickActions,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickActionCard(
                    icon = Icons.Filled.Bloodtype,
                    label = if (s == com.spondon.app.core.ui.i18n.Bn) "রক্ত\nচাই" else "Request\nBlood",
                    color = BloodRed,
                    onClick = { navController.navigate(Routes.CreateRequest.route) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                QuickActionCard(
                    icon = Icons.Filled.Search,
                    label = if (s == com.spondon.app.core.ui.i18n.Bn) "ডোনার\nখুঁজুন" else "Find\nDonor",
                    color = SoftRose,
                    onClick = { navController.navigate(Routes.FindDonor.route) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                QuickActionCard(
                    icon = Icons.Filled.VolunteerActivism,
                    label = if (s == com.spondon.app.core.ui.i18n.Bn) "ডোনেশন\nটিপস" else "Donation\nTips",
                    color = AvailableGreen,
                    onClick = { /* Tips screen */ },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                QuickActionCard(
                    icon = Icons.Filled.People,
                    label = s.communities,
                    color = PendingAmber,
                    onClick = { navController.navigate(Routes.CommunityList.route) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // ─── Urgent Requests Section ─────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = s.urgentRequests,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = { navController.navigate(Routes.RequestFeed.route) }) {
                    Text(s.viewAll, color = BloodRed)
                }
            }
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedLoadingIndicator()
                }
            }
        } else if (state.urgentRequests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = AvailableGreen,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No active blood requests",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        } else {
            items(state.urgentRequests.take(10), key = { it.id }) { request ->
                RequestCard(
                    request = request,
                    onClick = {
                        navController.navigate("request_detail/${request.id}")
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Reusable sub-components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = BloodRed,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 14.sp,
                    fontSize = 10.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Modern request card: accent bar + blood-group circle + info column.
 * Used on the Home screen, Feed, and Community detail feed.
 */
@Composable
fun RequestCard(
    request: BloodRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val urgencyColor = when (request.urgency) {
        Urgency.CRITICAL -> UrgencyCritical
        Urgency.MODERATE -> UrgencyModerate
        Urgency.NORMAL   -> UrgencyNormal
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // ── Left accent bar ────────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(urgencyColor, urgencyColor.copy(alpha = 0.4f)),
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
            )

            // ── Card content ──────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
            ) {

                // ── Row 1: blood group circle + info + chevron ─
                Row(verticalAlignment = Alignment.CenterVertically) {

                    // Blood group circle
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(urgencyColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = request.bloodGroup.ifBlank { "?" },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                            ),
                            color = urgencyColor,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Community name label
                        if (request.communityName.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = urgencyColor.copy(alpha = 0.7f),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = request.communityName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp,
                                        fontSize = 9.sp,
                                    ),
                                    color = urgencyColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                        // Patient name (primary)
                        Text(
                            text = request.patientName?.takeIf { it.isNotBlank() }
                                ?: "Patient",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // Requester name
                        if (request.requesterName.isNotBlank()) {
                            Text(
                                text = "Requested by: ${request.requesterName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        // Hospital row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.LocalHospital,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = request.hospital.ifBlank { "Hospital not specified" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // Urgency chip + chevron stacked vertically
                    Column(horizontalAlignment = Alignment.End) {
                        UrgencyTag(request.urgency)
                        Spacer(Modifier.height(6.dp))
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Row 2: info strip ──────────────────────────
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Location
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Outlined.LocationOn, null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = request.address.ifBlank { request.hospital.ifBlank { "\u2014" } },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Text(
                            "\u00b7",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )

                        // Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.AccessTime, null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = RequestViewModel.getRelativeTime(request.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }

                        Text(
                            "\u00b7",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )

                        // Respondents
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.People, null,
                                modifier = Modifier.size(12.dp),
                                tint = if (request.respondents.isNotEmpty())
                                    AvailableGreen.copy(alpha = 0.8f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${request.respondents.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (request.respondents.isNotEmpty())
                                        FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = if (request.respondents.isNotEmpty())
                                    AvailableGreen
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UrgencyTag(urgency: Urgency) {
    val (color, text, icon) = when (urgency) {
        Urgency.CRITICAL -> Triple(UrgencyCritical, "CRITICAL", Icons.Filled.Warning)
        Urgency.MODERATE -> Triple(UrgencyModerate, "URGENT", Icons.Filled.Schedule)
        Urgency.NORMAL -> Triple(UrgencyNormal, "NORMAL", Icons.Outlined.CheckCircle)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = color,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                ),
                color = color,
            )
        }
    }
}
