package com.spondon.app.feature.donor

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.User
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDonorScreen(
    navController: NavController,
    viewModel: DonorViewModel = hiltViewModel(),
) {
    val state by viewModel.findState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadFindDonor() }

    val s = S.strings

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(s.findDonor, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ─── Search Bar ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(BloodRed),
                            decorationBox = { innerTextField ->
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        "Search by name, blood group...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }
                }

                // Filter button
                FilledIconButton(
                    onClick = { viewModel.toggleFilterSheet() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (state.selectedBloodGroups.isNotEmpty() ||
                            state.selectedCommunityId != null ||
                            state.availableOnly
                        ) BloodRed else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (state.selectedBloodGroups.isNotEmpty() ||
                            state.selectedCommunityId != null ||
                            state.availableOnly
                        ) Color.White else MaterialTheme.colorScheme.onBackground,
                    ),
                    modifier = Modifier.size(46.dp),
                ) {
                    Icon(Icons.Outlined.FilterList, "Filters", modifier = Modifier.size(22.dp))
                }
            }

            // ─── Available Now Toggle ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AvailableGreen),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Available Now",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Switch(
                    checked = state.availableOnly,
                    onCheckedChange = { viewModel.toggleAvailableOnly() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AvailableGreen,
                        checkedThumbColor = Color.White,
                    ),
                )
            }

            // ─── Blood Group Filter Chips ────────────────────
            LazyRow(
                modifier = Modifier.padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(DonorViewModel.BLOOD_GROUPS) { bg ->
                    FilterChip(
                        selected = bg in state.selectedBloodGroups,
                        onClick = { viewModel.toggleBloodGroupFilter(bg) },
                        label = {
                            Text(
                                bg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BloodRed,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.height(32.dp),
                    )
                }
            }

            // ─── Results Count + Sort ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${state.donors.size} donor${if (state.donors.size != 1) "s" else ""} found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )

                // Sort dropdown
                var sortMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier.clickable { sortMenuExpanded = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Sort,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = BloodRed,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            state.sortBy.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = BloodRed,
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                    ) {
                        DonorSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    sortMenuExpanded = false
                                },
                                leadingIcon = {
                                    if (state.sortBy == option) {
                                        Icon(Icons.Filled.Check, null, tint = BloodRed, modifier = Modifier.size(18.dp))
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // ─── Donor List ──────────────────────────────────
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = BloodRed, strokeWidth = 2.dp)
                    }
                }

                state.donors.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Icon(
                                Icons.Outlined.PersonSearch,
                                null,
                                tint = BloodRed.copy(alpha = 0.3f),
                                modifier = Modifier.size(72.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No available donors found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Try adjusting your filters or search query",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.donors, key = { it.uid }) { donor ->
                            DonorCard(
                                donor = donor,
                                onClick = {
                                    navController.navigate("donor_profile/${donor.uid}")
                                },
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Donor Card
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DonorCard(
    donor: User,
    onClick: () -> Unit,
) {
    val isAvailable = donor.lastDonationDate?.let {
        val daysSince = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
            java.util.Date().time - it.time
        ).toInt()
        val requiredDays = if (donor.availabilityOverride) 90 else donor.donationInterval
        daysSince >= requiredDays
    } ?: true

    val cooldownDays = if (!isAvailable && donor.lastDonationDate != null) {
        val daysSince = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
            java.util.Date().time - donor.lastDonationDate.time
        ).toInt()
        val requiredDays = if (donor.availabilityOverride) 90 else donor.donationInterval
        (requiredDays - daysSince).coerceAtLeast(0)
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar with blood group badge
            Box {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(BloodRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        null,
                        tint = BloodRed.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp),
                    )
                }
                // Blood group badge overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = BloodRed,
                ) {
                    Text(
                        text = donor.bloodGroup.ifBlank { "?" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = donor.name.ifBlank { "Unknown Donor" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (donor.district.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                donor.district,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            )
                        }
                    }
                    Text(
                        "${donor.totalDonations} donation${if (donor.totalDonations != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
                Spacer(Modifier.height(4.dp))

                // Availability indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAvailable) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AvailableGreen),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Available",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AvailableGreen,
                        )
                    } else {
                        Icon(
                            Icons.Filled.Lock,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = UnavailableGrey,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$cooldownDays days",
                            style = MaterialTheme.typography.labelSmall,
                            color = UnavailableGrey,
                        )
                    }
                }
            }

            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            )
        }
    }
}