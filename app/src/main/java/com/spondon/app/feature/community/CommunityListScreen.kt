package com.spondon.app.feature.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.core.domain.model.Community
import com.spondon.app.core.domain.model.CommunityType

import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.DarkOnPrimary
import com.spondon.app.core.ui.theme.DarkRose
import com.spondon.app.core.ui.theme.PendingAmber
import com.spondon.app.core.ui.theme.SoftRose
import com.spondon.app.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityListScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.listState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCommunities() }

    // Collect events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityEvent.NavigateToCommunity -> {
                    navController.navigate("community_detail/${event.communityId}")
                }
                else -> {}
            }
        }
    }

    val s = S.strings

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        s.communities,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.CreateCommunity.route) },
                containerColor = BloodRed,
                contentColor = DarkOnPrimary,
                text = {Text("Create\nCommunity")},
                icon = {Icon(Icons.Default.Add, contentDescription = s.createCommunity)}
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ─── Tab Row ─────────────────────────────
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = BloodRed,
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.setListTab(0) },
                    text = {
                        Text(
                            s.myCommunities,
                            fontWeight = if (state.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.setListTab(1) },
                    text = {
                        Text(
                            "Discover",
                            fontWeight = if (state.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }

            // ─── Search bar (Discover tab only) ──────
            AnimatedVisibility(
                visible = state.selectedTab == 1,
                enter = expandVertically(animationSpec = tween(200, easing = LinearEasing)) + fadeIn(tween(200, easing = LinearEasing)),
                exit = shrinkVertically(animationSpec = tween(150, easing = LinearEasing)) + fadeOut(tween(150, easing = LinearEasing)),
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search by name, district...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                )
            }

            // ─── Content ─────────────────────────────
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ContainedLoadingIndicator()
                    }
                }
                state.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = BloodRed,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(state.error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { viewModel.loadCommunities() }) {
                                Text(s.retry, color = BloodRed)
                            }
                        }
                    }
                }
                else -> {
                    val communities = if (state.selectedTab == 0) {
                        state.myCommunities
                    } else {
                        viewModel.getFilteredDiscoverCommunities()
                    }

                    if (communities.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    if (state.selectedTab == 0) "You haven't joined any communities yet"
                                    else "No communities found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(communities, key = { it.id }) { community ->
                                val isJoined = state.myCommunities.any { it.id == community.id }
                                val isEligible = viewModel.isBloodGroupEligible(community)
                                val isPending = community.pendingIds.contains(viewModel.getCurrentUserId())
                                CommunityCard(
                                    community = community,
                                    isDiscover = state.selectedTab == 1,
                                    isJoined = isJoined,
                                    isPending = isPending,
                                    isEligible = isEligible,
                                    onClick = {
                                        navController.navigate("community_detail/${community.id}")
                                    },
                                    onJoin = {
                                        if (!isJoined && !isPending) {
                                            if (community.type == CommunityType.PUBLIC) {
                                                viewModel.joinPublicCommunity(community.id)
                                            } else {
                                                navController.navigate("join_request/${community.id}")
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityCard(
    community: Community,
    isDiscover: Boolean,
    isJoined: Boolean = false,
    isPending: Boolean = false,
    isEligible: Boolean = true,
    onClick: () -> Unit,
    onJoin: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // ── Header row: avatar + name + type badge ──────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Circle community picture
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (community.coverUrl.isNotEmpty()) {
                        AsyncImage(
                            model = community.coverUrl,
                            contentDescription = community.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(BloodRed, DarkRose),
                                    ),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    // Verified badge overlay
                    if (community.isVerified) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(1.dp),
                            tint = AvailableGreen,
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = community.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (community.type == CommunityType.PRIVATE) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PendingAmber.copy(alpha = 0.12f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Private",
                                        modifier = Modifier.size(10.dp),
                                        tint = PendingAmber,
                                    )
                                    Text(
                                        "Private",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                        ),
                                        color = PendingAmber,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Posted: ${community.district.ifEmpty { "Unknown location" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!isDiscover) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            // ── Info chips grid ──────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Members chip
                CommunityInfoChip(
                    icon = Icons.Default.People,
                    label = "Members",
                    value = "${community.memberCount}",
                    chipColor = MaterialTheme.colorScheme.primary,
                    chipBg = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f),
                )
                // Donations chip
                CommunityInfoChip(
                    icon = Icons.Default.Favorite,
                    label = "Donations",
                    value = "${community.donationCount}",
                    chipColor = SoftRose,
                    chipBg = SoftRose.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f),
                )
            }

            if (community.district.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CommunityInfoChip(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = buildString {
                            append(community.district)
                            if (community.upazila.isNotEmpty()) {
                                append(" · ${community.upazila}")
                            }
                        },
                        chipColor = MaterialTheme.colorScheme.tertiary,
                        chipBg = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    // Join button or spacer for alignment
                    if (isDiscover) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            if (isJoined) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AvailableGreen.copy(alpha = 0.1f),
                                ) {
                                    Text(
                                        "Joined",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AvailableGreen,
                                    )
                                }
                            } else if (isPending) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PendingAmber.copy(alpha = 0.1f),
                                ) {
                                    Text(
                                        "⏳ Pending",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PendingAmber,
                                    )
                                }
                            } else if (!isEligible) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                ) {
                                    Text(
                                        "Mismatch",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onJoin,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 34.dp),
                                ) {
                                    Text(
                                        if (community.type == CommunityType.PUBLIC) "Join"
                                        else "Request",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            } else if (isDiscover) {
                // No location but still show join button
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (isJoined) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AvailableGreen.copy(alpha = 0.1f),
                        ) {
                            Text(
                                "Joined",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AvailableGreen,
                            )
                        }
                    } else if (isPending) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PendingAmber.copy(alpha = 0.1f),
                        ) {
                            Text(
                                "⏳ Pending",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PendingAmber,
                            )
                        }
                    } else if (!isEligible) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        ) {
                            Text(
                                "Mismatch",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            )
                        }
                    } else {
                        Button(
                            onClick = onJoin,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 34.dp),
                        ) {
                            Text(
                                if (community.type == CommunityType.PUBLIC) "Join"
                                else "Request",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    chipColor: Color,
    chipBg: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = chipColor,
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = chipBg,
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = chipColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}