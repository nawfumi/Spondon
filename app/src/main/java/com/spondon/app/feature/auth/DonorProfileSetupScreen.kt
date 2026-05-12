package com.spondon.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.SpondonButton
import com.spondon.app.core.ui.components.SpondonTextField
import com.spondon.app.core.ui.components.StepProgressBar
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorProfileSetupScreen(
    navController: NavController,
    viewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    var showDobPicker by remember { mutableStateOf(false) }
    var showLastDonationPicker by remember { mutableStateOf(false) }

    // Date of birth picker
    if (showDobPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDobPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDateOfBirth(it) }
                    showDobPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDobPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Last donation date picker
    if (showLastDonationPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showLastDonationPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateLastDonationDate(it) }
                    showLastDonationPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showLastDonationPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
        ) {
            // Step progress
            StepProgressBar(
                currentStep = 1,
                totalSteps = 3,
                stepLabels = listOf("Basic Info", "Health Profile", "Location"),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Health Profile",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Step 2 of 3 — Donor Information",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Blood Group Selector
            Text(
                text = "Blood Group",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Grid of blood group pills (2 rows of 4)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in bloodGroups.chunked(4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { group ->
                            val isSelected = state.selectedBloodGroup == group
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(BloodRed)
                                        } else {
                                            Modifier
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    RoundedCornerShape(12.dp),
                                                )
                                        }
                                    )
                                    .clickable { viewModel.selectBloodGroup(group) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Date of Birth
            Text(
                text = "Date of Birth",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedCard(
                onClick = { showDobPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BloodRed)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = state.dateOfBirth?.let {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Select date of birth",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (state.dateOfBirth != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
            // Age validation
            val age = viewModel.getAge()
            if (age != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val ageValid = age in 18..60
                Text(
                    text = if (ageValid) "Age: $age years ✓" else "Must be 18–60 years old",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ageValid) AvailableGreen else UrgencyCritical,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Weight
            SpondonTextField(
                value = state.weight,
                onValueChange = { viewModel.updateWeight(it) },
                label = "Weight (kg)",
                leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.weight.isNotEmpty() && (state.weight.toFloatOrNull() ?: 0f) < 50f,
                errorMessage = if (state.weight.isNotEmpty() && (state.weight.toFloatOrNull() ?: 0f) < 50f)
                    "Minimum 50 kg required to donate" else null,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Last Donation Date (optional)
            Text(
                text = "Last Donation Date (Optional)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedCard(
                onClick = { showLastDonationPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Bloodtype, contentDescription = null, tint = SoftRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = state.lastDonationDate?.let {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "When did you last donate?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (state.lastDonationDate != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Donor toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.wantsToBeDonor) BloodRed.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I want to be a donor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Turning this on makes you discoverable to people requesting blood",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = state.wantsToBeDonor,
                        onCheckedChange = { viewModel.toggleDonorWillingness() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = BloodRed,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue button
            SpondonButton(
                text = "Continue",
                onClick = {
                    navController.navigate(Routes.LocationSetup.route)
                },
                enabled = viewModel.isStep2Valid(),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
