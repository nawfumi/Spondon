package com.spondon.app.feature.request

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.BloodRequest
import com.spondon.app.core.domain.model.Urgency
import com.spondon.app.core.ui.components.TipOfTheDayCard
import com.spondon.app.core.ui.i18n.LocalAppLanguage
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.DarkRose
import com.spondon.app.core.ui.theme.PendingAmber
import com.spondon.app.core.ui.theme.SoftRose
import com.spondon.app.core.ui.theme.UrgencyCritical
import com.spondon.app.core.ui.theme.UrgencyModerate
import com.spondon.app.core.ui.theme.UrgencyNormal
import com.spondon.app.feature.donor.TipsViewModel
import com.spondon.app.feature.notification.NotificationViewModel
import com.spondon.app.navigation.Routes

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: RequestViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    tipsViewModel: TipsViewModel = hiltViewModel(),
) {
    val state by viewModel.homeState.collectAsState()
    val notificationState by notificationViewModel.state.collectAsState()
    val tipsState by tipsViewModel.state.collectAsState()
    val currentLanguage = LocalAppLanguage.current

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

        // ─── Tip of the Day ─────────────────────────────
        if (tipsState.tipOfTheDay != null && !tipsState.tipDismissed) {
            item {
                TipOfTheDayCard(
                    tip = tipsState.tipOfTheDay!!,
                    language = currentLanguage,
                    onSeeAllTips = { navController.navigate(Routes.TipsLibrary.route) },
                    onDismiss = { tipsViewModel.dismissTipOfTheDay() },
                )
                Spacer(Modifier.height(8.dp))
            }
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
                    onClick = { navController.navigate(Routes.TipsLibrary.route) },
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
                    currentUserId = state.user?.uid,
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
 * Redesigned request card with colored badge chips.
 * Used on the Home screen, Feed, and Community detail feed.
 */
@Composable
fun RequestCard(
    modifier: Modifier = Modifier,
    request: BloodRequest,
    currentUserId: String? = null,
    onClick: () -> Unit,
) {
    val urgencyColor = when (request.urgency) {
        Urgency.CRITICAL -> UrgencyCritical
        Urgency.MODERATE -> UrgencyModerate
        Urgency.NORMAL   -> UrgencyNormal
    }
    val urgencyText = when (request.urgency) {
        Urgency.CRITICAL -> "CRITICAL"
        Urgency.MODERATE -> "URGENT"
        Urgency.NORMAL   -> "NORMAL"
    }

    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }
    val timeFormat = remember { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()) }
    val donationDateStr = request.donationDateTime?.let { dateFormat.format(it) } ?: "Not set"
    val donationTimeStr = request.donationDateTime?.let { timeFormat.format(it) } ?: "Not set"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Requester Name and Post Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        val displayName = if (request.requesterId == currentUserId) {
                            "${request.requesterName.ifBlank { "Unknown" }} (you)"
                        } else {
                            request.requesterName.ifBlank { "Unknown Requester" }
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Posted: ${RequestViewModel.getRelativeTime(request.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val context = androidx.compose.ui.platform.LocalContext.current
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                menuExpanded = false
                                val shareText = "🩸 ${request.bloodGroup} blood needed at ${request.hospital}. ${request.unitsNeeded} unit(s) required. Help save a life!"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Request"))
                            },
                            leadingIcon = { Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp)) },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Info") },
                            onClick = {
                                menuExpanded = false
                                val copyText = "Blood Group: ${request.bloodGroup}\nHospital: ${request.hospital}\nUnits: ${request.unitsNeeded}\nUrgency: ${request.urgency.name}\nContact: ${request.contactNumber}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Request Info", copyText))
                            },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Grid of InfoChipRows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChipRow(
                    icon = Icons.Filled.Bloodtype,
                    label = "Blood Group",
                    value = request.bloodGroup.ifBlank { "?" },
                    valueColor = BloodRed,
                    valueBg = BloodRed.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                )
                InfoChipRow(
                    icon = Icons.Filled.Warning,
                    label = "Urgency",
                    value = urgencyText,
                    valueColor = urgencyColor,
                    valueBg = urgencyColor.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChipRow(
                    icon = Icons.Filled.WaterDrop,
                    label = "Quantity",
                    value = "${request.unitsNeeded} Bag${if (request.unitsNeeded > 1) "s" else ""}",
                    valueColor = MaterialTheme.colorScheme.primary,
                    valueBg = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                )
                InfoChipRow(
                    icon = Icons.Filled.LocalHospital,
                    label = "Hospital",
                    value = request.hospital.ifBlank { "Not specified" },
                    valueColor = MaterialTheme.colorScheme.secondary,
                    valueBg = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChipRow(
                    icon = Icons.Filled.CalendarMonth,
                    label = "Donation Date",
                    value = donationDateStr,
                    valueColor = MaterialTheme.colorScheme.tertiary,
                    valueBg = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                InfoChipRow(
                    icon = Icons.Filled.AccessTime,
                    label = "Time",
                    value = donationTimeStr,
                    valueColor = MaterialTheme.colorScheme.tertiary,
                    valueBg = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun InfoChipRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    valueBg: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = valueColor
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = valueBg
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

