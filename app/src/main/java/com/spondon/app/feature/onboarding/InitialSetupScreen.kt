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

    // Blood drop pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ), label = "dropScale",
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
                    Canvas(modifier = Modifier.size(80.dp)) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        val r = size.width * 0.28f * scale

                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            radius = r * 1.6f,
                            center = Offset(cx, cy),
                        )

                        val path = Path().apply {
                            moveTo(cx, cy - r * 1.5f)
                            cubicTo(cx + r * 0.8f, cy - r * 0.5f, cx + r, cy + r * 0.3f, cx, cy + r)
                            cubicTo(cx - r, cy + r * 0.3f, cx - r * 0.8f, cy - r * 0.5f, cx, cy - r * 1.5f)
                            close()
                        }
                        drawPath(path, Color.White.copy(alpha = 0.9f))

                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f),
                            radius = r * 0.2f,
                            center = Offset(cx - r * 0.2f, cy - r * 0.1f),
                        )
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
                Column {
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
                Column {
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
