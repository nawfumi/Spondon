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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAPrivacyScreen(
    navController: NavController,
    viewModel: SAPrivacyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }

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
                // ─── Privacy Toggle Card ─────────────────────
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
                                // Icon
                                Card(
                                    shape = RoundedCornerShape(50),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (state.isPrivacyEnabled)
                                            SAOrange.copy(alpha = 0.15f)
                                        else
                                            SAGreen.copy(alpha = 0.15f),
                                    ),
                                ) {
                                    Icon(
                                        if (state.isPrivacyEnabled)
                                            Icons.Filled.VisibilityOff
                                        else
                                            Icons.Filled.Visibility,
                                        null,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .size(28.dp),
                                        tint = if (state.isPrivacyEnabled) SAOrange else SAGreen,
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Member Privacy Mode",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        if (state.isPrivacyEnabled)
                                            "Sensitive data is hidden from members"
                                        else
                                            "All members can see each other's data",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                    )
                                }

                                Switch(
                                    checked = state.isPrivacyEnabled,
                                    onCheckedChange = { viewModel.togglePrivacy() },
                                    enabled = !state.isToggling,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SAOrange,
                                        checkedTrackColor = SAOrange.copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                                    ),
                                )
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
                                        "When enabled, regular members cannot see:\n" +
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

                // ─── Status Badge ────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.isPrivacyEnabled)
                                SAOrange.copy(alpha = 0.1f)
                            else
                                SAGreen.copy(alpha = 0.1f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (state.isPrivacyEnabled) Icons.Filled.Shield else Icons.Outlined.ShieldMoon,
                                null,
                                tint = if (state.isPrivacyEnabled) SAOrange else SAGreen,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (state.isPrivacyEnabled)
                                    "PRIVACY MODE ACTIVE"
                                else
                                    "PRIVACY MODE OFF",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                ),
                                color = if (state.isPrivacyEnabled) SAOrange else SAGreen,
                            )
                        }
                    }
                }

                // ─── Authorized Admins Section ───────────────
                if (state.isPrivacyEnabled) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "AUTHORIZED ADMINS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                ),
                                color = SAGold.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f),
                            )
                            FilledTonalButton(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SAGold.copy(alpha = 0.15f),
                                    contentColor = SAGold,
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Add Admin",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
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
                                        "Add admins who can view private member data",
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
    }

    // ─── Add Admin Dialog ────────────────────────────────────
    if (showAddDialog) {
        val filteredUsers = viewModel.getFilteredUsers()

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                viewModel.updateSearchQuery("")
            },
            containerColor = SADarkCard,
            title = {
                Text(
                    "Grant Privacy Access",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Search by name, email, or phone…",
                                color = Color.White.copy(alpha = 0.3f),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                null,
                                tint = Color.White.copy(alpha = 0.3f),
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SAGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = SAGold,
                        ),
                    )

                    Spacer(Modifier.height(12.dp))

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (state.searchQuery.isNotBlank()) "No users found"
                                else "Type to search users",
                                color = Color.White.copy(alpha = 0.3f),
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(
                                filteredUsers.take(20),
                                key = { it.uid },
                            ) { user ->
                                Card(
                                    onClick = {
                                        viewModel.grantAccess(user.uid)
                                        showAddDialog = false
                                        viewModel.updateSearchQuery("")
                                    },
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
                                        // Avatar
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

                                        Icon(
                                            Icons.Default.Add,
                                            null,
                                            tint = SAGreen,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    viewModel.updateSearchQuery("")
                }) {
                    Text("Close", color = Color.White.copy(alpha = 0.5f))
                }
            },
        )
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
            // Avatar
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

            // Revoke button
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
