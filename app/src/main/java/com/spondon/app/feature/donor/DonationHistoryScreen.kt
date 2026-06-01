package com.spondon.app.feature.donor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.Donation
import com.spondon.app.core.domain.model.DonationStatus
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.DarkRose
import com.spondon.app.core.ui.theme.PendingAmber
import com.spondon.app.core.ui.theme.UnavailableGrey
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationHistoryScreen(
    navController: NavController,
    viewModel: DonorViewModel = hiltViewModel(),
) {
    val state by viewModel.historyState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadDonationHistory() }

    // Show Snackbar when certificate message changes
    LaunchedEffect(state.certificateMessage) {
        state.certificateMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearCertificateMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donation History", fontWeight = FontWeight.Bold) },
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
            if (state.totalDonations > 0 && !state.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.generateCertificate(context) },
                    containerColor = BloodRed,
                    contentColor = Color.White,
                    icon = {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Download Certificate",
                        )
                    },
                    text = { Text("Certificate", fontWeight = FontWeight.SemiBold) },
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedLoadingIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ─── Summary Card ────────────────────────
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(BloodRed, DarkRose),
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                    )
                                    .padding(24.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Total Donations",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "${state.totalDonations}",
                                            style = MaterialTheme.typography.displaySmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                            ),
                                            color = Color.White,
                                        )
                                        Spacer(Modifier.height(8.dp))

                                        // Badges earned
                                        val badgeCount = state.user?.badges?.size ?: 0
                                        if (badgeCount > 0) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    Icons.Filled.EmojiEvents,
                                                    null,
                                                    tint = PendingAmber,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "$badgeCount badge${if (badgeCount > 1) "s" else ""} earned",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PendingAmber,
                                                )
                                            }
                                        }
                                    }

                                    // Large blood drop icon
                                    Icon(
                                        Icons.Filled.Bloodtype,
                                        null,
                                        tint = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(72.dp),
                                    )
                                }
                            }
                        }
                    }

                    // ─── Next Eligible Date ──────────────────
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isEligibleNow)
                                    AvailableGreen.copy(alpha = 0.08f)
                                else if (state.nextEligibleDays <= 30)
                                    PendingAmber.copy(alpha = 0.08f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val (icon, color, text) = when {
                                    state.isEligibleNow -> Triple(
                                        Icons.Filled.CheckCircle,
                                        AvailableGreen,
                                        "You are eligible to donate now!",
                                    )
                                    state.nextEligibleDays <= 30 -> Triple(
                                        Icons.Outlined.Schedule,
                                        PendingAmber,
                                        "Eligible in ${state.nextEligibleDays} days",
                                    )
                                    else -> Triple(
                                        Icons.Filled.Lock,
                                        UnavailableGrey,
                                        "Eligible in ${state.nextEligibleDays} days",
                                    )
                                }

                                Icon(
                                    icon,
                                    null,
                                    tint = color,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = color,
                                    )
                                    if (!state.isEligibleNow && state.nextEligibleDays > 0) {
                                        Text(
                                            "Please wait until the cooldown period ends",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = color.copy(alpha = 0.6f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ─── Timeline Header ─────────────────────
                    item {
                        Text(
                            "Donation Timeline",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    // ─── Timeline Items ──────────────────────
                    if (state.donations.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                ),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        Icons.Outlined.VolunteerActivism,
                                        null,
                                        tint = BloodRed.copy(alpha = 0.3f),
                                        modifier = Modifier.size(56.dp),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "No donations yet",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Your donation history will appear here once you make your first donation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(state.donations) { index, donation ->
                            DonationTimelineItem(
                                donation = donation,
                                isFirst = index == 0,
                                isLast = index == state.donations.lastIndex,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationTimelineItem(
    donation: Donation,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isConfirmed = donation.status == DonationStatus.CONFIRMED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        // Timeline line + dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp),
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(BloodRed.copy(alpha = 0.2f)),
                )
            } else {
                Spacer(Modifier.height(12.dp))
            }

            // Dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConfirmed) AvailableGreen else PendingAmber,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isConfirmed) {
                    Icon(
                        Icons.Filled.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(8.dp),
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(BloodRed.copy(alpha = 0.2f)),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Content card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        donation.hospital.ifBlank { "Hospital" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        donation.date?.let {
                            Text(
                                dateFormat.format(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            )
                        }
                        if (donation.bloodGroup.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BloodRed.copy(alpha = 0.1f),
                            ) {
                                Text(
                                    donation.bloodGroup,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                    ),
                                    color = BloodRed,
                                )
                            }
                        }
                    }
                }

                // Status chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isConfirmed)
                        AvailableGreen.copy(alpha = 0.1f)
                    else
                        PendingAmber.copy(alpha = 0.1f),
                ) {
                    Text(
                        if (isConfirmed) "Confirmed" else "Pending",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        color = if (isConfirmed) AvailableGreen else PendingAmber,
                    )
                }
            }
        }
    }
}