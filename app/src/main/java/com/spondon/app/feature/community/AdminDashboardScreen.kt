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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is CommunityEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
            state.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ─── Quick Stats ─────────────────
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            DashboardStatCard(
                                icon = Icons.Default.HourglassTop,
                                label = "Pending",
                                value = "${state.pendingCount}",
                                color = PendingAmber,
                                modifier = Modifier.weight(1f),
                            )
                            DashboardStatCard(
                                icon = Icons.Default.People,
                                label = "Members",
                                value = "${state.activeMembers}",
                                color = AvailableGreen,
                                modifier = Modifier.weight(1f),
                            )
                            DashboardStatCard(
                                icon = Icons.Default.Favorite,
                                label = "Donations",
                                value = "${state.monthlyDonations}",
                                color = SoftRose,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // ─── Pending Join Requests ───────
                    item {
                        Text(
                            "Pending Join Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (state.pendingRequests.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp),
                                            tint = AvailableGreen.copy(alpha = 0.5f),
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "No pending requests",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(state.pendingRequests, key = { it.id }) { request ->
                            JoinRequestCard(
                                request = request,
                                onApprove = {
                                    viewModel.approveJoinRequest(communityId, request.id, request.userId)
                                },
                                onReject = {
                                    viewModel.rejectJoinRequest(communityId, request.id, request.userId, null)
                                },
                            )
                        }
                    }

                    // ─── Member Management ──────────
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Member Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (state.members.isEmpty()) {
                        item {
                            Text(
                                "No members yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                        }
                    } else {
                        items(state.members, key = { it.uid }) { member ->
                            AdminMemberCard(
                                user = member,
                                community = state.community!!,
                                viewModel = viewModel,
                                communityId = communityId,
                            )
                        }
                    }

                    // ─── Broadcast Notification ────────
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Broadcast Notification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Send a notification to all community members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = state.broadcastMessage,
                                    onValueChange = { viewModel.updateBroadcastMessage(it) },
                                    label = { Text("Message") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 2,
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BloodRed,
                                        cursorColor = BloodRed,
                                    ),
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.sendBroadcastNotification(communityId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = state.broadcastMessage.isNotBlank() && !state.isBroadcasting,
                                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                ) {
                                    if (state.isBroadcasting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    } else {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(if (state.isBroadcasting) "Sending..." else "Send to All Members")
                                }
                            }
                        }
                    }

                    // ─── Edit Community Info ─────────
                    item {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { /* TODO: Navigate to edit community screen */ },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit Community Info")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = color)
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = request.userName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BloodRed,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.userName.ifEmpty { "Unknown User" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (request.userBloodGroup.isNotEmpty()) {
                            BloodGroupBadge(bloodGroup = request.userBloodGroup)
                        }
                        if (request.userDistrict.isNotEmpty()) {
                            Text(
                                request.userDistrict,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            // Message
            if (request.message.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(
                        text = "\"${request.message}\"",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reject")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AvailableGreen,
                    ),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Approve")
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.name.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
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
                        isAvailable = isAvailable,
                        daysRemaining = daysRemaining,
                    )
                }
            }

            // Actions menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    if (role == CommunityRole.MEMBER) {
                        DropdownMenuItem(
                            text = { Text("Promote to Moderator") },
                            onClick = {
                                viewModel.promoteMember(communityId, user.uid, CommunityRole.MODERATOR)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, Modifier.size(18.dp)) },
                        )
                    }
                    if (role == CommunityRole.MODERATOR) {
                        DropdownMenuItem(
                            text = { Text("Promote to Admin") },
                            onClick = {
                                viewModel.promoteMember(communityId, user.uid, CommunityRole.ADMIN)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, Modifier.size(18.dp)) },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Mark Donation") },
                        onClick = {
                            showDonationDialog = true
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Bloodtype, contentDescription = null, Modifier.size(18.dp)) },
                    )
                    if (canOverride) {
                        DropdownMenuItem(
                            text = { Text("Override Availability") },
                            onClick = {
                                showOverrideDialog = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, Modifier.size(18.dp)) },
                        )
                    }
                    if (role != CommunityRole.ADMIN) {
                        DropdownMenuItem(
                            text = { Text("Remove Member", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                viewModel.removeMember(communityId, user.uid)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = null,
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

    // ─── Mark Donation Dialog ────────────────────
    if (showDonationDialog) {
        AlertDialog(
            onDismissRequest = { showDonationDialog = false },
            title = { Text("Mark Donation", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Mark ${user.name} as having donated today? This will update their last donation date and increment their donation count.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMemberDonationStatus(
                            user.uid,
                            java.util.Date(),
                            user.totalDonations + 1,
                        )
                        showDonationDialog = false
                    },
                ) {
                    Text("Confirm", color = BloodRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDonationDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // ─── Override Availability Dialog ────────────
    if (showOverrideDialog) {
        val daysSinceLast = user.lastDonationDate?.daysSince()?.toInt() ?: 0
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false },
            title = { Text("Override Availability", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "${user.name}'s last donation was $daysSinceLast days ago " +
                        "(minimum ${Constants.MIN_OVERRIDE_DAYS} days required).",
                    )
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = PendingAmber.copy(alpha = 0.15f),
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PendingAmber,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "This is an early override. Use with caution. " +
                                "The standard cooldown is ${user.donationInterval} days.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.overrideMemberAvailability(user.uid)
                        showOverrideDialog = false
                    },
                ) {
                    Text("Override", color = BloodRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}