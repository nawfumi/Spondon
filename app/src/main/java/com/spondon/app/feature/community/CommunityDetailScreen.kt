package com.spondon.app.feature.community

import com.spondon.app.core.common.formatDisplay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.core.domain.model.*
import com.spondon.app.core.ui.components.AvailabilityIndicator
import com.spondon.app.core.ui.components.BloodGroupBadge
import com.spondon.app.core.ui.components.RoleBadge
import com.spondon.app.core.ui.components.StatCard
import com.spondon.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsState()

    // Extract communityId from navController back stack
    val communityId = navController.currentBackStackEntry
        ?.arguments?.getString("communityId") ?: ""

    LaunchedEffect(communityId) {
        if (communityId.isNotEmpty()) viewModel.loadCommunityDetail(communityId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityEvent.ShowSnackbar -> { /* handled by snackbar host */ }
                is CommunityEvent.NavigateBack -> navController.popBackStack()
                else -> {}
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is CommunityEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val isAdminOrMod = state.currentUserRole == CommunityRole.ADMIN ||
                    state.currentUserRole == CommunityRole.MODERATOR
            if (isAdminOrMod && state.community != null) {
                FloatingActionButton(
                    onClick = { navController.navigate("admin_dashboard/$communityId") },
                    containerColor = BloodRed,
                    contentColor = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Dashboard",
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedLoadingIndicator()
                }
            }
            state.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            }
            state.community != null -> {
                val community = state.community!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // ─── Cover Header ────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                        ) {
                            // Cover image
                            if (community.coverUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = community.coverUrl,
                                    contentDescription = community.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(BloodRed, DarkRose, BloodRed.copy(alpha = 0.6f)),
                                            )
                                        ),
                                )
                            }

                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                            startY = 80f,
                                        )
                                    ),
                            )

                            // Back button
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.4f),
                                ),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }

                            // Membership status chip
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = when (state.membershipStatus) {
                                    MembershipStatus.JOINED -> AvailableGreen
                                    MembershipStatus.PENDING -> PendingAmber
                                    MembershipStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant
                                },
                            ) {
                                Text(
                                    text = when (state.membershipStatus) {
                                        MembershipStatus.JOINED -> "✓ Member"
                                        MembershipStatus.PENDING -> "⏳ Pending"
                                        MembershipStatus.NONE -> "Not Joined"
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (state.membershipStatus) {
                                        MembershipStatus.JOINED -> Color.White
                                        MembershipStatus.PENDING -> Color.Black
                                        MembershipStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }

                            // Community info at bottom of header
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                            ) {
                                Text(
                                    text = community.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                if (community.district.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.White.copy(alpha = 0.7f),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            buildString {
                                                append(community.district)
                                                if (community.upazila.isNotEmpty()) append(" · ${community.upazila}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ─── Stats Row ───────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatCard(
                                label = "Members",
                                value = "${community.memberCount}",
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                label = "Donations",
                                value = "${community.donationCount}",
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                label = "Active Requests",
                                value = "0", // Will be populated in Phase 4
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // ─── Action Buttons ──────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Join / Leave button
                            when (state.membershipStatus) {
                                MembershipStatus.NONE -> {
                                    Button(
                                        onClick = {
                                            if (community.type == CommunityType.PUBLIC) {
                                                viewModel.joinPublicCommunity(communityId)
                                            } else {
                                                navController.navigate("join_request/$communityId")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Icon(Icons.Default.GroupAdd, contentDescription = null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (community.type == CommunityType.PUBLIC) "Join" else "Request to Join")
                                    }
                                }
                                MembershipStatus.JOINED -> {
                                    OutlinedButton(
                                        onClick = { viewModel.leaveCommunity(communityId) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Text("Leave Community")
                                    }
                                }
                                MembershipStatus.PENDING -> {
                                    OutlinedButton(
                                        onClick = {},
                                        modifier = Modifier.weight(1f),
                                        enabled = false,
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Text("⏳ Pending Approval")
                                    }
                                }
                            }
                        }
                    }

                    // ─── Tab Row ─────────────────────────
                    item {
                        Spacer(Modifier.height(8.dp))
                        TabRow(
                            selectedTabIndex = state.selectedTab,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = BloodRed,
                        ) {
                            Tab(
                                selected = state.selectedTab == 0,
                                onClick = { viewModel.setDetailTab(0) },
                                text = { Text("Feed") },
                            )
                            Tab(
                                selected = state.selectedTab == 1,
                                onClick = { viewModel.setDetailTab(1) },
                                text = { Text("Members") },
                            )
                            Tab(
                                selected = state.selectedTab == 2,
                                onClick = { viewModel.setDetailTab(2) },
                                text = { Text("About") },
                            )
                        }
                    }

                    // ─── Tab Content ─────────────────────
                    when (state.selectedTab) {
                        0 -> {
                            // Feed tab — placeholder until Phase 4
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Feed,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "No blood requests yet",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        )
                                        Text(
                                            "Requests will appear here",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Members tab
                            if (state.members.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "No members found",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        )
                                    }
                                }
                            } else {
                                items(state.members, key = { it.uid }) { user ->
                                    MemberRow(
                                        user = user,
                                        community = community,
                                        viewModel = viewModel,
                                        onProfileClick = {
                                            navController.navigate("donor_profile/${user.uid}")
                                        },
                                    )
                                }
                            }
                        }
                        2 -> {
                            // About tab
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    if (community.description.isNotEmpty()) {
                                        Text(
                                            "About",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            community.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        )
                                        Spacer(Modifier.height(16.dp))
                                    }

                                    // Community info cards
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        ),
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            InfoRow("Type", if (community.type == CommunityType.PUBLIC) "Public" else "Private")
                                            InfoRow("District", community.district.ifEmpty { "—" })
                                            InfoRow("Upazila", community.upazila.ifEmpty { "—" })
                                            InfoRow("Founded", community.createdAt?.formatDisplay() ?: "—")
                                            InfoRow("Verified", if (community.isVerified) "✅ Yes" else "No")
                                        }
                                    }

                                    if (community.bloodGroups.isNotEmpty()) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            "Supported Blood Groups",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            community.bloodGroups.forEach { group ->
                                                BloodGroupBadge(bloodGroup = group)
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
}

@Composable
private fun MemberRow(
    user: User,
    community: Community,
    viewModel: CommunityViewModel,
    onProfileClick: () -> Unit,
) {
    val role = when {
        community.adminIds.contains(user.uid) -> CommunityRole.ADMIN
        community.moderatorIds.contains(user.uid) -> CommunityRole.MODERATOR
        else -> CommunityRole.MEMBER
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onProfileClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BloodRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (user.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BloodRed,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (role != CommunityRole.MEMBER) {
                        Spacer(Modifier.width(6.dp))
                        RoleBadge(role = role)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (user.bloodGroup.isNotEmpty()) {
                        BloodGroupBadge(bloodGroup = user.bloodGroup)
                    }
                    AvailabilityIndicator(
                        isAvailable = viewModel.isUserAvailable(user),
                        daysRemaining = viewModel.getDaysUntilAvailable(user),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}