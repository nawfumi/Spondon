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
import androidx.compose.material.icons.automirrored.outlined.Article
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
import androidx.compose.ui.text.style.TextAlign
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
import com.spondon.app.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpondonCommunityScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.spondonState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSpondonCommunity() }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is CommunityEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // canPost: both admin and moderator can create posts
    val canPost = state.currentUserRole == CommunityRole.ADMIN ||
            state.currentUserRole == CommunityRole.MODERATOR
    // isCommunityAdmin: only admin can delete any post & manage members
    val isCommunityAdmin = state.currentUserRole == CommunityRole.ADMIN
    val currentUserId = viewModel.fetchCurrentUserId()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (canPost && state.community != null) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.CreateSpondonPost.route) },
                    containerColor = BloodRed,
                    contentColor = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Create Post",
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
                    }
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

                    // ─── Stats Row ───────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SpondonStatCard(
                                icon = Icons.Outlined.People,
                                label = "Members",
                                value = "${community.memberCount}",
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            SpondonStatCard(
                                icon = Icons.AutoMirrored.Outlined.Article,
                                label = "Posts",
                                value = "${state.posts.size}",
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            SpondonStatCard(
                                icon = Icons.Outlined.VolunteerActivism,
                                label = "Donations",
                                value = "${community.donationCount}",
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }

                    // ─── Tab Row ─────────────────────────
                    item {
                        TabRow(
                            selectedTabIndex = state.selectedTab,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = BloodRed,
                        ) {
                            Tab(
                                selected = state.selectedTab == 0,
                                onClick = { viewModel.setSpondonTab(0) },
                                text = { Text("Feed") },
                            )
                            Tab(
                                selected = state.selectedTab == 1,
                                onClick = { viewModel.setSpondonTab(1) },
                                text = { Text("Members") },
                            )
                            Tab(
                                selected = state.selectedTab == 2,
                                onClick = { viewModel.setSpondonTab(2) },
                                text = { Text("About") },
                            )
                        }
                    }

                    // ─── Tab Content ─────────────────────
                    when (state.selectedTab) {
                        0 -> {
                            // Feed tab — show community posts
                            if (state.isPostsLoading) {
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
                            } else if (state.posts.isEmpty()) {
                                item {
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
                                                "No posts yet",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Admin posts will appear here",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(state.posts, key = { it.id }) { post ->
                                    // Admin can delete any post; moderator can only delete own posts
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
                                    SpondonMemberRow(
                                        user = user,
                                        community = community,
                                        viewModel = viewModel,
                                        isAdmin = isCommunityAdmin,
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
                                    Text(
                                        "About",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        community.description.ifEmpty {
                                            "স্পন্দন — the official community of the Spondon platform. " +
                                                    "Every user is automatically a member. Admin posts announcements, " +
                                                    "news, and updates here."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    )
                                    Spacer(Modifier.height(16.dp))

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            SpondonAboutRow(
                                                icon = Icons.Outlined.Shield,
                                                label = "Type",
                                                value = "Official Platform Community",
                                                chipColor = AvailableGreen,
                                                chipBg = AvailableGreen.copy(alpha = 0.1f),
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            SpondonAboutRow(
                                                icon = Icons.Outlined.People,
                                                label = "Membership",
                                                value = "All Users (Auto-join)",
                                                chipColor = MaterialTheme.colorScheme.primary,
                                                chipBg = MaterialTheme.colorScheme.primaryContainer,
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            SpondonAboutRow(
                                                icon = Icons.Outlined.Edit,
                                                label = "Who Can Post",
                                                value = "Admin & Sub-Admin",
                                                chipColor = BloodRed,
                                                chipBg = BloodRed.copy(alpha = 0.1f),
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            SpondonAboutRow(
                                                icon = Icons.Outlined.CalendarMonth,
                                                label = "Founded",
                                                value = community.createdAt?.formatDisplay() ?: "—",
                                                chipColor = MaterialTheme.colorScheme.secondary,
                                                chipBg = MaterialTheme.colorScheme.secondaryContainer,
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

// ─── Stat Card ────────────────────────────────────────────────────

@Composable
private fun SpondonStatCard(
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

// ─── Member Row ──────────────────────────────────────────────────

@Composable
private fun SpondonMemberRow(
    user: User,
    community: Community,
    viewModel: CommunityViewModel,
    isAdmin: Boolean,
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
                    AvailabilityIndicator(
                        isAvailable = viewModel.isUserAvailable(user),
                        daysRemaining = viewModel.getDaysUntilAvailable(user),
                    )
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
