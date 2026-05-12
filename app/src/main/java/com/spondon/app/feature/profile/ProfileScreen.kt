package com.spondon.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.profileState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    val s = S.strings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.myProfile, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.EditProfile.route) }) {
                        Icon(Icons.Outlined.Edit, s.editProfile)
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
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedLoadingIndicator()
                }
            }

            state.user != null -> {
                val user = state.user!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ─── Avatar + Name ─────────────────────
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box {
                                if (user.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = user.avatarUrl,
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
                                                    listOf(BloodRed.copy(alpha = 0.15f), SoftRose.copy(alpha = 0.1f)),
                                                ),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Filled.Person, null, tint = BloodRed.copy(alpha = 0.6f), modifier = Modifier.size(52.dp))
                                    }
                                }
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = BloodRed,
                                    shadowElevation = 4.dp,
                                ) {
                                    Text(
                                        user.bloodGroup.ifBlank { "?" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(user.name.ifBlank { "User" }, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                            if (user.district.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        buildString { append(user.district); if (user.upazila.isNotBlank()) append(", ${user.upazila}") },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }

                    // ─── Stats Row ─────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(IntrinsicSize.Max),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatItem("${user.totalDonations}", s.totalDonations, Icons.Outlined.VolunteerActivism,
                                onClick = { navController.navigate(Routes.DonationHistory.route) }, modifier = Modifier.weight(1f).fillMaxHeight())
                            StatItem("${user.communityIds.size}", s.communities, Icons.Outlined.Groups,
                                onClick = { navController.navigate(Routes.CommunityList.route) }, modifier = Modifier.weight(1f).fillMaxHeight())
                            StatItem("${user.badges.size}", s.achievements, Icons.Outlined.EmojiEvents,
                                onClick = { navController.navigate(Routes.Achievements.route) }, modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }

                    // ─── Availability Toggle ──────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isAvailable) AvailableGreen.copy(alpha = 0.08f) else UnavailableGrey.copy(alpha = 0.08f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (state.isAvailable) s.availableToDonate else s.unavailable,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (state.isAvailable) AvailableGreen else UnavailableGrey,
                                    )
                                    if (!state.isAvailable && state.cooldownDaysRemaining > 0) {
                                        Text(
                                            s.availableIn.replace("%d", state.cooldownDaysRemaining.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = UnavailableGrey.copy(alpha = 0.7f),
                                        )
                                    }
                                    if (user.availabilityOverride) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = PendingAmber.copy(alpha = 0.15f),
                                            modifier = Modifier.padding(top = 4.dp),
                                        ) {
                                            Text("Admin override", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = PendingAmber)
                                        }
                                    }
                                }
                                Switch(
                                    checked = state.isAvailable,
                                    onCheckedChange = null,
                                    enabled = false,
                                    colors = SwitchDefaults.colors(checkedTrackColor = AvailableGreen, checkedThumbColor = Color.White),
                                )
                            }
                        }
                    }

                    // ─── My Communities ────────────────────
                    if (state.communities.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                Text(s.myCommunities, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.communities) { community ->
                                        Surface(
                                            modifier = Modifier.clickable { navController.navigate("community_detail/${community.id}") },
                                            shape = RoundedCornerShape(12.dp),
                                            color = BloodRed.copy(alpha = 0.08f),
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.Groups, null, modifier = Modifier.size(16.dp), tint = BloodRed)
                                                Spacer(Modifier.width(6.dp))
                                                Text(community.name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = BloodRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─── Quick Links ──────────────────────
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Text(s.quickLinks, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.height(8.dp))
                            QuickLinkItem(Icons.Outlined.History, s.donationHistory, onClick = { navController.navigate(Routes.DonationHistory.route) })
                            QuickLinkItem(Icons.Outlined.Bloodtype, s.myRequests, onClick = { navController.navigate(Routes.RequestFeed.route) })
                            QuickLinkItem(Icons.Outlined.EmojiEvents, s.achievements, onClick = { navController.navigate(Routes.Achievements.route) })
                            QuickLinkItem(Icons.Outlined.Notifications, s.notifications, onClick = { navController.navigate(Routes.Notifications.route) })
                            QuickLinkItem(Icons.Outlined.Settings, s.settings, onClick = { navController.navigate(Routes.Settings.route) })
                            QuickLinkItem(Icons.Outlined.Info, "About", onClick = { navController.navigate(Routes.About.route) })
                            QuickLinkItem(Icons.Filled.VolunteerActivism, s.supportDeveloper, onClick = { navController.navigate(Routes.Support.route) })
                        }
                    }

                    // ─── App Footer ───────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "স্পন্দন — Spondon",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = BloodRed.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Made with ❤ by Ash",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            ) {
            Icon(icon, null, tint = BloodRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickLinkItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = BloodRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        }
    }
}
