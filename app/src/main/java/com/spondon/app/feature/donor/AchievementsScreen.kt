package com.spondon.app.feature.donor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    navController: NavController,
    viewModel: DonorViewModel = hiltViewModel(),
) {
    val state by viewModel.achievementsState.collectAsState()
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }

    LaunchedEffect(Unit) { viewModel.loadAchievements() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Badges & Achievements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BloodRed, strokeWidth = 2.dp)
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ─── Stats Header ────────────────────────
                    item(span = { GridItemSpan(2) }) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(BloodRed, DarkRose, BloodRed.copy(alpha = 0.8f)),
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                    )
                                    .padding(24.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            "Your Achievements",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = Color.White,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "${state.totalDonations} total donations",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        val earned = state.badges.count { it.earnedDate != null }
                                        Text(
                                            "$earned / ${state.badges.size} badges earned",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PendingAmber,
                                        )
                                    }
                                    Icon(
                                        Icons.Filled.EmojiEvents,
                                        null,
                                        tint = PendingAmber.copy(alpha = 0.4f),
                                        modifier = Modifier.size(56.dp),
                                    )
                                }
                            }
                        }
                    }

                    // ─── Progress ────────────────────────────
                    item(span = { GridItemSpan(2) }) {
                        val earned = state.badges.count { it.earnedDate != null }
                        val progress = if (state.badges.isNotEmpty())
                            earned.toFloat() / state.badges.size
                        else 0f

                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Progress",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                )
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = BloodRed,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = BloodRed,
                                trackColor = BloodRed.copy(alpha = 0.1f),
                            )
                        }
                    }

                    // ─── Badge Grid ──────────────────────────
                    items(state.badges) { badge ->
                        BadgeCard(
                            badge = badge,
                            totalDonations = state.totalDonations,
                            onClick = { selectedBadge = badge },
                        )
                    }
                }
            }
        }
    }

    // ─── Badge Detail Modal ──────────────────────────────────
    if (selectedBadge != null) {
        val badge = selectedBadge!!
        val isEarned = badge.earnedDate != null
        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        badge.icon,
                        fontSize = 28.sp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        badge.name,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            text = {
                Column {
                    Text(
                        badge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Requirement: ${badge.criteria} donation${if (badge.criteria > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                    if (isEarned) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AvailableGreen.copy(alpha = 0.1f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = AvailableGreen,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Earned",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = AvailableGreen,
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        val remaining = badge.criteria - state.totalDonations
                        Text(
                            "$remaining more donation${if (remaining > 1) "s" else ""} needed",
                            style = MaterialTheme.typography.bodySmall,
                            color = PendingAmber,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBadge = null }) {
                    Text("Close", color = BloodRed)
                }
            },
        )
    }
}

@Composable
private fun BadgeCard(
    badge: Badge,
    totalDonations: Int,
    onClick: () -> Unit,
) {
    val isEarned = badge.earnedDate != null
    val progress = (totalDonations.toFloat() / badge.criteria).coerceIn(0f, 1f)

    val bgColor by animateColorAsState(
        targetValue = if (isEarned)
            PendingAmber.copy(alpha = 0.08f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        label = "badgeBg",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isEarned) 2.dp else 0.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .then(
                    if (!isEarned) Modifier.alpha(0.5f) else Modifier,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Badge icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEarned) PendingAmber.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isEarned) {
                    Text(badge.icon, fontSize = 28.sp)
                } else {
                    Icon(
                        Icons.Filled.Lock,
                        null,
                        tint = UnavailableGrey.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                badge.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "${badge.criteria} donation${if (badge.criteria > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )

            if (!isEarned) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = BloodRed,
                    trackColor = BloodRed.copy(alpha = 0.1f),
                )
            }
        }
    }
}