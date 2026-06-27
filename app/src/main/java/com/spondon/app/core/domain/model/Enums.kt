package com.spondon.app.core.domain.model

import com.spondon.app.core.util.NotificationChannelHelper

enum class BloodGroup(val display: String) {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
}

enum class Urgency { CRITICAL, MODERATE, NORMAL }
enum class RequestStatus { ACTIVE, FULFILLED, EXPIRED, CANCELLED }
enum class CommunityType { PUBLIC, PRIVATE, SPONDON }
enum class CommunityRole { ADMIN, MODERATOR, MEMBER }
enum class DonationStatus { CONFIRMED, PENDING }

/**
 * Notification types — includes both legacy names (REQUEST, JOIN, DONATION,
 * ADMIN, REMINDER) and the expanded roadmap names.
 */
enum class NotificationType {
    // ── Legacy (kept for backward compatibility) ─────────────────
    REQUEST,
    JOIN,
    DONATION,
    ADMIN,
    REMINDER,

    // ── Roadmap types ────────────────────────────────────────────
    BLOOD_REQUEST,
    REQUEST_ACCEPTED,
    DONATION_CONFIRMED,
    COMMUNITY_JOIN_REQUEST,
    JOIN_REQUEST_ACCEPTED,
    JOIN_REQUEST_REJECTED,
    COMMUNITY_BROADCAST,
    SUPERADMIN_ANNOUNCEMENT,
}

/**
 * Maps a [NotificationType] to its Android notification channel ID.
 */
fun NotificationType.channelId(): String = when (this) {
    NotificationType.REQUEST,
    NotificationType.BLOOD_REQUEST,
    NotificationType.REQUEST_ACCEPTED,
    NotificationType.DONATION,
    NotificationType.DONATION_CONFIRMED,
    -> NotificationChannelHelper.CHANNEL_BLOOD

    NotificationType.JOIN,
    NotificationType.COMMUNITY_JOIN_REQUEST,
    NotificationType.JOIN_REQUEST_ACCEPTED,
    NotificationType.JOIN_REQUEST_REJECTED,
    NotificationType.ADMIN,
    NotificationType.COMMUNITY_BROADCAST,
    -> NotificationChannelHelper.CHANNEL_COMMUNITY

    NotificationType.SUPERADMIN_ANNOUNCEMENT,
    -> NotificationChannelHelper.CHANNEL_ANNOUNCEMENTS

    NotificationType.REMINDER,
    -> NotificationChannelHelper.CHANNEL_ID
}

/** Platform-wide role. SUPER_ADMIN has unrestricted access. */
enum class UserRole { SUPER_ADMIN, USER }

/** Tracks a user's relationship to a community. */
enum class MembershipStatus { JOINED, PENDING, NONE }