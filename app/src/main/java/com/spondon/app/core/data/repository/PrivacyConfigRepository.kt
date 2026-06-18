package com.spondon.app.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight repository to check privacy config for member-facing features.
 * When privacy mode is ON, sensitive data (availability status, last donation
 * date, contact number) should be hidden from regular members. Only authorized
 * admins and the SuperAdmin can see this data.
 */
@Singleton
class PrivacyConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    /** Returns true if privacy mode is enabled by the SuperAdmin. */
    suspend fun isPrivacyEnabled(): Boolean {
        return try {
            val doc = firestore.document("config/privacy_settings").get().await()
            doc.getBoolean("enabled") ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns true if the current user is authorized to see private data.
     * SuperAdmin or users in the authorizedAdmins list can see private data.
     */
    suspend fun isCurrentUserAuthorized(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            // Check if user is SuperAdmin
            val userDoc = firestore.collection("users").document(uid).get().await()
            val role = userDoc.getString("role") ?: "USER"
            if (role == "SUPER_ADMIN") return true

            // Check if user is in authorized admins list
            val privacyDoc = firestore.document("config/privacy_settings").get().await()
            val authorizedAdmins = (privacyDoc.data?.get("authorizedAdmins") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            uid in authorizedAdmins
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Determines if sensitive member data should be hidden for the current user.
     * Returns true if privacy is ON and the current user is NOT authorized.
     */
    suspend fun shouldHideSensitiveData(): Boolean {
        if (!isPrivacyEnabled()) return false
        return !isCurrentUserAuthorized()
    }
}
