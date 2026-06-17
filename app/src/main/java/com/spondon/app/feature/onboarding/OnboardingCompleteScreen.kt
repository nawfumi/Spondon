package com.spondon.app.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.EligibilityChip
import com.spondon.app.core.ui.i18n.LocalAppLanguage
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.EligibleGreen
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun OnboardingCompleteScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val language = LocalAppLanguage.current
    val isBn = language == "bn"

    // Checkmark scale animation
    val checkScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        checkScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }

    // Confetti particles
    val confettiColors = listOf(
        BloodRed, EligibleGreen, Color(0xFFFFC107), Color(0xFF2196F3),
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFFFF5722),
    )
    data class Particle(val x: Float, val speed: Float, val size: Float, val color: Color, val angle: Float)
    val particles = remember {
        List(30) {
            Particle(
                x = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 0.7f,
                size = 4f + Random.nextFloat() * 8f,
                color = confettiColors.random(),
                angle = Random.nextFloat() * 360f,
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)),
        label = "confettiTime",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val y = ((time * p.speed + p.x) % 1.2f) * size.height
                val x = p.x * size.width + sin(y / 50f + p.angle) * 30f

                drawCircle(
                    color = p.color.copy(alpha = 0.7f),
                    radius = p.size,
                    center = Offset(x, y),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(0.2f))

            // Animated checkmark
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(checkScale.value)
                    .clip(CircleShape)
                    .background(EligibleGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = EligibleGreen,
                    modifier = Modifier.size(64.dp),
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = if (isBn) "আপনি জীবন বাঁচাতে প্রস্তুত!" else "You're ready to save lives!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (isBn) "আপনার অনবোর্ডিং সম্পন্ন হয়েছে। এখন ড্যাশবোর্ডে যান এবং জীবন বাঁচানো শুরু করুন।"
                else "Your onboarding is complete. Head to the dashboard and start saving lives.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // Eligibility status card
            state.eligibilityProfile?.let { profile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (isBn) "আপনার যোগ্যতার স্থিতি" else "Your Eligibility Status",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(8.dp))
                        EligibilityChip(
                            status = profile.overallStatus,
                            language = language,
                            deferralEndDate = profile.deferralEndDate,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(0.3f))

            Button(
                onClick = {
                    viewModel.completeOnboarding()
                    navController.navigate("permissions") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
            ) {
                Text(
                    text = if (isBn) "ড্যাশবোর্ডে যান" else "Go to Dashboard",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
