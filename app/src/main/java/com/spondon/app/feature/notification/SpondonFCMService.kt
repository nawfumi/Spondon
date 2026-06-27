package com.spondon.app.feature.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.spondon.app.MainActivity
import com.spondon.app.R
import com.spondon.app.core.common.Constants
import com.spondon.app.core.data.local.dao.NotificationDao
import com.spondon.app.core.data.local.entity.NotificationEntity
import com.spondon.app.core.domain.model.NotificationType
import com.spondon.app.core.domain.model.channelId
import com.spondon.app.core.util.NotificationChannelHelper
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SpondonFCMService : FirebaseMessagingService() {

    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var notificationDao: NotificationDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when an FCM data message arrives (sent by our Cloud Function).
     *
     * The notification Firestore document already exists (it triggered the
     * Cloud Function), so we do NOT create another one here — that would
     * cause an infinite loop.
     *
     * We DO save the notification to the local Room DB so the inbox screen
     * has data even if the ViewModel hasn't synced yet.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Spondon"
        val body = data["body"] ?: message.notification?.body ?: ""
        val typeStr = data["type"] ?: "REQUEST"
        val deepLink = data["deepLink"] ?: ""
        val notifId = data["notificationId"] ?: UUID.randomUUID().toString()

        val type = try {
            NotificationType.valueOf(typeStr)
        } catch (_: Exception) {
            NotificationType.REQUEST
        }

        // Save to Room DB (non-blocking) so the inbox is up-to-date
        saveToLocalDb(notifId, type, title, body, deepLink)

        // Build and show system notification with correct channel + actions
        showNotification(notifId, type, title, body, deepLink, data)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection(Constants.USERS_COLLECTION)
            .document(uid)
            .update("fcmToken", token)
    }

    // ── Local DB persistence ────────────────────────────────────────

    private fun saveToLocalDb(
        notifId: String,
        type: NotificationType,
        title: String,
        body: String,
        deepLink: String,
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        serviceScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    id = notifId,
                    type = type.name,
                    title = title,
                    body = body,
                    deepLink = deepLink,
                    isRead = false,
                    createdAt = System.currentTimeMillis(),
                    userId = uid,
                )
            )
        }
    }

    // ── Show notification ───────────────────────────────────────────

    private fun showNotification(
        notifId: String,
        type: NotificationType,
        title: String,
        body: String,
        deepLink: String,
        data: Map<String, String>,
    ) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        NotificationChannelHelper.ensureChannels(this)

        val channelId = type.channelId()

        // Tap intent → open app at the relevant screen
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notif_type", type.name)
            putExtra("deepLink", deepLink)
            putExtra("notifId", notifId)
            data.forEach { (k, v) -> putExtra(k, v) }
        }
        val tapPending = PendingIntent.getActivity(
            this,
            notifId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPending)

        // ── Add action buttons based on type ────────────────────────
        when (type) {
            NotificationType.BLOOD_REQUEST,
            NotificationType.REQUEST,
            -> {
                builder.addAction(
                    buildAction("VIEW_REQUEST", "View Request", notifId, data)
                )
                builder.addAction(
                    buildAction("ACCEPT_REQUEST", "Accept & Donate", notifId, data)
                )
            }

            NotificationType.COMMUNITY_JOIN_REQUEST,
            NotificationType.JOIN,
            -> {
                builder.addAction(
                    buildAction("APPROVE_JOIN", "Approve", notifId, data)
                )
                builder.addAction(
                    buildAction("REJECT_JOIN", "Reject", notifId, data)
                )
            }

            // Other types: no action buttons (just tap to open)
            else -> {}
        }

        manager.notify(notifId.hashCode(), builder.build())
    }

    // ── Action button helper ────────────────────────────────────────

    private fun buildAction(
        action: String,
        label: String,
        notifId: String,
        data: Map<String, String>,
    ): NotificationCompat.Action {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra("notifId", notifId)
            data.forEach { (k, v) -> putExtra(k, v) }
        }
        val pi = PendingIntent.getBroadcast(
            this,
            "${action}_$notifId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action(0, label, pi)
    }
}