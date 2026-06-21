package com.spondon.app.core.domain.model

import java.util.Date

data class BloodRequest(
    val id: String = "",
    val communityIds: List<String> = emptyList(),
    val requesterId: String = "",
    val bloodGroup: String = "",
    val urgency: Urgency = Urgency.NORMAL,
    val unitsNeeded: Int = 1,
    val patientName: String? = null,
    val requesterName: String = "",
    val communityName: String = "",
    val hospital: String = "",
    val address: String = "",
    val donationDateTime: Date? = null,
    val contactNumber: String = "",
    val patientCondition: String = "",
    val respondents: List<String> = emptyList(),
    val confirmedDonors: List<String> = emptyList(),
    val status: RequestStatus = RequestStatus.ACTIVE,
    val isPinned: Boolean = false,
    val createdAt: Date? = null,
    val expiresAt: Date? = null,
)