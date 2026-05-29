package com.spondon.app.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.TimelineStep
import com.spondon.app.core.ui.i18n.LocalAppLanguage
import com.spondon.app.core.ui.theme.BloodRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(navController: NavController) {
    val language = LocalAppLanguage.current
    val isBn = language == "bn"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBn) "কিভাবে কাজ করে" else "How It Works",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isBn) "৩টি সহজ ধাপে রক্তদান করুন" else "Donate blood in 3 simple steps",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(32.dp))

            // Timeline steps
            TimelineStep(
                stepNumber = 1,
                title = if (isBn) "নিবন্ধন ও যোগ্যতা যাচাই" else "Register & Check Eligibility",
                description = if (isBn) "আপনার অ্যাকাউন্ট তৈরি করুন এবং একটি দ্রুত যোগ্যতা কুইজ দিন"
                else "Create your account and take a quick eligibility quiz",
                icon = Icons.Filled.PersonAdd,
            )

            TimelineStep(
                stepNumber = 2,
                title = if (isBn) "অ্যাপয়েন্টমেন্ট বুক করুন" else "Book Your Appointment",
                description = if (isBn) "কাছের রক্তদান কেন্দ্র খুঁজুন এবং সময় নির্ধারণ করুন"
                else "Find nearby donation centers and schedule",
                icon = Icons.Filled.CalendarMonth,
            )

            TimelineStep(
                stepNumber = 3,
                title = if (isBn) "রক্তদান করুন ও প্রভাব দেখুন" else "Donate & Track Impact",
                description = if (isBn) "জীবন বাঁচান এবং আপনার রক্তদানের ইতিহাস দেখুন"
                else "Save lives and track your donation history",
                icon = Icons.Filled.Favorite,
                isLast = true,
            )

            Spacer(Modifier.weight(1f))

            // CTA
            Button(
                onClick = { navController.navigate("onboarding_quiz") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
            ) {
                Text(
                    text = if (isBn) "আমি যোগ্য কিনা যাচাই করুন" else "Check if I'm eligible",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
