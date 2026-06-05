package com.spondon.app.core.domain.model

import java.util.Date

data class Community(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val type: CommunityType = CommunityType.PUBLIC,
    val adminIds: List<String> = emptyList(),
    val moderatorIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val pendingIds: List<String> = emptyList(),
    val district: String = "",
    val upazila: String = "",
    val bloodGroups: List<String> = emptyList(),
    val memberCount: Int = 0,
    val donationCount: Int = 0,
    val isVerified: Boolean = false,
    val isSpondon: Boolean = false,
    val createdAt: Date? = null,
)