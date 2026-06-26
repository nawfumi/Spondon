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
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
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

            Section("1. Information We Collect")
            BodyText(
                "We collect the following information when you create an account or use Spondon:\n\n" +
                        "• Personal details: name, phone number, email address, date of birth, and blood group.\n" +
                        "• Health information: donor eligibility status, donation history, and any medical " +
                        "information you voluntarily provide.\n" +
                        "• Location data: your general location to help match you with nearby blood requests.\n" +
                        "• Usage data: app interactions, features used, and crash reports."
            )

            Spacer(Modifier.height(16.dp))
            Section("2. How We Use Your Information")
            BodyText(
                "Your information is used solely to:\n\n" +
                        "• Facilitate blood donation requests and donor-recipient connections.\n" +
                        "• Verify your eligibility and maintain your donation history.\n" +
                        "• Send important notifications about blood requests and account updates.\n" +
                        "• Improve the app experience and ensure platform safety.\n\n" +
                        "We never sell your personal data to third parties."
            )

            Spacer(Modifier.height(16.dp))
            Section("3. Data Sharing")
            BodyText(
                "Your contact information is shared only with verified users who are part of a " +
                        "blood request you have responded to or created. Your health information is " +
                        "never publicly displayed. We may share data with healthcare providers if " +
                        "required for emergency purposes or by law."
            )

            Spacer(Modifier.height(16.dp))
            Section("4. Data Security")
            BodyText(
                "We implement industry-standard security measures including encryption in transit " +
                        "and at rest. Your password is hashed and never stored in plain text. " +
                        "However, no method of electronic storage is 100% secure."
            )

            Spacer(Modifier.height(16.dp))
            Section("5. Your Rights")
            BodyText(
                "You may request access to, correction of, or deletion of your personal data at any " +
                        "time. You can update most information directly in your profile settings. " +
                        "To delete your account entirely, use the option in Settings or contact us."
            )

            Spacer(Modifier.height(16.dp))
            Section("6. Data Retention")
            BodyText(
                "We retain your account information for as long as your account is active. " +
                        "After account deletion, we may retain anonymized donation statistics and " +
                        "legal records for up to 90 days as required by applicable law."
            )

            Spacer(Modifier.height(16.dp))
            Section("7. Third-Party Services")
            BodyText(
                "Spondon uses Firebase (Google) for authentication, database storage, and " +
                        "notifications. These services have their own privacy policies. " +
                        "We do not control how these third parties process your data."
            )

            Spacer(Modifier.height(16.dp))
            Section("8. Contact")
            BodyText(
                "For privacy-related inquiries, email us at privacy@spondon.app or write to " +
                        "Spondon."
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
