package com.spondon.app.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Centralises notification channel creation.
 *
 * Three channels mirror the notification types in the roadmap:
 *  • blood_requests  — urgent blood donation alerts (high priority, vibration)
 *  • community       — join requests, broadcasts (default priority)
 *  • announcements   — SuperAdmin announcements (high priority)
 */
object NotificationChannelHelper {

    /** Legacy channel ID — kept as the default/fallback. */
    const val CHANNEL_ID = "spondon_notifications"

    const val CHANNEL_BLOOD = "blood_requests"
    const val CHANNEL_COMMUNITY = "community"
    const val CHANNEL_ANNOUNCEMENTS = "announcements"

    /**
     * Creates (or updates) all notification channels.
     * Safe to call repeatedly — Android ignores duplicate registrations.
     */
    fun ensureChannels(context: Context) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            // ── Default / legacy ─────────────────────────────────
            NotificationChannel(
                CHANNEL_ID,
                "Spondon Notifications",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "General Spondon notifications"
                enableVibration(true)
            },

            // ── Blood requests ───────────────────────────────────
            NotificationChannel(
                CHANNEL_BLOOD,
                "Blood Requests",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Urgent blood donation requests"
                enableVibration(true)
            },

            // ── Community ────────────────────────────────────────
            NotificationChannel(
                CHANNEL_COMMUNITY,
                "Community",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Community join requests and broadcasts"
            },

            // ── Announcements ────────────────────────────────────
            NotificationChannel(
                CHANNEL_ANNOUNCEMENTS,
                "Announcements",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "SuperAdmin announcements"
            },
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }

    /**
     * Backward-compatible single-channel convenience.
     * Delegates to [ensureChannels].
     */
    fun ensureChannel(context: Context) = ensureChannels(context)
}
