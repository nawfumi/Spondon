package com.spondon.app.feature.superadmin.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Every destructive SA action. Stored in sa_audit collection. */
enum class SAAction {
    SA_REGISTERED,
    SA_LOGIN,
    BAN_USER,
    UNBAN_USER,
    DELETE_USER,
    BAN_COMMUNITY,
    DELETE_COMMUNITY,
    VERIFY_COMMUNITY,
    BROADCAST,
    SEND_NOTIFICATION,
    MAINTENANCE_ON,
    MAINTENANCE_OFF,
    FORCE_UPDATE_SET,
}

/** Immutable audit trail for all superadmin actions. */
@Singleton
class SAAuditLogger @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun log(
        action: SAAction,
        targetId: String? = null,
        targetName: String? = null,
        reason: String? = null,
        metadata: Map<String, Any> = emptyMap(),
    ) {
        val log = hashMapOf<String, Any?>(
            "action" to action.name,
            "targetId" to targetId,
            "targetName" to targetName,
            "reason" to reason,
            "metadata" to metadata,
            "performedBy" to FirebaseAuth.getInstance().currentUser?.uid,
            "performedAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("sa_audit").add(log).await()
    }
}
