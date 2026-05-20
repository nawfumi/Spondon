package com.spondon.app.feature.settings

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.BuildConfig
import com.spondon.app.R
import com.spondon.app.core.ui.i18n.LocalAppLanguage
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.feature.update.UpdateInfo
import com.spondon.app.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    updateAvailable: UpdateInfo?,
    isCheckingUpdate: Boolean,
    isUpToDate: Boolean?,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: (String) -> Unit,
    onDismissUpdate: () -> Unit,
    onClearUpToDate: () -> Unit,
) {
    val language = LocalAppLanguage.current
    val isBn = language == "bn"

    // Dismiss the up-to-date snackbar after showing
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(isUpToDate) {
        if (isUpToDate == true) {
            snackbarHostState.showSnackbar(
                if (isBn) "অ্যাপটি সর্বশেষ সংস্করণে আছে" else "App is up to date"
            )
            onClearUpToDate()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isBn) "অ্যাপ সম্পর্কে" else "About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = if (isBn) "পেছনে" else "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ─── Hidden SA Login: tap Build 7 times ─────
            var saTapCount by remember { mutableIntStateOf(0) }
            var saToastMsg by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(saToastMsg) {
                saToastMsg?.let {
                    snackbarHostState.showSnackbar(it)
                    saToastMsg = null
                }
            }

            Spacer(Modifier.height(32.dp))

            // App Name
            Text(
                text = "স্পন্দন — Spondon",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = BloodRed,
            )
            Spacer(Modifier.height(4.dp))

            // Version
            Text(
                text = "${if (isBn) "সংস্করণ" else "Version"} ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(28.dp))

            // ─── Action Cards Row ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Check for Update
                Card(
                    onClick = {
                        if (!isCheckingUpdate && updateAvailable == null) onCheckForUpdate()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = BloodRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = BloodRed,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = if (isBn) "আপডেট চেক" else "Check Update",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Send Feedback
                Card(
                    onClick = { navController.navigate(Routes.SendFeedback.route) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.Feedback,
                            contentDescription = null,
                            tint = Color(0xFF7C4DFF).copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (isBn) "মতামত দিন" else "Feedback",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // ─── Update Available Expansion ────────────────
            AnimatedVisibility(
                visible = updateAvailable != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                updateAvailable?.let { info ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BloodRed.copy(alpha = 0.08f)
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.NewReleases,
                                    contentDescription = null,
                                    tint = BloodRed,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "নতুন সংস্করণ ${info.version} পাওয়া গেছে!"
                                        else "Version ${info.version} available!",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = BloodRed,
                                )
                            }
                            if (info.releaseNotes.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = info.releaseNotes.take(200),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                OutlinedButton(
                                    onClick = { onDismissUpdate() },
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(if (isBn) "পরে" else "Later")
                                }
                                Button(
                                    onClick = { onDownloadUpdate(info.downloadUrl) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                                ) {
                                    Icon(
                                        Icons.Filled.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isBn) "ডাউনলোড" else "Download")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── App Info Card ─────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    InfoRow(
                        icon = Icons.Outlined.Info,
                        label = if (isBn) "অ্যাপ" else "App",
                        value = "Spondon",
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                    InfoRow(
                        icon = Icons.Outlined.NewReleases,
                        label = if (isBn) "সংস্করণ" else "Version",
                        value = BuildConfig.VERSION_NAME,
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                saTapCount++
                                when {
                                    saTapCount >= 7 -> {
                                        saTapCount = 0
                                        navController.navigate("sa_login")
                                    }
                                    saTapCount >= 4 -> {
                                        saToastMsg = "${7 - saTapCount} taps remaining"
                                    }
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            tint = BloodRed.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isBn) "বিল্ড" else "Build",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${BuildConfig.VERSION_CODE}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── Description ──────────────────────────────
            Text(
                text = if (isBn)
                    "স্পন্দন একটি ফ্রি ও ওপেন-সোর্স রক্তদান অ্যাপ যা বাংলাদেশে রক্তদাতা ও প্রার্থীদের দ্রুত সংযুক্ত করতে তৈরি।"
                else
                    "Spondon is a free and open-source blood donation app built to quickly connect blood donors with recipients in Bangladesh.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(32.dp))

            // Footer
            Text(
                text = "Made with ❤ by Ash",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = BloodRed.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
