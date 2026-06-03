package com.spondon.app.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.core.ui.components.SpondonButton
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.*
import com.spondon.app.feature.auth.BangladeshData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.editState.collectAsState()
    val scrollState = rememberScrollState()

    var districtExpanded by remember { mutableStateOf(false) }
    var upazilaExpanded by remember { mutableStateOf(false) }
    val districts = BangladeshData.districtNames
    val upazilas = if (state.district.isNotBlank()) BangladeshData.getUpazilas(state.district) else emptyList()

    LaunchedEffect(Unit) { viewModel.loadEditProfile() }

    val s = S.strings

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) navController.popBackStack()
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.editProfile, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // ─── Avatar Picker ───────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                            ) {
                                // Avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(BloodRed.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = state.avatarUrl,
                                            contentDescription = "Profile picture",
                                            modifier = Modifier
                                                .size(90.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = BloodRed.copy(alpha = 0.5f),
                                            modifier = Modifier.size(44.dp),
                                        )
                                    }
                                    // Dim overlay + spinner while uploading
                                    if (state.isUploadingAvatar) {
                                        Box(
                                            modifier = Modifier
                                                .size(90.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                color = Color.White,
                                                strokeWidth = 2.5.dp,
                                            )
                                        }
                                    }
                                }
                                // Camera badge button
                                FilledIconButton(
                                    onClick = {
                                        if (!state.isUploadingAvatar) {
                                            avatarPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(28.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = BloodRed,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Filled.CameraAlt,
                                        contentDescription = "Change photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }

                        // ─── Personal Info ────────────────
                        Text(s.personalInfo, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))

                        OutlinedTextField(
                            value = state.name, onValueChange = { viewModel.updateName(it) },
                            label = { Text(s.fullName) }, leadingIcon = { Icon(Icons.Outlined.Person, null) },
                            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.phone, onValueChange = { viewModel.updatePhone(it) },
                            label = { Text(s.phone) }, leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, singleLine = true,
                        )
                        OutlinedTextField(
                            value = state.email, onValueChange = { viewModel.updateEmail(it) },
                            label = { Text(s.email) }, leadingIcon = { Icon(Icons.Outlined.Email, null) },
                            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, singleLine = true,
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // ─── Health Info ──────────────────
                        Text(s.healthInfo, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))

                        // Blood group (read-only)
                        OutlinedTextField(
                            value = state.bloodGroup, onValueChange = {},
                            label = { Text(s.bloodGroup) }, leadingIcon = { Icon(Icons.Outlined.Bloodtype, null) },
                            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                            readOnly = true, enabled = false,
                        )

                        OutlinedTextField(
                            value = state.weight, onValueChange = { viewModel.updateWeight(it) },
                            label = { Text(s.weightKg) }, leadingIcon = { Icon(Icons.Outlined.MonitorWeight, null) },
                            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, singleLine = true,
                        )

                        // Donor toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(s.registerAsDonor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(s.appearInDonorSearch, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                            Switch(
                                checked = state.isDonor, onCheckedChange = { viewModel.toggleDonor() },
                                colors = SwitchDefaults.colors(checkedTrackColor = BloodRed, checkedThumbColor = androidx.compose.ui.graphics.Color.White),
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // ─── Location ────────────────────
                        Text(s.location, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))

                        ExposedDropdownMenuBox(expanded = districtExpanded, onExpandedChange = { districtExpanded = !districtExpanded }) {
                            OutlinedTextField(
                                value = state.district, onValueChange = {}, readOnly = true,
                                label = { Text(s.district) }, leadingIcon = { Icon(Icons.Default.LocationCity, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = MaterialTheme.shapes.medium,
                            )
                            ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                                districts.forEach { d ->
                                    DropdownMenuItem(text = { Text(d) }, onClick = { viewModel.updateDistrict(d); districtExpanded = false })
                                }
                            }
                        }

                        if (state.district.isNotBlank()) {
                            ExposedDropdownMenuBox(expanded = upazilaExpanded, onExpandedChange = { if (upazilas.isNotEmpty()) upazilaExpanded = !upazilaExpanded }) {
                                OutlinedTextField(
                                    value = state.upazila, onValueChange = {}, readOnly = true,
                                    label = { Text(s.upazila) }, leadingIcon = { Icon(Icons.Default.Map, null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upazilaExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = MaterialTheme.shapes.medium,
                                )
                                ExposedDropdownMenu(expanded = upazilaExpanded, onDismissRequest = { upazilaExpanded = false }) {
                                    upazilas.forEach { u ->
                                        DropdownMenuItem(text = { Text(u) }, onClick = { viewModel.updateUpazila(u); upazilaExpanded = false })
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // ─── Privacy ─────────────────────
                        Text(s.privacy, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(s.showPhoneNumber, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(s.visibleToMembers, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                            Switch(
                                checked = state.isPhoneVisible, onCheckedChange = { viewModel.togglePhoneVisible() },
                                colors = SwitchDefaults.colors(checkedTrackColor = AvailableGreen, checkedThumbColor = androidx.compose.ui.graphics.Color.White),
                            )
                        }

                        // Error
                        if (state.error != null) {
                            Card(colors = CardDefaults.cardColors(containerColor = UrgencyCritical.copy(alpha = 0.1f)), shape = MaterialTheme.shapes.small) {
                                Text(state.error!!, color = UrgencyCritical, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    // ─── Save Button ─────────────────
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Box(modifier = Modifier.padding(20.dp)) {
                            SpondonButton(
                                text = if (state.isSaving) s.saving else s.saveChanges,
                                onClick = { viewModel.saveProfile() },
                                enabled = !state.isSaving && state.name.isNotBlank(),
                            )
                        }
                    }
                }
            }
        }
    }
}
