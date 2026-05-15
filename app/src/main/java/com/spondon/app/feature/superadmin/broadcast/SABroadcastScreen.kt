package com.spondon.app.feature.superadmin.broadcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Locale

private val SAGold = Color(0xFFFFD700)
private val SADark = Color(0xFF0D0D0D)
private val SADarkCard = Color(0xFF1A1A2E)
private val SAGreen = Color(0xFF4CAF50)
private val SARed = Color(0xFFEF5350)
private val SABlue = Color(0xFF42A5F5)
private val SAOrange = Color(0xFFFFA726)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SABroadcastScreen(
    navController: NavController,
    viewModel: SABroadcastViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LaunchedEffect(state.sendSuccess) {
        if (state.sendSuccess) {
            snackbarHostState.showSnackbar("Broadcast sent successfully!")
            viewModel.clearSendSuccess()
        }
    }

    val broadcastTypes = listOf("Announcement", "Alert", "Feature", "Maintenance")
    val targetOptions = listOf("All Users", "Donors Only", "Specific District", "Specific Blood Group")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Campaign, null, tint = SAGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Broadcast", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleHistory() }) {
                        Icon(
                            if (state.showHistory) Icons.Outlined.Edit else Icons.Outlined.History,
                            null,
                            tint = SAGold,
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SADark),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SADark,
    ) { padding ->
        if (state.showHistory) {
            // ─── History View ────────────────────────────────
            if (state.isLoadingHistory) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            "BROADCAST HISTORY",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = SAGold.copy(alpha = 0.4f),
                        )
                    }

                    if (state.history.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SADarkCard),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("No broadcasts sent yet", color = Color.White.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }

                    items(state.history) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SADarkCard),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val typeColor = when (item.type) {
                                        "Alert" -> SARed
                                        "Maintenance" -> SAOrange
                                        "Feature" -> SABlue
                                        else -> SAGreen
                                    }
                                    Card(
                                        shape = RoundedCornerShape(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.15f)),
                                    ) {
                                        Text(
                                            item.type,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = typeColor,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(6.dp))
                                Row {
                                    Text(
                                        "→ ${item.target}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SAGold.copy(alpha = 0.4f),
                                    )
                                    Spacer(Modifier.weight(1f))
                                    item.sentAt?.let {
                                        Text(
                                            dateFormat.format(it),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.3f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        } else {
            // ─── Composer View ───────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "COMPOSE BROADCAST",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = SAGold.copy(alpha = 0.4f),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SADarkCard),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Title
                            Text("Title", style = MaterialTheme.typography.labelSmall, color = SAGold.copy(alpha = 0.5f))
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = state.title,
                                onValueChange = viewModel::updateTitle,
                                placeholder = { Text("Notification title (max 65 chars)", color = Color.White.copy(alpha = 0.3f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                    cursorColor = SAGold,
                                    focusedBorderColor = SAGold.copy(alpha = 0.4f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                ),
                            )
                            Text(
                                "${state.title.length}/65",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.align(Alignment.End),
                            )

                            Spacer(Modifier.height(12.dp))

                            // Body
                            Text("Body", style = MaterialTheme.typography.labelSmall, color = SAGold.copy(alpha = 0.5f))
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = state.body,
                                onValueChange = viewModel::updateBody,
                                placeholder = { Text("Notification body (max 240 chars)", color = Color.White.copy(alpha = 0.3f)) },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                                    cursorColor = SAGold,
                                    focusedBorderColor = SAGold.copy(alpha = 0.4f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                ),
                            )
                            Text(
                                "${state.body.length}/240",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.align(Alignment.End),
                            )
                        }
                    }
                }

                // Type Selection
                item {
                    Text("TYPE", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = SAGold.copy(alpha = 0.4f))
                }
                item {
                    // Use a wrapping layout for type chips
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        broadcastTypes.forEach { type ->
                            FilterChip(
                                selected = state.type == type,
                                onClick = { viewModel.updateType(type) },
                                label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SAGold.copy(alpha = 0.2f),
                                    selectedLabelColor = SAGold,
                                    containerColor = SADarkCard,
                                    labelColor = Color.White.copy(alpha = 0.6f),
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color.Transparent,
                                    selectedBorderColor = SAGold.copy(alpha = 0.3f),
                                    enabled = true,
                                    selected = state.type == type,
                                ),
                            )
                        }
                    }
                }

                // Target Selection
                item {
                    Text("TARGET AUDIENCE", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = SAGold.copy(alpha = 0.4f))
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        targetOptions.forEach { target ->
                            Card(
                                onClick = { viewModel.updateTarget(target) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (state.target == target) SAGold.copy(alpha = 0.1f) else SADarkCard,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = state.target == target,
                                        onClick = { viewModel.updateTarget(target) },
                                        colors = RadioButtonDefaults.colors(selectedColor = SAGold),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        target,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (state.target == target) SAGold else Color.White.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }

                // Error
                item {
                    AnimatedVisibility(visible = state.error != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SARed.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                state.error ?: "",
                                modifier = Modifier.padding(12.dp),
                                color = SARed,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                // Send Button
                item {
                    Button(
                        onClick = viewModel::sendBroadcast,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !state.isSending,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SAGold,
                            contentColor = SADark,
                            disabledContainerColor = SAGold.copy(alpha = 0.3f),
                        ),
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SADark,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            Icon(Icons.Outlined.Send, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send Broadcast", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
