package com.spondon.app.feature.superadmin.developerinfo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

private val SAGold = Color(0xFFFFD700)
private val SADark = Color(0xFF0D0D0D)
private val SADarkCard = Color(0xFF1A1A2E)
private val SAGreen = Color(0xFF4CAF50)
private val SARed = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SADeveloperInfoScreen(
    navController: NavController,
    viewModel: SADeveloperInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePhoto(it) }
    }

    // Show success snackbar
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Developer info saved successfully!")
            viewModel.clearSaveSuccess()
        }
    }

    // Show error snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.PersonPin,
                            contentDescription = null,
                            tint = SAGold,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Developer Info",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SADark,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ─── Profile Photo ────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SADarkCard),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "PROFILE PHOTO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                            ),
                            color = SAGold.copy(alpha = 0.5f),
                        )

                        Spacer(Modifier.height(16.dp))

                        // Photo preview
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(SAGold.copy(alpha = 0.1f))
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.isUploading) {
                                CircularProgressIndicator(
                                    color = SAGold,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(32.dp),
                                )
                            } else if (state.profilePhotoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = state.profilePhotoUrl,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = SAGold.copy(alpha = 0.5f),
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Text(
                                if (state.profilePhotoUrl.isNotEmpty()) "Change Photo" else "Upload Photo",
                                color = SAGold,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ─── Basic Info ───────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SADarkCard),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "BASIC INFO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                            ),
                            color = SAGold.copy(alpha = 0.5f),
                        )

                        Spacer(Modifier.height(16.dp))

                        SATextField(
                            value = state.name,
                            onValueChange = viewModel::updateName,
                            label = "Developer Name",
                            icon = Icons.Outlined.Person,
                        )

                        Spacer(Modifier.height(12.dp))

                        SATextField(
                            value = state.subtitle,
                            onValueChange = viewModel::updateSubtitle,
                            label = "Subtitle",
                            icon = Icons.Outlined.Description,
                        )

                        Spacer(Modifier.height(12.dp))

                        SATextField(
                            value = state.supportUrl,
                            onValueChange = viewModel::updateSupportUrl,
                            label = "Support / Donate URL",
                            icon = Icons.Outlined.Link,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ─── Social Links ─────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SADarkCard),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "SOCIAL LINKS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                            ),
                            color = SAGold.copy(alpha = 0.5f),
                        )

                        Spacer(Modifier.height(16.dp))

                        SATextField(
                            value = state.facebook,
                            onValueChange = viewModel::updateFacebook,
                            label = "Facebook URL",
                            icon = Icons.Outlined.Facebook,
                            iconTint = Color(0xFF1877F2),
                        )

                        Spacer(Modifier.height(12.dp))

                        SATextField(
                            value = state.whatsapp,
                            onValueChange = viewModel::updateWhatsapp,
                            label = "WhatsApp (number or URL)",
                            icon = Icons.AutoMirrored.Outlined.Chat,
                            iconTint = Color(0xFF25D366),
                        )

                        Spacer(Modifier.height(12.dp))

                        SATextField(
                            value = state.instagram,
                            onValueChange = viewModel::updateInstagram,
                            label = "Instagram URL",
                            icon = Icons.Outlined.CameraAlt,
                            iconTint = Color(0xFFE4405F),
                        )

                        Spacer(Modifier.height(12.dp))

                        SATextField(
                            value = state.linkedin,
                            onValueChange = viewModel::updateLinkedin,
                            label = "LinkedIn URL",
                            icon = Icons.Outlined.Work,
                            iconTint = Color(0xFF0A66C2),
                        )

                        Spacer(Modifier.height(12.dp))

                        SATextField(
                            value = state.github,
                            onValueChange = viewModel::updateGithub,
                            label = "GitHub URL",
                            icon = Icons.Outlined.Code,
                            iconTint = Color(0xFF9E9E9E),
                        )

                        Spacer(Modifier.height(12.dp))

                        SATextField(
                            value = state.twitter,
                            onValueChange = viewModel::updateTwitter,
                            label = "X / Twitter URL",
                            icon = Icons.Outlined.Tag,
                            iconTint = Color(0xFF1DA1F2),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ─── Save Button ──────────────────────────────
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SAGold),
                    enabled = !state.isSaving && !state.isUploading,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = SADark,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (state.isSaving) "Saving..." else "Save Developer Info",
                        fontWeight = FontWeight.Bold,
                        color = SADark,
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SATextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    iconTint: Color = SAGold,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.4f)) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White.copy(alpha = 0.8f),
            focusedBorderColor = SAGold,
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            cursorColor = SAGold,
            focusedLabelColor = SAGold,
        ),
        singleLine = true,
    )
}
