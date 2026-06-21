package com.spondon.app.feature.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.RequestStatus
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.BloodRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestFeedScreen(
    navController: NavController,
    viewModel: RequestViewModel = hiltViewModel(),
) {
    val state by viewModel.feedState.collectAsState()
    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
    val s = S.strings

    LaunchedEffect(Unit) { viewModel.loadFeed() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.bloodRequests, fontWeight = FontWeight.Bold) },
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
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("create_request") },
                containerColor = BloodRed,
                contentColor = Color.White,
                text = {Text("Request")},
                icon = {Icon(Icons.Filled.Bloodtype, "Create Request")}
            )
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
                    text = { Text(s.feed, fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = BloodRed,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.setFeedTab(1) },
                    text = { Text(s.myRequests, fontWeight = FontWeight.SemiBold) },
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
                        ContainedLoadingIndicator()
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
                                    currentUserId = currentUserId,
                                    onClick = {
                                        navController.navigate("request_detail/${request.id}")
                                    },
                                    onCancelRequest = {
                                        viewModel.deleteRequest(request.id)
                                    },
                                    onMarkFulfilled = {
                                        viewModel.updateRequestStatusById(request.id, RequestStatus.FULFILLED)
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
                                RequestCard(
                                    request = request,
                                    currentUserId = currentUserId,
                                    onClick = {
                                        navController.navigate("request_detail/${request.id}")
                                    },
                                    onCancelRequest = {
                                        viewModel.deleteRequest(request.id)
                                    },
                                    onMarkFulfilled = {
                                        viewModel.updateRequestStatusById(request.id, RequestStatus.FULFILLED)
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

// MyRequestCard removed in favor of standard RequestCard