package com.spondon.app.core.util

import com.spondon.app.core.domain.model.BloodRequest
import com.spondon.app.core.domain.model.Urgency
import java.text.SimpleDateFormat
import java.util.Locale

object ShareUtils {

    /** Creates a new formatter per call — SimpleDateFormat is NOT thread-safe. */
    private fun dateFormat() = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun buildShareText(request: BloodRequest): String {
        val urgencyLabel = when (request.urgency) {
            Urgency.CRITICAL -> "\u26A0\uFE0F CRITICAL"
            Urgency.MODERATE -> "\u26A0\uFE0F URGENT"
            Urgency.NORMAL -> "NORMAL"
        }

        val donationDate = request.donationDateTime?.let { dateFormat().format(it) } ?: "Not set"

        return buildString {
            appendLine("\uD83E\uDE78 BLOOD REQUEST - $urgencyLabel")
            appendLine()
            appendLine("\uD83D\uDCA5 Blood Group: ${request.bloodGroup}")
            appendLine("\uD83D\uDCAA Units Needed: ${request.unitsNeeded}")
            appendLine("\uD83C\uDFE5 Hospital: ${request.hospital}")
            if (request.address.isNotBlank()) {
                appendLine("\uD83D\uDCCD Address: ${request.address}")
            }
            appendLine("\uD83D\uDCC5 Donation Date: $donationDate")
            if (!request.patientName.isNullOrBlank()) {
                appendLine("\uD83D\uDC64 Patient: ${request.patientName}")
            }
            if (request.patientCondition.isNotBlank()) {
                appendLine("\uD83D\uDCC8 Condition: ${request.patientCondition}")
            }
            if (request.contactNumber.isNotBlank()) {
                appendLine("\uD83D\uDCDE Contact: ${request.contactNumber}")
            }
            if (request.requesterName.isNotBlank()) {
                appendLine("\uD83D\uDE4F Requested by: ${request.requesterName}")
            }
            if (request.communityName.isNotBlank()) {
                appendLine("\uD83D\uDC65 Community: ${request.communityName}")
            }
            appendLine()
            appendLine("Please help save a life! \u2764\uFE0F")
        }.trim()
    }

    fun buildShortShareText(request: BloodRequest): String {
        val urgencyLabel = when (request.urgency) {
            Urgency.CRITICAL -> "\u26A0\uFE0F CRITICAL"
            Urgency.MODERATE -> "\u26A0\uFE0F URGENT"
            Urgency.NORMAL -> ""
        }
        val prefix = if (urgencyLabel.isNotEmpty()) "$urgencyLabel - " else ""
        return "\uD83E\uDE78 ${prefix}${request.bloodGroup} blood needed at ${request.hospital}. " +
                "${request.unitsNeeded} unit(s) required. " +
                "Contact: ${request.contactNumber}. Help save a life! \u2764\uFE0F"
    }
}
