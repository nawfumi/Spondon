package com.spondon.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spondon.app.core.domain.model.DonationTip
import com.spondon.app.core.domain.model.TipCategory
import com.spondon.app.core.ui.theme.*

fun tipCategoryColor(category: TipCategory) = when (category) {
    TipCategory.HYDRATION_NUTRITION -> TipHydration
    TipCategory.BEFORE_APPOINTMENT -> TipBefore
    TipCategory.DURING_DONATION -> TipDuring
    TipCategory.AFTER_DONATION -> TipAfter
    TipCategory.MEDICATION_GUIDANCE -> TipMedication
    TipCategory.TRAVEL_DEFERRAL -> TipTravel
}

fun tipCategoryLabel(category: TipCategory, language: String) = when (category) {
    TipCategory.HYDRATION_NUTRITION -> if (language == "bn") "হাইড্রেশন ও পুষ্টি" else "Hydration & Nutrition"
    TipCategory.BEFORE_APPOINTMENT -> if (language == "bn") "অ্যাপয়েন্টমেন্টের আগে" else "Before Appointment"
    TipCategory.DURING_DONATION -> if (language == "bn") "রক্তদানের সময়" else "During Donation"
    TipCategory.AFTER_DONATION -> if (language == "bn") "রক্তদানের পরে" else "After Donation"
    TipCategory.MEDICATION_GUIDANCE -> if (language == "bn") "ওষুধ" else "Medication"
    TipCategory.TRAVEL_DEFERRAL -> if (language == "bn") "ভ্রমণ" else "Travel"
}

fun tipIconFromName(name: String): ImageVector = when (name) {
    "WaterDrop" -> Icons.Filled.WaterDrop
    "Restaurant" -> Icons.Filled.Restaurant
    "KingBed" -> Icons.Filled.KingBed
    "Checkroom" -> Icons.Filled.Checkroom
    "FitnessCenter" -> Icons.Filled.FitnessCenter
    "LocalHospital" -> Icons.Filled.LocalHospital
    "Medication" -> Icons.Filled.Medication
    "Flight" -> Icons.Filled.Flight
    "Favorite" -> Icons.Filled.Favorite
    "SelfImprovement" -> Icons.Filled.SelfImprovement
    "BatteryChargingFull" -> Icons.Filled.BatteryChargingFull
    "MonitorHeart" -> Icons.Filled.MonitorHeart
    "Inventory2" -> Icons.Filled.Inventory2
    "DirectionsRun" -> Icons.AutoMirrored.Filled.DirectionsRun
    "Badge" -> Icons.Filled.Badge
    "BreakfastDining" -> Icons.Filled.BreakfastDining
    "AirlineSeatReclineNormal" -> Icons.Filled.AirlineSeatReclineNormal
    "Psychology" -> Icons.Filled.Psychology
    "Forum" -> Icons.Filled.Forum
    "BandageThin" -> Icons.Filled.Healing
    "Healing" -> Icons.Filled.Healing
    "NoFood" -> Icons.Filled.NoFood
    else -> Icons.Filled.Lightbulb
}

@Composable
fun TipCard(
    tip: DonationTip,
    language: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val color = tipCategoryColor(tip.category)
    val icon = tipIconFromName(tip.iconName)
    val title = if (language == "bn") tip.titleBn else tip.title
    val body = if (language == "bn") tip.bodyBn else tip.body
    val categoryLabel = tipCategoryLabel(tip.category, language)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color),
            )

            if (compact) {
                // Compact layout: icon + title + short body in row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else {
                // Full layout: category chip + icon + title + full body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = color.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = categoryLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = color,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}
