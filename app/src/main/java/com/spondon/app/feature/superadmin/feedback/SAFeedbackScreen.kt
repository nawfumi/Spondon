package com.spondon.app.feature.superadmin.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
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
private val SAPurple = Color(0xFFAB47BC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SAFeedbackScreen(
    navController: NavController,
    viewModel: SAFeedbackViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionSuccess) {
        state.actionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Feedback, null, tint = SAGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Feedback Inbox", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SADark),
            )
        },
        containerColor = SADark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = SAGold, strokeWidth = 2.dp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // ─── Filter Chips ────────────────────────
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Type filters
                    val types = listOf(null to "All", "BUG" to "Bug", "FEATURE" to "Feature", "COMPLAINT" to "Complaint", "OTHER" to "Other")
                    items(types) { (type, label) ->
                        FilterChip(
                            selected = state.typeFilter == type,
                            onClick = { viewModel.setTypeFilter(type) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SAGold.copy(alpha = 0.2f),
                                selectedLabelColor = SAGold,
                                containerColor = SADarkCard,
                                labelColor = Color.White.copy(alpha = 0.6f),
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = SAGold.copy(alpha = 0.4f),
                                enabled = true,
                                selected = state.typeFilter == type,
                            ),
                        )
                    }
                }

                // Status filters
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val statuses = listOf(null to "All Status", "UNREAD" to "Unread", "READ" to "Read", "RESOLVED" to "Resolved", "SPAM" to "Spam")
                    items(statuses) { (status, label) ->
                        FilterChip(
                            selected = state.statusFilter == status,
                            onClick = { viewModel.setStatusFilter(status) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SABlue.copy(alpha = 0.2f),
                                selectedLabelColor = SABlue,
                                containerColor = SADarkCard,
                                labelColor = Color.White.copy(alpha = 0.6f),
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = SABlue.copy(alpha = 0.4f),
                                enabled = true,
                                selected = state.statusFilter == status,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                val filtered = viewModel.filteredItems()

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Inbox,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White.copy(alpha = 0.2f),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No feedback found",
                                color = Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            FeedbackCard(
                                item = item,
                                onClick = { viewModel.selectFeedback(item) },
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }

        // ─── Detail Bottom Sheet ─────────────────────
        if (state.showDetail && state.selectedFeedback != null) {
            FeedbackDetailSheet(
                feedback = state.selectedFeedback!!,
                replyText = state.replyText,
                onReplyChange = viewModel::updateReply,
                onSendReply = viewModel::sendReply,
                isSending = state.isSendingReply,
                replySent = state.replySent,
                onMarkResolved = { viewModel.markResolved(state.selectedFeedback!!.id) },
                onMarkSpam = { viewModel.markSpam(state.selectedFeedback!!.id) },
                onDismiss = viewModel::dismissDetail,
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    item: SAFeedbackItem,
    onClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val statusColor = when (item.status) {
        "UNREAD" -> SAOrange
        "READ" -> SABlue
        "RESOLVED" -> SAGreen
        "SPAM" -> Color(0xFF9E9E9E)
        else -> Color.White
    }
    val typeColor = when (item.type) {
        "BUG" -> SARed
        "FEATURE" -> SABlue
        "COMPLAINT" -> SAOrange
        else -> SAPurple
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.status == "UNREAD") SADarkCard.copy(alpha = 1f)
            else SADarkCard.copy(alpha = 0.7f),
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Type badge
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.15f)),
                ) {
                    Text(
                        item.type,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = typeColor,
                    )
                }

                Spacer(Modifier.weight(1f))

                // Status badge
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
                ) {
                    Text(
                        item.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Person,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.4f),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    item.userName.ifBlank { "Anonymous" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    item.createdAt?.let { dateFormat.format(it) } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.3f),
                )
            }

            if (item.appVersion.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "v${item.appVersion} · ${item.deviceModel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.25f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackDetailSheet(
    feedback: SAFeedbackItem,
    replyText: String,
    onReplyChange: (String) -> Unit,
    onSendReply: () -> Unit,
    isSending: Boolean,
    replySent: Boolean,
    onMarkResolved: () -> Unit,
    onMarkSpam: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SADarkCard,
        tonalElevation = 0.dp,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            item {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Feedback Detail",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Spacer(Modifier.weight(1f))
                    val typeColor = when (feedback.type) {
                        "BUG" -> SARed
                        "FEATURE" -> SABlue
                        "COMPLAINT" -> SAOrange
                        else -> SAPurple
                    }
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.15f)),
                    ) {
                        Text(
                            feedback.type,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = typeColor,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Body
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                ) {
                    Text(
                        feedback.body,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Meta info
                FeedbackMetaRow("User", feedback.userName.ifBlank { "Anonymous" })
                FeedbackMetaRow("Date", feedback.createdAt?.let { dateFormat.format(it) } ?: "—")
                FeedbackMetaRow("App Version", feedback.appVersion.ifBlank { "—" })
                FeedbackMetaRow("Device", feedback.deviceModel.ifBlank { "—" })
                FeedbackMetaRow("OS", feedback.osVersion.ifBlank { "—" })
                FeedbackMetaRow("Status", feedback.status)

                Spacer(Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (feedback.status != "RESOLVED") {
                        Button(
                            onClick = onMarkResolved,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SAGreen.copy(alpha = 0.2f),
                                contentColor = SAGreen,
                            ),
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Resolve", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (feedback.status != "SPAM") {
                        OutlinedButton(
                            onClick = onMarkSpam,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9E9E9E)),
                        ) {
                            Icon(Icons.Outlined.ReportOff, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Spam", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Reply section
                Text(
                    "REPLY TO USER",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = SAGold.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(8.dp))

                if (replySent) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SAGreen.copy(alpha = 0.1f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = SAGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Reply sent as push notification", style = MaterialTheme.typography.bodySmall, color = SAGreen)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = onReplyChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type your reply...", color = Color.White.copy(alpha = 0.3f)) },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                            cursorColor = SAGold,
                            focusedBorderColor = SAGold.copy(alpha = 0.4f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onSendReply,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = replyText.isNotBlank() && !isSending,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SAGold,
                            contentColor = SADark,
                        ),
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = SADark, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send Reply", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FeedbackMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.4f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}
