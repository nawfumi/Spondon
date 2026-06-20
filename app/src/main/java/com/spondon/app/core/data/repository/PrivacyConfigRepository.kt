package com.spondon.app.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight repository to check per-user privacy config.
 *
 * Privacy is now **per-user**: the SuperAdmin selects specific users (or all
 * members of a community) whose sensitive data should be hidden. Only
 * authorized admins and the SuperAdmin can see the data of those protected
 * users.
 *
 * Firestore doc: `config/privacy_settings`
 *   - `protectedUserIds`: List<String> — UIDs whose data is hidden
 *   - `authorizedAdmins`: List<String> — UIDs who can bypass privacy
 */
@Singleton
class PrivacyConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    /** Cached set of protected user IDs — refreshed each time [loadProtectedUserIds] is called. */
    private var cachedProtectedIds: Set<String> = emptySet()
    private var cachedAuthorized: Boolean? = null

    /**
     * Loads the full list of protected user IDs from Firestore.
     * Call this once per screen/ViewModel init to cache the list.
     */
    suspend fun loadProtectedUserIds(): Set<String> {
        return try {
            val doc = firestore.document("config/privacy_settings").get().await()
            val ids = (doc.data?.get("protectedUserIds") as? List<*>)
                ?.filterIsInstance<String>()?.toSet() ?: emptySet()
            cachedProtectedIds = ids
            ids
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Returns true if the given [targetUserId] is in the protected list.
     * Uses the cached list; call [loadProtectedUserIds] first.
     */
    fun isUserProtected(targetUserId: String): Boolean {
        return targetUserId in cachedProtectedIds
    }

    /**
     * Returns true if the current user is authorized to see private data.
     * SuperAdmin or users in the authorizedAdmins list can see private data.
     */
    suspend fun isCurrentUserAuthorized(): Boolean {
        cachedAuthorized?.let { return it }
        val uid = auth.currentUser?.uid ?: return false
        return try {
            // Check if user is SuperAdmin
            val userDoc = firestore.collection("users").document(uid).get().await()
            val role = userDoc.getString("role") ?: "USER"
            if (role == "SUPER_ADMIN") {
                cachedAuthorized = true
                return true
            }

            // Check if user is in authorized admins list
            val privacyDoc = firestore.document("config/privacy_settings").get().await()
            val authorizedAdmins = (privacyDoc.data?.get("authorizedAdmins") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            val result = uid in authorizedAdmins
            cachedAuthorized = result
            result
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Determines if sensitive data should be hidden for a specific [targetUserId].
     * Returns true if the target is protected AND the current user is NOT authorized.
     */
    suspend fun shouldHideSensitiveDataForUser(targetUserId: String): Boolean {
        if (!isUserProtected(targetUserId)) return false
        return !isCurrentUserAuthorized()
    }

    /**
     * Backward-compatible: returns true if there are ANY protected users
     * and the viewer is NOT authorized to view them. Used for global UI
     * elements like "Available Now" toggle.
     */
    suspend fun shouldHideSensitiveData(): Boolean {
        if (cachedProtectedIds.isEmpty()) {
            loadProtectedUserIds()
        }
        if (cachedProtectedIds.isEmpty()) return false
        return !isCurrentUserAuthorized()
    }

    /** Returns the cached set of protected user IDs. */
    fun getProtectedUserIds(): Set<String> = cachedProtectedIds
}
