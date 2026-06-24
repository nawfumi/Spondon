package com.spondon.app.feature.community

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.core.domain.model.CommunityType
import com.spondon.app.core.ui.components.SpondonButton
import com.spondon.app.core.ui.components.SpondonTextField
import com.spondon.app.core.ui.theme.*

private val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

// Bangladesh districts — a representative subset for MVP
private val DISTRICTS = listOf(
    "Dhaka", "Chittagong", "Rajshahi", "Khulna", "Sylhet",
    "Rangpur", "Barisal", "Mymensingh", "Comilla", "Gazipur",
    "Narayanganj", "Jessore", "Bogra", "Cox's Bazar", "Dinajpur",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCommunityScreen(
    navController: NavController,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.createState.collectAsState()

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.updateCreateCoverUri(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.resetCreateState()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityEvent.NavigateToCommunity -> {
                    navController.popBackStack()
                    navController.navigate("community_detail/${event.communityId}")
                }
                is CommunityEvent.NavigateBack -> navController.popBackStack()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Community",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ─── Cover Image Upload ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (state.coverUri != null) {
                    AsyncImage(
                        model = state.coverUri,
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    // Change icon overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change",
                            modifier = Modifier.padding(6.dp).size(18.dp),
                            tint = BloodRed,
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap to add cover photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                }
            }

            // ─── Name & Description ──────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SpondonTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateCreateName(it) },
                    label = "Community Name *",
                    isError = state.error?.contains("name", ignoreCase = true) == true,
                )
                Spacer(Modifier.height(12.dp))
                SpondonTextField(
                    value = state.description,
                    onValueChange = { viewModel.updateCreateDescription(it) },
                    label = "Description",
                    singleLine = false,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ─── Community Type ──────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Community Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CommunityTypeCard(
                        title = "Public",
                        icon = Icons.Default.Public,
                        isSelected = state.type == CommunityType.PUBLIC,
                        onClick = { viewModel.updateCreateType(CommunityType.PUBLIC) },
                        modifier = Modifier.weight(1f),
                    )
                    CommunityTypeCard(
                        title = "Private",
                        icon = Icons.Default.Lock,
                        isSelected = state.type == CommunityType.PRIVATE,
                        onClick = { viewModel.updateCreateType(CommunityType.PRIVATE) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Area Coverage ───────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Area Coverage",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))

                // District dropdown
                var districtExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded },
                ) {
                    OutlinedTextField(
                        value = state.district,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("District") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false },
                    ) {
                        DISTRICTS.forEach { district ->
                            DropdownMenuItem(
                                text = { Text(district) },
                                onClick = {
                                    viewModel.updateCreateDistrict(district)
                                    districtExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Upazila text field
                SpondonTextField(
                    value = state.upazila,
                    onValueChange = { viewModel.updateCreateUpazila(it) },
                    label = "Upazila (optional)",
                )
            }

            Spacer(Modifier.height(16.dp))

            // ─── Blood Groups Multi-select ───────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Supported Blood Groups",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(BLOOD_GROUPS) { group ->
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.toggleBloodGroup(group) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.selectedBloodGroups.contains(group)) BloodRed
                                    else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.selectedBloodGroups.contains(group))
                                        MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ─── Info note ───────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BloodRed.copy(alpha = 0.08f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = BloodRed,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "You will be automatically assigned as the Admin of this community.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }

            // ─── Error message ───────────────────────
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            // ─── Create Button ───────────────────────
            SpondonButton(
                text = "Create Community",
                onClick = { viewModel.createCommunity() },
                modifier = Modifier.padding(16.dp),
                enabled = state.name.isNotBlank(),
                isLoading = state.isLoading,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CommunityTypeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, BloodRed, RoundedCornerShape(12.dp))
                else Modifier,
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BloodRed.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (isSelected) BloodRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) BloodRed else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}