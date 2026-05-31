package com.spondon.app.feature.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.spondon.app.core.data.local.PreferencesManager
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.DarkRose
import com.spondon.app.core.ui.theme.SoftRose
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

// ─── ViewModel ─────────────────────────────────────────────

data class InitialSetupState(
    val language: String = "bn",
    val isDarkMode: Boolean = true,
    val isReady: Boolean = false,
)

@HiltViewModel
class InitialSetupViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(InitialSetupState())
    val state: StateFlow<InitialSetupState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lang = preferencesManager.language.first()
            val dark = preferencesManager.isDarkMode.first()
            _state.value = InitialSetupState(language = lang, isDarkMode = dark, isReady = true)
        }
    }

    fun setLanguage(lang: String) {
        _state.value = _state.value.copy(language = lang)
        viewModelScope.launch { preferencesManager.setLanguage(lang) }
    }

    fun setDarkMode(dark: Boolean) {
        _state.value = _state.value.copy(isDarkMode = dark)
        viewModelScope.launch { preferencesManager.setDarkMode(dark) }
    }

    fun completeSetup() {
        viewModelScope.launch { preferencesManager.setInitialSetupComplete(true) }
    }
}

// ─── Screen ────────────────────────────────────────────────

@Composable
fun InitialSetupScreen(
    navController: NavController,
    viewModel: InitialSetupViewModel = hiltViewModel(),
) {
    val setupState by viewModel.state.collectAsState()

    // Stagger animation
    var showLogo by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200); showLogo = true
        delay(300); showLanguage = true
        delay(300); showTheme = true
        delay(200); showButton = true
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
                .padding(horizontal = 28.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Logo ──
            AnimatedVisibility(
                visible = showLogo,
                enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.5f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(modifier = Modifier.size(90.dp)) {
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

                        // ECG wave path — draw the classic P-QRS-T waveform
                        // The wave scrolls continuously from right to left
                        val waveLeft = cx - radius * 0.82f
                        val waveRight = cx + radius * 0.82f
                        val waveWidth = waveRight - waveLeft
                        val baseline = cy

                        // ECG keyframes as (fraction, yOffset) where yOffset is relative to baseline
                        // One full heartbeat cycle
                        val ecgPoints = listOf(
                            0.00f to 0f,       // flat
                            0.10f to 0f,       // flat
                            0.14f to -0.06f,   // P wave up
                            0.18f to 0f,       // P wave down
                            0.22f to 0f,       // PR segment
                            0.26f to 0.04f,    // Q dip
                            0.30f to -0.42f,   // R peak (tall spike up)
                            0.34f to 0.18f,    // S dip (below baseline)
                            0.38f to 0f,       // return to baseline
                            0.42f to 0f,       // ST segment
                            0.48f to -0.10f,   // T wave up
                            0.54f to 0f,       // T wave down
                            0.60f to 0f,       // flat
                            1.00f to 0f,       // flat rest
                        )

                        // Build the wave path, offset by ecgProgress for scrolling effect
                        val ecgPath = Path()
                        val totalSteps = 120
                        var firstPoint = true

                        for (i in 0..totalSteps) {
                            val rawFrac = i.toFloat() / totalSteps
                            // Shift the waveform by progress to create scrolling
                            val shiftedFrac = (rawFrac + ecgProgress) % 1f

                            // Interpolate y from ECG keyframes
                            var yOffset = 0f
                            for (j in 0 until ecgPoints.size - 1) {
                                val (f1, y1) = ecgPoints[j]
                                val (f2, y2) = ecgPoints[j + 1]
                                if (shiftedFrac in f1..f2) {
                                    val t = if (f2 - f1 > 0f) (shiftedFrac - f1) / (f2 - f1) else 0f
                                    // Smooth interpolation
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

                        // Clip the wave to the circle so it stays inside
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

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "স্পন্দন",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                    Text(
                        text = "Spondon",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Light,
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Language Selection ──
            AnimatedVisibility(
                visible = showLanguage,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose your language",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Text(
                        text = "আপনার ভাষা নির্বাচন করুন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LanguageCard(
                            title = "বাংলা",
                            subtitle = "Bengali",
                            selected = setupState.language == "bn",
                            onClick = { viewModel.setLanguage("bn") },
                            modifier = Modifier.weight(1f),
                        )
                        LanguageCard(
                            title = "English",
                            subtitle = "ইংরেজি",
                            selected = setupState.language == "en",
                            onClick = { viewModel.setLanguage("en") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Theme Selection ──
            AnimatedVisibility(
                visible = showTheme,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (setupState.language == "bn") "থিম নির্বাচন করুন" else "Choose your theme",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Text(
                        text = if (setupState.language == "bn") "আপনার পছন্দের রঙ সেট করুন" else "Set your preferred appearance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ThemeCard(
                            title = if (setupState.language == "bn") "ডার্ক" else "Dark",
                            icon = Icons.Filled.DarkMode,
                            selected = setupState.isDarkMode,
                            isDark = true,
                            onClick = { viewModel.setDarkMode(true) },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeCard(
                            title = if (setupState.language == "bn") "লাইট" else "Light",
                            icon = Icons.Filled.LightMode,
                            selected = !setupState.isDarkMode,
                            isDark = false,
                            onClick = { viewModel.setDarkMode(false) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Continue Button ──
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 },
            ) {
                Button(
                    onClick = {
                        viewModel.completeSetup()
                        navController.navigate("onboarding_welcome") {
                            popUpTo("initial_setup") { inclusive = true }
                        }
                    },
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
                        text = if (setupState.language == "bn") "চালিয়ে যান" else "Continue",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

// ─── Composable helpers ────────────────────────────────────

@Composable
private fun LanguageCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) Color.White else Color.White.copy(alpha = 0.25f)
    val bgAlpha = if (selected) 0.2f else 0.08f

    Card(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = bgAlpha),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color.White,
                        unselectedColor = Color.White.copy(alpha = 0.5f),
                    ),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = Color.White,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun ThemeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) Color.White else Color.White.copy(alpha = 0.25f)
    val bgAlpha = if (selected) 0.2f else 0.08f
    val previewBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
    val previewFg = if (isDark) Color(0xFFE8E8E8) else Color(0xFF1A1A1A)

    Card(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = bgAlpha),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Mini preview circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(previewBg)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.3f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = previewFg,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                    modifier = Modifier.size(20.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color.White,
                        unselectedColor = Color.White.copy(alpha = 0.5f),
                    ),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}
