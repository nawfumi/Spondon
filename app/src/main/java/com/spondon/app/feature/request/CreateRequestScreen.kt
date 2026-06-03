package com.spondon.app.feature.request

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.Urgency
import com.spondon.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    navController: NavController,
    viewModel: RequestViewModel = hiltViewModel(),
) {
    val state by viewModel.createState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadCreateForm() }

    // Navigate back on success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Blood Request", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ─── Blood Group Grid ────────────────────────────
            item {
                SectionHeader("Blood Group Required *")
                Spacer(Modifier.height(8.dp))
                BloodGroupGrid(
                    selected = state.bloodGroup,
                    onSelect = viewModel::updateBloodGroup,
                )
            }

            // ─── Urgency Selector ────────────────────────────
            item {
                SectionHeader("Urgency Level *")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Urgency.entries.forEach { urgency ->
                        val isSelected = state.urgency == urgency
                        val color = when (urgency) {
                            Urgency.CRITICAL -> UrgencyCritical
                            Urgency.MODERATE -> UrgencyModerate
                            Urgency.NORMAL -> UrgencyNormal
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateUrgency(urgency) },
                            label = {
                                Text(
                                    urgency.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color,
                                selectedLabelColor = Color.White,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ─── Units Needed ────────────────────────────────
            item {
                SectionHeader(
                    text = "Units Needed",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                ) {
                    FilledIconButton(
                        onClick = { viewModel.updateUnits(state.unitsNeeded - 1) },
                        enabled = state.unitsNeeded > 1,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = BloodRed.copy(alpha = 0.1f),
                        ),
                    ) {
                        Icon(Icons.Filled.Remove, "Decrease")
                    }
                    Text(
                        text = "${state.unitsNeeded}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    FilledIconButton(
                        onClick = { viewModel.updateUnits(state.unitsNeeded + 1) },
                        enabled = state.unitsNeeded < 20,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = BloodRed.copy(alpha = 0.1f),
                        ),
                    ) {
                        Icon(Icons.Filled.Add, "Increase")
                    }
                    Text(
                        text = "unit${if (state.unitsNeeded > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }
            }

            // ─── Patient Name ────────────────────────────────
            item {
                OutlinedTextField(
                    value = state.patientName,
                    onValueChange = viewModel::updatePatientName,
                    label = { Text("Patient Name *") },
                    leadingIcon = { Icon(Icons.Outlined.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                )
            }

            // ─── Patient Condition ──────────────────────────
            item {
                OutlinedTextField(
                    value = state.patientCondition,
                    onValueChange = viewModel::updatePatientCondition,
                    label = { Text("Patient Condition (optional)") },
                    leadingIcon = { Icon(Icons.Outlined.Info, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = false,
                    maxLines = 2,
                )
            }

            // ─── Hospital ────────────────────────────────────
            item {
                OutlinedTextField(
                    value = state.hospital,
                    onValueChange = viewModel::updateHospital,
                    label = { Text("Hospital Name *") },
                    leadingIcon = { Icon(Icons.Outlined.LocalHospital, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                )
            }

            // ─── Address ────────────────────────────────────
            item {
                OutlinedTextField(
                    value = state.address,
                    onValueChange = viewModel::updateAddress,
                    label = { Text("Address *") },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = false,
                    maxLines = 2,
                )
            }

            // ─── Donation Date & Time ────────────────────────
            item {
                val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
                val calendar = remember { Calendar.getInstance() }

                OutlinedTextField(
                    value = state.donationDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Donation Date & Time") },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    calendar.set(y, m, d)
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            calendar.set(Calendar.HOUR_OF_DAY, h)
                                            calendar.set(Calendar.MINUTE, min)
                                            viewModel.updateDonationDate(calendar.time)
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false,
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                            ).show()
                        },
                    readOnly = true,
                    enabled = false,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onBackground,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    ),
                )
            }

            // ─── Contact Number ──────────────────────────────
            item {
                OutlinedTextField(
                    value = state.contactNumber,
                    onValueChange = viewModel::updateContactNumber,
                    label = { Text("Contact Number") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                )
            }

            // ─── Community Scope ─────────────────────────────
            item {
                SectionHeader("Broadcast to Communities *")
                Spacer(Modifier.height(8.dp))

                if (state.availableCommunities.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "Join a community first to broadcast requests",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.availableCommunities.forEach { community ->
                            val isSelected = state.selectedCommunityIds.contains(community.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) BloodRed.copy(alpha = 0.08f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    )
                                    .clickable { viewModel.toggleCommunity(community.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleCommunity(community.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = BloodRed,
                                    ),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = community.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${community.memberCount} members",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── Error ───────────────────────────────────────
            if (state.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = UrgencyCritical.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = UrgencyCritical,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            // ─── Submit Button ───────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.submitRequest() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                    enabled = !state.isSubmitting,
                ) {
                    if (state.isSubmitting) {
                        LoadingIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Post Request",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = textAlign,
    )
}

@Composable
private fun BloodGroupGrid(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val groups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { group ->
                    val isSelected = selected == group
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (!isSelected) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp),
                                ) else Modifier
                            )
                            .clickable { onSelect(group) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) BloodRed else Color.Transparent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (isSelected) Color.White
                                else MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }
        }
    }
}