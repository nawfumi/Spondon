package com.spondon.app.core.domain.model

import java.util.Date

/**
 * Represents a pending join request for a private community.
 * Stored as a subcollection: communities/{communityId}/joinRequests/{requestId}
 */
data class JoinRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userBloodGroup: String = "",
    val userDistrict: String = "",
    val userUpazila: String = "",
    val message: String = "",
    val serialId: String? = null,
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    val rejectionNote: String? = null,
    val createdAt: Date? = null,
)

enum class JoinRequestStatus { PENDING, APPROVED, REJECTED }
