package com.spondon.app.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.spondon.app.core.common.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Handles notification action button taps (Accept & Donate, Approve, Reject).
 *
 * Uses [goAsync] to keep the BroadcastReceiver alive while doing coroutine work
 * within the 10-second BroadcastReceiver window.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val notifId = intent.getStringExtra("notifId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    "ACCEPT_REQUEST" -> handleAcceptRequest(intent)
                    "VIEW_REQUEST" -> { /* No-op; the tap intent handles navigation */ }
                    "APPROVE_JOIN" -> handleApproveJoin(intent)
                    "REJECT_JOIN" -> handleRejectJoin(intent)
                }
            } catch (_: Exception) {
                // Silently fail — user can always retry from the app
            } finally {
                // Dismiss the notification
                if (notifId != null) {
                    NotificationManagerCompat.from(context).cancel(notifId.hashCode())
                }
                pendingResult.finish()
            }
        }
    }

    // ── Accept blood request ────────────────────────────────────────

    private suspend fun handleAcceptRequest(intent: Intent) {
        val requestId = intent.getStringExtra("requestId")
            ?: extractFromDeepLink(intent.getStringExtra("deepLink"), "request_detail/")
            ?: return
        val donorId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection(Constants.REQUESTS_COLLECTION)
            .document(requestId)
            .update("respondents", FieldValue.arrayUnion(donorId))
            .await()
    }

    // ── Approve join request (look up by userId) ────────────────────

    private suspend fun handleApproveJoin(intent: Intent) {
        val communityId = intent.getStringExtra("communityId") ?: return
        val userId = intent.getStringExtra("requesterId") ?: return
        val firestore = FirebaseFirestore.getInstance()

        // Find the join request document by userId
        val joinReqDocs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
            .document(communityId)
            .collection(Constants.JOIN_REQUESTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "PENDING")
            .limit(1)
            .get()
            .await()

        val joinReqDoc = joinReqDocs.documents.firstOrNull() ?: return

        // Update join request status
        joinReqDoc.reference.update("status", "APPROVED").await()

        // Remove from pending
        firestore.collection(Constants.COMMUNITIES_COLLECTION)
            .document(communityId)
            .update("pendingIds", FieldValue.arrayRemove(userId))
            .await()

        // Add as member
        firestore.collection(Constants.COMMUNITIES_COLLECTION)
            .document(communityId)
            .update(
                mapOf(
                    "memberIds" to FieldValue.arrayUnion(userId),
                    "memberCount" to FieldValue.increment(1),
                )
            ).await()

        // Add community to user's communityIds
        firestore.collection(Constants.USERS_COLLECTION)
            .document(userId)
            .update("communityIds", FieldValue.arrayUnion(communityId))
            .await()
    }

    // ── Reject join request (look up by userId) ─────────────────────

    private suspend fun handleRejectJoin(intent: Intent) {
        val communityId = intent.getStringExtra("communityId") ?: return
        val userId = intent.getStringExtra("requesterId") ?: return
        val firestore = FirebaseFirestore.getInstance()

        // Find the join request document by userId
        val joinReqDocs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
            .document(communityId)
            .collection(Constants.JOIN_REQUESTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "PENDING")
            .limit(1)
            .get()
            .await()

        val joinReqDoc = joinReqDocs.documents.firstOrNull() ?: return

        // Update join request status
        joinReqDoc.reference.update("status", "REJECTED").await()

        // Remove from pending
        firestore.collection(Constants.COMMUNITIES_COLLECTION)
            .document(communityId)
            .update("pendingIds", FieldValue.arrayRemove(userId))
            .await()
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun extractFromDeepLink(deepLink: String?, prefix: String): String? {
        if (deepLink == null) return null
        return if (deepLink.startsWith(prefix)) deepLink.removePrefix(prefix) else null
    }
}
