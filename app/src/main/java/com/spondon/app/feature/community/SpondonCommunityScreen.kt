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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spondon.app.core.common.formatDisplay
import com.spondon.app.core.domain.model.CommunityPost
import com.spondon.app.core.domain.model.CommunityRole
import com.spondon.app.core.domain.model.User
import com.spondon.app.core.domain.model.UserRole
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.navigation.Routes


@Composable
fun SpondonCommunityScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.spondonState.collectAsState()
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
    val currentUserId = viewModel.fetchCurrentUserId()

    // Pull to refresh state
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { padding ->
        when {
            // ─── Shimmer Loading ─────────────────────────────────
            state.isLoading -> {
                ShimmerLoadingPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding()),
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
                        .padding(bottom = padding.calculateBottomPadding()),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // ─── Cover Banner ────────────────────
                        item(key = "cover") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                            ) {
                                // Cover image or gradient fallback
                                if (community.coverUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = community.coverUrl,
                                        contentDescription = community.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                    // Gradient overlay on top of image
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
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF7A1212), Color(0xFF2B0606))
                                                )
                                            ),
                                    )
                                }

                                // "Official" badge
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(50),
                                    color = AvailableGreen,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.White,
                                        )
                                        Text(
                                            "Official",
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                        )
                                    }
                                }

                                // Community info at bottom of banner
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                                ) {
                                    // Clickable community name → navigates to Spondon Info screen
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            navController.navigate(Routes.SpondonInfo.route)
                                        },
                                    ) {
                                        Text(
                                            text = community.name,
                                            color = Color.White,
                                            fontSize = 27.sp,
                                            fontWeight = FontWeight.Black,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "Open community details",
                                            tint = Color.White.copy(alpha = 0.85f),
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Tagline
                                    Text(
                                        text = "Every heartbeat counts · Everyone is a member",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 14.5.sp,
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    // Inline stats
                                    Text(
                                        text = "${community.memberCount} members  •  ${state.posts.size} posts  •  ${community.donationCount} donations",
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }

                        // ─── Feed Content (no tabs) ─────────────────────

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
                                        "New Post",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        if (state.isPostsLoading) {
                            item(key = "posts_loading") {
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
                                            "There are no posts",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        )
                                    }
                                }
                            }
                        } else {
                            items(state.posts, key = { it.id }) { post ->
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
                text = "Write something...",
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

// ─── Post Card ─────────────────────────────────────────────────────

@Composable
fun PostCard(
    post: CommunityPost,
    isAdmin: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    var selectedImages by remember { mutableStateOf<List<String>>(emptyList()) }

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
                        val isSuperAdmin = post.authorName == "SuperAdmin"
                        val displayName = if (isSuperAdmin) "Platform Admin 🛡️" else post.authorName.ifEmpty { "Admin" }
                        Text(
                            text = displayName,
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

            // Post images
            val displayImages = if (post.imageUrls.isNotEmpty()) post.imageUrls else listOfNotNull(post.imageUrl)
            
            if (displayImages.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                PostImageCollage(
                    images = displayImages,
                    onImageClick = { index ->
                        // Would typically open full screen viewer here
                        // For now we'll pass state to a parent or handle it locally
                        selectedImageIndex = index
                        selectedImages = displayImages
                        showImageViewer = true
                    }
                )
            }
        }
    }
    
    // Full screen viewer overlay
    if (showImageViewer && selectedImages.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            FullScreenImageViewer(
                images = selectedImages,
                initialIndex = selectedImageIndex,
                onDismiss = { showImageViewer = false }
            )
        }
    }
}

// ─── Image Collage ────────────────────────────────────────────────────────

@Composable
fun PostImageCollage(
    images: List<String>,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return

    val roundedCornerShape = RoundedCornerShape(12.dp)

    when (images.size) {
        1 -> {
            AsyncImage(
                model = images[0],
                contentDescription = "Post image",
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .clip(roundedCornerShape)
                    .clickable { onImageClick(0) },
                contentScale = ContentScale.Crop,
            )
        }
        2 -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(roundedCornerShape),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = images[0],
                    contentDescription = "Post image 1",
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { onImageClick(0) },
                    contentScale = ContentScale.Crop,
                )
                AsyncImage(
                    model = images[1],
                    contentDescription = "Post image 2",
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { onImageClick(1) },
                    contentScale = ContentScale.Crop,
                )
            }
        }
        3 -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(roundedCornerShape),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = images[0],
                    contentDescription = "Post image 1",
                    modifier = Modifier.weight(1.5f).fillMaxHeight().clickable { onImageClick(0) },
                    contentScale = ContentScale.Crop,
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AsyncImage(
                        model = images[1],
                        contentDescription = "Post image 2",
                        modifier = Modifier.weight(1f).fillMaxWidth().clickable { onImageClick(1) },
                        contentScale = ContentScale.Crop,
                    )
                    AsyncImage(
                        model = images[2],
                        contentDescription = "Post image 3",
                        modifier = Modifier.weight(1f).fillMaxWidth().clickable { onImageClick(2) },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        else -> { // 4 or more
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(roundedCornerShape),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1.5f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AsyncImage(
                        model = images[0],
                        contentDescription = "Post image 1",
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onImageClick(0) },
                        contentScale = ContentScale.Crop,
                    )
                    AsyncImage(
                        model = images[1],
                        contentDescription = "Post image 2",
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onImageClick(1) },
                        contentScale = ContentScale.Crop,
                    )
                }
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AsyncImage(
                        model = images[2],
                        contentDescription = "Post image 3",
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onImageClick(2) },
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onImageClick(3) }
                    ) {
                        AsyncImage(
                            model = images[3],
                            contentDescription = "Post image 4",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        if (images.size > 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+${images.size - 4}",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Full Screen Viewer ────────────────────────────────────────────────────

@Composable
fun FullScreenImageViewer(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(initialIndex) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        @OptIn(ExperimentalFoundationApi::class)
        val pagerState = rememberPagerState(
            initialPage = initialIndex,
            pageCount = { images.size }
        )

        @OptIn(ExperimentalFoundationApi::class)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = images[page],
                    contentDescription = "Full screen image",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            if (images.size > 1) {
                @OptIn(ExperimentalFoundationApi::class)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${images.size}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }}

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
                    .height(280.dp)
                    .background(shimmerBrush),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.2f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
            }
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
