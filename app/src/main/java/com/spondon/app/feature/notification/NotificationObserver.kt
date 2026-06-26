package com.spondon.app.feature.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.spondon.app.MainActivity
import com.spondon.app.R
import com.spondon.app.core.common.Constants
import com.spondon.app.core.util.NotificationChannelHelper

/**
 * Observes the Firestore notifications collection in real-time and posts
 * local system notifications for any new unread documents.
 *
 * Since the app doesn't have a Cloud Function backend to send FCM pushes,
 * this observer bridges the gap by converting Firestore writes into local
 * Android notifications visible in the notification panel.
 */
class NotificationObserver(private val context: Context) {



    private var listener: ListenerRegistration? = null
    // Track notification IDs we've already shown so we don't re-notify
    private val shownNotificationIds = mutableSetOf<String>()
    private var isFirstSnapshot = true

    fun startObserving() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        stopObserving()
        isFirstSnapshot = true
        shownNotificationIds.clear()

        listener = FirebaseFirestore.getInstance()
            .collection(Constants.NOTIFICATIONS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                if (isFirstSnapshot) {
                    // On first snapshot, just record existing IDs without notifying
                    snapshot.documents.forEach { doc ->
                        shownNotificationIds.add(doc.id)
                    }
                    isFirstSnapshot = false
                    return@addSnapshotListener
                }

                // For subsequent snapshots, only notify for NEW documents
                snapshot.documentChanges.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val docId = change.document.id
                        if (!shownNotificationIds.contains(docId)) {
                            shownNotificationIds.add(docId)
                            val data = change.document.data
                            val title = data["title"] as? String ?: "Spondon"
                            val body = data["body"] as? String ?: ""
                            showLocalNotification(title, body, docId)
                        }
                    }
                }
            }
    }

    fun stopObserving() {
        listener?.remove()
        listener = null
    }

    private fun showLocalNotification(title: String, body: String, docId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannelHelper.ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, docId.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannelHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(docId.hashCode(), notification)
    }
}
