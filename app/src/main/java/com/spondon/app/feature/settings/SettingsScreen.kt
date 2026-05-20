package com.spondon.app.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.R
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.i18n.SpondonStrings
import com.spondon.app.core.ui.theme.*
import com.spondon.app.core.util.BiometricHelper
import com.spondon.app.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val s = S.strings
    val biometricAvailable = remember { BiometricHelper.canAuthenticate(context) }

    // Snackbar for feedback
    val snackbarHostState = remember { SnackbarHostState() }
    var showLanguageSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(showLanguageSnackbar) {
        if (showLanguageSnackbar) {
            snackbarHostState.showSnackbar(s.languageChangeRestart)
            showLanguageSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settings, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // ─── Appearance ────────────────────
                item { SectionHeader(s.appearance) }
                item {
                    SettingsToggleItem(Icons.Outlined.DarkMode, s.darkMode, s.darkModeDesc, state.isDarkMode) {
                        viewModel.toggleDarkMode()
                    }
                }

                // ─── Language ─────────────────────
                item { SectionHeader(s.language) }
                item {
                    SettingsRadioItem(Icons.Outlined.Language, s.bangla, state.language == "bn") {
                        viewModel.setLanguage("bn")
                        showLanguageSnackbar = true
                    }
                }
                item {
                    SettingsRadioItem(Icons.Outlined.Language, s.english, state.language == "en") {
                        viewModel.setLanguage("en")
                        showLanguageSnackbar = true
                    }
                }

                // ─── Notifications ────────────────
                item { SectionHeader(s.notificationSettings) }
                item {
                    SettingsToggleItem(Icons.Outlined.NotificationsActive, s.newRequests, s.bloodRequestAlerts, state.notifyNewRequests) {
                        viewModel.toggleNotifyNewRequests()
                    }
                }
                item {
                    SettingsToggleItem(Icons.Outlined.HowToReg, s.joinApprovals, s.communityJoinUpdates, state.notifyJoinApprovals) {
                        viewModel.toggleNotifyJoinApprovals()
                    }
                }
                item {
                    SettingsToggleItem(Icons.Outlined.Alarm, s.donationReminders, s.upcomingDonationAlerts, state.notifyDonationReminders) {
                        viewModel.toggleNotifyDonationReminders()
                    }
                }
                item {
                    SettingsToggleItem(Icons.Outlined.AdminPanelSettings, s.adminAlerts, s.adminActionNotifications, state.notifyAdminAlerts) {
                        viewModel.toggleNotifyAdminAlerts()
                    }
                }

                // ─── Privacy ─────────────────────
                item { SectionHeader(s.privacy) }
                item {
                    SettingsToggleItem(Icons.Outlined.Phone, s.showPhoneNumberSetting, s.visibleToMembers, state.showPhoneNumber) {
                        viewModel.togglePhoneVisibility()
                    }
                }
                item {
                    SettingsToggleItem(Icons.Outlined.Search, s.showInDonorSearch, s.appearInSearchResults, state.showInDonorSearch) {
                        viewModel.toggleDonorSearchVisibility()
                    }
                }

                // ─── Security ────────────────────
                item { SectionHeader(s.security) }
                item {
                    SettingsClickItem(Icons.Outlined.Fingerprint, s.security, s.biometricDesc, BloodRed) {
                        navController.navigate(Routes.SecuritySettings.route)
                    }
                }

                // ─── Account ─────────────────────
                item { SectionHeader(s.account) }
                item {
                    SettingsClickItem(Icons.AutoMirrored.Outlined.Logout, s.logout, s.logoutDesc, Color(0xFFFF9100)) {
                        viewModel.showLogoutDialog()
                    }
                }
                item {
                    SettingsClickItem(Icons.Outlined.DeleteForever, s.deleteAccount, s.deleteAccountDesc, UrgencyCritical) {
                        viewModel.showDeleteDialog()
                    }
                }

                // ─── Feedback ─────────────────────
                item { SectionHeader(s.sendFeedback) }
                item {
                    SettingsClickItem(Icons.Outlined.Feedback, s.sendFeedback, s.sendFeedbackDesc, Color(0xFF7C4DFF)) {
                        navController.navigate(Routes.SendFeedback.route)
                    }
                }

                // ─── Support Developer ──────────────
                item { SectionHeader(s.supportDeveloper) }
                item {
                    SupportDeveloperCard(s) {
                        navController.navigate(Routes.Support.route)
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        // ─── Dialogs ──────────────────────────
        if (state.showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideLogoutDialog() },
                title = { Text(s.logout) },
                text = { Text(s.logoutConfirm) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.logout {
                            navController.navigate(Routes.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }) {
                        Text(s.logout, color = BloodRed)
                    }
                },
                dismissButton = { TextButton(onClick = { viewModel.hideLogoutDialog() }) { Text(s.cancel) } },
            )
        }

        if (state.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteDialog() },
                title = { Text(s.deleteAccount, color = UrgencyCritical) },
                text = { Text(s.deleteAccountConfirm) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAccount {
                                navController.navigate(Routes.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        enabled = !state.isDeleting,
                    ) {
                        Text(if (state.isDeleting) s.deleting else s.delete, color = UrgencyCritical)
                    }
                },
                dismissButton = { TextButton(onClick = { viewModel.hideDeleteDialog() }) { Text(s.cancel) } },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
        color = BloodRed.copy(alpha = 0.8f),
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsToggleItem(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            Switch(
                checked = checked, onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = BloodRed, checkedThumbColor = Color.White),
            )
        }
    }
}

@Composable
private fun SettingsDisabledItem(icon: ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
            }
            Switch(checked = false, onCheckedChange = null, enabled = false)
        }
    }
}

@Composable
private fun SettingsRadioItem(icon: ImageVector, title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) BloodRed.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) BloodRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = BloodRed))
        }
    }
}

@Composable
private fun SettingsClickItem(icon: ImageVector, title: String, subtitle: String, accentColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.06f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = accentColor)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            Icon(Icons.Filled.ChevronRight, null, tint = accentColor.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun SupportDeveloperCard(s: SpondonStrings, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.supportDeveloper,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = s.supportDeveloperDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}