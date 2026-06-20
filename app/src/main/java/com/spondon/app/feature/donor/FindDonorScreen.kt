package com.spondon.app.feature.donor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.UnavailableGrey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDonorScreen(
    navController: NavController,
    viewModel: DonorViewModel = hiltViewModel(),
) {
    val state by viewModel.findState.collectAsState()
    val hideSensitiveData by viewModel.hideSensitiveData.collectAsState()

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
                    shape = RoundedCornerShape(16.dp),
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
            if (!hideSensitiveData) {
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
                            Icons.AutoMirrored.Outlined.Sort,
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
                        ContainedLoadingIndicator()
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.donors, key = { it.uid }) { donor ->
                            DonorCard(
                                donor = donor,
                                hideSensitiveData = viewModel.shouldHideForUser(donor.uid),
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
// Donor Card — redesigned to match RequestCard M3 style
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DonorCard(
    donor: User,
    hideSensitiveData: Boolean = false,
    onClick: () -> Unit,
) {
    val isAvailable = donor.lastDonationDate?.let {
        val daysSince = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
            java.util.Date().time - it.time
        ).toInt()
        val requiredDays = if (donor.availabilityOverride) 90 else donor.donationInterval
        daysSince >= requiredDays
    } ?: true

    val cooldownDays = if (!isAvailable) {
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
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: Avatar + Name + Blood badge ────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BloodRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        null,
                        tint = BloodRed.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = donor.name.ifBlank { "Unknown Donor" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (donor.district.isNotBlank()) {
                        Text(
                            text = donor.district,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Blood group badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BloodRed,
                ) {
                    Text(
                        text = donor.bloodGroup.ifBlank { "?" },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            // ── Info chips row ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Donations chip
                DonorInfoChip(
                    icon = Icons.Outlined.VolunteerActivism,
                    label = "Donations",
                    value = "${donor.totalDonations}",
                    chipColor = MaterialTheme.colorScheme.primary,
                    chipBg = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f),
                )
                // Availability chip
                if (!hideSensitiveData) {
                    DonorInfoChip(
                        icon = if (isAvailable) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                        label = "Status",
                        value = if (isAvailable) "Available" else "$cooldownDays days",
                        chipColor = if (isAvailable) AvailableGreen else UnavailableGrey,
                        chipBg = if (isAvailable) AvailableGreen.copy(alpha = 0.1f) else UnavailableGrey.copy(alpha = 0.1f),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    DonorInfoChip(
                        icon = Icons.Filled.Lock,
                        label = "Status",
                        value = "Private",
                        chipColor = UnavailableGrey,
                        chipBg = UnavailableGrey.copy(alpha = 0.1f),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DonorInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = chipColor,
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