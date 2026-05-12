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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.RequestStatus
import com.spondon.app.core.domain.model.Urgency
import com.spondon.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    navController: NavController,
    viewModel: RequestViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsState()
    val context = LocalContext.current

    // Load from savedStateHandle or nav arg
    val requestId = navController.currentBackStackEntry
        ?.arguments?.getString("requestId") ?: ""

    LaunchedEffect(requestId) {
        if (requestId.isNotBlank()) viewModel.loadRequestDetail(requestId)
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
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT,
                                    "🩸 Urgent blood request: ${state.request?.bloodGroup} needed at ${state.request?.hospital}. Help save a life!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Request"))
                        }) {
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ─── Header: Blood Group + Urgency ───────
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (request.urgency) {
                                    Urgency.CRITICAL -> UrgencyCritical.copy(alpha = 0.08f)
                                    Urgency.MODERATE -> UrgencyModerate.copy(alpha = 0.08f)
                                    Urgency.NORMAL -> UrgencyNormal.copy(alpha = 0.08f)
                                },
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                // Large blood group
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (request.urgency) {
                                                Urgency.CRITICAL -> UrgencyCritical
                                                Urgency.MODERATE -> UrgencyModerate
                                                Urgency.NORMAL -> UrgencyNormal
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = request.bloodGroup,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                        ),
                                        color = Color.White,
                                    )
                                }

                                Spacer(Modifier.height(12.dp))
                                UrgencyTag(request.urgency)
                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "${request.unitsNeeded} unit${if (request.unitsNeeded > 1) "s" else ""} needed",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )

                                Text(
                                    text = RequestViewModel.getRelativeTime(request.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                )

                                // Status chip
                                if (request.status != RequestStatus.ACTIVE) {
                                    Spacer(Modifier.height(8.dp))
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
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
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
                        }
                    }

                    // ─── Patient Info ────────────────────────
                    item {
                        DetailSection(title = "Patient Information") {
                            if (!request.patientName.isNullOrBlank()) {
                                DetailRow(Icons.Outlined.Person, "Patient", request.patientName)
                            }
                            DetailRow(Icons.Outlined.LocalHospital, "Hospital", request.hospital)
                            request.donationDateTime?.let {
                                DetailRow(
                                    Icons.Outlined.CalendarMonth,
                                    "Donation Date",
                                    dateFormat.format(it),
                                )
                            }
                        }
                    }

                    // ─── Contact Section ─────────────────────
                    item {
                        DetailSection(title = "Contact") {
                            DetailRow(Icons.Outlined.Person, "Requester", state.requesterName)
                            if (request.contactNumber.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    DetailRow(
                                        Icons.Outlined.Phone,
                                        "Phone",
                                        request.contactNumber,
                                    )
                                    FilledIconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${request.contactNumber}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = AvailableGreen,
                                        ),
                                    ) {
                                        Icon(Icons.Filled.Phone, "Call", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // ─── Respondents ─────────────────────────
                    item {
                        DetailSection(
                            title = "Respondents (${request.respondents.size})",
                        ) {
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
                                        val donorName = profile?.name?.takeIf { it.isNotBlank() } ?: "Donor"
                                        val donorPhone = if (profile?.isPhoneVisible == true) profile.phone else ""
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(BloodRed.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Filled.Person,
                                                    null,
                                                    tint = BloodRed,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    donorName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                )
                                                if (donorPhone.isNotBlank()) {
                                                    Text(
                                                        donorPhone,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                                    )
                                                }
                                            }
                                            if (donorPhone.isNotBlank()) {
                                                FilledIconButton(
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                                            data = Uri.parse("tel:$donorPhone")
                                                        }
                                                        context.startActivity(intent)
                                                    },
                                                    colors = IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = AvailableGreen,
                                                    ),
                                                    modifier = Modifier.size(32.dp),
                                                ) {
                                                    Icon(Icons.Filled.Phone, "Call", tint = Color.White, modifier = Modifier.size(16.dp))
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
                        when {
                            // Requester's view: manage actions
                            state.isCurrentUserRequester -> {
                                if (request.status == RequestStatus.ACTIVE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.updateStatus(RequestStatus.CANCELLED) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = UrgencyCritical,
                                            ),
                                        ) {
                                            Text("Cancel Request")
                                        }
                                        Button(
                                            onClick = { viewModel.updateStatus(RequestStatus.FULFILLED) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AvailableGreen,
                                            ),
                                        ) {
                                            Text("Mark Fulfilled")
                                        }
                                    }

                                    // ── Confirm Donation per respondent ──
                                    if (request.respondents.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "Confirm a successful donation:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        request.respondents.forEach { donorId ->
                                            val donorProfile = state.respondentProfiles[donorId]
                                            val donorName = donorProfile?.name?.takeIf { it.isNotBlank() } ?: "this donor"
                                            val donorPhone = if (donorProfile?.isPhoneVisible == true) donorProfile.phone else ""
                                            var showConfirmDialog by remember { mutableStateOf(false) }

                                            FilledTonalButton(
                                                onClick = { showConfirmDialog = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = AvailableGreen.copy(alpha = 0.12f),
                                                    contentColor = AvailableGreen,
                                                ),
                                            ) {
                                                Icon(Icons.Filled.VolunteerActivism, null, Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Confirm: $donorName",
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))

                                            if (showConfirmDialog) {
                                                AlertDialog(
                                                    onDismissRequest = { showConfirmDialog = false },
                                                    confirmButton = {
                                                        Button(
                                                            onClick = {
                                                                viewModel.confirmDonation(donorId)
                                                                showConfirmDialog = false
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = AvailableGreen),
                                                        ) {
                                                            Text("Confirm")
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { showConfirmDialog = false }) {
                                                            Text("Cancel")
                                                        }
                                                    },
                                                    title = { Text("Confirm $donorName's Donation") },
                                                    text = {
                                                        Column {
                                                            Text("Are you sure $donorName has successfully donated? This will update their donation count and mark this request as fulfilled.")
                                                            if (donorPhone.isNotBlank()) {
                                                                Spacer(Modifier.height(8.dp))
                                                                Text("📞 $donorPhone", style = MaterialTheme.typography.bodySmall, color = AvailableGreen)
                                                            }
                                                        }
                                                    },
                                                    icon = { Icon(Icons.Filled.VolunteerActivism, null, tint = AvailableGreen) },
                                                )
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
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            null,
            tint = BloodRed.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
