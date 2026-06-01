package com.spondon.app.feature.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.i18n.LocalAppLanguage
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.DarkRose
import com.spondon.app.core.ui.theme.SoftRose
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(navController: NavController) {
    val language = LocalAppLanguage.current
    val isBn = language == "bn"

    // Stagger animation
    var showTitle by remember { mutableStateOf(false) }
    var showSubtext by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300); showTitle = true
        delay(400); showSubtext = true
        delay(300); showButtons = true
    }

    // ECG wave sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "ecg")
    val ecgProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "ecgSweep",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "glowPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkRose, BloodRed, SoftRose.copy(alpha = 0.9f)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(0.2f))

            // ECG animation (same as InitialSetupScreen)
            Canvas(modifier = Modifier.size(140.dp)) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val radius = w * 0.44f

                // Outer glow circle
                drawCircle(
                    color = Color.White.copy(alpha = glowAlpha),
                    radius = radius * 1.15f,
                    center = Offset(cx, cy),
                )

                // Background circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = radius,
                    center = Offset(cx, cy),
                )

                // Circle border
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f),
                )

                // ECG wave path — classic P-QRS-T waveform
                val waveLeft = cx - radius * 0.82f
                val waveRight = cx + radius * 0.82f
                val waveWidth = waveRight - waveLeft
                val baseline = cy

                val ecgPoints = listOf(
                    0.00f to 0f,
                    0.10f to 0f,
                    0.14f to -0.06f,
                    0.18f to 0f,
                    0.22f to 0f,
                    0.26f to 0.04f,
                    0.30f to -0.42f,
                    0.34f to 0.18f,
                    0.38f to 0f,
                    0.42f to 0f,
                    0.48f to -0.10f,
                    0.54f to 0f,
                    0.60f to 0f,
                    1.00f to 0f,
                )

                val ecgPath = Path()
                val totalSteps = 120
                var firstPoint = true

                for (i in 0..totalSteps) {
                    val rawFrac = i.toFloat() / totalSteps
                    val shiftedFrac = (rawFrac + ecgProgress) % 1f

                    var yOffset = 0f
                    for (j in 0 until ecgPoints.size - 1) {
                        val (f1, y1) = ecgPoints[j]
                        val (f2, y2) = ecgPoints[j + 1]
                        if (shiftedFrac in f1..f2) {
                            val t = if (f2 - f1 > 0f) (shiftedFrac - f1) / (f2 - f1) else 0f
                            val smoothT = t * t * (3f - 2f * t)
                            yOffset = y1 + (y2 - y1) * smoothT
                            break
                        }
                    }

                    val x = waveLeft + rawFrac * waveWidth
                    val y = baseline + yOffset * radius * 1.2f

                    if (firstPoint) {
                        ecgPath.moveTo(x, y)
                        firstPoint = false
                    } else {
                        ecgPath.lineTo(x, y)
                    }
                }

                val ecgClipPath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(
                        cx - radius + 2f, cy - radius + 2f,
                        cx + radius - 2f, cy + radius - 2f,
                    ))
                }
                clipPath(ecgClipPath) {
                    drawPath(
                        path = ecgPath,
                        color = Color.White.copy(alpha = 0.9f),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Title
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 },
            ) {
                Text(
                    text = if (isBn) "প্রতিটি ফোঁটা\nগুরুত্বপূর্ণ" else "Every drop\ncounts",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        lineHeight = 42.sp,
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Subtext
            AnimatedVisibility(
                visible = showSubtext,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 },
            ) {
                Text(
                    text = if (isBn) "বাংলাদেশ জুড়ে হাজারো দাতা জীবন বাঁচাচ্ছে, আপনিও যোগ দিন"
                    else "Join thousands of donors saving lives across Bangladesh",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(0.3f))

            // Buttons
            AnimatedVisibility(
                visible = showButtons,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { navController.navigate("onboarding_quiz") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BloodRed,
                        ),
                    ) {
                        Text(
                            text = if (isBn) "শুরু করুন" else "Get Started",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    ) {
                        Text(
                            text = if (isBn) "আমি ইতিমধ্যে রক্তদান করি" else "I already donate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}
