package com.spondon.app

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import com.spondon.app.core.util.NotificationChannelHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpondonApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create notification channels early so they exist before any
        // FCM message arrives (important for background delivery).
        NotificationChannelHelper.ensureChannels(this)

        // Subscribe all users to the "all_users" FCM topic for
        // SuperAdmin announcements.
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
    }
}