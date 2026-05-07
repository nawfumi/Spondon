package com.spondon.app.feature.superadmin.users

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

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
fun SAUserDetailScreen(
    navController: NavController,
    uid: String,
    viewModel: SAUserViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uid) { viewModel.loadUserDetail(uid) }

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
                    Text(
                        "User Profile",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
            }
        } else {
            val user = state.detail.user
            val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ─── Profile Header ──────────────────────────
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(SAGold.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SAGold,
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                user.name.ifBlank { "Unnamed" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )

                            // Blood group + role
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (user.bloodGroup.isNotBlank()) {
                                    DetailChip(user.bloodGroup, SARed)
                                }
                                DetailChip(
                                    if (user.isBanned) "BANNED" else "ACTIVE",
                                    if (user.isBanned) SARed else SAGreen,
                                )
                                if (user.isDonor) {
                                    DetailChip("DONOR", SABlue)
                                }
                                DetailChip(user.role, SAPurple)
                            }

                            // Ban reason
                            if (user.isBanned && !user.banReason.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = SARed.copy(alpha = 0.1f),
                                    ),
                                ) {
                                    Text(
                                        "Ban reason: ${user.banReason}",
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SARed.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── Profile Fields ──────────────────────────
                item {
                    SectionLabel("PROFILE INFORMATION")
                }

                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ProfileField(Icons.Outlined.Email, "Email", user.email.ifBlank { "—" })
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.05f),
                            )
                            ProfileField(Icons.Outlined.Phone, "Phone", user.phone.ifBlank { "—" })
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.05f),
                            )
                            ProfileField(Icons.Outlined.LocationOn, "District", user.district.ifBlank { "—" })
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.05f),
                            )
                            ProfileField(
                                Icons.Outlined.CalendarMonth,
                                "Joined",
                                user.createdAt?.let { dateFormat.format(it) } ?: "—",
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.05f),
                            )
                            ProfileField(
                                Icons.Outlined.Bloodtype,
                                "Total Donations",
                                "${user.totalDonations}",
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.05f),
                            )
                            ProfileField(
                                Icons.Outlined.Key,
                                "UID",
                                user.uid,
                            )
                        }
                    }
                }

                // ─── Communities ──────────────────────────────
                if (state.detail.communities.isNotEmpty()) {
                    item { SectionLabel("COMMUNITIES (${state.detail.communities.size})") }
                    items(state.detail.communities) { community ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SADarkCard),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Groups,
                                    null,
                                    tint = SAGold.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        community.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        "${community.memberCount} members",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── Blood Requests ──────────────────────────
                if (state.detail.requests.isNotEmpty()) {
                    item { SectionLabel("BLOOD REQUESTS (${state.detail.requests.size})") }
                    items(state.detail.requests) { request ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SADarkCard),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Blood group badge
                                Card(
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = SARed.copy(alpha = 0.15f),
                                    ),
                                ) {
                                    Text(
                                        request.bloodGroup,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = SARed,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        request.hospital.ifBlank { "No hospital" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${request.urgency} • ${request.status}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (request.urgency) {
                                            "CRITICAL" -> SARed
                                            "MODERATE" -> SAOrange
                                            else -> SAGreen
                                        }.copy(alpha = 0.7f),
                                    )
                                }
                                if (request.createdAt != null) {
                                    Text(
                                        dateFormat.format(request.createdAt),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.3f),
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── Action Buttons ──────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("ACTIONS")
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Ban / Unban
                        if (user.isBanned) {
                            SAActionButton(
                                icon = Icons.Outlined.LockOpen,
                                label = "Unban User",
                                color = SAGreen,
                                isLoading = state.isBanning,
                                onClick = { viewModel.unbanUser() },
                            )
                        } else {
                            SAActionButton(
                                icon = Icons.Outlined.Block,
                                label = "Ban User",
                                color = SAOrange,
                                isLoading = state.isBanning,
                                onClick = { viewModel.showBanDialog() },
                            )
                        }

                        // Send Notification
                        SAActionButton(
                            icon = Icons.Outlined.Notifications,
                            label = "Send Notification",
                            color = SABlue,
                            isLoading = state.isSendingNotification,
                            onClick = { viewModel.showNotifyDialog() },
                        )

                        // Delete User
                        SAActionButton(
                            icon = Icons.Outlined.DeleteForever,
                            label = "Delete User",
                            color = SARed,
                            isLoading = state.isDeleting,
                            onClick = { viewModel.showDeleteDialog() },
                        )
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }

            // ─── Ban Dialog ──────────────────────────────────
            if (state.showBanDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.hideBanDialog() },
                    containerColor = SADarkCard,
                    title = {
                        Text(
                            "Ban User",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    },
                    text = {
                        Column {
                            Text(
                                "This will immediately block ${user.name} from accessing the app.",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.banReason,
                                onValueChange = viewModel::updateBanReason,
                                label = { Text("Ban Reason *", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SAOrange,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    cursorColor = SAGold,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.banUser() },
                            enabled = state.banReason.isNotBlank() && !state.isBanning,
                            colors = ButtonDefaults.buttonColors(containerColor = SAOrange),
                        ) {
                            if (state.isBanning) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Text("Ban", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.hideBanDialog() }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                )
            }

            // ─── Delete Dialog ───────────────────────────────
            if (state.showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.hideDeleteDialog() },
                    containerColor = SADarkCard,
                    title = {
                        Text(
                            "⚠️ Delete User",
                            fontWeight = FontWeight.Bold,
                            color = SARed,
                        )
                    },
                    text = {
                        Column {
                            Text(
                                "This action is IRREVERSIBLE. All of ${user.name}'s data, requests, and community memberships will be permanently deleted.",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Type \"${user.name}\" to confirm:",
                                color = SARed.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.deleteConfirmName,
                                onValueChange = viewModel::updateDeleteConfirmName,
                                placeholder = {
                                    Text(user.name, color = Color.White.copy(alpha = 0.2f))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SARed,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    cursorColor = SARed,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.deleteUser { navController.popBackStack() } },
                            enabled = state.deleteConfirmName.trim() == user.name.trim()
                                    && !state.isDeleting,
                            colors = ButtonDefaults.buttonColors(containerColor = SARed),
                        ) {
                            if (state.isDeleting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Text("Delete Permanently", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                )
            }

            // ─── Notification Dialog ─────────────────────────
            if (state.showNotifyDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.hideNotifyDialog() },
                    containerColor = SADarkCard,
                    title = {
                        Text(
                            "Send Notification",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Send a notification to ${user.name}",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                            )

                            // Type selector
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("INFO", "WARNING", "ALERT").forEach { type ->
                                    val isSelected = state.notifyType == type
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateNotifyType(type) },
                                        label = {
                                            Text(
                                                type,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (type) {
                                                "WARNING" -> SAOrange
                                                "ALERT" -> SARed
                                                else -> SABlue
                                            }.copy(alpha = 0.2f),
                                            selectedLabelColor = when (type) {
                                                "WARNING" -> SAOrange
                                                "ALERT" -> SARed
                                                else -> SABlue
                                            },
                                            containerColor = Color.White.copy(alpha = 0.05f),
                                            labelColor = Color.White.copy(alpha = 0.5f),
                                        ),
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = state.notifyTitle,
                                onValueChange = viewModel::updateNotifyTitle,
                                label = { Text("Title", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SABlue,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    cursorColor = SAGold,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                            )

                            OutlinedTextField(
                                value = state.notifyBody,
                                onValueChange = viewModel::updateNotifyBody,
                                label = { Text("Message", color = Color.White.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SABlue,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    cursorColor = SAGold,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.sendNotification() },
                            enabled = state.notifyTitle.isNotBlank()
                                    && state.notifyBody.isNotBlank()
                                    && !state.isSendingNotification,
                            colors = ButtonDefaults.buttonColors(containerColor = SABlue),
                        ) {
                            if (state.isSendingNotification) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                Icon(Icons.Outlined.Send, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Send", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.hideNotifyDialog() }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                )
            }
        }
    }
}

// ─── Reusable Components ─────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.sp,
        ),
        color = SAGold.copy(alpha = 0.4f),
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun DetailChip(text: String, color: Color) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f),
        ),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            ),
            color = color,
        )
    }
}

@Composable
private fun ProfileField(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            null,
            tint = SAGold.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SAActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.12f),
            contentColor = color,
        ),
        enabled = !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = color,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
