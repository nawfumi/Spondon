package com.spondon.app.core.domain.model

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
enum class NotificationType { REQUEST, JOIN, DONATION, ADMIN, REMINDER }

/** Platform-wide role. SUPER_ADMIN has unrestricted access. */
enum class UserRole { SUPER_ADMIN, USER }

/** Tracks a user's relationship to a community. */
enum class MembershipStatus { JOINED, PENDING, NONE }