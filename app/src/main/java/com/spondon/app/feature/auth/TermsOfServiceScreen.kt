package com.spondon.app.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.spondon.app.core.ui.theme.BloodRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = BloodRed,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Last Updated: June 2026",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )

            Spacer(Modifier.height(20.dp))

            Section("1. Acceptance of Terms")
            BodyText(
                "By creating an account or using the Spondon blood donation platform, you agree to these " +
                        "Terms of Service. If you do not agree, do not use the app."
            )

            Spacer(Modifier.height(16.dp))
            Section("2. Eligibility")
            BodyText(
                "You must be at least 18 years old and medically eligible to donate blood as per the " +
                        "guidelines of your local health authority. You agree to provide accurate and " +
                        "complete information during registration."
            )

            Spacer(Modifier.height(16.dp))
            Section("3. User Responsibilities")
            BodyText(
                "You are responsible for maintaining the confidentiality of your account credentials. " +
                        "You agree not to misuse the platform — including posting false blood requests, " +
                        "harassing other users, or using the app for any unlawful purpose."
            )

            Spacer(Modifier.height(16.dp))
            Section("4. Blood Donation Requests")
            BodyText(
                "Spondon facilitates connections between blood donors and those in need. We do not " +
                        "guarantee that a donor will be found for any request. All medical decisions, " +
                        "including donor eligibility and blood safety, remain the responsibility of " +
                        "licensed medical professionals."
            )

            Spacer(Modifier.height(16.dp))
            Section("5. Limitation of Liability")
            BodyText(
                "Spondon is a technology platform that connects donors with recipients. We are not " +
                        "a medical facility and are not liable for any outcomes arising from blood " +
                        "donations arranged through the app. Use the platform at your own risk."
            )

            Spacer(Modifier.height(16.dp))
            Section("6. Termination")
            BodyText(
                "We reserve the right to suspend or terminate accounts that violate these terms or " +
                        "engage in abusive behavior. You may delete your account at any time from " +
                        "the settings page."
            )

            Spacer(Modifier.height(16.dp))
            Section("7. Changes to Terms")
            BodyText(
                "We may update these terms from time to time. Continued use of the app after changes " +
                        "constitutes acceptance of the new terms. We will notify you of material changes " +
                        "via email or in-app notification."
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = "If you have questions about these terms, contact us at support@spondon.app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = BloodRed,
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
    )
}
