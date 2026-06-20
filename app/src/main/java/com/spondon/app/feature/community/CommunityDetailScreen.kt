package com.spondon.app.feature.community

import com.spondon.app.core.common.formatDisplay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.core.domain.model.*
import com.spondon.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsState()

    // Extract communityId from navController back stack
    val communityId = navController.currentBackStackEntry
        ?.arguments?.getString("communityId") ?: ""

    LaunchedEffect(communityId) {
        if (communityId.isNotEmpty()) viewModel.loadCommunityDetail(communityId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityEvent.ShowSnackbar -> { /* handled by snackbar host */ }
                is CommunityEvent.NavigateBack -> navController.popBackStack()
                else -> {}
            }
        }
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
        floatingActionButton = {
            val isAdminOrMod = state.currentUserRole == CommunityRole.ADMIN ||
                    state.currentUserRole == CommunityRole.MODERATOR
            if (isAdminOrMod && state.community != null) {
                FloatingActionButton(
                    onClick = { navController.navigate("admin_dashboard/$communityId") },
                    containerColor = BloodRed,
                    contentColor = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Dashboard",
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedLoadingIndicator()
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
            state.community != null -> {
                val community = state.community!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // ─── Cover Banner ────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                        ) {
                            // Cover image or gradient fallback
                            if (community.coverUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = community.coverUrl,
                                    contentDescription = community.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                // Gradient overlay on top of image
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                                startY = 80f,
                                            )
                                        ),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF7A1212), Color(0xFF2B0606))
                                            )
                                        ),
                                )
                            }

                            // Back button
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopStart)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.28f)),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }

                            // Membership status chip
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(50),
                                color = when (state.membershipStatus) {
                                    MembershipStatus.JOINED -> Color(0xFF3C9B56)
                                    MembershipStatus.PENDING -> PendingAmber
                                    MembershipStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    when (state.membershipStatus) {
                                        MembershipStatus.JOINED -> {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Member",
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                        MembershipStatus.PENDING -> {
                                            Text(
                                                "⏳ Pending",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                        MembershipStatus.NONE -> {
                                            Text(
                                                "Not Joined",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                            }

                            // Community info at bottom of banner
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                            ) {
                                // Clickable community name → navigates to Info screen
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        navController.navigate("community_info/$communityId")
                                    },
                                ) {
                                    Text(
                                        text = community.name,
                                        color = Color.White,
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Open community details",
                                        tint = Color.White.copy(alpha = 0.85f),
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // District row
                                if (community.district.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            buildString {
                                                append(community.district)
                                                if (community.upazila.isNotEmpty()) append(" · ${community.upazila}")
                                            },
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 14.5.sp,
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }

                                // Inline stats
                                Text(
                                    text = "${community.memberCount} Members  •  ${community.donationCount} Donations  •  ${state.requests.size} Requests",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }

                    // ─── Action Buttons (hidden for Spondon) ──────────────────
                    if (!community.isSpondon) {
                        item {
                            when (state.membershipStatus) {
                                MembershipStatus.NONE -> {
                                    val isEligible = viewModel.isBloodGroupEligible(community)
                                    if (isEligible) {
                                        OutlinedButton(
                                            onClick = {
                                                if (community.type == CommunityType.PUBLIC) {
                                                    viewModel.joinPublicCommunity(communityId)
                                                } else {
                                                    navController.navigate("join_request/$communityId")
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp)
                                                .height(50.dp),
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.4.dp, BloodRed),
                                        ) {
                                            Icon(Icons.Default.GroupAdd, contentDescription = null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                if (community.type == CommunityType.PUBLIC) "Join Community"
                                                else "Request to Join",
                                                fontSize = 16.sp,
                                                color = BloodRed,
                                            )
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {},
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp)
                                                .height(50.dp),
                                            enabled = false,
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.4.dp, Color(0xFFC9C5C2)),
                                        ) {
                                            Text(
                                                "Blood Group Mismatch",
                                                fontSize = 16.sp,
                                                color = Color(0xFF3A3A3A),
                                            )
                                        }
                                    }
                                }
                                MembershipStatus.JOINED -> {
                                    OutlinedButton(
                                        onClick = { viewModel.leaveCommunity(communityId) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)
                                            .height(50.dp),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.4.dp, Color(0xFFC9C5C2)),
                                    ) {
                                        Text(
                                            "Leave Community",
                                            fontSize = 16.sp,
                                            color = Color(0xFF3A3A3A),
                                        )
                                    }
                                }
                                MembershipStatus.PENDING -> {
                                    OutlinedButton(
                                        onClick = {},
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)
                                            .height(50.dp),
                                        enabled = false,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.4.dp, Color(0xFFC9C5C2)),
                                    ) {
                                        Text(
                                            "⏳ Pending Approval",
                                            fontSize = 16.sp,
                                            color = Color(0xFF3A3A3A),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ─── Feed Content (no tabs) ─────────────────────
                    if (state.isRequestsLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    } else if (state.requests.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Feed,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "No blood requests yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Requests posted to this community will appear here",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    } else {
                        items(state.requests, key = { it.id }) { request ->
                            com.spondon.app.feature.request.RequestCard(
                                request = request,
                                onClick = { navController.navigate("request_detail/${request.id}") },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
