package com.spondon.app.feature.superadmin.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MaintenanceAmber = Color(0xFFFFD700)
private val MaintenanceDark = Color(0xFF0D0D0D)

/**
 * Full-screen maintenance gate shown when `config/maintenance.isEnabled` is true.
 * Non-dismissable — the user cannot interact with the app.
 */
@Composable
fun MaintenanceGateScreen(
    title: String,
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1500), MaintenanceDark),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Maintenance icon
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = MaintenanceAmber.copy(alpha = 0.12f),
                ),
            ) {
                Icon(
                    Icons.Outlined.Engineering,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(56.dp),
                    tint = MaintenanceAmber,
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                title.ifBlank { "আমরা আপগ্রেড করছি" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "App is under maintenance",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            if (message.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaintenanceAmber.copy(alpha = 0.1f),
                ),
            ) {
                Text(
                    "অনুগ্রহ করে কিছুক্ষণ পরে আবার চেষ্টা করুন",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaintenanceAmber.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Please try again later. We are working to improve the app for you.",
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                ),
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
