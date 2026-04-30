package com.spondon.app.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.util.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    navController: NavController,
    viewModel: SecuritySettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = remember { BiometricHelper.canAuthenticate(context) }
    val s = S.strings

    // ─── Handle pending auth actions via biometric prompt ─────
    LaunchedEffect(state.pendingAuthAction) {
        val action = state.pendingAuthAction ?: return@LaunchedEffect
        if (activity == null) {
            viewModel.onAuthResult(false)
            return@LaunchedEffect
        }
        BiometricHelper.showBiometricPrompt(
            activity = activity,
            title = action.promptTitle,
            subtitle = "Verify your identity to continue",
            negativeButtonText = s.cancel,
            onSuccess = { viewModel.onAuthResult(true) },
            onError = { viewModel.onAuthResult(false) },
        )
    }

    // Auto-lock options
    val autoLockOptions = listOf(
        "always" to "Always",
        "1" to "After 1 minute",
        "2" to "After 2 minutes",
        "5" to "After 5 minutes",
        "10" to "After 10 minutes",
        "never" to "Never",
    )

    // Secure screen options
    val secureScreenOptions = listOf(
        "off" to "Off",
        "always" to "Always",
        "when_locked" to "Only when locked",
    )

    // Dropdown expand states
    var autoLockExpanded by remember { mutableStateOf(false) }
    var secureScreenExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        s.security,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BloodRed, strokeWidth = 2.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // ════════════════════════════════════════════════
                // 1. Biometric Lock
                // ════════════════════════════════════════════════
                item {
                    SecuritySectionHeader("Biometric Authentication")
                }

                item {
                    if (biometricAvailable) {
                        SecurityToggleCard(
                            icon = Icons.Outlined.Fingerprint,
                            title = "Lock with biometrics",
                            subtitle = "Require fingerprint or face unlock to open the app",
                            checked = state.isBiometricEnabled,
                            onToggle = { viewModel.requestToggleBiometric() },
                        )
                    } else {
                        SecurityDisabledCard(
                            icon = Icons.Outlined.Fingerprint,
                            title = "Lock with biometrics",
                            subtitle = "Biometric hardware not available on this device",
                        )
                    }
                }

                // ════════════════════════════════════════════════
                // 2. Auto-Lock Timeout
                // ════════════════════════════════════════════════
                item {
                    SecuritySectionHeader("Auto-Lock")
                }

                item {
                    val isEnabled = biometricAvailable && state.isBiometricEnabled
                    val currentLabel = autoLockOptions.firstOrNull { it.first == state.autoLockTimeout }
                        ?.second ?: "Always"

                    SecurityDropdownCard(
                        icon = Icons.Outlined.Timer,
                        title = "Lock when idle",
                        subtitle = if (isEnabled) currentLabel else "Enable biometric lock first",
                        enabled = isEnabled,
                        expanded = autoLockExpanded,
                        onExpandToggle = { if (isEnabled) autoLockExpanded = !autoLockExpanded },
                        options = autoLockOptions,
                        selectedValue = state.autoLockTimeout,
                        onOptionSelected = { value ->
                            autoLockExpanded = false
                            viewModel.requestChangeAutoLock(value)
                        },
                    )
                }

                // ════════════════════════════════════════════════
                // 3. Hide Notification Content
                // ════════════════════════════════════════════════
                item {
                    SecuritySectionHeader("Notification Privacy")
                }

                item {
                    SecurityToggleCard(
                        icon = Icons.Outlined.VisibilityOff,
                        title = "Hide notification content",
                        subtitle = "Notification previews will show a generic message",
                        checked = state.hideNotificationContent,
                        onToggle = { viewModel.toggleHideNotificationContent() },
                    )
                }

                // ════════════════════════════════════════════════
                // 4. Secure Screen
                // ════════════════════════════════════════════════
                item {
                    SecuritySectionHeader("Screen Protection")
                }

                item {
                    val currentLabel = secureScreenOptions.firstOrNull { it.first == state.secureScreen }
                        ?.second ?: "Off"

                    SecurityDropdownCard(
                        icon = Icons.Outlined.ScreenLockPortrait,
                        title = "Secure screen",
                        subtitle = currentLabel,
                        enabled = true,
                        expanded = secureScreenExpanded,
                        onExpandToggle = { secureScreenExpanded = !secureScreenExpanded },
                        options = secureScreenOptions,
                        selectedValue = state.secureScreen,
                        onOptionSelected = { value ->
                            secureScreenExpanded = false
                            viewModel.setSecureScreen(value)
                        },
                    )
                }

                // Info text below secure screen
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "When secure screen is enabled, the app content will be hidden " +
                                    "in the recent apps view and screenshots will be blocked. " +
                                    "\"Only when locked\" applies this only when biometric lock is active.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 18.sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Reusable UI Components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SecuritySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        ),
        color = BloodRed.copy(alpha = 0.8f),
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SecurityToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (checked) BloodRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = BloodRed,
                    checkedThumbColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun SecurityDisabledCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                )
            }
            Switch(checked = false, onCheckedChange = null, enabled = false)
        }
    }
}

@Composable
private fun SecurityDropdownCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onOptionSelected: (String) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onExpandToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            },
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 1.dp else 0.dp,
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f * alpha),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) BloodRed.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    )
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f * alpha),
                )
            }

            // Dropdown options
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 48.dp, end = 14.dp, bottom = 8.dp,
                    ),
                ) {
                    options.forEach { (value, label) ->
                        val isSelected = value == selectedValue
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOptionSelected(value) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BloodRed.copy(alpha = 0.08f)
                            else Color.Transparent,
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp, vertical = 10.dp,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onOptionSelected(value) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = BloodRed,
                                    ),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold
                                        else FontWeight.Normal,
                                    ),
                                    color = if (isSelected) BloodRed
                                    else MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
