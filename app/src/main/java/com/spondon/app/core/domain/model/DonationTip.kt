package com.spondon.app.core.domain.model

data class DonationTip(
    val id: String,
    val category: TipCategory,
    val title: String,
    val titleBn: String,
    val body: String,
    val bodyBn: String,
    val iconName: String,
)

enum class TipCategory {
    HYDRATION_NUTRITION,
    BEFORE_APPOINTMENT,
    DURING_DONATION,
    AFTER_DONATION,
    MEDICATION_GUIDANCE,
    TRAVEL_DEFERRAL,
}
