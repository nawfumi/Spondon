package com.spondon.app.feature.superadmin.users

import androidx.compose.animation.*
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
private val SADarkSurface = Color(0xFF16213E)
private val SAGreen = Color(0xFF4CAF50)
private val SARed = Color(0xFFEF5350)
private val SABlue = Color(0xFF42A5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAUserListScreen(
    navController: NavController,
    viewModel: SAUserViewModel = hiltViewModel(),
) {
    val state by viewModel.listState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.People,
                            contentDescription = null,
                            tint = SAGold,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "User Management",
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
        containerColor = SADark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ─── Search Bar ──────────────────────────────────
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = {
                    Text(
                        "Search by name, email, phone, blood group...",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 13.sp,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, null, tint = SAGold.copy(alpha = 0.6f))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SAGold.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    cursorColor = SAGold,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )

            // ─── Filter Chips ────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SAUserFilter.entries.forEach { filter ->
                    val isSelected = state.filter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    SAUserFilter.ALL -> "All"
                                    SAUserFilter.ACTIVE -> "Active"
                                    SAUserFilter.BANNED -> "Banned"
                                    SAUserFilter.DONORS -> "Donors"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SAGold.copy(alpha = 0.2f),
                            selectedLabelColor = SAGold,
                            containerColor = SADarkCard,
                            labelColor = Color.White.copy(alpha = 0.6f),
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = SAGold.copy(alpha = 0.3f),
                            enabled = true,
                            selected = isSelected,
                        ),
                    )
                }
            }

            // ─── Sort Row ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.filteredUsers.size} users",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SAUserSort.entries.forEach { sort ->
                        val isSelected = state.sort == sort
                        TextButton(
                            onClick = { viewModel.setSort(sort) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isSelected) SAGold else Color.White.copy(alpha = 0.4f),
                            ),
                        ) {
                            Text(
                                when (sort) {
                                    SAUserSort.NEWEST -> "Newest"
                                    SAUserSort.MOST_DONATIONS -> "Top Donors"
                                    SAUserSort.NAME -> "A→Z"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            // ─── User List ──────────────────────────────────
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
                }
            } else if (state.filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.PersonSearch,
                            null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No users found",
                            color = Color.White.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.filteredUsers, key = { it.uid }) { user ->
                        SAUserCard(
                            user = user,
                            onClick = { navController.navigate("sa_user_detail/${user.uid}") },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SAUserCard(
    user: SAUserItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SADarkCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SAGold.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = SAGold,
                    fontSize = 18.sp,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.name.ifBlank { "Unnamed" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    // Blood group badge
                    if (user.bloodGroup.isNotBlank()) {
                        Card(
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SARed.copy(alpha = 0.15f),
                            ),
                        ) {
                            Text(
                                user.bloodGroup,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = SARed,
                            )
                        }
                    }
                }

                // Sub-info
                Text(
                    buildString {
                        if (user.email.isNotBlank()) append(user.email)
                        if (user.district.isNotBlank()) append(" • ${user.district}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Status chips row
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Account status
                    StatusMiniChip(
                        text = if (user.isBanned) "Banned" else "Active",
                        color = if (user.isBanned) SARed else SAGreen,
                    )
                    // Donor badge
                    if (user.isDonor) {
                        StatusMiniChip(text = "Donor", color = SABlue)
                    }
                    // Donations count
                    if (user.totalDonations > 0) {
                        StatusMiniChip(
                            text = "${user.totalDonations} donations",
                            color = SAGold,
                        )
                    }
                }
            }

            // Chevron
            Icon(
                Icons.Outlined.ChevronRight,
                null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun StatusMiniChip(text: String, color: Color) {
    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f),
        ),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = color,
        )
    }
}
