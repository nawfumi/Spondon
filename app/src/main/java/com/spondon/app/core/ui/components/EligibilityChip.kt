package com.spondon.app.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spondon.app.core.domain.model.DeferralType
import com.spondon.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EligibilityChip(
    status: DeferralType,
    language: String = "en",
    deferralEndDate: Long? = null,
    modifier: Modifier = Modifier,
) {
    val (color, icon, label) = when (status) {
        DeferralType.ELIGIBLE -> Triple(
            EligibleGreen,
            Icons.Filled.CheckCircle,
            if (language == "bn") "যোগ্য" else "Eligible",
        )
        DeferralType.TEMPORARY -> {
            val dateStr = deferralEndDate?.let {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdf.format(Date(it))
            }
            Triple(
                DeferredAmber,
                Icons.Filled.Schedule,
                if (language == "bn") {
                    if (dateStr != null) "স্থগিত $dateStr পর্যন্ত" else "সাময়িকভাবে স্থগিত"
                } else {
                    if (dateStr != null) "Deferred until $dateStr" else "Temporarily Deferred"
                },
            )
        }
        DeferralType.PERMANENT -> Triple(
            IneligibleRed,
            Icons.Filled.Cancel,
            if (language == "bn") "অযোগ্য" else "Not eligible",
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color,
            )
        }
    }
}
