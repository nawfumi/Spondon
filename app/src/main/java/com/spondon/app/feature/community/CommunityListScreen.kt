package com.spondon.app.feature.community

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.core.domain.model.Community
import com.spondon.app.core.domain.model.CommunityType
import com.spondon.app.core.ui.components.BloodGroupBadge
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.*
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
            FloatingActionButton(
                onClick = { navController.navigate(Routes.CreateCommunity.route) },
                containerColor = BloodRed,
                contentColor = DarkOnPrimary,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = s.createCommunity)
            }
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
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                )
            }

            // ─── Content ─────────────────────────────
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BloodRed)
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
                                CommunityCard(
                                    community = community,
                                    isDiscover = state.selectedTab == 1,
                                    isJoined = isJoined,
                                    onClick = {
                                        navController.navigate("community_detail/${community.id}")
                                    },
                                    onJoin = {
                                        if (!isJoined) {
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
    onClick: () -> Unit,
    onJoin: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Cover image / gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            ) {
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
                                    colors = listOf(BloodRed, DarkRose),
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
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                ),
                                startY = 40f,
                            )
                        ),
                )

                // Community type badge
                if (community.type == CommunityType.PRIVATE) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = PendingAmber,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = PendingAmber,
                            )
                        }
                    }
                }

                // Verified badge
                if (community.isVerified) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verified",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(20.dp),
                        tint = AvailableGreen,
                    )
                }
            }

            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = community.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (community.district.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp),
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = buildString {
                                        append(community.district)
                                        if (community.upazila.isNotEmpty()) {
                                            append(" · ${community.upazila}")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }

                    // Join button (Discover tab)
                    if (isDiscover) {
                        Spacer(Modifier.width(8.dp))
                        if (isJoined) {
                            OutlinedButton(
                                onClick = {},
                                shape = RoundedCornerShape(20.dp),
                                enabled = false,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "Joined",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        } else {
                            Button(
                                onClick = onJoin,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    if (community.type == CommunityType.PUBLIC) "Join"
                                    else "Request",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${community.memberCount} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    if (community.donationCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = SoftRose,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${community.donationCount} donations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }

                // Blood group chips
                if (community.bloodGroups.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        community.bloodGroups.take(4).forEach { group ->
                            BloodGroupBadge(bloodGroup = group)
                        }
                        if (community.bloodGroups.size > 4) {
                            Text(
                                "+${community.bloodGroups.size - 4}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
        }
    }
}