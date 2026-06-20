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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                    // ─── Profile Header Card ────────────────
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // Avatar with blood group badge
                                Box {
                                    if (donor.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = donor.avatarUrl,
                                            contentDescription = "Profile picture",
                                            modifier = Modifier
                                                .size(88.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(88.dp)
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
                                                modifier = Modifier.size(48.dp),
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

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = donor.name.ifBlank { "Unknown" },
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = buildString {
                                                append(donor.district)
                                                if (donor.upazila.isNotBlank()) append(", ${donor.upazila}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ─── Stats Row ───────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ProfileStatCard(
                                value = "${donor.totalDonations}",
                                label = "Donations",
                                icon = Icons.Outlined.VolunteerActivism,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            ProfileStatCard(
                                value = "${donor.communityIds.size}",
                                label = "Communities",
                                icon = Icons.Outlined.Groups,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            ProfileStatCard(
                                value = donor.createdAt?.let { dateFormat.format(it) } ?: "—",
                                label = "Joined",
                                icon = Icons.Outlined.CalendarMonth,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }

                    // ─── Availability Status ─────────────────
                    if (!viewModel.shouldHideForUser(userId)) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Status icon in colored circle
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (state.isAvailable)
                                                    AvailableGreen.copy(alpha = 0.1f)
                                                else
                                                    UnavailableGrey.copy(alpha = 0.1f)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            if (state.isAvailable) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                                            null,
                                            tint = if (state.isAvailable) AvailableGreen else UnavailableGrey,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (state.isAvailable) "Available to Donate" else "Unavailable",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = if (state.isAvailable) AvailableGreen else UnavailableGrey,
                                        )
                                        Text(
                                            if (state.isAvailable)
                                                "This donor is ready to help"
                                            else
                                                "${state.cooldownDaysRemaining} days remaining in cooldown",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (state.isAvailable)
                                                AvailableGreen.copy(alpha = 0.7f)
                                            else
                                                UnavailableGrey.copy(alpha = 0.7f),
                                        )
                                    }
                                    // Status badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (state.isAvailable)
                                            AvailableGreen.copy(alpha = 0.1f)
                                        else
                                            UnavailableGrey.copy(alpha = 0.1f),
                                    ) {
                                        Text(
                                            text = if (state.isAvailable) "READY" else "COOLDOWN",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (state.isAvailable) AvailableGreen else UnavailableGrey,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Privacy mode - show a private notice
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(UnavailableGrey.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Lock,
                                            null,
                                            tint = UnavailableGrey,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Private",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = UnavailableGrey,
                                        )
                                        Text(
                                            "Availability status is hidden by admin",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = UnavailableGrey.copy(alpha = 0.7f),
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = UnavailableGrey.copy(alpha = 0.1f),
                                    ) {
                                        Text(
                                            text = "PRIVATE",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = UnavailableGrey,
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
                                        letterSpacing = 0.5.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
                    if (state.donationHistory.isNotEmpty() && !viewModel.shouldHideForUser(userId)) {
                        item {
                            Text(
                                "Recent Donations",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }

                        items(state.donationHistory) { donation ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BloodRed.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.VolunteerActivism,
                                            null,
                                            tint = BloodRed,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            donation.hospital.ifBlank { "Hospital" },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        donation.date?.let {
                                            val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                            Text(
                                                df.format(it),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    // Donation badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AvailableGreen.copy(alpha = 0.1f),
                                    ) {
                                        Text(
                                            text = "Donated",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AvailableGreen,
                                        )
                                    }
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
                                        letterSpacing = 0.5.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
                                            shape = RoundedCornerShape(12.dp),
                                            color = PendingAmber.copy(alpha = 0.1f),
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
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                null,
                tint = BloodRed,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
