package com.spondon.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.data.local.PreferencesManager
import com.spondon.app.core.data.repository.EligibilityRepository
import com.spondon.app.core.data.repository.TipsRepository
import com.spondon.app.core.domain.model.DeferralType
import com.spondon.app.core.domain.model.DonationTip
import com.spondon.app.core.domain.model.UserEligibilityProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val currentQuizStep: Int = 0,
    val totalQuizSteps: Int = 8,
    val quizAnswers: Map<String, String> = emptyMap(),
    val isEvaluating: Boolean = false,
    val eligibilityProfile: UserEligibilityProfile? = null,
    val showResult: Boolean = false,
    val previewTips: List<DonationTip> = emptyList(),
    val onboardingComplete: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val tipsRepository: TipsRepository,
    private val eligibilityRepository: EligibilityRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun answerQuizQuestion(questionId: String, answer: String) {
        _state.update { current ->
            val updatedAnswers = current.quizAnswers.toMutableMap().apply { put(questionId, answer) }
            val nextStep = (current.currentQuizStep + 1).coerceAtMost(current.totalQuizSteps)
            current.copy(
                quizAnswers = updatedAnswers,
                currentQuizStep = nextStep,
            )
        }
    }

    fun updateAnswer(questionId: String, answer: String) {
        _state.update { current ->
            val updatedAnswers = current.quizAnswers.toMutableMap().apply { put(questionId, answer) }
            current.copy(quizAnswers = updatedAnswers)
        }
    }

    fun goToNextStep() {
        _state.update { current ->
            current.copy(
                currentQuizStep = (current.currentQuizStep + 1).coerceAtMost(current.totalQuizSteps),
            )
        }
    }

    fun goBackQuizStep() {
        _state.update { current ->
            current.copy(
                currentQuizStep = (current.currentQuizStep - 1).coerceAtLeast(0),
                showResult = false,
            )
        }
    }

    fun evaluateEligibility() {
        viewModelScope.launch {
            _state.update { it.copy(isEvaluating = true) }
            val profile = eligibilityRepository.evaluateQuiz(_state.value.quizAnswers)
            _state.update {
                it.copy(
                    isEvaluating = false,
                    eligibilityProfile = profile,
                    showResult = true,
                )
            }
        }
    }

    fun loadPreviewTips() {
        viewModelScope.launch {
            val allTips = tipsRepository.getAllTips()
            // Get one tip per category for preview
            val preview = allTips
                .groupBy { it.category }
                .mapNotNull { (_, tips) -> tips.firstOrNull() }
                .take(5)
            _state.update { it.copy(previewTips = preview) }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingComplete(true)
            _state.update { it.copy(onboardingComplete = true) }
        }
    }
}
