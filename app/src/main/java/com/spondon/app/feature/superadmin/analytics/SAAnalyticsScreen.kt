package com.spondon.app.feature.superadmin.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

// ─── Theme Colors ────────────────────────────────────────────
private val SAGold = Color(0xFFFFD700)
private val SADark = Color(0xFF0D0D0D)
private val SADarkCard = Color(0xFF1A1A2E)
private val SAGreen = Color(0xFF4CAF50)
private val SARed = Color(0xFFEF5350)
private val SABlue = Color(0xFF42A5F5)
private val SAOrange = Color(0xFFFFA726)
private val SAPurple = Color(0xFFAB47BC)
private val SACyan = Color(0xFF26C6DA)
private val SAPink = Color(0xFFEC407A)
private val SALime = Color(0xFFCDDC39)
private val SATeal = Color(0xFF26A69A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAAnalyticsScreen(
    navController: NavController,
    viewModel: SAAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Analytics, null, tint = SAGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analytics", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAnalytics() }) {
                        Icon(Icons.Outlined.Refresh, "Refresh", tint = SAGold.copy(alpha = 0.7f))
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SADark),
            )
        },
        containerColor = SADark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Aggregating platform data…",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ═══════════════════════════════════════════════
                // CRITICAL ALERTS (if any)
                // ═══════════════════════════════════════════════
                if (state.criticalAlerts.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SARed.copy(alpha = 0.08f)),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Warning, null,
                                        tint = SARed, modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "CRITICAL ALERTS",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                        ),
                                        color = SARed,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Card(
                                        shape = RoundedCornerShape(6.dp),
                                        colors = CardDefaults.cardColors(containerColor = SARed.copy(alpha = 0.2f)),
                                    ) {
                                        Text(
                                            "${state.criticalAlerts.size}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SARed,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                state.criticalAlerts.forEach { alert ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = SARed.copy(alpha = 0.04f)),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(Icons.Outlined.Bloodtype, null, Modifier.size(14.dp), tint = SARed.copy(alpha = 0.6f))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "${alert["bloodGroup"]} — ${alert["hospital"]}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                ">12h",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = SARed.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Unfulfilled CRITICAL requests older than 12 hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.3f),
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════
                // FULFILLMENT RATE + TOTAL REQUESTS
                // ═══════════════════════════════════════════════
                item {
                    Text(
                        "OVERVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val rate = if (state.totalRequestCount > 0)
                            (state.fulfilledCount * 100.0 / state.totalRequestCount)
                        else 0.0
                        val rateColor = when {
                            rate >= 70 -> SAGreen
                            rate >= 40 -> SAOrange
                            else -> SARed
                        }

                        AnalyticsStatCard(
                            icon = Icons.Outlined.CheckCircle,
                            label = "Fulfillment Rate",
                            value = "${String.format("%.1f", rate)}%",
                            subtitle = "${state.fulfilledCount} / ${state.totalRequestCount} requests",
                            color = rateColor,
                            modifier = Modifier.weight(1f),
                        )
                        AnalyticsStatCard(
                            icon = Icons.Outlined.Groups,
                            label = "Communities",
                            value = "${state.totalCommunities}",
                            subtitle = "Avg ${String.format("%.1f", state.avgMembersPerCommunity)} members",
                            color = SAPurple,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ═══════════════════════════════════════════════
                // SIGNUPS BAR CHART (7 days)
                // ═══════════════════════════════════════════════
                item {
                    Text(
                        "NEW SIGNUPS (7 DAYS)",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val total = state.signupsPerDay.values.sum()
                            Text(
                                "$total new users this week",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                            Spacer(Modifier.height(14.dp))
                            SimpleBarChart(
                                data = state.signupsPerDay,
                                barColor = SABlue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════════
                // REQUEST URGENCY BREAKDOWN
                // ═══════════════════════════════════════════════
                item {
                    Text(
                        "REQUEST URGENCY",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val urgencyData = state.requestsByUrgency
                        UrgencyCard("Normal", urgencyData["NORMAL"] ?: 0, SAGreen, Modifier.weight(1f))
                        UrgencyCard("Urgent", urgencyData["URGENT"] ?: 0, SAOrange, Modifier.weight(1f))
                        UrgencyCard("Critical", urgencyData["CRITICAL"] ?: 0, SARed, Modifier.weight(1f))
                    }
                }

                // ═══════════════════════════════════════════════
                // BLOOD GROUP DISTRIBUTION
                // ═══════════════════════════════════════════════
                item {
                    Text(
                        "BLOOD GROUP REQUESTS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (state.requestsByBloodGroup.isEmpty()) {
                                Text(
                                    "No request data available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.3f),
                                )
                            } else {
                                SimpleBarChart(
                                    data = state.requestsByBloodGroup,
                                    barColor = SARed,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════
                // TOP DISTRICTS
                // ═══════════════════════════════════════════════
                item {
                    Text(
                        "TOP DISTRICTS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (state.topDistricts.isEmpty()) {
                                Text(
                                    "No district data available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.3f),
                                )
                            } else {
                                val maxCount = state.topDistricts.maxOfOrNull { it.second } ?: 1
                                state.topDistricts.forEachIndexed { index, (district, count) ->
                                    RankedRow(
                                        rank = index + 1,
                                        label = district,
                                        value = "$count users",
                                        progress = count.toFloat() / maxCount,
                                        color = when (index) {
                                            0 -> SAGold
                                            1 -> Color(0xFFC0C0C0)
                                            2 -> Color(0xFFCD7F32)
                                            else -> SABlue.copy(alpha = 0.6f)
                                        },
                                    )
                                    if (index < state.topDistricts.lastIndex) {
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════
                // TOP DONORS
                // ═══════════════════════════════════════════════
                item {
                    Text(
                        "TOP DONORS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (state.topDonors.isEmpty()) {
                                Text(
                                    "No donation data available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.3f),
                                )
                            } else {
                                val maxDonations = state.topDonors.maxOfOrNull { it.second } ?: 1
                                state.topDonors.forEachIndexed { index, (name, count) ->
                                    RankedRow(
                                        rank = index + 1,
                                        label = name,
                                        value = "$count donations",
                                        progress = count.toFloat() / maxDonations,
                                        color = when (index) {
                                            0 -> SAGold
                                            1 -> Color(0xFFC0C0C0)
                                            2 -> Color(0xFFCD7F32)
                                            else -> SAGreen.copy(alpha = 0.6f)
                                        },
                                    )
                                    if (index < state.topDonors.lastIndex) {
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════
                // COMMUNITY STATS
                // ═══════════════════════════════════════════════
                item {
                    Text(
                        "COMMUNITIES",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            CommunityStatRow("Total Communities", "${state.totalCommunities}", SAPurple)
                            Spacer(Modifier.height(8.dp))
                            CommunityStatRow(
                                "Avg Members",
                                String.format("%.1f", state.avgMembersPerCommunity),
                                SABlue,
                            )
                            Spacer(Modifier.height(8.dp))
                            CommunityStatRow("Largest", state.largestCommunity, SAGold)
                        }
                    }
                }

                // ═══════════════════════════════════════════════
                // CRASHLYTICS LINK
                // ═══════════════════════════════════════════════
                item {
                    Text(
                        "PLATFORM HEALTH",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Outlined.BugReport, null,
                                modifier = Modifier.size(32.dp),
                                tint = SAOrange.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Crashlytics Dashboard",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Crash reports, stack traces, and device breakdown are available in the Firebase Console. " +
                                        "Direct API integration requires a server proxy for secure access.",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = Color.White.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = SAOrange.copy(alpha = 0.1f)),
                            ) {
                                Text(
                                    "console.firebase.google.com",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = SAOrange,
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Reusable Components
// ══════════════════════════════════════════════════════════════

@Composable
private fun AnalyticsStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SADarkCard),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
            ) {
                Icon(
                    icon, null,
                    tint = color,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun UrgencyCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun RankedRow(
    rank: Int,
    label: String,
    value: String,
    progress: Float,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank badge
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (rank <= 3) color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
            ),
        ) {
            Text(
                "#$rank",
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (rank <= 3) color else Color.White.copy(alpha = 0.4f),
            )
        }
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
            Spacer(Modifier.height(4.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .background(color.copy(alpha = 0.6f), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun CommunityStatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
        )
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        ) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
            )
        }
    }
}

/**
 * A simple bar chart drawn with Canvas. Labels below each bar.
 */
@Composable
private fun SimpleBarChart(
    data: Map<String, Int>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return
    val entries = data.entries.toList()
    val maxValue = (entries.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)

    Column(modifier = modifier) {
        // Chart area
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val barCount = entries.size
            val totalGap = 6.dp.toPx() * (barCount - 1).coerceAtLeast(0)
            val barWidth = ((size.width - totalGap) / barCount).coerceAtLeast(8f)
            val gap = if (barCount > 1) 6.dp.toPx() else 0f

            entries.forEachIndexed { index, entry ->
                val barHeight = if (maxValue > 0) {
                    (entry.value.toFloat() / maxValue) * (size.height - 4.dp.toPx())
                } else 0f
                val x = index * (barWidth + gap)
                val y = size.height - barHeight.coerceAtLeast(2.dp.toPx())

                drawRoundRect(
                    color = barColor.copy(alpha = 0.7f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight.coerceAtLeast(2.dp.toPx())),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            entries.forEach { entry ->
                Text(
                    entry.key,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Value labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            entries.forEach { entry ->
                Text(
                    "${entry.value}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = barColor.copy(alpha = 0.5f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
