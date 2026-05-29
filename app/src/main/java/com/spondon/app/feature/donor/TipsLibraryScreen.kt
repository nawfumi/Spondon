package com.spondon.app.feature.donor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.TipCategory
import com.spondon.app.core.ui.components.TipCard
import com.spondon.app.core.ui.components.tipCategoryColor
import com.spondon.app.core.ui.components.tipCategoryLabel
import com.spondon.app.core.ui.i18n.LocalAppLanguage
import com.spondon.app.core.ui.theme.BloodRed

private fun tipCategoryIcon(category: TipCategory): ImageVector = when (category) {
    TipCategory.HYDRATION_NUTRITION -> Icons.Filled.WaterDrop
    TipCategory.BEFORE_APPOINTMENT -> Icons.Filled.NightsStay
    TipCategory.DURING_DONATION -> Icons.Filled.Bloodtype
    TipCategory.AFTER_DONATION -> Icons.Filled.DirectionsRun
    TipCategory.MEDICATION_GUIDANCE -> Icons.Filled.Medication
    TipCategory.TRAVEL_DEFERRAL -> Icons.Filled.Flight
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsLibraryScreen(
    navController: NavController,
    viewModel: TipsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val language = LocalAppLanguage.current
    val isBn = language == "bn"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBn) "ডোনেশন টিপস" else "Donation Tips",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.searchTips(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(if (isBn) "টিপস খুঁজুন" else "Search tips") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchTips("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BloodRed)
                }
            } else if (state.filteredTips.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isBn) "কোনো টিপস পাওয়া যায়নি" else "No tips found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.filteredTips.forEach { (category, tips) ->
                        // Category header
                        item(key = "header_${category.name}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val catColor = tipCategoryColor(category)
                                Icon(
                                    tipCategoryIcon(category),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = tipCategoryLabel(category, language),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = catColor,
                                    modifier = Modifier.weight(1f),
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = catColor.copy(alpha = 0.1f),
                                ) {
                                    Text(
                                        text = "${tips.size}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = catColor,
                                    )
                                }
                            }
                        }

                        // Tips in this category
                        items(tips, key = { it.id }) { tip ->
                            TipCard(
                                tip = tip,
                                language = language,
                                compact = false,
                            )
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
