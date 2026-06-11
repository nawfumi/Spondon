package com.spondon.app.feature.community

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.daysSince
import com.spondon.app.core.domain.model.*
import com.spondon.app.core.ui.components.AvailabilityIndicator
import com.spondon.app.core.ui.components.BloodGroupBadge
import com.spondon.app.core.ui.components.RoleBadge
import com.spondon.app.core.ui.theme.*
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.adminState.collectAsState()
    val communityId = navController.currentBackStackEntry
        ?.arguments?.getString("communityId") ?: ""

    LaunchedEffect(communityId) {
        if (communityId.isNotEmpty()) viewModel.loadAdminDashboard(communityId)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is CommunityEvent.SharePdf -> {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            event.file,
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Member List"))
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to share PDF: ${e.message}")
                    }
                }
                else -> {}
            }
        }
    }

    // Tab state (0 = Join Requests, 1 = Members, 2 = Broadcast)
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Show PDF Export FAB on Members tab
            if (selectedTab == 1 && state.members.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.exportMembersPdf(context, communityId) },
                    containerColor = BloodRed,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Admin Panel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        state.community?.let {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BloodRed.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = BloodRed,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                "ADMIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                ),
                                color = BloodRed,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // ── Stats Banner ──────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(BloodRed, DarkRose, BloodRed.copy(alpha = 0.85f)),
                                ),
                            )
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            StatBannerItem(
                                icon = Icons.Default.HourglassTop,
                                value = "${state.pendingCount}",
                                label = "Pending",
                            )
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                color = Color.White.copy(alpha = 0.3f),
                            )
                            StatBannerItem(
                                icon = Icons.Default.People,
                                value = "${state.activeMembers}",
                                label = "Members",
                            )
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                color = Color.White.copy(alpha = 0.3f),
                            )
                            StatBannerItem(
                                icon = Icons.Default.Favorite,
                                value = "${state.monthlyDonations}",
                                label = "Donations",
                            )
                        }
                    }

                    // ── Tab Row ───────────────────────────────────
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = BloodRed,
                        divider = {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        },
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            selectedContentColor = BloodRed,
                            unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.HourglassTop, null, modifier = Modifier.size(16.dp))
                                Text("Requests", style = MaterialTheme.typography.labelLarge)
                                if (state.pendingCount > 0) {
                                    Badge(containerColor = BloodRed) {
                                        Text("${state.pendingCount}", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            selectedContentColor = BloodRed,
                            unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp))
                                Text("Members", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            selectedContentColor = BloodRed,
                            unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.Campaign, null, modifier = Modifier.size(16.dp))
                                Text("Broadcast", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    // ── Tab Content ───────────────────────────────
                    when (selectedTab) {

                        // ══ Tab 0: Join Requests ══════════════════
                        0 -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (state.pendingRequests.isEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = AvailableGreen.copy(alpha = 0.06f),
                                            ),
                                            elevation = CardDefaults.cardElevation(0.dp),
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(36.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(44.dp),
                                                    tint = AvailableGreen.copy(alpha = 0.6f),
                                                )
                                                Spacer(Modifier.height(10.dp))
                                                Text(
                                                    "All caught up!",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AvailableGreen,
                                                )
                                                Text(
                                                    "No pending join requests",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    item {
                                        Text(
                                            "${state.pendingRequests.size} pending request${if (state.pendingRequests.size != 1) "s" else ""}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        )
                                    }
                                    items(state.pendingRequests, key = { it.id }) { request ->
                                        JoinRequestCard(
                                            request = request,
                                            onApprove = {
                                                viewModel.approveJoinRequest(
                                                    communityId, request.id, request.userId,
                                                )
                                            },
                                            onReject = {
                                                viewModel.rejectJoinRequest(
                                                    communityId, request.id, request.userId, null,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        // ══ Tab 1: Members ════════════════════════
                        1 -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (state.members.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(40.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "No members yet",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            )
                                        }
                                    }
                                } else {
                                    item {
                                        Text(
                                            "${state.members.size} member${if (state.members.size != 1) "s" else ""}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        )
                                    }
                                    items(state.members, key = { it.uid }) { member ->
                                        AdminMemberCard(
                                            user = member,
                                            community = state.community!!,
                                            viewModel = viewModel,
                                            communityId = communityId,
                                            serialId = state.memberSerials[member.uid],
                                        )
                                    }
                                }
                            }
                        }

                        // ══ Tab 2: Broadcast ══════════════════════
                        2 -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(BloodRed.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        Icons.Default.Campaign,
                                                        contentDescription = null,
                                                        tint = BloodRed,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        "Send Broadcast",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                    Text(
                                                        "Notify all ${state.activeMembers} members",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(16.dp))

                                            OutlinedTextField(
                                                value = state.broadcastMessage,
                                                onValueChange = { viewModel.updateBroadcastMessage(it) },
                                                label = { Text("Message") },
                                                placeholder = { Text("Write your announcement\u2026") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                minLines = 3,
                                                maxLines = 6,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = BloodRed,
                                                    cursorColor = BloodRed,
                                                    focusedLabelColor = BloodRed,
                                                ),
                                            )

                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "${state.broadcastMessage.length} characters",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                modifier = Modifier.align(Alignment.End),
                                            )

                                            Spacer(Modifier.height(12.dp))

                                            Button(
                                                onClick = { viewModel.sendBroadcastNotification(communityId) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(50.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                enabled = state.broadcastMessage.isNotBlank() && !state.isBroadcasting,
                                                colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                            ) {
                                                if (state.isBroadcasting) {
                                                    LoadingIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Sending\u2026")
                                                } else {
                                                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Send to All Members", fontWeight = FontWeight.SemiBold)
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
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun StatBannerItem(
    icon: ImageVector,
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun JoinRequestCard(
    request: JoinRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar / initial
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(BloodRed.copy(alpha = 0.2f), SoftRose.copy(alpha = 0.15f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = request.userName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BloodRed,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.userName.ifEmpty { "Unknown User" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        if (request.userBloodGroup.isNotEmpty()) {
                            BloodGroupBadge(bloodGroup = request.userBloodGroup)
                        }
                        if (request.userDistrict.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn, null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                                Text(
                                    request.userDistrict,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                            }
                        }
                        if (!request.serialId.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tag, null,
                                    modifier = Modifier.size(11.dp),
                                    tint = BloodRed.copy(alpha = 0.5f),
                                )
                                Text(
                                    request.serialId,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BloodRed.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            if (request.message.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = "\u201c${request.message}\u201d",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier.padding(10.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                    ),
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AvailableGreen),
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Approve", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AdminMemberCard(
    user: User,
    community: Community,
    viewModel: CommunityViewModel,
    communityId: String,
    serialId: String? = null,
) {
    val role = when {
        community.adminIds.contains(user.uid) -> CommunityRole.ADMIN
        community.moderatorIds.contains(user.uid) -> CommunityRole.MODERATOR
        else -> CommunityRole.MEMBER
    }
    val isAvailable = viewModel.isUserAvailable(user)
    val daysRemaining = viewModel.getDaysUntilAvailable(user)
    val canOverride = viewModel.canOverrideAvailability(user)

    var showMenu by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showOverrideDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(BloodRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                if (user.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        user.name.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (role != CommunityRole.MEMBER) {
                        RoleBadge(role = role)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    if (user.bloodGroup.isNotEmpty()) BloodGroupBadge(bloodGroup = user.bloodGroup)
                    AvailabilityIndicator(isAvailable = isAvailable, daysRemaining = daysRemaining)
                }

                // Serial ID display
                if (community.isSerialEnabled) {
                    var showSerialEdit by remember { mutableStateOf(false) }
                    var editSerial by remember { mutableStateOf(serialId ?: "") }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.width(4.dp))
                        if (showSerialEdit) {
                            OutlinedTextField(
                                value = editSerial,
                                onValueChange = { editSerial = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                textStyle = MaterialTheme.typography.labelSmall,
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (editSerial.isNotBlank()) {
                                                viewModel.assignSerialId(communityId, user.uid, editSerial.trim())
                                            }
                                            showSerialEdit = false
                                        },
                                        modifier = Modifier.size(20.dp),
                                    ) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = AvailableGreen)
                                    }
                                },
                            )
                        } else {
                            Text(
                                text = if (serialId.isNullOrBlank()) "No serial" else serialId,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (serialId.isNullOrBlank())
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = { showSerialEdit = true },
                                modifier = Modifier.size(18.dp),
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit serial",
                                    modifier = Modifier.size(12.dp),
                                    tint = BloodRed.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (role == CommunityRole.MEMBER) {
                        DropdownMenuItem(
                            text = { Text("Promote to Moderator") },
                            onClick = {
                                viewModel.promoteMember(communityId, user.uid, CommunityRole.MODERATOR)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(18.dp)) },
                        )
                    }
                    if (role == CommunityRole.MODERATOR) {
                        DropdownMenuItem(
                            text = { Text("Promote to Admin") },
                            onClick = {
                                viewModel.promoteMember(communityId, user.uid, CommunityRole.ADMIN)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null, Modifier.size(18.dp)) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Mark Donation") },
                        onClick = { showDonationDialog = true; showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Bloodtype, null, Modifier.size(18.dp)) },
                    )
                    if (canOverride) {
                        DropdownMenuItem(
                            text = { Text("Override Availability") },
                            onClick = { showOverrideDialog = true; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.LockOpen, null, Modifier.size(18.dp)) },
                        )
                    }
                    if (role != CommunityRole.ADMIN) {
                        DropdownMenuItem(
                            text = { Text("Remove Member", color = MaterialTheme.colorScheme.error) },
                            onClick = { viewModel.removeMember(communityId, user.uid); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PersonRemove, null,
                                    Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    // ─── Mark Donation Dialog ─────────────────────────────────────────
    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            title = { Text("Mark Donation", fontWeight = FontWeight.Bold) },
            text = {
                Text("Mark ${user.name} as having donated today? This will update their last donation date and increment their donation count.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateMemberDonationStatus(user.uid, java.util.Date(), user.totalDonations + 1)
                    showDonationDialog = false
                }) { Text("Confirm", color = BloodRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDonationDialog = false }) { Text("Cancel") }
            },
        )
    }

    // ─── Override Availability Dialog ─────────────────────────────────
    if (showOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false },
            title = { Text("Override Availability", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Allow ${user.name} to donate again? They last donated ${user.lastDonationDate?.let { d -> "${d.daysSince()} days ago" } ?: "N/A"}. Minimum override requires ${Constants.MIN_OVERRIDE_DAYS} days since last donation.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.overrideMemberAvailability(user.uid)
                    showOverrideDialog = false
                }) { Text("Override", color = PendingAmber) }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideDialog = false }) { Text("Cancel") }
            },
        )
    }
}
