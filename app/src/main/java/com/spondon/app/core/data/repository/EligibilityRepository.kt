package com.spondon.app.core.data.repository

import com.spondon.app.core.domain.model.EligibilityCriteria
import com.spondon.app.core.domain.model.UserEligibilityProfile

interface EligibilityRepository {
    suspend fun evaluateQuiz(answers: Map<String, String>): UserEligibilityProfile
    suspend fun getUserProfile(): UserEligibilityProfile?
    suspend fun saveUserProfile(profile: UserEligibilityProfile)
    suspend fun getCriteria(): List<EligibilityCriteria>
}
