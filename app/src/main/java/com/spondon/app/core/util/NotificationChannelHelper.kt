package com.spondon.app.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Centralises notification channel creation so that
 * [SpondonFCMService] and [NotificationObserver] don't duplicate
 * the same channel setup code.
 */
object NotificationChannelHelper {

    const val CHANNEL_ID = "spondon_notifications"
    private const val CHANNEL_NAME = "Spondon Notifications"

    /**
     * Creates (or updates) the app's default notification channel.
     * Safe to call repeatedly — Android ignores duplicate channel registrations.
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
    }
}
