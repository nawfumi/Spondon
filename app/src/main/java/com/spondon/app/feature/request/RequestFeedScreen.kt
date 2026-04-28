package com.spondon.app.feature.request

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.spondon.app.core.domain.model.BloodRequest
import com.spondon.app.core.domain.model.RequestStatus
import com.spondon.app.core.domain.model.Urgency
import com.spondon.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestFeedScreen(
    navController: NavController,
    viewModel: RequestViewModel = hiltViewModel(),
) {
    val state by viewModel.feedState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadFeed() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blood Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_request") },
                containerColor = BloodRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Add, "Create Request")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ─── Tabs ────────────────────────────────────────
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = BloodRed,
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.setFeedTab(0) },
                    text = { Text("Feed", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = BloodRed,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.setFeedTab(1) },
                    text = { Text("My Requests", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = BloodRed,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = BloodRed, strokeWidth = 2.dp)
                    }
                }

                state.selectedTab == 0 -> {
                    // ─── Community Feed ──────────────────────
                    if (state.feedRequests.isEmpty()) {
                        EmptyFeedMessage("No blood requests from your communities yet")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.feedRequests, key = { it.id }) { request ->
                                RequestCard(
                                    request = request,
                                    onClick = {
                                        navController.navigate("request_detail/${request.id}")
                                    },
                                )
                            }
                        }
                    }
                }

                state.selectedTab == 1 -> {
                    // ─── My Requests ─────────────────────────
                    if (state.myRequests.isEmpty()) {
                        EmptyFeedMessage("You haven't created any blood requests yet")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.myRequests, key = { it.id }) { request ->
                                MyRequestCard(
                                    request = request,
                                    onClick = {
                                        navController.navigate("request_detail/${request.id}")
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Bloodtype,
                null,
                tint = BloodRed.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun MyRequestCard(
    request: BloodRequest,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Blood group
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = request.bloodGroup,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = BloodRed,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.hospital.ifBlank { "Hospital" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UrgencyTag(request.urgency)
                    Text(
                        text = "${request.respondents.size} responded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
            }

            // Status chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (request.status) {
                    RequestStatus.ACTIVE -> PendingAmber.copy(alpha = 0.15f)
                    RequestStatus.FULFILLED -> AvailableGreen.copy(alpha = 0.15f)
                    RequestStatus.CANCELLED -> UrgencyCritical.copy(alpha = 0.15f)
                    RequestStatus.EXPIRED -> UnavailableGrey.copy(alpha = 0.15f)
                },
            ) {
                Text(
                    text = request.status.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    ),
                    color = when (request.status) {
                        RequestStatus.ACTIVE -> PendingAmber
                        RequestStatus.FULFILLED -> AvailableGreen
                        RequestStatus.CANCELLED -> UrgencyCritical
                        RequestStatus.EXPIRED -> UnavailableGrey
                    },
                )
            }
        }
    }
}