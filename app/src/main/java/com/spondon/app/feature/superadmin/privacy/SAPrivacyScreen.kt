package com.spondon.app.feature.superadmin.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

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
fun SAPrivacyScreen(
    navController: NavController,
    viewModel: SAPrivacyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var showCommunityDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Show success messages
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    // Show error messages
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = SAGold,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Privacy Control",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SADark,
                ),
            )
        },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ─── Status Card ─────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Card(
                                    shape = RoundedCornerShape(50),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (state.protectedUserIds.isNotEmpty())
                                            SAOrange.copy(alpha = 0.15f)
                                        else
                                            SAGreen.copy(alpha = 0.15f),
                                    ),
                                ) {
                                    Icon(
                                        if (state.protectedUserIds.isNotEmpty())
                                            Icons.Filled.Shield
                                        else
                                            Icons.Outlined.ShieldMoon,
                                        null,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .size(28.dp),
                                        tint = if (state.protectedUserIds.isNotEmpty()) SAOrange else SAGreen,
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Per-User Privacy",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        if (state.protectedUserIds.isNotEmpty())
                                            "${state.protectedUserIds.size} user${if (state.protectedUserIds.size > 1) "s" else ""} protected"
                                        else
                                            "No users are currently protected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(Modifier.height(12.dp))

                            // Info text
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = SABlue.copy(alpha = 0.08f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        null,
                                        tint = SABlue,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Select specific users or entire communities to protect. " +
                                                "Protected users' data is hidden:\n" +
                                                "• Availability status\n" +
                                                "• Last donation date\n" +
                                                "• Contact number\n\n" +
                                                "Only SuperAdmin and authorized admins can see this data.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SABlue.copy(alpha = 0.8f),
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── Action Buttons ──────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Add individual user
                        FilledTonalButton(
                            onClick = { showAddUserDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SAGold.copy(alpha = 0.15f),
                                contentColor = SAGold,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add User", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        // Protect community
                        FilledTonalButton(
                            onClick = { showCommunityDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SAPurple.copy(alpha = 0.15f),
                                contentColor = SAPurple,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Groups, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Community", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // ─── Protected Users Section ─────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "PROTECTED USERS (${state.protectedUserIds.size})",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = SAOrange.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                        )
                        if (state.protectedUserIds.isNotEmpty()) {
                            TextButton(onClick = { showClearConfirm = true }) {
                                Text(
                                    "Clear All",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SARed.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }

                if (state.protectedUsers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SADarkCard),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Outlined.Shield,
                                    null,
                                    tint = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No protected users",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.3f),
                                )
                                Text(
                                    "Add users or communities to protect their data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.2f),
                                )
                            }
                        }
                    }
                } else {
                    items(state.protectedUsers, key = { it.uid }) { user ->
                        ProtectedUserCard(
                            user = user,
                            onRemove = { viewModel.removeProtectedUser(user.uid) },
                        )
                    }
                }

                // ─── Authorized Admins Section ───────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "AUTHORIZED ADMINS",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = SAGold.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalButton(
                            onClick = { showAddAdminDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SAGold.copy(alpha = 0.15f),
                                contentColor = SAGold,
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Add Admin",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }

                if (state.authorizedAdmins.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SADarkCard),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Outlined.AdminPanelSettings,
                                    null,
                                    tint = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No authorized admins yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.3f),
                                )
                                Text(
                                    "Add admins who can view protected member data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.2f),
                                )
                            }
                        }
                    }
                } else {
                    items(state.authorizedAdmins, key = { it.uid }) { admin ->
                        AuthorizedAdminCard(
                            admin = admin,
                            onRevoke = { viewModel.revokeAccess(admin.uid) },
                        )
                    }
                }
            }
        }
    }

    // ─── Add Protected User Dialog ───────────────────────────
    if (showAddUserDialog) {
        val filteredUsers = viewModel.getFilteredUsersForProtection()

        AlertDialog(
            onDismissRequest = {
                showAddUserDialog = false
                viewModel.updateSearchQuery("")
            },
            containerColor = SADarkCard,
            title = {
                Text("Protect User", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "Select a user to hide their availability, donation date, and contact from non-authorized members.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.height(12.dp))
                    SearchField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = "Search by name, email, or phone…",
                    )
                    Spacer(Modifier.height(12.dp))
                    UserSearchResults(
                        users = filteredUsers,
                        emptyText = if (state.searchQuery.isNotBlank()) "No users found" else "Type to search users",
                        onUserClick = { user ->
                            viewModel.addProtectedUser(user.uid)
                            showAddUserDialog = false
                            viewModel.updateSearchQuery("")
                        },
                        actionIcon = Icons.Default.Shield,
                        actionColor = SAOrange,
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showAddUserDialog = false
                    viewModel.updateSearchQuery("")
                }) {
                    Text("Close", color = Color.White.copy(alpha = 0.5f))
                }
            },
        )
    }

    // ─── Community Protection Dialog ─────────────────────────
    if (showCommunityDialog) {
        AlertDialog(
            onDismissRequest = { showCommunityDialog = false },
            containerColor = SADarkCard,
            title = {
                Text("Protect by Community", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "Add or remove all members of a community from the protected list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.height(12.dp))

                    if (state.communities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No communities found", color = Color.White.copy(alpha = 0.3f))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(state.communities, key = { it.id }) { community ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(SAPurple.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Default.Groups,
                                                null,
                                                tint = SAPurple,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                community.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "${community.memberCount} members",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.4f),
                                            )
                                        }

                                        // Protect / Unprotect buttons
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.protectCommunity(community.id)
                                                    showCommunityDialog = false
                                                },
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = SAOrange.copy(alpha = 0.15f),
                                                    contentColor = SAOrange,
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            ) {
                                                Icon(Icons.Default.Shield, null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Add", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.unprotectCommunity(community.id)
                                                    showCommunityDialog = false
                                                },
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = SARed.copy(alpha = 0.15f),
                                                    contentColor = SARed,
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            ) {
                                                Icon(Icons.Default.RemoveCircleOutline, null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Remove", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCommunityDialog = false }) {
                    Text("Close", color = Color.White.copy(alpha = 0.5f))
                }
            },
        )
    }

    // ─── Add Admin Dialog ────────────────────────────────────
    if (showAddAdminDialog) {
        val filteredUsers = viewModel.getFilteredUsersForAuthorization()

        AlertDialog(
            onDismissRequest = {
                showAddAdminDialog = false
                viewModel.updateSearchQuery("")
            },
            containerColor = SADarkCard,
            title = {
                Text("Grant Privacy Access", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Column {
                    SearchField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = "Search by name, email, or phone…",
                    )
                    Spacer(Modifier.height(12.dp))
                    UserSearchResults(
                        users = filteredUsers,
                        emptyText = if (state.searchQuery.isNotBlank()) "No users found" else "Type to search users",
                        onUserClick = { user ->
                            viewModel.grantAccess(user.uid)
                            showAddAdminDialog = false
                            viewModel.updateSearchQuery("")
                        },
                        actionIcon = Icons.Default.Add,
                        actionColor = SAGreen,
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showAddAdminDialog = false
                    viewModel.updateSearchQuery("")
                }) {
                    Text("Close", color = Color.White.copy(alpha = 0.5f))
                }
            },
        )
    }

    // ─── Clear All Confirmation ──────────────────────────────
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = SADarkCard,
            title = { Text("Clear All Protected Users?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "This will remove all ${state.protectedUserIds.size} users from the protected list. " +
                            "Their data will become visible to all members again.",
                    color = Color.White.copy(alpha = 0.6f),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllProtected()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SARed),
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            },
        )
    }
}

// ─── Reusable Components ─────────────────────────────────────

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = Color.White.copy(alpha = 0.3f))
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.3f))
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SAGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = SAGold,
        ),
    )
}

@Composable
private fun UserSearchResults(
    users: List<com.spondon.app.feature.superadmin.users.SAUserItem>,
    emptyText: String,
    onUserClick: (com.spondon.app.feature.superadmin.users.SAUserItem) -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionColor: Color,
) {
    if (users.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(emptyText, color = Color.White.copy(alpha = 0.3f))
        }
    } else {
        LazyColumn(
            modifier = Modifier.heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(users.take(20), key = { it.uid }) { user ->
                Card(
                    onClick = { onUserClick(user) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SAGold.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                user.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SAGold,
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                user.name.ifEmpty { "Unknown" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                user.email.ifEmpty { user.phone },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Icon(actionIcon, null, tint = actionColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtectedUserCard(
    user: com.spondon.app.feature.superadmin.users.SAUserItem,
    onRemove: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SADarkCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SAOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    user.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SAOrange,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user.bloodGroup.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SARed.copy(alpha = 0.15f),
                            ),
                        ) {
                            Text(
                                user.bloodGroup,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SARed,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        user.email.ifEmpty { user.phone },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Unprotect button
            if (showConfirm) {
                Row {
                    TextButton(onClick = { showConfirm = false }) {
                        Text("Cancel", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                    }
                    Button(
                        onClick = {
                            onRemove()
                            showConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SARed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text("Remove", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                IconButton(onClick = { showConfirm = true }) {
                    Icon(
                        Icons.Default.RemoveCircleOutline,
                        contentDescription = "Remove protection",
                        tint = SARed.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthorizedAdminCard(
    admin: com.spondon.app.feature.superadmin.users.SAUserItem,
    onRevoke: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SADarkCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SAGold.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    admin.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SAGold,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    admin.name.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (admin.bloodGroup.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SARed.copy(alpha = 0.15f),
                            ),
                        ) {
                            Text(
                                admin.bloodGroup,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SARed,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        admin.email.ifEmpty { admin.phone },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (showConfirm) {
                Row {
                    TextButton(onClick = { showConfirm = false }) {
                        Text("Cancel", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                    }
                    Button(
                        onClick = {
                            onRevoke()
                            showConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SARed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text("Revoke", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                IconButton(onClick = { showConfirm = true }) {
                    Icon(
                        Icons.Default.RemoveCircleOutline,
                        contentDescription = "Revoke access",
                        tint = SARed.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
