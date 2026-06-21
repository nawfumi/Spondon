package com.spondon.app.feature.superadmin.community

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.outlinedButtonBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.core.common.formatDisplay

private val SAGold = Color(0xFFFFD700)
private val SADark = Color(0xFF0D0D0D)
private val SADarkCard = Color(0xFF1A1A2E)
private val SARed = Color(0xFFEF5350)
private val SABlue = Color(0xFF42A5F5)
private val SAOrange = Color(0xFFFFA726)
private val SAPurple = Color(0xFFAB47BC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SASpondonCommunityScreen(
    navController: NavController,
    viewModel: SASpondonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Posts", "Members")

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Favorite,
                            null,
                            tint = SARed,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Spondon Community",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SADark),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0 && state.communityId != null) {
                FloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    containerColor = SAGold,
                    contentColor = SADark,
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Create Post")
                }
            }
        },
        containerColor = SADark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SADark,
                contentColor = SAGold,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SAGold,
                        )
                    }
                },
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) SAGold else Color.White.copy(alpha = 0.5f),
                            )
                        },
                    )
                }
            }

            // Content
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                null,
                                tint = SARed,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(state.error ?: "Error", color = SARed)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.loadSpondonCommunity() },
                                colors = ButtonDefaults.buttonColors(containerColor = SAGold),
                            ) {
                                Text("Retry", color = SADark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {
                    when (selectedTab) {
                        0 -> SASpondonPostsTab(state, viewModel)
                        1 -> SASpondonMembersTab(state, viewModel)
                    }
                }
            }
        }
    }

    // ─── Create Post Dialog ──────────────────────────────────
    if (state.showCreateDialog) {
        SACreatePostDialog(
            state = state,
            onContentChange = viewModel::updateCreatePostContent,
            onImageChange = viewModel::updateCreatePostImageUri,
            onConfirm = viewModel::createPost,
            onDismiss = viewModel::hideCreateDialog,
        )
    }

    // ─── Delete Post Dialog ──────────────────────────────────
    if (state.showDeleteDialog && state.postToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("Delete Post", color = SARed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Permanently delete this post by ${state.postToDelete!!.authorName}?",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePost() }) {
                    Text("Delete", color = SARed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            },
            containerColor = SADarkCard,
        )
    }

    // ─── Member Action Dialog ────────────────────────────────
    if (state.showMemberActionDialog && state.memberToAction != null) {
        val isPromote = state.memberAction == "PROMOTE"
        AlertDialog(
            onDismissRequest = { viewModel.hideMemberActionDialog() },
            title = {
                Text(
                    if (isPromote) "Promote to Sub-Admin" else "Remove Sub-Admin",
                    color = if (isPromote) SAGold else SAOrange,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    if (isPromote)
                        "Promote ${state.memberToAction!!.name} to Sub-Admin? They will be able to create posts and delete their own posts."
                    else
                        "Demote ${state.memberToAction!!.name} back to regular member? They will no longer be able to create or delete posts.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmMemberAction() }) {
                    Text(
                        if (isPromote) "Promote" else "Demote",
                        color = if (isPromote) SAGold else SARed,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideMemberActionDialog() }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            },
            containerColor = SADarkCard,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Posts Tab
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SASpondonPostsTab(
    state: SASpondonState,
    viewModel: SASpondonViewModel,
) {
    if (state.posts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Outlined.Article,
                    null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "No posts yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.4f),
                )
                Text(
                    "Tap + to create the first post",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.3f),
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "${state.posts.size} posts",
                    style = MaterialTheme.typography.labelSmall,
                    color = SAGold.copy(alpha = 0.4f),
                )
            }
            items(state.posts, key = { it.id }) { post ->
                SASpondonPostCard(
                    post = post,
                    onDelete = { viewModel.showDeleteDialog(post) },
                )
            }
            item { Spacer(Modifier.height(72.dp)) } // FAB clearance
        }
    }
}

@Composable
private fun SASpondonPostCard(
    post: SASpondonPost,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SADarkCard),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SAPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (post.authorAvatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = post.authorName,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            post.authorName.firstOrNull()?.uppercase() ?: "S",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SAGold,
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            post.authorName.ifEmpty { "Admin" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(6.dp))
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SAGold.copy(alpha = 0.15f),
                            ),
                        ) {
                            Text(
                                "Author",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = SAGold,
                            )
                        }
                    }
                    Text(
                        post.createdAt?.formatDisplay() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.35f),
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = SARed.copy(alpha = 0.5f),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Post content
            if (post.content.isNotEmpty()) {
                Text(
                    post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 20.sp,
                )
            }

            // Post image
            if (!post.imageUrl.isNullOrEmpty()) {
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Members Tab
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SASpondonMembersTab(
    state: SASpondonState,
    viewModel: SASpondonViewModel,
) {
    if (state.members.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("No members", color = Color.White.copy(alpha = 0.4f))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    "${state.members.size} members",
                    style = MaterialTheme.typography.labelSmall,
                    color = SAGold.copy(alpha = 0.4f),
                )
            }
            items(state.members, key = { it.uid }) { member ->
                SASpondonMemberCard(
                    member = member,
                    onPromote = { viewModel.showPromoteDialog(member) },
                    onDemote = { viewModel.showDemoteDialog(member) },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SASpondonMemberCard(
    member: SASpondonMember,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SADarkCard),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar with role indicator
            val roleColor = when (member.role) {
                "ADMIN" -> SAGold
                "MODERATOR" -> SAOrange
                else -> SABlue
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(roleColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                if (member.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = member.name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        member.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = roleColor,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        member.name.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    // Role badge
                    val (badgeColor, badgeText) = when (member.role) {
                        "ADMIN" -> SAGold to "ADMIN"
                        "MODERATOR" -> SAOrange to "SUB-ADMIN"
                        else -> null to null
                    }
                    if (badgeColor != null && badgeText != null) {
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.15f)),
                        ) {
                            Text(
                                badgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = badgeColor,
                            )
                        }
                    }
                }
                if (member.bloodGroup.isNotBlank()) {
                    Text(
                        member.bloodGroup,
                        style = MaterialTheme.typography.bodySmall,
                        color = SARed.copy(alpha = 0.6f),
                    )
                }
            }

            // Action button (no actions for ADMIN)
            if (member.role != "ADMIN") {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        if (member.role == "MEMBER") {
                            DropdownMenuItem(
                                text = { Text("Make Sub-Admin") },
                                onClick = {
                                    showMenu = false
                                    onPromote()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Star,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = SAOrange,
                                    )
                                },
                            )
                        }
                        if (member.role == "MODERATOR") {
                            DropdownMenuItem(
                                text = { Text("Remove Sub-Admin") },
                                onClick = {
                                    showMenu = false
                                    onDemote()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.PersonRemove,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = SARed,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Create Post Dialog
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SACreatePostDialog(
    state: SASpondonState,
    onContentChange: (String) -> Unit,
    onImageChange: (Uri?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        onImageChange(uri)
    }

    AlertDialog(
        onDismissRequest = { if (!state.isCreatingPost) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Edit, null, tint = SAGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Post", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = state.createPostContent,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = {
                        Text(
                            "Write your post...",
                            color = Color.White.copy(alpha = 0.3f),
                        )
                    },
                    enabled = !state.isCreatingPost,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                        cursorColor = SAGold,
                        focusedBorderColor = SAGold.copy(alpha = 0.4f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // Image section
                if (state.createPostImageUri != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = state.createPostImageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.FillWidth,
                        )
                        IconButton(
                            onClick = { onImageChange(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f),
                            ),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                } else {
                    // Add image button
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !state.isCreatingPost,
                        border = outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                Color.White.copy(alpha = 0.15f)
                            ),
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = SAGold.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Attach Image",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isCreatingPost && state.createPostContent.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SAGold),
                shape = RoundedCornerShape(10.dp),
            ) {
                if (state.isCreatingPost) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SADark,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Publishing...", color = SADark)
                } else {
                    Text("Publish", color = SADark, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isCreatingPost,
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.5f))
            }
        },
        containerColor = SADarkCard,
    )
}
