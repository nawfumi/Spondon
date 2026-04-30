package com.spondon.app.core.domain.model

import java.util.Date

data class User(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val bloodGroup: String = "",
    val dob: Date? = null,
    val weight: Float = 0f,
    val isDonor: Boolean = false,
    val lastDonationDate: Date? = null,
    val donationInterval: Int = 120,
    val availabilityOverride: Boolean = false,
    val totalDonations: Int = 0,
    val communityIds: List<String> = emptyList(),
    val district: String = "",
    val upazila: String = "",
    val isPhoneVisible: Boolean = true,
    val badges: List<String> = emptyList(),
    val fcmToken: String = "",
    val role: UserRole = UserRole.USER,
    val createdAt: Date? = null,
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val bannedAt: Date? = null,
)