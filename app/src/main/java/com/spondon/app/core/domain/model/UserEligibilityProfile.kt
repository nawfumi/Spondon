package com.spondon.app.core.domain.model

data class UserEligibilityProfile(
    val overallStatus: DeferralType = DeferralType.ELIGIBLE,
    val deferralEndDate: Long? = null,
    val deferralReason: String? = null,
    val deferralReasonBn: String? = null,
    val quizAnswers: Map<String, String> = emptyMap(),
    val lastQuizDate: Long = 0L,
)
