package com.spondon.app.feature.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.spondon.app.MainActivity
import com.spondon.app.R
import com.spondon.app.core.common.Constants
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SpondonFCMService : FirebaseMessagingService() {

    @Inject lateinit var firestore: FirebaseFirestore

    companion object {
        private const val CHANNEL_ID = "spondon_notifications"
        private const val CHANNEL_NAME = "Spondon Notifications"
    }

    /**
     * Called when an FCM data message arrives (sent by our Cloud Function).
     *
     * The notification Firestore document already exists (it triggered the
     * Cloud Function), so we do NOT create another one here — that would
     * cause an infinite loop.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Read from data payload (Cloud Function sends data-only messages)
        val title = message.data["title"] ?: message.notification?.title ?: "Spondon"
        val body = message.data["body"] ?: message.notification?.body ?: ""

        // Show system notification
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection(Constants.USERS_COLLECTION)
            .document(uid)
            .update("fcmToken", token)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Blood donation alerts and community notifications"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}