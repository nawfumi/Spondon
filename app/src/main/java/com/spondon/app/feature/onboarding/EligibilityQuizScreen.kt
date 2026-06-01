package com.spondon.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.domain.model.DeferralType
import com.spondon.app.core.ui.components.EligibilityChip
import com.spondon.app.core.ui.i18n.LocalAppLanguage
import com.spondon.app.core.ui.theme.AvailableGreen
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.DeferredAmber
import com.spondon.app.core.ui.theme.EligibleGreen
import com.spondon.app.core.ui.theme.IneligibleRed

@Composable
fun EligibilityQuizScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val language = LocalAppLanguage.current
    val isBn = language == "bn"

    if (state.showResult) {
        QuizResultContent(
            profile = state.eligibilityProfile,
            language = language,
            isEvaluating = state.isEvaluating,
            onContinue = { navController.navigate("onboarding_tips_preview") },
            onBack = { viewModel.goBackQuizStep() },
        )
        return
    }

    Scaffold(
        topBar = {
            Column {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(
                            text = if (isBn) "যোগ্যতা কুইজ" else "Eligibility Quiz",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (state.currentQuizStep > 0) viewModel.goBackQuizStep()
                            else navController.popBackStack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    windowInsets = WindowInsets(0.dp),
                )
                // Progress

                LinearWavyProgressIndicator(
                    progress = { (state.currentQuizStep + 1).toFloat() / state.totalQuizSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    color = BloodRed,
                    trackColor = BloodRed.copy(alpha = 0.1f),
                )
                Text(
                    text = if (isBn) "ধাপ ${state.currentQuizStep + 1}/${state.totalQuizSteps}"
                    else "Step ${state.currentQuizStep + 1} of ${state.totalQuizSteps}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp),
                )
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = state.currentQuizStep,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "quizStep",
        ) { step ->
            when (step) {
                0 -> AgeStep(state, viewModel, isBn)
                1 -> WeightStep(state, viewModel, isBn)
                2 -> DonatedBeforeStep(state, viewModel, isBn)
                3 -> RecentIllnessStep(state, viewModel, isBn)
                4 -> MedicationsStep(state, viewModel, isBn)
                5 -> TravelStep(state, viewModel, isBn)
                6 -> PregnancyStep(state, viewModel, isBn)
                7 -> TattooPiercingStep(state, viewModel, isBn)
            }
        }
    }
}

@Composable
private fun QuestionLayout(
    question: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(32.dp))
        content()
    }
}

@Composable
private fun OptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) BloodRed.copy(alpha = 0.1f) else Color.Transparent,
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            width = if (selected) 2.dp else 1.dp,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (selected) BloodRed else MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ─── Step 1: Age ───────────────────────────────────────────
@Composable
private fun AgeStep(state: OnboardingState, viewModel: OnboardingViewModel, isBn: Boolean) {
    val selected = state.quizAnswers["age"] ?: ""
    QuestionLayout(
        question = if (isBn) "আপনার বয়স কত?" else "How old are you?",
    ) {
        OptionButton(
            text = if (isBn) "১৭-এর নিচে" else "Under 17",
            selected = selected == "under_17",
            onClick = { viewModel.answerQuizQuestion("age", "under_17") },
        )
        Spacer(Modifier.height(12.dp))
        OptionButton(
            text = if (isBn) "১৭-৬৫" else "17–65",
            selected = selected == "17_to_65",
            onClick = { viewModel.answerQuizQuestion("age", "17_to_65") },
        )
        Spacer(Modifier.height(12.dp))
        OptionButton(
            text = if (isBn) "৬৫+" else "65+",
            selected = selected == "65_plus",
            onClick = { viewModel.answerQuizQuestion("age", "65_plus") },
        )
    }
}

// ─── Step 2: Weight ────────────────────────────────────────
@Composable
private fun WeightStep(state: OnboardingState, viewModel: OnboardingViewModel, isBn: Boolean) {
    var weightText by remember { mutableStateOf(state.quizAnswers["weight_value"] ?: "") }
    val weightNum = weightText.toIntOrNull()

    QuestionLayout(
        question = if (isBn) "আপনার ওজন কত?" else "How much do you weigh?",
    ) {
        OutlinedTextField(
            value = weightText,
            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) weightText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (isBn) "ওজন (কেজি)" else "Weight (kg)") },
            suffix = { Text("kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
        )

        if (weightNum != null && weightNum < 50) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isBn) "রক্তদাতাদের ওজন কমপক্ষে ৫০ কেজি হতে হবে"
                else "Donors must weigh at least 50 kg (110 lbs)",
                style = MaterialTheme.typography.bodySmall,
                color = IneligibleRed,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val answer = if (weightNum != null && weightNum < 50) "under_50" else (weightText.ifEmpty { "0" })
                viewModel.answerQuizQuestion("weight", answer)
            },
            enabled = weightText.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
        ) {
            Text(
                text = if (isBn) "পরবর্তী" else "Next",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

// ─── Step 3: Donated Before ────────────────────────────────
@Composable
private fun DonatedBeforeStep(state: OnboardingState, viewModel: OnboardingViewModel, isBn: Boolean) {
    val selected = state.quizAnswers["donated_before"] ?: ""
    QuestionLayout(
        question = if (isBn) "আপনি কি আগে রক্তদান করেছেন?" else "Have you donated blood before?",
    ) {
        OptionButton(
            text = if (isBn) "হ্যাঁ" else "Yes",
            selected = selected == "yes",
            onClick = { viewModel.answerQuizQuestion("donated_before", "yes") },
        )
        Spacer(Modifier.height(12.dp))
        OptionButton(
            text = if (isBn) "না" else "No",
            selected = selected == "no",
            onClick = { viewModel.answerQuizQuestion("donated_before", "no") },
        )
        Spacer(Modifier.height(12.dp))
        OptionButton(
            text = if (isBn) "নিশ্চিত নই" else "Not sure",
            selected = selected == "not_sure",
            onClick = { viewModel.answerQuizQuestion("donated_before", "not_sure") },
        )
    }
}

// ─── Step 4: Recent Illness ────────────────────────────────
@Composable
private fun RecentIllnessStep(state: OnboardingState, viewModel: OnboardingViewModel, isBn: Boolean) {
    val currentAnswer = state.quizAnswers["recent_illness"] ?: ""
    var fever by remember { mutableStateOf(false) }
    var coldFlu by remember { mutableStateOf(false) }
    var infection by remember { mutableStateOf(false) }
    var none by remember { mutableStateOf(currentAnswer == "false") }

    QuestionLayout(
        question = if (isBn) "গত ২ সপ্তাহে কি কোনো অসুস্থতা হয়েছে?" else "Any recent illness in the last 2 weeks?",
    ) {
        val items = listOf(
            Triple(if (isBn) "জ্বর" else "Fever", fever) { v: Boolean -> fever = v; if (v) none = false },
            Triple(if (isBn) "সর্দি-কাশি" else "Cold or flu", coldFlu) { v: Boolean -> coldFlu = v; if (v) none = false },
            Triple(if (isBn) "সংক্রমণ" else "Infection", infection) { v: Boolean -> infection = v; if (v) none = false },
        )

        items.forEach { (label, checked, onChange) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = checked,
                    onCheckedChange = onChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = BloodRed),
                )
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(
                checked = none,
                onCheckedChange = { v ->
                    none = v
                    if (v) { fever = false; coldFlu = false; infection = false }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = AvailableGreen),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isBn) "কোনোটিই নয়" else "None of these",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val hasIllness = fever || coldFlu || infection
                viewModel.answerQuizQuestion("recent_illness", if (hasIllness) "true" else "false")
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
        ) {
            Text(if (isBn) "পরবর্তী" else "Next", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

// ─── Step 5: Medications ───────────────────────────────────
@Composable
private fun MedicationsStep(state: OnboardingState, viewModel: OnboardingViewModel, isBn: Boolean) {
    val meds = listOf(
        "aspirin" to (if (isBn) "অ্যাসপিরিন" else "Aspirin"),
        "antibiotics" to (if (isBn) "অ্যান্টিবায়োটিক" else "Antibiotics"),
        "blood_thinners" to (if (isBn) "রক্ত পাতলাকারী" else "Blood thinners"),
        "insulin" to (if (isBn) "ইনসুলিন" else "Insulin"),
        "none" to (if (isBn) "কোনোটিই নয়" else "None"),
    )
    var selected by remember { mutableStateOf(setOf<String>()) }

    QuestionLayout(
        question = if (isBn) "আপনি কি বর্তমানে কোনো ওষুধ খাচ্ছেন?" else "Are you currently on any medications?",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Use FlowRow-like wrapping via Column of Rows
        }
        meds.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (key, label) ->
                    FilterChip(
                        selected = key in selected,
                        onClick = {
                            selected = if (key == "none") {
                                setOf("none")
                            } else {
                                val new = selected.toMutableSet().apply { remove("none") }
                                if (key in new) new.remove(key) else new.add(key)
                                new
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BloodRed.copy(alpha = 0.12f),
                            selectedLabelColor = BloodRed,
                        ),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val answer = if ("none" in selected || selected.isEmpty()) "none"
                else selected.first() // Use worst deferral med
                viewModel.answerQuizQuestion("medications", answer)
            },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
        ) {
            Text(if (isBn) "পরবর্তী" else "Next", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

// ─── Step 6: Travel ────────────────────────────────────────
@Composable
private fun TravelStep(state: OnboardingState, viewModel: OnboardingViewModel, isBn: Boolean) {
    var hasTravel by remember { mutableStateOf(state.quizAnswers["recent_travel"] == "true") }

    QuestionLayout(
        question = if (isBn) "সম্প্রতি ম্যালেরিয়া/জিকা অঞ্চলে ভ্রমণ?" else "Recent travel to malaria/Zika regions?",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = hasTravel,
                    onClick = { hasTravel = true },
                    colors = RadioButtonDefaults.colors(selectedColor = BloodRed),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isBn) "হ্যাঁ" else "Yes",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = !hasTravel,
                    onClick = { hasTravel = false },
                    colors = RadioButtonDefaults.colors(selectedColor = BloodRed),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isBn) "না" else "No",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (hasTravel) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isBn) "ভ্রমণের পর ৯০ দিন অপেক্ষা করতে হতে পারে"
                else "You may need to wait 90 days after travel",
                style = MaterialTheme.typography.bodySmall,
                color = DeferredAmber,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.answerQuizQuestion("recent_travel", if (hasTravel) "true" else "false") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
        ) {
            Text(if (isBn) "পরবর্তী" else "Next", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

// ─── Step 7: Pregnancy ─────────────────────────────────────
@Composable
private fun PregnancyStep(state: OnboardingState, viewModel: OnboardingViewModel, isBn: Boolean) {
    var isPregnant by remember { mutableStateOf(state.quizAnswers["pregnancy"] == "true") }

    QuestionLayout(
        question = if (isBn) "গর্ভাবস্থা বা সাম্প্রতিক সন্তান জন্ম?" else "Pregnancy or recent birth?",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = isPregnant,
                    onClick = { isPregnant = true },
                    colors = RadioButtonDefaults.colors(selectedColor = BloodRed),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isBn) "হ্যাঁ" else "Yes",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = !isPregnant,
                    onClick = { isPregnant = false },
                    colors = RadioButtonDefaults.colors(selectedColor = BloodRed),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isBn) "না" else "No",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.answerQuizQuestion("pregnancy", if (isPregnant) "true" else "false") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
        ) {
            Text(if (isBn) "পরবর্তী" else "Next", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

// ─── Step 8: Tattoo/Piercing ───────────────────────────────
@Composable
private fun TattooPiercingStep(state: OnboardingState, viewModel: OnboardingViewModel, isBn: Boolean) {
    var hasTattoo by remember { mutableStateOf(state.quizAnswers["tattoo_piercing"] == "true") }

    QuestionLayout(
        question = if (isBn) "গত ৩ মাসে ট্যাটু বা পিয়ার্সিং?" else "Tattoo or piercing in last 3 months?",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = hasTattoo,
                    onClick = { hasTattoo = true },
                    colors = RadioButtonDefaults.colors(selectedColor = BloodRed),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isBn) "হ্যাঁ" else "Yes",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = !hasTattoo,
                    onClick = { hasTattoo = false },
                    colors = RadioButtonDefaults.colors(selectedColor = BloodRed),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isBn) "না" else "No",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.updateAnswer("tattoo_piercing", if (hasTattoo) "true" else "false")
                viewModel.evaluateEligibility()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
        ) {
            Text(
                text = if (isBn) "ফলাফল দেখুন" else "See Results",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

// ─── Quiz Result Screen ────────────────────────────────────
@Composable
private fun QuizResultContent(
    profile: com.spondon.app.core.domain.model.UserEligibilityProfile?,
    language: String,
    isEvaluating: Boolean,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val isBn = language == "bn"

    if (isEvaluating || profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BloodRed)
        }
        return
    }

    val (bgColor, iconBg, icon, headline, message) = when (profile.overallStatus) {
        DeferralType.ELIGIBLE -> ResultInfo(
            bgColor = EligibleGreen,
            iconBg = EligibleGreen,
            icon = Icons.Filled.CheckCircle,
            headline = if (isBn) "আপনি রক্তদানের যোগ্য!" else "You're eligible to donate!",
            message = if (isBn) "অভিনন্দন! আপনি রক্তদানের সকল মানদণ্ড পূরণ করেছেন।"
            else "Congratulations! You meet all the criteria for blood donation.",
        )
        DeferralType.TEMPORARY -> ResultInfo(
            bgColor = DeferredAmber,
            iconBg = DeferredAmber,
            icon = Icons.Filled.Schedule,
            headline = if (isBn) "সাময়িকভাবে স্থগিত" else "Temporarily Deferred",
            message = if (isBn) (profile.deferralReasonBn ?: "আপনি সাময়িকভাবে রক্তদান থেকে বিরত আছেন।")
            else (profile.deferralReason ?: "You're temporarily deferred from donating blood."),
        )
        DeferralType.PERMANENT -> ResultInfo(
            bgColor = IneligibleRed,
            iconBg = IneligibleRed,
            icon = Icons.Filled.Cancel,
            headline = if (isBn) "এই মুহূর্তে যোগ্য নন" else "Not eligible at this time",
            message = if (isBn) (profile.deferralReasonBn ?: "দুঃখিত, বর্তমানে আপনি রক্তদানের জন্য যোগ্য নন।")
            else (profile.deferralReason ?: "Sorry, you're currently not eligible to donate blood."),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))

        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(iconBg.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        EligibilityChip(
            status = profile.overallStatus,
            language = language,
            deferralEndDate = profile.deferralEndDate,
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
        ) {
            Text(
                text = when (profile.overallStatus) {
                    DeferralType.ELIGIBLE -> if (isBn) "চালিয়ে যান" else "Continue"
                    DeferralType.TEMPORARY -> if (isBn) "তবুও চালিয়ে যান" else "Continue anyway"
                    DeferralType.PERMANENT -> if (isBn) "চালিয়ে যান" else "Continue"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text(
                text = if (isBn) "আবার কুইজ দিন" else "Retake Quiz",
                color = BloodRed,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

private data class ResultInfo(
    val bgColor: Color,
    val iconBg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val headline: String,
    val message: String,
)
