package com.spondon.app.feature.request

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.RequestStatus
import com.spondon.app.core.domain.model.User
import com.spondon.app.core.domain.model.Urgency
import com.spondon.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    navController: NavController,
    viewModel: RequestViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsState()
    val context = LocalContext.current
    var showShareSheet by remember { mutableStateOf(false) }

    // Load from savedStateHandle or nav arg
    val requestId = navController.currentBackStackEntry
        ?.arguments?.getString("requestId") ?: ""

    LaunchedEffect(requestId) {
        if (requestId.isNotBlank()) viewModel.loadRequestDetail(requestId)
    }

    if (showShareSheet && state.request != null) {
        ShareBottomSheet(
            request = state.request!!,
            onDismiss = { showShareSheet = false },
        )
    }

    if (state.isEditing && state.request != null) {
        EditRequestDialog(
            request = state.request!!,
            onDismiss = { viewModel.toggleEditing() },
            onSave = { edited ->
                viewModel.updateRequest(edited)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                actions = {
                    if (state.request != null) {
                        if (state.isEditable) {
                            IconButton(onClick = { viewModel.toggleEditing() }) {
                                Icon(Icons.Outlined.Edit, "Edit")
                            }
                        }
                        IconButton(onClick = { showShareSheet = true }) {
                            Icon(Icons.Outlined.Share, "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
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

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null,
                            tint = UrgencyCritical,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(state.error ?: "Error loading request")
                    }
                }
            }

            state.request != null -> {
                val request = state.request!!
                val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
                val dateOnlyFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                val timeOnlyFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

                val urgencyColor = when (request.urgency) {
                    Urgency.CRITICAL -> UrgencyCritical
                    Urgency.MODERATE -> UrgencyModerate
                    Urgency.NORMAL -> UrgencyNormal
                }
                val urgencyText = when (request.urgency) {
                    Urgency.CRITICAL -> "CRITICAL"
                    Urgency.MODERATE -> "URGENT"
                    Urgency.NORMAL -> "NORMAL"
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ─── Request Info Card (matches RequestCard style) ─────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header: Requester avatar + name + time
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.requesterName.ifBlank { "Unknown Requester" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "Posted: ${RequestViewModel.getRelativeTime(request.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    // Status chip for non-active
                                    if (request.status != RequestStatus.ACTIVE) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when (request.status) {
                                                RequestStatus.FULFILLED -> AvailableGreen.copy(alpha = 0.15f)
                                                RequestStatus.CANCELLED -> UrgencyCritical.copy(alpha = 0.15f)
                                                RequestStatus.EXPIRED -> UnavailableGrey.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            },
                                        ) {
                                            Text(
                                                text = request.status.name,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = when (request.status) {
                                                    RequestStatus.FULFILLED -> AvailableGreen
                                                    RequestStatus.CANCELLED -> UrgencyCritical
                                                    RequestStatus.EXPIRED -> UnavailableGrey
                                                    else -> Color.Unspecified
                                                },
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                Spacer(Modifier.height(16.dp))

                                // Info grid — same pattern as RequestCard
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    InfoChipRow(
                                        icon = Icons.Filled.Bloodtype,
                                        label = "Blood Group",
                                        value = request.bloodGroup.ifBlank { "?" },
                                        valueColor = BloodRed,
                                        valueBg = BloodRed.copy(alpha = 0.1f),
                                        modifier = Modifier.weight(1f),
                                    )
                                    InfoChipRow(
                                        icon = Icons.Filled.Warning,
                                        label = "Urgency",
                                        value = urgencyText,
                                        valueColor = urgencyColor,
                                        valueBg = urgencyColor.copy(alpha = 0.1f),
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    InfoChipRow(
                                        icon = Icons.Filled.WaterDrop,
                                        label = "Quantity",
                                        value = "${request.unitsNeeded} Bag${if (request.unitsNeeded > 1) "s" else ""}",
                                        valueColor = MaterialTheme.colorScheme.primary,
                                        valueBg = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    InfoChipRow(
                                        icon = Icons.Filled.LocalHospital,
                                        label = "Hospital",
                                        value = request.hospital.ifBlank { "Not specified" },
                                        valueColor = MaterialTheme.colorScheme.secondary,
                                        valueBg = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    InfoChipRow(
                                        icon = Icons.Filled.CalendarMonth,
                                        label = "Donation Date",
                                        value = request.donationDateTime?.let { dateOnlyFormat.format(it) } ?: "Not set",
                                        valueColor = MaterialTheme.colorScheme.tertiary,
                                        valueBg = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    InfoChipRow(
                                        icon = Icons.Filled.AccessTime,
                                        label = "Time",
                                        value = request.donationDateTime?.let { timeOnlyFormat.format(it) } ?: "Not set",
                                        valueColor = MaterialTheme.colorScheme.tertiary,
                                        valueBg = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    // ─── Patient Info Card ─────────────────────────
                    if (!request.patientName.isNullOrBlank() || request.address.isNotBlank() || request.patientCondition.isNotBlank()) {
                        item {
                            DetailSection(title = "Patient Information") {
                                if (!request.patientName.isNullOrBlank()) {
                                    DetailInfoRow(
                                        icon = Icons.Outlined.Person,
                                        label = "Patient",
                                        value = request.patientName,
                                        chipColor = MaterialTheme.colorScheme.primary,
                                        chipBg = MaterialTheme.colorScheme.primaryContainer,
                                    )
                                }
                                if (request.patientCondition.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    DetailInfoRow(
                                        icon = Icons.Outlined.Info,
                                        label = "Condition",
                                        value = request.patientCondition,
                                        chipColor = PendingAmber,
                                        chipBg = PendingAmber.copy(alpha = 0.1f),
                                    )
                                }
                                if (request.address.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    DetailInfoRow(
                                        icon = Icons.Outlined.LocationOn,
                                        label = "Address",
                                        value = request.address,
                                        chipColor = MaterialTheme.colorScheme.tertiary,
                                        chipBg = MaterialTheme.colorScheme.tertiaryContainer,
                                    )
                                }
                            }
                        }
                    }

                    // ─── Contact Section ─────────────────────
                    item {
                        DetailSection(title = "Contact") {
                            DetailInfoRow(
                                icon = Icons.Outlined.Person,
                                label = "Requester",
                                value = state.requesterName.ifBlank { "Unknown" },
                                chipColor = MaterialTheme.colorScheme.primary,
                                chipBg = MaterialTheme.colorScheme.primaryContainer,
                            )
                            if (request.contactNumber.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    DetailInfoRow(
                                        icon = Icons.Outlined.Phone,
                                        label = "Phone",
                                        value = request.contactNumber,
                                        chipColor = AvailableGreen,
                                        chipBg = AvailableGreen.copy(alpha = 0.1f),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = "tel:${request.contactNumber}".toUri()
                                            }
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = AvailableGreen.copy(alpha = 0.12f),
                                            contentColor = AvailableGreen,
                                        ),
                                    ) {
                                        Icon(Icons.Filled.Phone, "Call", modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Call", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // ─── Respondents ─────────────────────────
                    item {
                        val confirmedCount = state.confirmedDonorIds.size
                        val unitsNeeded = request.unitsNeeded

                        // Determine if current user is the requester
                        val isRequester = state.isCurrentUserRequester

                        DetailSection(
                            title = "Respondents (${request.respondents.size})",
                        ) {
                            // ── Donation progress indicator ──
                            if (confirmedCount > 0 || request.respondents.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (confirmedCount >= unitsNeeded) AvailableGreen.copy(alpha = 0.12f)
                                            else BloodRed.copy(alpha = 0.1f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            if (confirmedCount >= unitsNeeded) Icons.Filled.CheckCircle
                                            else Icons.Filled.VolunteerActivism,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (confirmedCount >= unitsNeeded) AvailableGreen else BloodRed,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "$confirmedCount / $unitsNeeded bag${if (unitsNeeded > 1) "s" else ""} confirmed",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (confirmedCount >= unitsNeeded) AvailableGreen else BloodRed,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            if (request.respondents.isEmpty()) {
                                Text(
                                    text = "No one has responded yet. Be the first!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    request.respondents.forEach { respondentId ->
                                        val profile = state.respondentProfiles[respondentId]
                                        val isCurrentUser = respondentId == viewModel.currentUserId

                                        // Privacy: requester sees all; donors see only their own info
                                        val donorName = when {
                                            isRequester || isCurrentUser -> profile?.name?.takeIf { it.isNotBlank() } ?: "Donor"
                                            else -> "Donor"
                                        }
                                        val donorPhone = when {
                                            isRequester -> if (profile?.isPhoneVisible == true) profile.phone else ""
                                            isCurrentUser -> profile?.phone ?: ""
                                            else -> ""
                                        }
                                        val showBloodGroup = isRequester || isCurrentUser
                                        val isConfirmed = respondentId in state.confirmedDonorIds

                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isConfirmed) AvailableGreen.copy(alpha = 0.08f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isConfirmed) AvailableGreen.copy(alpha = 0.15f)
                                                            else BloodRed.copy(alpha = 0.1f)
                                                        ),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (isConfirmed) {
                                                        Icon(
                                                            Icons.Filled.CheckCircle,
                                                            contentDescription = "Confirmed",
                                                            tint = AvailableGreen,
                                                            modifier = Modifier.size(22.dp),
                                                        )
                                                    } else {
                                                        Text(
                                                            text = donorName.first().uppercase(),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BloodRed,
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            donorName,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.SemiBold,
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                        )
                                                        if (isCurrentUser && !isRequester) {
                                                            Spacer(Modifier.width(6.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                            ) {
                                                                Text(
                                                                    text = "You",
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                )
                                                            }
                                                        }
                                                        if (isConfirmed) {
                                                            Spacer(Modifier.width(6.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = AvailableGreen.copy(alpha = 0.15f),
                                                            ) {
                                                                Text(
                                                                    text = "\u2705 Donated",
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = AvailableGreen,
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (donorPhone.isNotBlank()) {
                                                        Text(
                                                            donorPhone,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                                if (showBloodGroup && profile?.bloodGroup?.isNotBlank() == true) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = BloodRed.copy(alpha = 0.1f),
                                                    ) {
                                                        Text(
                                                            text = profile.bloodGroup,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BloodRed,
                                                        )
                                                    }
                                                    Spacer(Modifier.width(8.dp))
                                                }
                                                if (donorPhone.isNotBlank()) {
                                                    FilledIconButton(
                                                        onClick = {
                                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                                data = "tel:$donorPhone".toUri()
                                                            }
                                                            context.startActivity(intent)
                                                        },
                                                        colors = IconButtonDefaults.filledIconButtonColors(
                                                            containerColor = AvailableGreen.copy(alpha = 0.12f),
                                                            contentColor = AvailableGreen,
                                                        ),
                                                        modifier = Modifier.size(36.dp),
                                                    ) {
                                                        Icon(Icons.Filled.Phone, "Call", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─── Action Buttons ──────────────────────
                    item {
                        var showConfirmDialog by remember { mutableStateOf(false) }

                        when {
                            // Requester's or Admin/Mod view: manage actions
                            state.isCurrentUserRequester || state.isCurrentUserAdminOrMod -> {
                                if (request.status == RequestStatus.ACTIVE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.updateStatus(RequestStatus.CANCELLED) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = UrgencyCritical,
                                            ),
                                        ) {
                                            Text("Cancel Request")
                                        }
                                        Button(
                                            onClick = { viewModel.updateStatus(RequestStatus.FULFILLED) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AvailableGreen,
                                            ),
                                        ) {
                                            Text("Mark Fulfilled")
                                        }
                                    }

                                    // ── Confirm Donations Button (opens dialog) ──
                                    if (request.respondents.isNotEmpty()) {
                                        val unconfirmedCount = request.respondents.count { it !in state.confirmedDonorIds }
                                        if (unconfirmedCount > 0) {
                                            Spacer(Modifier.height(12.dp))
                                            Button(
                                                onClick = { showConfirmDialog = true },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AvailableGreen,
                                                ),
                                                enabled = !state.isConfirming,
                                            ) {
                                                if (state.isConfirming) {
                                                    LoadingIndicator(
                                                        color = Color.White,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Confirming...", fontWeight = FontWeight.SemiBold)
                                                } else {
                                                    Icon(Icons.Filled.VolunteerActivism, null, Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        "Confirm Donations (${state.confirmedDonorIds.size}/${request.unitsNeeded})",
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Already responded
                            state.hasResponded -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = AvailableGreen.copy(alpha = 0.1f),
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            null,
                                            tint = AvailableGreen,
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "You've volunteered to donate. The requester will contact you.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AvailableGreen,
                                        )
                                    }
                                }
                            }

                            // Can donate
                            state.canDonate && request.status == RequestStatus.ACTIVE -> {
                                Button(
                                    onClick = { viewModel.respondToRequest() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                    enabled = !state.isResponding,
                                ) {
                                    if (state.isResponding) {
                                        LoadingIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    } else {
                                        Icon(Icons.Filled.VolunteerActivism, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "I'll Donate",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                        )
                                    }
                                }
                            }

                            // Cooldown
                            !state.canDonate && state.cooldownDaysRemaining > 0 -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = UnavailableGrey.copy(alpha = 0.1f),
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Filled.Lock, null, tint = UnavailableGrey)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "You are in donation cooldown",
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                            Text(
                                                "${state.cooldownDaysRemaining} days remaining",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = UnavailableGrey,
                                            )
                                        }
                                    }
                                }
                            }

                            // Blood group mismatch
                            !state.bloodGroupMatch && !state.isCurrentUserRequester -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = UnavailableGrey.copy(alpha = 0.1f),
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Outlined.Bloodtype, null, tint = UnavailableGrey)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "Blood group doesn't match",
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                            Text(
                                                "Only ${request.bloodGroup} donors can respond to this request",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = UnavailableGrey,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Multi-select Confirm Donations Dialog ──
                        if (showConfirmDialog) {
                            ConfirmDonationsDialog(
                                respondents = request.respondents,
                                respondentProfiles = state.respondentProfiles,
                                confirmedDonorIds = state.confirmedDonorIds,
                                maxSelectable = request.unitsNeeded,
                                onDismiss = { showConfirmDialog = false },
                                onConfirm = { selectedIds ->
                                    showConfirmDialog = false
                                    viewModel.confirmMultipleDonations(selectedIds)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Multi-select Confirm Donations Dialog
// ═══════════════════════════════════════════════════════════════

@Composable
fun ConfirmDonationsDialog(
    respondents: List<String>,
    respondentProfiles: Map<String, User>,
    confirmedDonorIds: Set<String>,
    maxSelectable: Int,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    // Only show unconfirmed respondents as selectable
    val unconfirmedRespondents = respondents.filter { it !in confirmedDonorIds }
    val alreadyConfirmedRespondents = respondents.filter { it in confirmedDonorIds }
    val remainingSlots = (maxSelectable - confirmedDonorIds.size).coerceAtLeast(0)

    // Track which donors are checked
    val selectedIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.VolunteerActivism, null, tint = AvailableGreen) },
        title = {
            Text(
                "Confirm Donations",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    "Select the donors who have successfully donated blood. " +
                    "You can select up to $remainingSlots more donor${if (remainingSlots != 1) "s" else ""}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                // Already confirmed donors (shown as disabled)
                if (alreadyConfirmedRespondents.isNotEmpty()) {
                    Text(
                        "Already confirmed:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AvailableGreen,
                    )
                    Spacer(Modifier.height(4.dp))
                    alreadyConfirmedRespondents.forEach { donorId ->
                        val profile = respondentProfiles[donorId]
                        val name = profile?.name?.takeIf { it.isNotBlank() } ?: "Donor"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = true,
                                onCheckedChange = null,
                                enabled = false,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AvailableGreen,
                                    disabledCheckedColor = AvailableGreen.copy(alpha = 0.5f),
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "✅",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Unconfirmed donors (selectable)
                if (unconfirmedRespondents.isNotEmpty()) {
                    Text(
                        "Select donors to confirm:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    unconfirmedRespondents.forEach { donorId ->
                        val profile = respondentProfiles[donorId]
                        val name = profile?.name?.takeIf { it.isNotBlank() } ?: "Donor"
                        val bloodGroup = profile?.bloodGroup ?: ""
                        val isSelected = donorId in selectedIds
                        val canSelect = isSelected || selectedIds.size < remainingSlots

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked && canSelect) {
                                        selectedIds.add(donorId)
                                    } else {
                                        selectedIds.remove(donorId)
                                    }
                                },
                                enabled = canSelect || isSelected,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AvailableGreen,
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                color = if (canSelect || isSelected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                            if (bloodGroup.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BloodRed.copy(alpha = 0.1f),
                                ) {
                                    Text(
                                        text = bloodGroup,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BloodRed,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "All respondents have already been confirmed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AvailableGreen),
            ) {
                Text("Confirm ${selectedIds.size} Donation${if (selectedIds.size != 1) "s" else ""}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// ═══════════════════════════════════════════════════════════════
// Edit Request Dialog
// ═══════════════════════════════════════════════════════════════

@Composable
fun EditRequestDialog(
    request: com.spondon.app.core.domain.model.BloodRequest,
    onDismiss: () -> Unit,
    onSave: (com.spondon.app.core.domain.model.BloodRequest) -> Unit,
) {
    var unitsNeeded by remember { mutableIntStateOf(request.unitsNeeded) }
    var patientName by remember { mutableStateOf(request.patientName ?: "") }
    var hospital by remember { mutableStateOf(request.hospital) }
    var address by remember { mutableStateOf(request.address) }
    var contactNumber by remember { mutableStateOf(request.contactNumber) }
    var condition by remember { mutableStateOf(request.patientCondition) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Request", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Text("Units Needed", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    ) {
                        FilledIconButton(
                            onClick = { if (unitsNeeded > 1) unitsNeeded-- },
                            enabled = unitsNeeded > 1,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = BloodRed.copy(alpha = 0.1f)),
                        ) {
                            Icon(Icons.Filled.Remove, "Decrease")
                        }
                        Text("$unitsNeeded", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                        FilledIconButton(
                            onClick = { if (unitsNeeded < 10) unitsNeeded++ },
                            enabled = unitsNeeded < 10,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = BloodRed.copy(alpha = 0.1f)),
                        ) {
                            Icon(Icons.Filled.Add, "Increase")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                item {
                    OutlinedTextField(
                        value = condition,
                        onValueChange = { condition = it },
                        label = { Text("Patient Condition") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                item {
                    OutlinedTextField(
                        value = hospital,
                        onValueChange = { hospital = it },
                        label = { Text("Hospital Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Hospital Area / Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                item {
                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        label = { Text("Contact Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        request.copy(
                            unitsNeeded = unitsNeeded,
                            patientName = patientName.trim().takeIf { it.isNotBlank() },
                            hospital = hospital.trim(),
                            address = address.trim(),
                            contactNumber = contactNumber.trim(),
                            patientCondition = condition.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AvailableGreen)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// Helper composables
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    chipColor: Color,
    chipBg: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            null,
            tint = chipColor,
            modifier = Modifier.size(18.dp),
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = chipBg,
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = chipColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
