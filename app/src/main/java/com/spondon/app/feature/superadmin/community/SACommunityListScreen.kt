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

private val SAGold = Color(0xFFFFD700)
private val SADark = Color(0xFF0D0D0D)
private val SADarkCard = Color(0xFF1A1A2E)
private val SAGreen = Color(0xFF4CAF50)
private val SARed = Color(0xFFEF5350)
private val SAOrange = Color(0xFFFFA726)
private val SAPurple = Color(0xFFAB47BC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SACommunityListScreen(
    navController: NavController,
    viewModel: SACommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.listState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Groups, null, tint = SAGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Communities", fontWeight = FontWeight.Bold, color = Color.White)
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
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Search communities...", color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, null, tint = SAGold.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                    cursorColor = SAGold,
                    focusedBorderColor = SAGold.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = SADarkCard,
                    unfocusedContainerColor = SADarkCard,
                ),
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SACommunityFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    SACommunityFilter.ALL -> "All"
                                    SACommunityFilter.VERIFIED -> "Verified"
                                    SACommunityFilter.UNVERIFIED -> "Unverified"
                                    SACommunityFilter.SUSPENDED -> "Suspended"
                                },
                                style = MaterialTheme.typography.labelSmall,
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
                            selected = state.filter == filter,
                        ),
                    )
                }
            }

            // Content
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
                }
            } else if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.error ?: "Error", color = SARed)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "${state.filteredCommunities.size} communities",
                            style = MaterialTheme.typography.labelSmall,
                            color = SAGold.copy(alpha = 0.4f),
                        )
                    }
                    items(state.filteredCommunities, key = { it.id }) { community ->
                        SACommunityCard(
                            community = community,
                            onClick = { navController.navigate("sa_community_detail/${community.id}") },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SACommunityCard(
    community: SACommunityItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
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
            // Community icon
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SAPurple.copy(alpha = 0.1f),
                ),
            ) {
                Icon(
                    Icons.Outlined.Groups,
                    null,
                    tint = SAPurple,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        community.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    // Status chip
                    val (statusColor, statusText) = when (community.status) {
                        "VERIFIED" -> SAGreen to "Verified"
                        "SUSPENDED" -> SARed to "Suspended"
                        else -> SAOrange to "Unverified"
                    }
                    Card(
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
                    ) {
                        Text(
                            statusText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = statusColor,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row {
                    Text(
                        "${community.memberCount} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                    if (community.district.isNotBlank()) {
                        Text(
                            " • ${community.district}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f),
                        )
                    }
                    Text(
                        " • ${community.type.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            Icon(
                Icons.Outlined.ChevronRight,
                null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
