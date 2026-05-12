package com.spondon.app.feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.AppNotification
import com.spondon.app.core.domain.model.NotificationType
import com.spondon.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val notificationId = navController.currentBackStackEntry
        ?.arguments?.getString("notificationId") ?: ""

    // Mark as read the moment this screen is opened
    LaunchedEffect(notificationId) {
        if (notificationId.isNotBlank()) {
            viewModel.markAsRead(notificationId)
        }
    }

    val notification = state.notifications.find { it.id == notificationId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            // ── Still loading ────────────────────────────────────────
            state.isLoading && notification == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedLoadingIndicator()
                }
            }

            // ── Not found ────────────────────────────────────────────
            notification == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.NotificationsOff, null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Notification not found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            // ── Detail view ──────────────────────────────────────────
            else -> {
                val (icon, accentColor) = getNotificationStyle(notification.type)
                val action = resolveAction(notification)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // ── Scrollable body ──────────────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // Type icon badge
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                icon, null,
                                tint = accentColor,
                                modifier = Modifier.size(34.dp),
                            )
                        }

                        // Type label chip
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.10f),
                        ) {
                            Text(
                                text = notification.type.label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                ),
                                color = accentColor,
                            )
                        }

                        // Title
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 32.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        // Body / description
                        if (notification.body.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
                            Text(
                                text = notification.body,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 26.sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            )
                        }

                        // Timestamp
                        if (notification.createdAt != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Schedule, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = SimpleDateFormat(
                                        "dd MMM yyyy  •  hh:mm a",
                                        Locale.getDefault(),
                                    ).format(notification.createdAt),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                )
                            }
                        }
                    }

                    // ── Action button (only when deepLink is contextual) ──
                    if (action != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            Box(modifier = Modifier.padding(20.dp)) {
                                Button(
                                    onClick = {
                                        try {
                                            navController.navigate(action.deepLink)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                    ),
                                ) {
                                    Icon(
                                        action.icon, null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = action.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Resolved action for the bottom CTA button. Null = no button (system / super-admin message). */
private data class NotificationAction(
    val label: String,
    val deepLink: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private fun resolveAction(notification: AppNotification): NotificationAction? {
    val link = notification.deepLink
    if (link.isBlank()) return null          // system / super-admin broadcast — no button

    return when {
        link.startsWith("request_detail/") -> NotificationAction(
            label = "View Blood Request",
            deepLink = link,
            icon = Icons.Outlined.Bloodtype,
        )
        link.startsWith("community_detail/") -> NotificationAction(
            label = "View Community",
            deepLink = link,
            icon = Icons.Outlined.Groups,
        )
        else -> null                          // unrecognised deep link — no button
    }
}

/** Human-readable label for each notification type. */
private val NotificationType.label: String
    get() = when (this) {
        NotificationType.REQUEST   -> "BLOOD REQUEST"
        NotificationType.JOIN      -> "COMMUNITY"
        NotificationType.DONATION  -> "DONATION"
        NotificationType.ADMIN     -> "ADMIN"
        NotificationType.REMINDER  -> "REMINDER"
    }
