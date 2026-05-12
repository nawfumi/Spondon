package com.spondon.app.feature.donor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorProfileScreen(
    navController: NavController,
    viewModel: DonorViewModel = hiltViewModel(),
) {
    val state by viewModel.profileState.collectAsState()
    val userId = navController.currentBackStackEntry
        ?.arguments?.getString("userId") ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.loadDonorProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donor Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                    ContainedLoadingIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null,
                            tint = UrgencyCritical,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(state.error ?: "Error loading profile")
                    }
                }
            }

            state.donor != null -> {
                val donor = state.donor!!
                val dateFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ─── Profile Header ──────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Avatar with blood group badge
                            Box {
                                if (donor.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = donor.avatarUrl,
                                        contentDescription = "Profile picture",
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        BloodRed.copy(alpha = 0.15f),
                                                        SoftRose.copy(alpha = 0.1f),
                                                    ),
                                                ),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Person,
                                            null,
                                            tint = BloodRed.copy(alpha = 0.6f),
                                            modifier = Modifier.size(52.dp),
                                        )
                                    }
                                }
                                // Blood group badge
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 4.dp, y = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = BloodRed,
                                    shadowElevation = 4.dp,
                                ) {
                                    Text(
                                        text = donor.bloodGroup.ifBlank { "?" },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = donor.name.ifBlank { "Unknown" },
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )

                            if (donor.district.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Outlined.LocationOn,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = buildString {
                                            append(donor.district)
                                            if (donor.upazila.isNotBlank()) append(", ${donor.upazila}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }

                    // ─── Stats Row ───────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ProfileStatCard(
                                value = "${donor.totalDonations}",
                                label = "Donations",
                                icon = Icons.Outlined.VolunteerActivism,
                                modifier = Modifier.weight(1f),
                            )
                            ProfileStatCard(
                                value = "${donor.communityIds.size}",
                                label = "Communities",
                                icon = Icons.Outlined.Groups,
                                modifier = Modifier.weight(1f),
                            )
                            ProfileStatCard(
                                value = donor.createdAt?.let { dateFormat.format(it) } ?: "—",
                                label = "Joined",
                                icon = Icons.Outlined.CalendarMonth,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // ─── Availability Status ─────────────────
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isAvailable)
                                    AvailableGreen.copy(alpha = 0.08f)
                                else
                                    UnavailableGrey.copy(alpha = 0.08f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (state.isAvailable) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        null,
                                        tint = AvailableGreen,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Available to Donate",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = AvailableGreen,
                                        )
                                        Text(
                                            "This donor is ready to help",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AvailableGreen.copy(alpha = 0.7f),
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Filled.Lock,
                                        null,
                                        tint = UnavailableGrey,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Unavailable",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = UnavailableGrey,
                                        )
                                        Text(
                                            "${state.cooldownDaysRemaining} days remaining in cooldown",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = UnavailableGrey.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ─── Send Donation Request ───────────────
                    if (state.sharedCommunities.isNotEmpty()) {
                        item {
                            Button(
                                onClick = {
                                    navController.navigate("create_request")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                            ) {
                                Icon(Icons.Filled.Bloodtype, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Send Donation Request",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }

                    // ─── Shared Communities ───────────────────
                    if (state.sharedCommunities.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp),
                            ) {
                                Text(
                                    "Shared Communities",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(state.sharedCommunities) { community ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BloodRed.copy(alpha = 0.08f),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 14.dp,
                                                    vertical = 8.dp,
                                                ),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Groups,
                                                    null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = BloodRed,
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    community.name,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                    ),
                                                    color = BloodRed,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─── Donation History (public) ────────────
                    if (state.donationHistory.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp),
                            ) {
                                Text(
                                    "Recent Donations",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        items(state.donationHistory) { donation ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(BloodRed.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.VolunteerActivism,
                                            null,
                                            tint = BloodRed,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            donation.hospital.ifBlank { "Hospital" },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                            ),
                                        )
                                        donation.date?.let {
                                            val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                            Text(
                                                df.format(it),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            )
                                        }
                                    }
                                    // No patient details shown (public)
                                }
                            }
                        }
                    }

                    // ─── Badges Display ──────────────────────
                    if (donor.badges.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp),
                            ) {
                                Text(
                                    "Badges",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(donor.badges) { badgeId ->
                                        val emoji = when (badgeId) {
                                            "first_drop" -> "🩸"
                                            "life_saver" -> "💉"
                                            "hero_donor" -> "🦸"
                                            "legend" -> "🏆"
                                            "champion" -> "👑"
                                            "century" -> "💯"
                                            else -> "🏅"
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = PendingAmber.copy(alpha = 0.15f),
                                        ) {
                                            Text(
                                                emoji,
                                                modifier = Modifier.padding(10.dp),
                                                fontSize = 20.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                null,
                tint = BloodRed,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
    }
}
