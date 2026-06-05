package com.spondon.app.core.common

object Constants {
    const val DEFAULT_COOLDOWN_DAYS = 120
    const val MIN_OVERRIDE_DAYS = 90
    const val MIN_DONOR_AGE = 18
    const val MAX_DONOR_AGE = 60
    const val MIN_DONOR_WEIGHT_KG = 50f
    const val OTP_LENGTH = 6
    const val OTP_TIMEOUT_SECONDS = 60L

    const val USERS_COLLECTION = "users"
    const val COMMUNITIES_COLLECTION = "communities"
    const val REQUESTS_COLLECTION = "requests"
    const val DONATIONS_COLLECTION = "donations"
    const val NOTIFICATIONS_COLLECTION = "notifications"
    const val JOIN_REQUESTS_COLLECTION = "joinRequests"

    // Spondon global community
    const val COMMUNITY_POSTS_COLLECTION = "communityPosts"
    const val SPONDON_COMMUNITY_NAME = "Spondon · স্পন্দন"
    const val SPONDON_CONFIG_DOC = "config/spondon_community"
}