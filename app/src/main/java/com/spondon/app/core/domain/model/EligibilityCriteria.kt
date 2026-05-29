package com.spondon.app.core.domain.model

data class EligibilityCriteria(
    val id: String,
    val group: CriteriaGroup,
    val label: String,
    val labelBn: String,
    val description: String,
    val descriptionBn: String,
    val deferralType: DeferralType,
    val deferralDays: Int? = null,
)

enum class CriteriaGroup {
    GENERAL, MEDICAL, MEDICATION, TRAVEL,
    VACCINATION, LIFESTYLE, REPRODUCTIVE,
}

enum class DeferralType {
    ELIGIBLE, TEMPORARY, PERMANENT,
}
