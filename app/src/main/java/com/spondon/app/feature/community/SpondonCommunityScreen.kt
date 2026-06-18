package com.spondon.app.feature.community

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.core.common.formatDisplay
import com.spondon.app.core.domain.model.Community
import com.spondon.app.core.domain.model.CommunityPost
import com.spondon.app.core.domain.model.CommunityRole
import com.spondon.app.core.domain.model.User
import com.spondon.app.core.domain.model.UserRole
import com.spondon.app.core.ui.components.AvailabilityIndicator
import com.spondon.app.core.ui.components.BloodGroupBadge
import com.spondon.app.core.ui.components.RoleBadge
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.DarkRose
import com.spondon.app.core.ui.theme.PendingAmber
import com.spondon.app.navigation.Routes


@Composable
fun SpondonCommunityScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.spondonState.collectAsState()
    val hideSensitiveData by viewModel.hideSensitiveData.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadSpondonCommunity() }

    // Collect events for snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    // canPost: community admin/moderator OR platform SUPER_ADMIN can create posts
    val canPost = state.currentUserRole == CommunityRole.ADMIN ||
            state.currentUserRole == CommunityRole.MODERATOR ||
            state.currentUserPlatformRole == UserRole.SUPER_ADMIN
    // isCommunityAdmin: admin or SUPER_ADMIN can delete any post & manage members
    val isCommunityAdmin = state.currentUserRole == CommunityRole.ADMIN ||
            state.currentUserPlatformRole == UserRole.SUPER_ADMIN
    val isAdminOrMod = canPost // Can post = admin/mod/superadmin
    val currentUserId = viewModel.fetchCurrentUserId()

    // Tab indices
    val tabTitles = buildList {
        add("ফিড")         // 0 - Feed
        add("সদস্যরা")     // 1 - Members
        add("সম্পর্কে")     // 2 - About
        if (isAdminOrMod) add("পরিচালনা")  // 3 - Manage (admin/mod only)
    }

    // Pull to refresh state
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // FAB removed — replaced by Write Something bar + Manage tab
    ) { padding ->
        when {
            // ─── Shimmer Loading ─────────────────────────────────
            state.isLoading -> {
                ShimmerLoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onBack = { navController.popBackStack() },
                )
            }
            state.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = BloodRed,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.error ?: "Error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.loadSpondonCommunity() }) {
                            Text("Retry", color = BloodRed)
                        }
                    }
                }
            }
            state.community != null -> {
                val community = state.community!!

                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.loadSpondonCommunity() },
                    state = pullRefreshState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // ─── Cover Header ────────────────────
                        item(key = "cover") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                            ) {
                                // Cover image or gradient
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
                                                    listOf(
                                                        BloodRed,
                                                        DarkRose,
                                                        BloodRed.copy(alpha = 0.7f),
                                                    ),
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
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                                startY = 60f,
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

                                // "Official" badge
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = AvailableGreen,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.White,
                                        )
                                        Text(
                                            "Official",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                }

                                // Community name at bottom
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
                                    Text(
                                        text = "Every heartbeat counts · Everyone is a member",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }

                        // ─── Stats Row with Dividers ─────────
                        item(key = "stats") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SpondonStatItem(
                                    icon = Icons.Outlined.People,
                                    label = "সদস্য",
                                    value = "${community.memberCount}",
                                    modifier = Modifier.weight(1f),
                                )
                                VerticalDivider(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(vertical = 8.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                SpondonStatItem(
                                    icon = Icons.AutoMirrored.Outlined.Article,
                                    label = "পোস্ট",
                                    value = "${state.posts.size}",
                                    modifier = Modifier.weight(1f),
                                )
                                VerticalDivider(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(vertical = 8.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                SpondonStatItem(
                                    icon = Icons.Outlined.VolunteerActivism,
                                    label = "রক্তদান",
                                    value = "${community.donationCount}",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        // ─── Tab Bar ─────────────────────
                        item(key = "tabs") {
                            ScrollableTabRow(
                                selectedTabIndex = state.selectedTab,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = BloodRed,
                                edgePadding = 16.dp,
                                indicator = { tabPositions ->
                                    if (state.selectedTab < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                                            color = BloodRed,
                                        )
                                    }
                                },
                                divider = {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                },
                            ) {
                                tabTitles.forEachIndexed { index, title ->
                                    Tab(
                                        selected = state.selectedTab == index,
                                        onClick = { viewModel.setSpondonTab(index) },
                                        text = {
                                            Text(
                                                title,
                                                fontWeight = if (state.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                                color = if (state.selectedTab == index) BloodRed
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                    )
                                }
                            }
                        }

                        // ─── Tab Content ─────────────────────
                        when (state.selectedTab) {
                            0 -> {
                                // ─── Feed Tab ────────────────
                                // Write Something Bar (admin/mod/superadmin only)
                                if (canPost) {
                                    item(key = "write_bar") {
                                        WritePostBar(
                                            currentUser = state.currentUser,
                                            onClick = {
                                                navController.navigate(Routes.CreateSpondonPost.route)
                                            },
                                        )
                                    }
                                }

                                // Posts header
                                if (state.posts.isNotEmpty()) {
                                    item(key = "posts_header") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "নতুন পোস্ট",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }

                                if (state.isPostsLoading) {
                                    item(key = "posts_loading") {
                                        // Shimmer post placeholders
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            repeat(3) {
                                                ShimmerPostCard()
                                            }
                                        }
                                    }
                                } else if (state.posts.isEmpty()) {
                                    item(key = "posts_empty") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(40.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.AutoMirrored.Outlined.Article,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                )
                                                Spacer(Modifier.height(12.dp))
                                                Text(
                                                    "এখনো কোনো পোস্ট নেই",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "অ্যাডমিন পোস্ট করলে এখানে দেখাবে",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(state.posts, key = { it.id }) { post ->
                                        // Admin can delete any post; moderator can only delete own
                                        val canDelete = isCommunityAdmin ||
                                                (canPost && post.authorId == currentUserId)
                                        PostCard(
                                            post = post,
                                            isAdmin = canDelete,
                                            onDelete = { viewModel.deletePost(post.id) },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }

                            1 -> {
                                // ─── Members Tab ─────────────
                                // Search bar
                                item(key = "member_search") {
                                    OutlinedTextField(
                                        value = state.memberSearchQuery,
                                        onValueChange = { viewModel.updateSpondonMemberSearchQuery(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        placeholder = { Text("সদস্য খুঁজুন...") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        trailingIcon = {
                                            if (state.memberSearchQuery.isNotEmpty()) {
                                                IconButton(onClick = { viewModel.updateSpondonMemberSearchQuery("") }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        singleLine = true,
                                    )
                                }

                                // Member count header
                                item(key = "member_count") {
                                    val filteredCount = state.members.count { member ->
                                        state.memberSearchQuery.isBlank() ||
                                                member.name.contains(state.memberSearchQuery, ignoreCase = true) ||
                                                member.bloodGroup.contains(state.memberSearchQuery, ignoreCase = true)
                                    }
                                    Text(
                                        "$filteredCount জন সদস্য",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                val filteredMembers = state.members.filter { member ->
                                    state.memberSearchQuery.isBlank() ||
                                            member.name.contains(state.memberSearchQuery, ignoreCase = true) ||
                                            member.bloodGroup.contains(state.memberSearchQuery, ignoreCase = true)
                                }

                                if (filteredMembers.isEmpty()) {
                                    item(key = "members_empty") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                if (state.memberSearchQuery.isNotBlank()) "কোনো সদস্য পাওয়া যায়নি"
                                                else "কোনো সদস্য নেই",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            )
                                        }
                                    }
                                } else {
                                    items(filteredMembers, key = { it.uid }) { user ->
                                        SpondonMemberRow(
                                            user = user,
                                            community = community,
                                            viewModel = viewModel,
                                            isAdmin = isCommunityAdmin,
                                            hideSensitiveData = hideSensitiveData,
                                            onProfileClick = {
                                                navController.navigate("donor_profile/${user.uid}")
                                            },
                                        )
                                    }
                                }
                            }

                            2 -> {
                                // ─── About Tab ───────────────
                                item(key = "about") {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        // Description card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Info,
                                                        contentDescription = null,
                                                        tint = BloodRed,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Text(
                                                        "বিবরণ",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    community.description.ifEmpty {
                                                        "স্পন্দন — the official community of the Spondon platform. " +
                                                                "Every user is automatically a member. Admin posts announcements, " +
                                                                "news, and updates here."
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                    lineHeight = 22.sp,
                                                )
                                            }
                                        }

                                        // Info card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Shield,
                                                        contentDescription = null,
                                                        tint = BloodRed,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Text(
                                                        "তথ্য",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                                Spacer(Modifier.height(12.dp))

                                                SpondonAboutRow(
                                                    icon = Icons.Outlined.Shield,
                                                    label = "ধরন",
                                                    value = "Official Platform Community",
                                                    chipColor = AvailableGreen,
                                                    chipBg = AvailableGreen.copy(alpha = 0.1f),
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                SpondonAboutRow(
                                                    icon = Icons.Outlined.People,
                                                    label = "সদস্যপদ",
                                                    value = "সবাই (Auto-join)",
                                                    chipColor = MaterialTheme.colorScheme.primary,
                                                    chipBg = MaterialTheme.colorScheme.primaryContainer,
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                SpondonAboutRow(
                                                    icon = Icons.Outlined.Edit,
                                                    label = "কে পোস্ট করতে পারে",
                                                    value = "Admin & Sub-Admin",
                                                    chipColor = BloodRed,
                                                    chipBg = BloodRed.copy(alpha = 0.1f),
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                SpondonAboutRow(
                                                    icon = Icons.Outlined.CalendarMonth,
                                                    label = "প্রতিষ্ঠিত",
                                                    value = community.createdAt?.formatDisplay() ?: "—",
                                                    chipColor = MaterialTheme.colorScheme.secondary,
                                                    chipBg = MaterialTheme.colorScheme.secondaryContainer,
                                                )
                                            }
                                        }

                                        // Community rules card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Gavel,
                                                        contentDescription = null,
                                                        tint = BloodRed,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Text(
                                                        "কমিউনিটি নিয়ম",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                val rules = listOf(
                                                    "সবাইকে সম্মান করুন",
                                                    "মিথ্যা তথ্য দেবেন না",
                                                    "রক্তদানে উৎসাহিত করুন",
                                                    "প্রতারণামূলক কাজ থেকে বিরত থাকুন",
                                                )
                                                rules.forEachIndexed { index, rule ->
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                        verticalAlignment = Alignment.Top,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        Text(
                                                            "${index + 1}.",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BloodRed,
                                                        )
                                                        Text(
                                                            rule,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                // ─── Manage Tab (admin only) ─
                                item(key = "manage") {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            "পরিচালনা",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(4.dp))

                                        // Pending Join Requests
                                        ManageActionCard(
                                            icon = Icons.Outlined.HourglassTop,
                                            title = "অপেক্ষমাণ অনুরোধ",
                                            subtitle = "${community.pendingIds.size} জন অপেক্ষায়",
                                            badgeCount = community.pendingIds.size,
                                            onClick = {
                                                navController.navigate("admin_dashboard/${community.id}")
                                            },
                                        )

                                        // Member Management
                                        ManageActionCard(
                                            icon = Icons.Outlined.ManageAccounts,
                                            title = "সদস্য ব্যবস্থাপনা",
                                            subtitle = "সদস্যদের ভূমিকা পরিচালনা করুন",
                                            onClick = {
                                                navController.navigate("admin_dashboard/${community.id}")
                                            },
                                        )

                                        // Create Post (quick action)
                                        ManageActionCard(
                                            icon = Icons.Outlined.PostAdd,
                                            title = "পোস্ট তৈরি করুন",
                                            subtitle = "নতুন ঘোষণা বা আপডেট পোস্ট করুন",
                                            onClick = {
                                                navController.navigate(Routes.CreateSpondonPost.route)
                                            },
                                        )

                                        // Community Stats
                                        ManageActionCard(
                                            icon = Icons.Outlined.BarChart,
                                            title = "কমিউনিটি পরিসংখ্যান",
                                            subtitle = "${community.memberCount} সদস্য · ${state.posts.size} পোস্ট",
                                            onClick = { /* placeholder */ },
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom spacing
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ─── Write Post Bar ─────────────────────────────────────────────────

@Composable
private fun WritePostBar(
    currentUser: User?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // User avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BloodRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                if (currentUser?.avatarUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = currentUser.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = currentUser?.name?.firstOrNull()?.uppercase() ?: "A",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BloodRed,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Placeholder text
            Text(
                text = "কিছু লিখুন...",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(8.dp))

            // Image icon
            Icon(
                Icons.Rounded.Image,
                contentDescription = "Add image",
                modifier = Modifier.size(22.dp),
                tint = BloodRed,
            )
        }
    }
}

// ─── Stat Item (compact, for row with dividers) ─────────────────────

@Composable
private fun SpondonStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
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

// ─── Manage Action Card ─────────────────────────────────────────────

@Composable
private fun ManageActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeCount: Int = 0,
    onClick: () -> Unit,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BloodRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = BloodRed,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (badgeCount > 0) {
                Badge(
                    containerColor = BloodRed,
                    contentColor = Color.White,
                ) {
                    Text("$badgeCount")
                }
                Spacer(Modifier.width(8.dp))
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

// ─── Post Card ─────────────────────────────────────────────────────

@Composable
fun PostCard(
    post: CommunityPost,
    isAdmin: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BloodRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (post.authorAvatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = post.authorName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = post.authorName.firstOrNull()?.uppercase() ?: "A",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BloodRed,
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName.ifEmpty { "Admin" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BloodRed.copy(alpha = 0.1f),
                        ) {
                            Text(
                                "Admin",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BloodRed,
                                fontSize = 9.sp,
                            )
                        }
                    }
                    Text(
                        text = post.createdAt?.formatDisplay() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }

                // Delete button for admin
                if (isAdmin) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Post content
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                )
            }

            // Post image
            if (!post.imageUrl.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
    }
}

// ─── Member Row ──────────────────────────────────────────────────

@Composable
private fun SpondonMemberRow(
    user: User,
    community: Community,
    viewModel: CommunityViewModel,
    isAdmin: Boolean,
    hideSensitiveData: Boolean = false,
    onProfileClick: () -> Unit,
) {
    val role = when {
        community.adminIds.contains(user.uid) -> CommunityRole.ADMIN
        community.moderatorIds.contains(user.uid) -> CommunityRole.MODERATOR
        else -> CommunityRole.MEMBER
    }

    // Don't show admin actions for yourself
    val isSelf = user.uid == viewModel.fetchCurrentUserId()
    var showMenu by remember { mutableStateOf(false) }

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
                    if (!hideSensitiveData) {
                        AvailabilityIndicator(
                            isAvailable = viewModel.isUserAvailable(user),
                            daysRemaining = viewModel.getDaysUntilAvailable(user),
                        )
                    }
                }
            }

            // Admin actions menu or simple chevron
            if (isAdmin && !isSelf) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Member actions",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        // Promote to Moderator (sub-admin) if currently a regular member
                        if (role == CommunityRole.MEMBER) {
                            DropdownMenuItem(
                                text = { Text("Make Sub-Admin") },
                                onClick = {
                                    viewModel.promoteSpondonMember(user.uid, CommunityRole.MODERATOR)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = PendingAmber,
                                    )
                                },
                            )
                        }
                        // Promote to Admin if currently a moderator
                        if (role == CommunityRole.MODERATOR) {
                            DropdownMenuItem(
                                text = { Text("Promote to Admin") },
                                onClick = {
                                    viewModel.promoteSpondonMember(user.uid, CommunityRole.ADMIN)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = BloodRed,
                                    )
                                },
                            )
                            // Demote back to member
                            DropdownMenuItem(
                                text = { Text("Remove Sub-Admin") },
                                onClick = {
                                    viewModel.demoteSpondonMember(user.uid)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PersonRemove,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                            )
                        }
                        // View profile
                        DropdownMenuItem(
                            text = { Text("View Profile") },
                            onClick = {
                                onProfileClick()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── About Row ──────────────────────────────────────────────────

@Composable
private fun SpondonAboutRow(
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

// ─── Shimmer Loading Placeholder ─────────────────────────────────

@Composable
private fun ShimmerLoadingPlaceholder(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        start = Offset(translateAnim.value - 500f, 0f),
        end = Offset(translateAnim.value, 0f),
    )

    LazyColumn(modifier = modifier) {
        // Cover shimmer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(shimmerBrush),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.2f),
                    ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
            }
        }

        // Stats shimmer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(shimmerBrush),
                    )
                }
            }
        }

        // Tab shimmer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(shimmerBrush),
            )
        }

        // Post card shimmers
        items(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(shimmerBrush),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush),
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush),
                    )
                }
            }
        }
    }
}

// ─── Shimmer Post Card (for posts loading state) ─────────────────

@Composable
private fun ShimmerPostCard() {
    val transition = rememberInfiniteTransition(label = "shimmer_post")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_post_translate",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        start = Offset(translateAnim.value - 500f, 0f),
        end = Offset(translateAnim.value, 0f),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush),
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush),
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush),
            )
        }
    }
}
