package com.spondon.app.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spondon_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    companion object Keys {
        val INITIAL_SETUP_COMPLETE = booleanPreferencesKey("initial_setup_complete")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val SAVED_EMAIL = stringPreferencesKey("saved_email")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")

        // Eligibility
        val ELIGIBILITY_STATUS = stringPreferencesKey("eligibility_status")
        val DEFERRAL_END_DATE = longPreferencesKey("deferral_end_date")
        val DEFERRAL_REASON = stringPreferencesKey("deferral_reason")
        val DEFERRAL_REASON_BN = stringPreferencesKey("deferral_reason_bn")
        val QUIZ_ANSWERS_JSON = stringPreferencesKey("quiz_answers_json")
        val LAST_QUIZ_DATE = longPreferencesKey("last_quiz_date")
    }

    val isInitialSetupComplete: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[INITIAL_SETUP_COMPLETE] ?: false }

    val isOnboardingComplete: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ONBOARDING_COMPLETE] ?: false }

    val rememberMe: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[REMEMBER_ME] ?: false }

    val savedEmail: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SAVED_EMAIL] ?: "" }

    val isDarkMode: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DARK_MODE] ?: true }

    val language: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[LANGUAGE] ?: "bn" }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[BIOMETRIC_ENABLED] ?: false }



    suspend fun setInitialSetupComplete(complete: Boolean) {
        dataStore.edit { it[INITIAL_SETUP_COMPLETE] = complete }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setRememberMe(remember: Boolean) {
        dataStore.edit { it[REMEMBER_ME] = remember }
    }

    suspend fun setSavedEmail(email: String) {
        dataStore.edit { it[SAVED_EMAIL] = email }
    }

    suspend fun setDarkMode(dark: Boolean) {
        dataStore.edit { it[DARK_MODE] = dark }
    }

    suspend fun setLanguage(lang: String) {
        dataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    }



    // ─── Eligibility ────────────────────────────────────────
    val eligibilityStatus: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ELIGIBILITY_STATUS] ?: "" }

    val deferralEndDate: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DEFERRAL_END_DATE] ?: 0L }

    val deferralReason: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DEFERRAL_REASON] ?: "" }

    val deferralReasonBn: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DEFERRAL_REASON_BN] ?: "" }

    val quizAnswersJson: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[QUIZ_ANSWERS_JSON] ?: "" }

    val lastQuizDate: Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[LAST_QUIZ_DATE] ?: 0L }

    suspend fun setEligibilityStatus(status: String) {
        dataStore.edit { it[ELIGIBILITY_STATUS] = status }
    }

    suspend fun setDeferralEndDate(date: Long) {
        dataStore.edit { it[DEFERRAL_END_DATE] = date }
    }

    suspend fun setDeferralReason(reason: String) {
        dataStore.edit { it[DEFERRAL_REASON] = reason }
    }

    suspend fun setDeferralReasonBn(reason: String) {
        dataStore.edit { it[DEFERRAL_REASON_BN] = reason }
    }

    suspend fun setQuizAnswersJson(json: String) {
        dataStore.edit { it[QUIZ_ANSWERS_JSON] = json }
    }

    suspend fun setLastQuizDate(date: Long) {
        dataStore.edit { it[LAST_QUIZ_DATE] = date }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
