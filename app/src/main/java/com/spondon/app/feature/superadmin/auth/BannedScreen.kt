package com.spondon.app.feature.superadmin.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
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

/**
 * Full-screen ban gate shown when a user's `isBanned` flag is true.
 * Non-dismissable — back gesture disabled at the NavGraph level.
 */
@Composable
fun BannedScreen(
    banReason: String?,
    onSignOut: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A0000), Color(0xFF0D0D0D)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Ban icon
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3D1515),
                ),
            ) {
                Icon(
                    Icons.Outlined.Block,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(56.dp),
                    tint = Color(0xFFFF4444),
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Account Suspended",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "আপনার অ্যাকাউন্ট স্থগিত করা হয়েছে",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            if (!banReason.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Reason",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF6B6B),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            banReason,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            Text(
                "If you believe this is a mistake, please contact the platform administrator.",
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                ),
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF6B6B),
                ),
            ) {
                Text("Sign Out", fontWeight = FontWeight.Medium)
            }
        }
    }
}
