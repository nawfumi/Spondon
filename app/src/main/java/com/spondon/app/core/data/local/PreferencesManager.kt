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
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val SAVED_EMAIL = stringPreferencesKey("saved_email")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout")
        val HIDE_NOTIFICATION_CONTENT = booleanPreferencesKey("hide_notification_content")
        val SECURE_SCREEN = stringPreferencesKey("secure_screen")
    }

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

    val autoLockTimeout: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[AUTO_LOCK_TIMEOUT] ?: "always" }

    val hideNotificationContent: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[HIDE_NOTIFICATION_CONTENT] ?: false }

    val secureScreen: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SECURE_SCREEN] ?: "off" }

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

    suspend fun setAutoLockTimeout(timeout: String) {
        dataStore.edit { it[AUTO_LOCK_TIMEOUT] = timeout }
    }

    suspend fun setHideNotificationContent(hide: Boolean) {
        dataStore.edit { it[HIDE_NOTIFICATION_CONTENT] = hide }
    }

    suspend fun setSecureScreen(mode: String) {
        dataStore.edit { it[SECURE_SCREEN] = mode }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
