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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                                .padding(16.dp)
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CommunityStatCard(
                                icon = Icons.Outlined.People,
                                label = "Members",
                                value = "${community.memberCount}",
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            CommunityStatCard(
                                icon = Icons.Outlined.VolunteerActivism,
                                label = "Donations",
                                value = "${community.donationCount}",
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            CommunityStatCard(
                                icon = Icons.Outlined.Pending,
                                label = "Active",
                                value = "${state.requests.count { it.status == RequestStatus.ACTIVE }}",
                                modifier = Modifier.weight(1f).fillMaxHeight(),
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
                                    val isEligible = viewModel.isBloodGroupEligible(community)
                                    Button(
                                        onClick = {
                                            if (community.type == CommunityType.PUBLIC) {
                                                viewModel.joinPublicCommunity(communityId)
                                            } else {
                                                navController.navigate("join_request/$communityId")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = isEligible,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BloodRed,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                    ) {
                                        Icon(Icons.Default.GroupAdd, contentDescription = null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (!isEligible) "Blood Group Mismatch"
                                            else if (community.type == CommunityType.PUBLIC) "Join"
                                            else "Request to Join"
                                        )
                                    }
                                }
                                MembershipStatus.JOINED -> {
                                    OutlinedButton(
                                        onClick = { viewModel.leaveCommunity(communityId) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                    ) {
                                        Text("Leave Community")
                                    }
                                }
                                MembershipStatus.PENDING -> {
                                    OutlinedButton(
                                        onClick = {},
                                        modifier = Modifier.weight(1f),
                                        enabled = false,
                                        shape = RoundedCornerShape(16.dp),
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
                            // Loading state
                            if (state.isRequestsLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        ContainedLoadingIndicator()
                                    }
                                }
                            } else if (state.requests.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Feed,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                "No blood requests yet",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Requests posted to this community will appear here",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(state.requests, key = { it.id }) { request ->
                                    com.spondon.app.feature.request.RequestCard(
                                        request = request,
                                        onClick = { navController.navigate("request_detail/${request.id}") },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    )
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

                                    // Community info card — redesigned with chips
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            AboutInfoRow(
                                                icon = Icons.Outlined.Public,
                                                label = "Type",
                                                value = if (community.type == CommunityType.PUBLIC) "Public" else "Private",
                                                chipColor = if (community.type == CommunityType.PUBLIC) AvailableGreen else PendingAmber,
                                                chipBg = if (community.type == CommunityType.PUBLIC) AvailableGreen.copy(alpha = 0.1f) else PendingAmber.copy(alpha = 0.1f),
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            AboutInfoRow(
                                                icon = Icons.Outlined.LocationOn,
                                                label = "District",
                                                value = community.district.ifEmpty { "—" },
                                                chipColor = MaterialTheme.colorScheme.tertiary,
                                                chipBg = MaterialTheme.colorScheme.tertiaryContainer,
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            AboutInfoRow(
                                                icon = Icons.Outlined.Map,
                                                label = "Upazila",
                                                value = community.upazila.ifEmpty { "—" },
                                                chipColor = MaterialTheme.colorScheme.tertiary,
                                                chipBg = MaterialTheme.colorScheme.tertiaryContainer,
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            AboutInfoRow(
                                                icon = Icons.Outlined.CalendarMonth,
                                                label = "Founded",
                                                value = community.createdAt?.formatDisplay() ?: "—",
                                                chipColor = MaterialTheme.colorScheme.secondary,
                                                chipBg = MaterialTheme.colorScheme.secondaryContainer,
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            AboutInfoRow(
                                                icon = Icons.Outlined.Verified,
                                                label = "Verified",
                                                value = if (community.isVerified) "Yes ✅" else "No",
                                                chipColor = if (community.isVerified) AvailableGreen else UnavailableGrey,
                                                chipBg = if (community.isVerified) AvailableGreen.copy(alpha = 0.1f) else UnavailableGrey.copy(alpha = 0.1f),
                                            )
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
private fun CommunityStatCard(
    icon: ImageVector,
    label: String,
    value: String,
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
                contentDescription = null,
                tint = BloodRed,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
            )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BloodRed.copy(alpha = 0.1f)),
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

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    chipColor: Color,
    chipBg: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = chipColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = chipBg,
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = chipColor,
            )
        }
    }
}
