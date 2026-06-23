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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.spondon.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityInfoScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsState()

    val communityId = navController.currentBackStackEntry
        ?.arguments?.getString("communityId") ?: ""

    LaunchedEffect(communityId) {
        if (communityId.isNotEmpty()) viewModel.loadCommunityDetail(communityId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.community?.name ?: "Community Info",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BloodRed)
                }
            }
            state.community != null -> {
                val community = state.community!!
                val isPrivateAndNotMember = community.type == CommunityType.PRIVATE &&
                        state.membershipStatus != MembershipStatus.JOINED

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Tab Row — Members / About
                    if (isPrivateAndNotMember) {
                        // Non-member of private community: show only About tab
                        TabRow(
                            selectedTabIndex = 0,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = BloodRed,
                        ) {
                            Tab(
                                selected = true,
                                onClick = {},
                                text = { Text("About") },
                            )
                        }

                        // About content only
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                CommunityInfoAboutContent(community = community)
                            }
                        }
                    } else {
                        TabRow(
                            selectedTabIndex = state.selectedTab,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = BloodRed,
                        ) {
                            Tab(
                                selected = state.selectedTab == 0,
                                onClick = { viewModel.setDetailTab(0) },
                                text = { Text("Members") },
                            )
                            Tab(
                                selected = state.selectedTab == 1,
                                onClick = { viewModel.setDetailTab(1) },
                                text = { Text("About") },
                            )
                        }

                        // Tab Content
                        when (state.selectedTab) {
                            0 -> {
                                // Members tab
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                            CommunityInfoMemberRow(
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
                            }
                            1 -> {
                                // About tab
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        CommunityInfoAboutContent(community = community)
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
private fun CommunityInfoMemberRow(
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
                    if (!viewModel.shouldHideForUser(user.uid)) {
                        AvailabilityIndicator(
                            isAvailable = viewModel.isUserAvailable(user),
                            daysRemaining = viewModel.getDaysUntilAvailable(user),
                        )
                    }
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
private fun CommunityInfoAboutContent(community: Community) {
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                CommunityInfoAboutRow(
                    icon = Icons.Outlined.Public,
                    label = "Type",
                    value = if (community.type == CommunityType.PUBLIC) "Public" else "Private",
                    chipColor = if (community.type == CommunityType.PUBLIC) AvailableGreen else PendingAmber,
                    chipBg = if (community.type == CommunityType.PUBLIC) AvailableGreen.copy(alpha = 0.1f) else PendingAmber.copy(alpha = 0.1f),
                )
                Spacer(Modifier.height(10.dp))
                CommunityInfoAboutRow(
                    icon = Icons.Outlined.LocationOn,
                    label = "District",
                    value = community.district.ifEmpty { "—" },
                    chipColor = MaterialTheme.colorScheme.tertiary,
                    chipBg = MaterialTheme.colorScheme.tertiaryContainer,
                )
                Spacer(Modifier.height(10.dp))
                CommunityInfoAboutRow(
                    icon = Icons.Outlined.Map,
                    label = "Upazila",
                    value = community.upazila.ifEmpty { "—" },
                    chipColor = MaterialTheme.colorScheme.tertiary,
                    chipBg = MaterialTheme.colorScheme.tertiaryContainer,
                )
                Spacer(Modifier.height(10.dp))
                CommunityInfoAboutRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Founded",
                    value = community.createdAt?.formatDisplay() ?: "—",
                    chipColor = MaterialTheme.colorScheme.secondary,
                    chipBg = MaterialTheme.colorScheme.secondaryContainer,
                )
                Spacer(Modifier.height(10.dp))
                CommunityInfoAboutRow(
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

@Composable
private fun CommunityInfoAboutRow(
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
