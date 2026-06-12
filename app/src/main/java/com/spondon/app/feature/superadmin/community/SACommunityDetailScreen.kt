package com.spondon.app.feature.superadmin.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Locale

private val SAGold = Color(0xFFFFD700)
private val SADark = Color(0xFF0D0D0D)
private val SADarkCard = Color(0xFF1A1A2E)
private val SAGreen = Color(0xFF4CAF50)
private val SARed = Color(0xFFEF5350)
private val SABlue = Color(0xFF42A5F5)
private val SAOrange = Color(0xFFFFA726)
private val SAPurple = Color(0xFFAB47BC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SACommunityDetailScreen(
    navController: NavController,
    communityId: String,
    viewModel: SACommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(communityId) { viewModel.loadCommunityDetail(communityId) }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    val community = state.detail.community

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        community.name.ifBlank { "Community" },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
        containerColor = SADark,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Error", color = SARed)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ─── Community Info Card ──────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = SAPurple.copy(alpha = 0.1f)),
                                ) {
                                    Icon(
                                        Icons.Outlined.Groups,
                                        null,
                                        tint = SAPurple,
                                        modifier = Modifier.padding(12.dp).size(28.dp),
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        community.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                    val (statusColor, statusText) = when (community.status) {
                                        "VERIFIED" -> SAGreen to "✓ Verified"
                                        "SUSPENDED" -> SARed to "⊘ Suspended"
                                        else -> SAOrange to "⚬ Unverified"
                                    }
                                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                                }
                            }

                            if (community.description.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    community.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            Spacer(Modifier.height(12.dp))

                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                            ) {
                                CommunityStatChip("Members", "${community.memberCount}", SABlue)
                                CommunityStatChip("Type", community.type.lowercase().replaceFirstChar { it.uppercase() }, SAPurple)
                                CommunityStatChip("District", community.district.ifBlank { "—" }, SAOrange)
                            }

                            community.createdAt?.let { date ->
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Created: ${dateFormat.format(date)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.3f),
                                )
                            }
                        }
                    }
                }

                // ─── Action Buttons ──────────────────────────
                item {
                    Text(
                        "ACTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Verify / Unverify
                        if (community.status != "VERIFIED") {
                            Button(
                                onClick = { viewModel.verifyCommunity() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SAGreen),
                                enabled = !state.isPerformingAction,
                            ) {
                                Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Verify", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Suspend / Unsuspend
                        if (community.status == "SUSPENDED") {
                            Button(
                                onClick = { viewModel.unsuspendCommunity() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SABlue),
                                enabled = !state.isPerformingAction,
                            ) {
                                Icon(Icons.Outlined.PlayArrow, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reactivate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.suspendCommunity() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SAOrange),
                                enabled = !state.isPerformingAction,
                            ) {
                                Icon(Icons.Outlined.Block, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Suspend", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Delete
                        Button(
                            onClick = { viewModel.showDeleteDialog() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SARed),
                            enabled = !state.isPerformingAction,
                        ) {
                            Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // ─── Serial Toggle ──────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = SAGold.copy(alpha = 0.1f),
                                ),
                            ) {
                                Icon(
                                    Icons.Outlined.Tag,
                                    null,
                                    tint = SAGold,
                                    modifier = Modifier.padding(8.dp).size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Serial IDs",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Text(
                                    if (community.isSerialEnabled) "Enabled — members can have serial numbers"
                                    else "Disabled — no serial tracking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.4f),
                                )
                            }
                            Switch(
                                checked = community.isSerialEnabled,
                                onCheckedChange = { viewModel.toggleSerialForCommunity() },
                                enabled = !state.isPerformingAction,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SAGold,
                                    checkedTrackColor = SAGold.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                                ),
                            )
                        }
                    }
                }

                // ─── Members ─────────────────────────────────
                item {
                    Text(
                        "MEMBERS (${state.detail.members.size})",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(state.detail.members) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (member.isAdmin) SAGold.copy(alpha = 0.1f) else SABlue.copy(alpha = 0.1f),
                                ),
                            ) {
                                Icon(
                                    if (member.isAdmin) Icons.Outlined.AdminPanelSettings else Icons.Outlined.Person,
                                    null,
                                    tint = if (member.isAdmin) SAGold else SABlue,
                                    modifier = Modifier.padding(8.dp).size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        member.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White,
                                    )
                                    if (member.isAdmin) {
                                        Spacer(Modifier.width(6.dp))
                                        Card(
                                            shape = RoundedCornerShape(3.dp),
                                            colors = CardDefaults.cardColors(containerColor = SAGold.copy(alpha = 0.15f)),
                                        ) {
                                            Text(
                                                "ADMIN",
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                color = SAGold,
                                            )
                                        }
                                    }
                                }
                                if (member.bloodGroup.isNotBlank()) {
                                    Text(
                                        member.bloodGroup,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SARed.copy(alpha = 0.7f),
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.showRemoveMemberDialog(member) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.PersonRemove,
                                    null,
                                    tint = SARed.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                // ─── Requests ────────────────────────────────
                if (state.detail.requests.isNotEmpty()) {
                    item {
                        Text(
                            "REQUESTS (${state.detail.requests.size})",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = SAGold.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    items(state.detail.requests) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SADarkCard),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = SARed.copy(alpha = 0.1f)),
                                ) {
                                    Icon(
                                        Icons.Outlined.Bloodtype,
                                        null,
                                        tint = SARed,
                                        modifier = Modifier.padding(8.dp).size(16.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${req.bloodGroup} — ${req.hospital}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White,
                                    )
                                    Text(
                                        "${req.urgency} • ${req.status}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        // ─── Delete Confirmation Dialog ──────────────────
        if (state.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteDialog() },
                title = {
                    Text("Delete Community", color = SARed, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            "This will permanently delete the community and all its data. Type the community name to confirm:",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            community.name,
                            fontWeight = FontWeight.Bold,
                            color = SAGold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.deleteConfirmName,
                            onValueChange = viewModel::updateDeleteConfirmName,
                            placeholder = { Text("Type community name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteCommunity { navController.popBackStack() } },
                        enabled = !state.isDeleting &&
                                state.deleteConfirmName.trim() == community.name.trim(),
                    ) {
                        Text(
                            if (state.isDeleting) "Deleting..." else "Delete",
                            color = SARed,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                        Text("Cancel")
                    }
                },
                containerColor = SADarkCard,
            )
        }

        // ─── Remove Member Dialog ────────────────────────
        if (state.showRemoveMemberDialog && state.memberToRemove != null) {
            AlertDialog(
                onDismissRequest = { viewModel.hideRemoveMemberDialog() },
                title = {
                    Text("Remove Member", color = SAOrange, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Remove ${state.memberToRemove!!.name} from ${community.name}? This overrides community admin controls.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.forceRemoveMember() },
                        enabled = !state.isRemovingMember,
                    ) {
                        Text(
                            if (state.isRemovingMember) "Removing..." else "Remove",
                            color = SARed,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideRemoveMemberDialog() }) {
                        Text("Cancel")
                    }
                },
                containerColor = SADarkCard,
            )
        }
    }
}

@Composable
private fun CommunityStatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
        )
    }
}
