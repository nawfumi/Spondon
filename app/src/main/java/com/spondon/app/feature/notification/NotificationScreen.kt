package com.spondon.app.feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.AppNotification
import com.spondon.app.core.domain.model.NotificationType
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val s = S.strings

    LaunchedEffect(Unit) { viewModel.loadNotifications() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.notifications, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                actions = {
                    val hasUnread = state.notifications.any { !it.isRead }
                    if (hasUnread) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text(s.markAllRead, style = MaterialTheme.typography.labelMedium, color = BloodRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator()
                }
            }

            state.notifications.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.NotificationsOff, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(s.noNotifications, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            s.noNotificationsDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteNotification(notification.id)
                                    true
                                } else false
                            },
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                        .background(UrgencyCritical.copy(alpha = 0.15f)).padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(Icons.Outlined.DeleteSweep, null, tint = UrgencyCritical)
                                }
                            },
                            enableDismissFromStartToEnd = false,
                        ) {
                            NotificationItem(
                                notification = notification,
                                onClick = {
                                    if (!notification.isRead) viewModel.markAsRead(notification.id)
                                    navController.navigate("notification_detail/${notification.id}")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: AppNotification, onClick: () -> Unit) {
    val (icon, color) = getNotificationStyle(notification.type)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            // Unread dot
            if (!notification.isRead) {
                Box(
                    modifier = Modifier.padding(top = 6.dp, end = 8.dp)
                        .size(8.dp).clip(CircleShape).background(BloodRed),
                )
            } else {
                Spacer(Modifier.width(16.dp))
            }

            // Icon
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    ),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (notification.body.isNotBlank()) {
                    Text(
                        notification.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (notification.createdAt != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatRelativeTime(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

internal fun getNotificationStyle(type: NotificationType): Pair<ImageVector, Color> {
    return when (type) {
        NotificationType.REQUEST -> Icons.Outlined.Bloodtype to UrgencyCritical
        NotificationType.JOIN -> Icons.Outlined.GroupAdd to PendingAmber
        NotificationType.DONATION -> Icons.Outlined.VolunteerActivism to AvailableGreen
        NotificationType.ADMIN -> Icons.Outlined.AdminPanelSettings to SoftRose
        NotificationType.REMINDER -> Icons.Outlined.Alarm to PendingAmber
    }
}

internal fun formatRelativeTime(date: Date): String {
    val diff = System.currentTimeMillis() - date.time
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}
