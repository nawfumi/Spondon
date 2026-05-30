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

    // Blood drop pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "dropScale",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "dropAlpha",
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

            // Blood drop animation
            Canvas(modifier = Modifier.size(140.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = size.width * 0.28f * scale

                // Outer glow
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f * alpha),
                    radius = r * 1.8f,
                    center = Offset(cx, cy),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f * alpha),
                    radius = r * 1.4f,
                    center = Offset(cx, cy),
                )

                // Blood drop shape
                val path = Path().apply {
                    moveTo(cx, cy - r * 1.5f)
                    cubicTo(cx + r * 0.8f, cy - r * 0.5f, cx + r, cy + r * 0.3f, cx, cy + r)
                    cubicTo(cx - r, cy + r * 0.3f, cx - r * 0.8f, cy - r * 0.5f, cx, cy - r * 1.5f)
                    close()
                }
                drawPath(path, Color.White.copy(alpha = 0.9f * alpha))

                // Inner highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = r * 0.25f,
                    center = Offset(cx - r * 0.2f, cy - r * 0.1f),
                )
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
