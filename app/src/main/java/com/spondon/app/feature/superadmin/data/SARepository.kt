package com.spondon.app.feature.superadmin.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.spondon.app.core.common.Resource
import com.spondon.app.feature.superadmin.broadcast.*
import com.spondon.app.feature.superadmin.community.*
import com.spondon.app.feature.superadmin.feedback.*
import com.spondon.app.feature.superadmin.forceupdate.*
import com.spondon.app.feature.superadmin.maintenance.*
import com.spondon.app.feature.superadmin.users.*
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central data access object for all SuperAdmin Firestore operations.
 * Handles registration, login verification, ban actions, and config reads.
 */
@Singleton
class SARepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val auditLogger: SAAuditLogger,
) {
    // ════════════════════════════════════════════════════════════
    // Registration & Identity
    // ════════════════════════════════════════════════════════════

    /** Check if a SuperAdmin has already been registered. */
    suspend fun isSuperAdminRegistered(): Boolean {
        return try {
            val doc = firestore.document("config/superadmin").get().await()
            doc.exists() && doc.getBoolean("registered") == true
        } catch (_: Exception) {
            false
        }
    }

    /** Register the very first SuperAdmin. Self-destructs after first use. */
    suspend fun registerSuperAdmin(
        email: String,
        password: String,
        passphrase: String,
    ): Resource<Unit> {
        return try {
            // Verify no SA already registered
            if (isSuperAdminRegistered()) {
                return Resource.Error("SuperAdmin already registered")
            }

            // Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return Resource.Error("Auth failed")

            // Hash the passphrase (SHA-256 for simplicity — production should use bcrypt)
            val passphraseHash = hashPassphrase(passphrase)

            // Write SA config to Firestore
            val saConfig = hashMapOf(
                "registered" to true,
                "uid" to uid,
                "email" to email,
                "passphraseHash" to passphraseHash,
                "registeredAt" to FieldValue.serverTimestamp(),
                "lastLoginAt" to FieldValue.serverTimestamp(),
                "additionalSAEnabled" to false,
            )
            firestore.document("config/superadmin").set(saConfig).await()

            // Also create a user document with SUPER_ADMIN role
            val userDoc = hashMapOf(
                "uid" to uid,
                "email" to email,
                "name" to "SuperAdmin",
                "role" to "SUPER_ADMIN",
                "createdAt" to FieldValue.serverTimestamp(),
                "isDonor" to false,
                "isBanned" to false,
            )
            firestore.collection("users").document(uid).set(userDoc).await()

            auditLogger.log(SAAction.SA_REGISTERED, targetId = uid)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Registration failed", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Login Verification
    // ════════════════════════════════════════════════════════════

    /** Verify the passphrase after email/password login succeeds. */
    suspend fun verifyPassphrase(passphrase: String): Resource<Unit> {
        return try {
            val doc = firestore.document("config/superadmin").get().await()
            val storedHash = doc.getString("passphraseHash") ?: return Resource.Error("No passphrase stored")
            val inputHash = hashPassphrase(passphrase)

            if (storedHash == inputHash) {
                // Update last login
                firestore.document("config/superadmin")
                    .update("lastLoginAt", FieldValue.serverTimestamp())
                    .await()
                auditLogger.log(SAAction.SA_LOGIN)
                Resource.Success(Unit)
            } else {
                Resource.Error("Invalid passphrase")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Verification failed", e)
        }
    }

    /** Check if the currently logged-in user is the SuperAdmin. */
    suspend fun isCurrentUserSuperAdmin(): Boolean {
        return try {
            val uid = auth.currentUser?.uid ?: return false
            val doc = firestore.document("config/superadmin").get().await()
            doc.getString("uid") == uid
        } catch (_: Exception) {
            false
        }
    }

    // ════════════════════════════════════════════════════════════
    // Ban Management
    // ════════════════════════════════════════════════════════════

    suspend fun banUser(uid: String, reason: String): Resource<Unit> {
        return try {
            firestore.collection("users").document(uid)
                .update(
                    mapOf(
                        "isBanned" to true,
                        "banReason" to reason,
                        "bannedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            auditLogger.log(SAAction.BAN_USER, targetId = uid, reason = reason)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ban failed", e)
        }
    }

    suspend fun unbanUser(uid: String): Resource<Unit> {
        return try {
            firestore.collection("users").document(uid)
                .update(
                    mapOf(
                        "isBanned" to false,
                        "banReason" to null,
                    ),
                ).await()
            auditLogger.log(SAAction.UNBAN_USER, targetId = uid)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unban failed", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Platform Stats (Dashboard)
    // ════════════════════════════════════════════════════════════

    suspend fun getTotalUserCount(): Int {
        return try {
            val snapshot = firestore.collection("users").get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getTotalCommunityCount(): Int {
        return try {
            val snapshot = firestore.collection("communities").get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getActiveRequestCount(): Int {
        return try {
            val snapshot = firestore.collection("requests")
                .whereEqualTo("status", "ACTIVE")
                .get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getTotalDonationCount(): Int {
        return try {
            val snapshot = firestore.collection("requests")
                .whereEqualTo("status", "FULFILLED")
                .get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getBannedUserCount(): Int {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("isBanned", true)
                .get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getPendingFeedbackCount(): Int {
        return try {
            val snapshot = firestore.collection("feedback")
                .whereEqualTo("status", "UNREAD")
                .get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getNewUsersTodayCount(): Int {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val snapshot = firestore.collection("users")
                .whereGreaterThanOrEqualTo("createdAt", Timestamp(today))
                .get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getMaintenanceStatus(): Boolean {
        return try {
            val doc = firestore.document("config/maintenance").get().await()
            doc.getBoolean("isEnabled") ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getForceUpdateVersion(): String? {
        return try {
            val doc = firestore.document("config/app_version").get().await()
            doc.getString("latestVersionName")
        } catch (_: Exception) {
            null
        }
    }

    // ════════════════════════════════════════════════════════════
    // User Management (Phase 2)
    // ════════════════════════════════════════════════════════════

    /** Fetch all users from Firestore. */
    suspend fun getAllUsers(): Resource<List<SAUserItem>> {
        return try {
            val snapshot = firestore.collection("users").get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                mapToSAUserItem(doc.id, data)
            }
            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load users", e)
        }
    }

    /** Fetch full detail for one user: profile + communities + requests. */
    suspend fun getUserDetail(uid: String): Resource<SAUserDetail> {
        return try {
            // User profile
            val userDoc = firestore.collection("users").document(uid).get().await()
            val userData = userDoc.data ?: return Resource.Error("User not found")
            val user = mapToSAUserItem(uid, userData)

            // Communities the user belongs to
            val communities = mutableListOf<SAUserCommunity>()
            if (user.communityIds.isNotEmpty()) {
                for (cid in user.communityIds) {
                    try {
                        val cDoc = firestore.collection("communities").document(cid).get().await()
                        val cData = cDoc.data ?: continue
                        communities.add(
                            SAUserCommunity(
                                id = cid,
                                name = cData["name"] as? String ?: "",
                                memberCount = ((cData["memberIds"] as? List<*>)?.size ?: 0),
                            ),
                        )
                    } catch (_: Exception) { /* skip */ }
                }
            }

            // Requests by this user
            val reqSnapshot = firestore.collection("requests")
                .whereEqualTo("requesterId", uid)
                .get().await()
            val requests = reqSnapshot.documents.mapNotNull { rDoc ->
                val rData = rDoc.data ?: return@mapNotNull null
                val createdAt = when (val d = rData["createdAt"]) {
                    is Timestamp -> d.toDate()
                    is Date -> d
                    else -> null
                }
                SAUserRequest(
                    id = rDoc.id,
                    bloodGroup = rData["bloodGroup"] as? String ?: "",
                    urgency = rData["urgency"] as? String ?: "NORMAL",
                    hospital = rData["hospital"] as? String ?: "",
                    status = rData["status"] as? String ?: "ACTIVE",
                    createdAt = createdAt,
                )
            }

            Resource.Success(
                SAUserDetail(user = user, communities = communities, requests = requests),
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load user detail", e)
        }
    }

    /** Delete a user — removes Firestore doc, their requests, and logs audit. */
    suspend fun deleteUser(uid: String): Resource<Unit> {
        return try {
            val userName = try {
                val doc = firestore.collection("users").document(uid).get().await()
                doc.getString("name") ?: "Unknown"
            } catch (_: Exception) { "Unknown" }

            // Delete user document
            firestore.collection("users").document(uid).delete().await()

            // Delete their requests
            val reqSnapshot = firestore.collection("requests")
                .whereEqualTo("requesterId", uid)
                .get().await()
            for (doc in reqSnapshot.documents) {
                doc.reference.delete().await()
            }

            // Remove from communities
            val communitySnapshot = firestore.collection("communities").get().await()
            for (doc in communitySnapshot.documents) {
                val memberIds = (doc.data?.get("memberIds") as? List<*>)
                    ?.filterIsInstance<String>() ?: continue
                if (uid in memberIds) {
                    doc.reference.update("memberIds", FieldValue.arrayRemove(uid)).await()
                }
                val adminIds = (doc.data?.get("adminIds") as? List<*>)
                    ?.filterIsInstance<String>() ?: continue
                if (uid in adminIds) {
                    doc.reference.update("adminIds", FieldValue.arrayRemove(uid)).await()
                }
            }

            auditLogger.log(SAAction.DELETE_USER, targetId = uid, targetName = userName)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed", e)
        }
    }

    /** Send a push notification to a specific user via the top-level notifications collection. */
    suspend fun sendNotificationToUser(
        uid: String,
        title: String,
        body: String,
        type: String = "ADMIN",
    ): Resource<Unit> {
        return try {
            val notification = hashMapOf(
                "userId" to uid,
                "title" to title,
                "body" to body,
                "type" to type,
                "deepLink" to "",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            firestore.collection("notifications").add(notification).await()

            auditLogger.log(
                SAAction.SEND_NOTIFICATION,
                targetId = uid,
                metadata = mapOf("title" to title, "type" to type),
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send notification", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Community Management (Phase 3)
    // ════════════════════════════════════════════════════════════

    /** Fetch all communities from Firestore. */
    suspend fun getAllCommunities(): Resource<List<SACommunityItem>> {
        return try {
            val snapshot = firestore.collection("communities").get().await()
            val communities = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                mapToSACommunityItem(doc.id, data)
            }
            Resource.Success(communities)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load communities", e)
        }
    }

    /** Fetch full detail for one community: info + members + requests. */
    suspend fun getCommunityDetail(communityId: String): Resource<SACommunityDetail> {
        return try {
            val cDoc = firestore.collection("communities").document(communityId).get().await()
            val cData = cDoc.data ?: return Resource.Error("Community not found")
            val community = mapToSACommunityItem(communityId, cData)

            // Fetch members
            val memberIds = (cData["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val adminIds = (cData["adminIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val members = mutableListOf<SACommunityMember>()
            for (mid in memberIds) {
                try {
                    val uDoc = firestore.collection("users").document(mid).get().await()
                    val uData = uDoc.data ?: continue
                    members.add(
                        SACommunityMember(
                            uid = mid,
                            name = uData["name"] as? String ?: "",
                            bloodGroup = uData["bloodGroup"] as? String ?: "",
                            isAdmin = mid in adminIds,
                            avatarUrl = uData["avatarUrl"] as? String ?: "",
                        ),
                    )
                } catch (_: Exception) { /* skip */ }
            }

            // Fetch requests in this community
            val reqSnapshot = firestore.collection("requests")
                .whereEqualTo("communityId", communityId)
                .get().await()
            val requests = reqSnapshot.documents.mapNotNull { rDoc ->
                val rData = rDoc.data ?: return@mapNotNull null
                val createdAt = when (val d = rData["createdAt"]) {
                    is Timestamp -> d.toDate()
                    is Date -> d
                    else -> null
                }
                SACommunityRequest(
                    id = rDoc.id,
                    bloodGroup = rData["bloodGroup"] as? String ?: "",
                    urgency = rData["urgency"] as? String ?: "NORMAL",
                    hospital = rData["hospital"] as? String ?: "",
                    status = rData["status"] as? String ?: "ACTIVE",
                    requesterName = rData["requesterName"] as? String ?: "",
                    createdAt = createdAt,
                )
            }

            Resource.Success(
                SACommunityDetail(community = community, members = members, requests = requests),
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load community detail", e)
        }
    }

    /** Verify a community. */
    suspend fun verifyCommunity(communityId: String): Resource<Unit> {
        return try {
            firestore.collection("communities").document(communityId)
                .update("status", "VERIFIED").await()
            auditLogger.log(SAAction.VERIFY_COMMUNITY, targetId = communityId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Verification failed", e)
        }
    }

    /** Suspend a community. */
    suspend fun suspendCommunity(communityId: String): Resource<Unit> {
        return try {
            firestore.collection("communities").document(communityId)
                .update("status", "SUSPENDED").await()
            auditLogger.log(SAAction.BAN_COMMUNITY, targetId = communityId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Suspension failed", e)
        }
    }

    /** Reactivate a suspended community. */
    suspend fun unsuspendCommunity(communityId: String): Resource<Unit> {
        return try {
            firestore.collection("communities").document(communityId)
                .update("status", "ACTIVE").await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Reactivation failed", e)
        }
    }

    /** Delete a community — removes doc and cleans up user refs. */
    suspend fun deleteCommunity(communityId: String): Resource<Unit> {
        return try {
            val communityName = try {
                val doc = firestore.collection("communities").document(communityId).get().await()
                doc.getString("name") ?: "Unknown"
            } catch (_: Exception) { "Unknown" }

            // Remove community from all users' communityIds
            val usersSnapshot = firestore.collection("users")
                .whereArrayContains("communityIds", communityId)
                .get().await()
            for (doc in usersSnapshot.documents) {
                doc.reference.update("communityIds", FieldValue.arrayRemove(communityId)).await()
            }

            // Delete requests in this community
            val reqSnapshot = firestore.collection("requests")
                .whereEqualTo("communityId", communityId)
                .get().await()
            for (doc in reqSnapshot.documents) {
                doc.reference.delete().await()
            }

            // Delete the community itself
            firestore.collection("communities").document(communityId).delete().await()

            auditLogger.log(SAAction.DELETE_COMMUNITY, targetId = communityId, targetName = communityName)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed", e)
        }
    }

    /** Force-remove a member from a community (override community admin). */
    suspend fun forceRemoveMember(communityId: String, uid: String): Resource<Unit> {
        return try {
            val cRef = firestore.collection("communities").document(communityId)
            cRef.update("memberIds", FieldValue.arrayRemove(uid)).await()
            cRef.update("adminIds", FieldValue.arrayRemove(uid)).await()

            // Also remove from user's communityIds
            firestore.collection("users").document(uid)
                .update("communityIds", FieldValue.arrayRemove(communityId)).await()

            auditLogger.log(
                SAAction.DELETE_USER, // reuse for member removal
                targetId = uid,
                metadata = mapOf("communityId" to communityId, "action" to "FORCE_REMOVE_MEMBER"),
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Remove failed", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Broadcast (Phase 3)
    // ════════════════════════════════════════════════════════════

    /** Send a broadcast notification via Firestore (to be picked up by Cloud Function). */
    suspend fun sendBroadcast(
        title: String,
        body: String,
        type: String,
        target: String,
        targetValue: String = "",
    ): Resource<Unit> {
        return try {
            val broadcast = hashMapOf(
                "title" to title,
                "body" to body,
                "type" to type,
                "target" to target,
                "targetValue" to targetValue,
                "sentBy" to (auth.currentUser?.uid ?: ""),
                "sentAt" to FieldValue.serverTimestamp(),
                "status" to "SENT",
            )
            firestore.collection("broadcasts").add(broadcast).await()
            auditLogger.log(
                SAAction.BROADCAST,
                metadata = mapOf("title" to title, "target" to target, "targetValue" to targetValue, "type" to type),
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Broadcast failed", e)
        }
    }

    /** Fetch broadcast history. */
    suspend fun getBroadcastHistory(): Resource<List<SABroadcastItem>> {
        return try {
            val snapshot = firestore.collection("broadcasts")
                .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val sentAt = when (val d = data["sentAt"]) {
                    is Timestamp -> d.toDate()
                    is Date -> d
                    else -> null
                }
                val rawTarget = data["target"] as? String ?: ""
                val targetValue = data["targetValue"] as? String ?: ""
                SABroadcastItem(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    body = data["body"] as? String ?: "",
                    type = data["type"] as? String ?: "",
                    target = if (targetValue.isNotEmpty()) "$rawTarget → $targetValue" else rawTarget,
                    sentAt = sentAt,
                    status = data["status"] as? String ?: "SENT",
                )
            }
            Resource.Success(items)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load history", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Feedback (Phase 4)
    // ════════════════════════════════════════════════════════════

    /** Fetch all user feedback from Firestore. */
    suspend fun getAllFeedback(): Resource<List<SAFeedbackItem>> {
        return try {
            val snapshot = firestore.collection("feedback")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val createdAt = when (val d = data["createdAt"]) {
                    is Timestamp -> d.toDate()
                    is Date -> d
                    else -> null
                }
                SAFeedbackItem(
                    id = doc.id,
                    userId = data["userId"] as? String ?: "",
                    userName = data["userName"] as? String ?: "",
                    type = data["type"] as? String ?: "OTHER",
                    body = data["body"] as? String ?: "",
                    screenshotUrl = data["screenshotUrl"] as? String,
                    appVersion = data["appVersion"] as? String ?: "",
                    deviceModel = data["deviceModel"] as? String ?: "",
                    osVersion = data["osVersion"] as? String ?: "",
                    status = data["status"] as? String ?: "UNREAD",
                    createdAt = createdAt,
                )
            }
            Resource.Success(items)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load feedback", e)
        }
    }

    /** Update feedback status (READ, RESOLVED, SPAM). */
    suspend fun updateFeedbackStatus(feedbackId: String, status: String): Resource<Unit> {
        return try {
            firestore.collection("feedback").document(feedbackId)
                .update("status", status).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Maintenance Mode (Phase 4)
    // ════════════════════════════════════════════════════════════

    /** Fetch the full maintenance config. */
    suspend fun getMaintenanceConfig(): Resource<MaintenanceConfig> {
        return try {
            val doc = firestore.document("config/maintenance").get().await()
            val data = doc.data
            if (data == null) {
                Resource.Success(MaintenanceConfig())
            } else {
                val enabledAt = when (val d = data["enabledAt"]) {
                    is Timestamp -> d.toDate()
                    is Date -> d
                    else -> null
                }
                val estimatedEnd = when (val d = data["estimatedEnd"]) {
                    is Timestamp -> d.toDate()
                    is Date -> d
                    else -> null
                }
                Resource.Success(
                    MaintenanceConfig(
                        isEnabled = data["isEnabled"] as? Boolean ?: false,
                        title = data["title"] as? String ?: "",
                        message = data["message"] as? String ?: "",
                        estimatedEnd = estimatedEnd,
                        enabledAt = enabledAt,
                        enabledBy = data["enabledBy"] as? String ?: "",
                    ),
                )
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load config", e)
        }
    }

    /** Toggle maintenance mode on/off. */
    suspend fun setMaintenanceMode(
        enabled: Boolean,
        title: String,
        message: String,
        estimatedMinutes: Int?,
    ): Resource<Unit> {
        return try {
            val config = hashMapOf<String, Any?>(
                "isEnabled" to enabled,
                "title" to title,
                "message" to message,
                "enabledAt" to if (enabled) FieldValue.serverTimestamp() else null,
                "enabledBy" to if (enabled) (auth.currentUser?.uid ?: "") else "",
            )
            if (estimatedMinutes != null && enabled) {
                val estimatedEnd = Date(System.currentTimeMillis() + estimatedMinutes * 60 * 1000L)
                config["estimatedEnd"] = Timestamp(estimatedEnd)
            } else {
                config["estimatedEnd"] = null
            }

            firestore.document("config/maintenance").set(config).await()

            auditLogger.log(
                if (enabled) SAAction.MAINTENANCE_ON else SAAction.MAINTENANCE_OFF,
                metadata = mapOf("title" to title),
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to set maintenance mode", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Force Update (Phase 4)
    // ════════════════════════════════════════════════════════════

    /** Fetch the full force update config. */
    suspend fun getForceUpdateConfig(): Resource<ForceUpdateConfig> {
        return try {
            val doc = firestore.document("config/app_version").get().await()
            val data = doc.data
            if (data == null) {
                Resource.Success(ForceUpdateConfig())
            } else {
                Resource.Success(
                    ForceUpdateConfig(
                        minimumVersionCode = (data["minimumVersionCode"] as? Number)?.toInt() ?: 0,
                        latestVersionCode = (data["latestVersionCode"] as? Number)?.toInt() ?: 0,
                        latestVersionName = data["latestVersionName"] as? String ?: "",
                        playStoreUrl = data["playStoreUrl"] as? String ?: "",
                        releaseNotes = data["releaseNotes"] as? String ?: "",
                        isForceUpdate = data["isForceUpdate"] as? Boolean ?: false,
                    ),
                )
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load config", e)
        }
    }

    /** Save force update config. */
    suspend fun setForceUpdateConfig(
        minimumVersionCode: Int,
        latestVersionCode: Int,
        latestVersionName: String,
        playStoreUrl: String,
        releaseNotes: String,
        isForceUpdate: Boolean,
    ): Resource<Unit> {
        return try {
            val config = hashMapOf(
                "minimumVersionCode" to minimumVersionCode,
                "latestVersionCode" to latestVersionCode,
                "latestVersionName" to latestVersionName,
                "playStoreUrl" to playStoreUrl,
                "releaseNotes" to releaseNotes,
                "isForceUpdate" to isForceUpdate,
            )
            firestore.document("config/app_version").set(config).await()

            auditLogger.log(
                SAAction.FORCE_UPDATE_SET,
                metadata = mapOf(
                    "minCode" to minimumVersionCode,
                    "latestCode" to latestVersionCode,
                    "versionName" to latestVersionName,
                    "isForceUpdate" to isForceUpdate,
                ),
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save config", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Analytics (Phase 5)
    // ════════════════════════════════════════════════════════════

    /** Get new user signups grouped by day for the last N days. Returns map of "MMM dd" → count. */
    suspend fun getSignupsPerDay(days: Int = 7): Map<String, Int> {
        return try {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -days)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startDate = cal.time
            val snapshot = firestore.collection("users")
                .whereGreaterThanOrEqualTo("createdAt", Timestamp(startDate))
                .get().await()

            val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            val result = linkedMapOf<String, Int>()

            // Pre-fill all days with 0
            val dayCal = Calendar.getInstance()
            for (i in (days - 1) downTo 0) {
                dayCal.timeInMillis = System.currentTimeMillis()
                dayCal.add(Calendar.DAY_OF_YEAR, -i)
                result[dateFormat.format(dayCal.time)] = 0
            }

            for (doc in snapshot.documents) {
                val createdAt = when (val d = doc.data?.get("createdAt")) {
                    is Timestamp -> d.toDate()
                    is Date -> d
                    else -> continue
                }
                val key = dateFormat.format(createdAt)
                result[key] = (result[key] ?: 0) + 1
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** Get requests count grouped by urgency (NORMAL, URGENT, CRITICAL). */
    suspend fun getRequestsByUrgency(): Map<String, Int> {
        return try {
            val snapshot = firestore.collection("requests").get().await()
            val result = mutableMapOf("NORMAL" to 0, "URGENT" to 0, "CRITICAL" to 0)
            for (doc in snapshot.documents) {
                val urgency = doc.getString("urgency") ?: "NORMAL"
                result[urgency] = (result[urgency] ?: 0) + 1
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** Get requests count grouped by blood group. */
    suspend fun getRequestsByBloodGroup(): Map<String, Int> {
        return try {
            val snapshot = firestore.collection("requests").get().await()
            val result = mutableMapOf<String, Int>()
            for (doc in snapshot.documents) {
                val bg = doc.getString("bloodGroup") ?: "Unknown"
                result[bg] = (result[bg] ?: 0) + 1
            }
            result.toSortedMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** Get donation fulfillment rate: fulfilled / total requests as a percentage. */
    suspend fun getDonationFulfillmentRate(): Pair<Int, Int> {
        return try {
            val snapshot = firestore.collection("requests").get().await()
            val total = snapshot.size()
            val fulfilled = snapshot.documents.count {
                it.getString("status") == "FULFILLED"
            }
            Pair(fulfilled, total)
        } catch (_: Exception) {
            Pair(0, 0)
        }
    }

    /** Get top districts by activity (requests + users). Returns list of (district, count). */
    suspend fun getTopDistricts(limit: Int = 5): List<Pair<String, Int>> {
        return try {
            val userSnapshot = firestore.collection("users").get().await()
            val districtCounts = mutableMapOf<String, Int>()
            for (doc in userSnapshot.documents) {
                val district = doc.getString("district")
                if (!district.isNullOrBlank()) {
                    districtCounts[district] = (districtCounts[district] ?: 0) + 1
                }
            }
            districtCounts.entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key to it.value }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Get top donors this month by totalDonations. */
    suspend fun getTopDonorsThisMonth(limit: Int = 5): List<Pair<String, Int>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereGreaterThan("totalDonations", 0)
                .get().await()
            snapshot.documents
                .mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val donations = (doc.get("totalDonations") as? Number)?.toInt() ?: 0
                    name to donations
                }
                .sortedByDescending { it.second }
                .take(limit)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Get CRITICAL requests that are still ACTIVE and older than 12 hours. */
    suspend fun getCriticalUnfulfilledRequests(): List<Map<String, String>> {
        return try {
            val cutoff = Date(System.currentTimeMillis() - 12 * 60 * 60 * 1000L)
            val snapshot = firestore.collection("requests")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("urgency", "CRITICAL")
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                val createdAt = when (val d = doc.data?.get("createdAt")) {
                    is Timestamp -> d.toDate()
                    is Date -> d
                    else -> null
                }
                if (createdAt != null && createdAt.before(cutoff)) {
                    mapOf(
                        "id" to doc.id,
                        "bloodGroup" to (doc.getString("bloodGroup") ?: ""),
                        "hospital" to (doc.getString("hospital") ?: ""),
                        "requesterName" to (doc.getString("requesterName") ?: ""),
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Get community growth stats: total, avg members, largest. */
    suspend fun getCommunityGrowthStats(): Triple<Int, Double, String> {
        return try {
            val snapshot = firestore.collection("communities").get().await()
            val total = snapshot.size()
            if (total == 0) return Triple(0, 0.0, "—")
            var totalMembers = 0
            var largest = ""
            var largestCount = 0
            for (doc in snapshot.documents) {
                val members = (doc.data?.get("memberIds") as? List<*>)?.size ?: 0
                totalMembers += members
                if (members > largestCount) {
                    largestCount = members
                    largest = doc.getString("name") ?: "Unknown"
                }
            }
            Triple(total, totalMembers.toDouble() / total, "$largest ($largestCount)")
        } catch (_: Exception) {
            Triple(0, 0.0, "—")
        }
    }

    // ════════════════════════════════════════════════════════════
    // Spondon Community Management
    // ════════════════════════════════════════════════════════════

    /** Find the Spondon community document (isSpondon == true). */
    suspend fun getSpondonCommunityId(): String? {
        return try {
            val snapshot = firestore.collection("communities")
                .whereEqualTo("isSpondon", true)
                .limit(1)
                .get().await()
            snapshot.documents.firstOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    /** Fetch all posts for the Spondon community, newest first. */
    suspend fun getSpondonPosts(communityId: String): Resource<List<SASpondonPost>> {
        return try {
            val snapshot = firestore.collection("communityPosts")
                .whereEqualTo("communityId", communityId)
                .get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val createdAt = when (val d = data["createdAt"]) {
                    is Timestamp -> d.toDate()
                    is Date -> d
                    else -> null
                }
                SASpondonPost(
                    id = doc.id,
                    authorId = data["authorId"] as? String ?: "",
                    authorName = data["authorName"] as? String ?: "",
                    authorAvatarUrl = data["authorAvatarUrl"] as? String ?: "",
                    content = data["content"] as? String ?: "",
                    imageUrl = data["imageUrl"] as? String,
                    createdAt = createdAt,
                )
            }.sortedByDescending { it.createdAt }
            Resource.Success(posts)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load posts", e)
        }
    }

    /** Create a post as super admin with optional image upload. */
    suspend fun createSpondonPost(
        communityId: String,
        content: String,
        imageUri: android.net.Uri?,
        storageService: com.spondon.app.core.data.remote.StorageService,
    ): Resource<Unit> {
        return try {
            // Upload image if provided
            var imageUrl: String? = null
            if (imageUri != null) {
                val tempId = System.currentTimeMillis().toString()
                when (val uploadResult = storageService.uploadPostImage(tempId, imageUri)) {
                    is Resource.Success -> imageUrl = uploadResult.data
                    is Resource.Error -> return Resource.Error("Image upload failed: ${uploadResult.message}")
                    is Resource.Loading -> {}
                }
            }

            val data = hashMapOf<String, Any?>(
                "communityId" to communityId,
                "authorId" to (auth.currentUser?.uid ?: ""),
                "authorName" to "SuperAdmin",
                "authorAvatarUrl" to "",
                "content" to content,
                "imageUrl" to imageUrl,
                "createdAt" to Timestamp.now(),
            )
            firestore.collection("communityPosts").add(data).await()
            auditLogger.log(SAAction.BROADCAST, metadata = mapOf("action" to "SPONDON_POST", "content" to content.take(50)))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create post", e)
        }
    }

    /** Delete a Spondon community post. */
    suspend fun deleteSpondonPost(postId: String): Resource<Unit> {
        return try {
            firestore.collection("communityPosts").document(postId).delete().await()
            auditLogger.log(SAAction.BROADCAST, metadata = mapOf("action" to "SPONDON_POST_DELETE", "postId" to postId))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete post", e)
        }
    }

    /** Fetch all members of the Spondon community with role info. */
    suspend fun getSpondonMembers(communityId: String): Resource<List<SASpondonMember>> {
        return try {
            val cDoc = firestore.collection("communities").document(communityId).get().await()
            val cData = cDoc.data ?: return Resource.Error("Community not found")
            val memberIds = (cData["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val adminIds = (cData["adminIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val moderatorIds = (cData["moderatorIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            val members = mutableListOf<SASpondonMember>()
            for (mid in memberIds) {
                try {
                    val uDoc = firestore.collection("users").document(mid).get().await()
                    val uData = uDoc.data ?: continue
                    val role = when {
                        mid in adminIds -> "ADMIN"
                        mid in moderatorIds -> "MODERATOR"
                        else -> "MEMBER"
                    }
                    members.add(
                        SASpondonMember(
                            uid = mid,
                            name = uData["name"] as? String ?: "",
                            bloodGroup = uData["bloodGroup"] as? String ?: "",
                            avatarUrl = uData["avatarUrl"] as? String ?: "",
                            role = role,
                        ),
                    )
                } catch (_: Exception) { /* skip */ }
            }
            // Sort: admins first, then moderators, then members
            val sorted = members.sortedBy {
                when (it.role) {
                    "ADMIN" -> 0
                    "MODERATOR" -> 1
                    else -> 2
                }
            }
            Resource.Success(sorted)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load members", e)
        }
    }

    /** Promote a member to moderator (sub-admin) in the Spondon community. */
    suspend fun promoteSpondonMember(communityId: String, uid: String): Resource<Unit> {
        return try {
            firestore.collection("communities").document(communityId)
                .update("moderatorIds", FieldValue.arrayUnion(uid)).await()
            auditLogger.log(SAAction.BROADCAST, metadata = mapOf("action" to "SPONDON_PROMOTE", "uid" to uid))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to promote member", e)
        }
    }

    /** Demote a moderator back to regular member in the Spondon community. */
    suspend fun demoteSpondonMember(communityId: String, uid: String): Resource<Unit> {
        return try {
            firestore.collection("communities").document(communityId)
                .update(
                    mapOf(
                        "adminIds" to FieldValue.arrayRemove(uid),
                        "moderatorIds" to FieldValue.arrayRemove(uid),
                    )
                ).await()
            auditLogger.log(SAAction.BROADCAST, metadata = mapOf("action" to "SPONDON_DEMOTE", "uid" to uid))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to demote member", e)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Serial Toggle
    // ═══════════════════════════════════════════════════════════

    /** Enable or disable serial IDs for a community. */
    suspend fun enableSerialForCommunity(communityId: String, enabled: Boolean): Resource<Unit> {
        return try {
            firestore.collection("communities").document(communityId)
                .update("isSerialEnabled", enabled)
                .await()
            auditLogger.log(
                SAAction.TOGGLE_SERIAL,
                targetId = communityId,
                metadata = mapOf("enabled" to enabled.toString()),
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to toggle serial", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Privacy Config
    // ════════════════════════════════════════════════════════════

    /** Check if the member privacy mode is enabled (at least 1 protected user). */
    suspend fun isPrivacyEnabled(): Boolean {
        return try {
            val doc = firestore.document("config/privacy_settings").get().await()
            val protectedIds = (doc.data?.get("protectedUserIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            protectedIds.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    /** Get the list of protected user IDs. */
    suspend fun getProtectedUserIds(): List<String> {
        return try {
            val doc = firestore.document("config/privacy_settings").get().await()
            (doc.data?.get("protectedUserIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Add a user to the protected list. */
    suspend fun addProtectedUser(uid: String): Resource<Unit> {
        return try {
            firestore.document("config/privacy_settings")
                .update("protectedUserIds", FieldValue.arrayUnion(uid))
                .await()
            auditLogger.log(
                SAAction.TOGGLE_PRIVACY,
                targetId = uid,
                metadata = mapOf("action" to "protect_user"),
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            // Document might not exist yet
            try {
                firestore.document("config/privacy_settings")
                    .set(
                        hashMapOf(
                            "protectedUserIds" to listOf(uid),
                            "authorizedAdmins" to emptyList<String>(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                            "updatedBy" to (auth.currentUser?.uid ?: ""),
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                auditLogger.log(
                    SAAction.TOGGLE_PRIVACY,
                    targetId = uid,
                    metadata = mapOf("action" to "protect_user"),
                )
                Resource.Success(Unit)
            } catch (e2: Exception) {
                Resource.Error(e2.message ?: "Failed to protect user", e2)
            }
        }
    }

    /** Remove a user from the protected list. */
    suspend fun removeProtectedUser(uid: String): Resource<Unit> {
        return try {
            firestore.document("config/privacy_settings")
                .update("protectedUserIds", FieldValue.arrayRemove(uid))
                .await()
            auditLogger.log(
                SAAction.TOGGLE_PRIVACY,
                targetId = uid,
                metadata = mapOf("action" to "unprotect_user"),
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unprotect user", e)
        }
    }

    /** Add all members of a community to the protected list. */
    suspend fun protectCommunityMembers(communityId: String): Resource<Int> {
        return try {
            val communityDoc = firestore.collection("communities").document(communityId).get().await()
            val memberIds = (communityDoc.data?.get("memberIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            val adminIds = (communityDoc.data?.get("adminIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            val moderatorIds = (communityDoc.data?.get("moderatorIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            val allIds = (memberIds + adminIds + moderatorIds).distinct()

            if (allIds.isEmpty()) return Resource.Success(0)

            // Firestore arrayUnion can handle adding existing values (no duplicates)
            firestore.document("config/privacy_settings")
                .set(
                    hashMapOf(
                        "protectedUserIds" to FieldValue.arrayUnion(*allIds.toTypedArray()),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "updatedBy" to (auth.currentUser?.uid ?: ""),
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()

            auditLogger.log(
                SAAction.TOGGLE_PRIVACY,
                targetId = communityId,
                metadata = mapOf("action" to "protect_community", "memberCount" to allIds.size.toString()),
            )
            Resource.Success(allIds.size)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to protect community members", e)
        }
    }

    /** Remove all members of a community from the protected list. */
    suspend fun unprotectCommunityMembers(communityId: String): Resource<Int> {
        return try {
            val communityDoc = firestore.collection("communities").document(communityId).get().await()
            val memberIds = (communityDoc.data?.get("memberIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            val adminIds = (communityDoc.data?.get("adminIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            val moderatorIds = (communityDoc.data?.get("moderatorIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            val allIds = (memberIds + adminIds + moderatorIds).distinct()

            if (allIds.isEmpty()) return Resource.Success(0)

            firestore.document("config/privacy_settings")
                .update("protectedUserIds", FieldValue.arrayRemove(*allIds.toTypedArray()))
                .await()

            auditLogger.log(
                SAAction.TOGGLE_PRIVACY,
                targetId = communityId,
                metadata = mapOf("action" to "unprotect_community", "memberCount" to allIds.size.toString()),
            )
            Resource.Success(allIds.size)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unprotect community members", e)
        }
    }

    /** Clear all protected users at once. */
    suspend fun clearAllProtectedUsers(): Resource<Unit> {
        return try {
            firestore.document("config/privacy_settings")
                .update("protectedUserIds", emptyList<String>())
                .await()
            auditLogger.log(
                SAAction.TOGGLE_PRIVACY,
                metadata = mapOf("action" to "clear_all_protected"),
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to clear protected users", e)
        }
    }

    /** Get the list of admin UIDs authorized to bypass privacy. */
    suspend fun getPrivacyAuthorizedAdmins(): List<String> {
        return try {
            val doc = firestore.document("config/privacy_settings").get().await()
            (doc.data?.get("authorizedAdmins") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Grant a user access to view private member data. */
    suspend fun grantPrivacyAccess(uid: String): Resource<Unit> {
        return try {
            firestore.document("config/privacy_settings")
                .update("authorizedAdmins", FieldValue.arrayUnion(uid))
                .await()
            auditLogger.log(
                SAAction.PRIVACY_ACCESS_GRANT,
                targetId = uid,
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to grant access", e)
        }
    }

    /** Revoke a user's access to view private member data. */
    suspend fun revokePrivacyAccess(uid: String): Resource<Unit> {
        return try {
            firestore.document("config/privacy_settings")
                .update("authorizedAdmins", FieldValue.arrayRemove(uid))
                .await()
            auditLogger.log(
                SAAction.PRIVACY_ACCESS_REVOKE,
                targetId = uid,
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to revoke access", e)
        }
    }

    // ════════════════════════════════════════════════════════════
    // Utility
    // ════════════════════════════════════════════════════════════

    private fun hashPassphrase(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun mapToSAUserItem(uid: String, data: Map<String, Any?>): SAUserItem {
        val createdAt = when (val d = data["createdAt"]) {
            is Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        val lastDonation = when (val d = data["lastDonationDate"]) {
            is Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        return SAUserItem(
            uid = uid,
            name = data["name"] as? String ?: "",
            email = data["email"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            avatarUrl = data["avatarUrl"] as? String ?: "",
            bloodGroup = data["bloodGroup"] as? String ?: "",
            district = data["district"] as? String ?: "",
            isDonor = data["isDonor"] as? Boolean ?: false,
            isBanned = data["isBanned"] as? Boolean ?: false,
            banReason = data["banReason"] as? String,
            totalDonations = (data["totalDonations"] as? Number)?.toInt() ?: 0,
            communityIds = (data["communityIds"] as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList(),
            createdAt = createdAt,
            fcmToken = data["fcmToken"] as? String ?: "",
            lastDonationDate = lastDonation,
            role = data["role"] as? String ?: "USER",
        )
    }

    private fun mapToSACommunityItem(id: String, data: Map<String, Any?>): SACommunityItem {
        val createdAt = when (val d = data["createdAt"]) {
            is Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        val memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val adminIds = (data["adminIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        return SACommunityItem(
            id = id,
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            district = data["district"] as? String ?: "",
            type = data["type"] as? String ?: "PUBLIC",
            status = data["status"] as? String ?: "ACTIVE",
            memberCount = memberIds.size,
            adminIds = adminIds,
            createdAt = createdAt,
            createdBy = data["createdBy"] as? String ?: "",
            totalDonations = (data["totalDonations"] as? Number)?.toInt() ?: 0,
            avatarUrl = data["avatarUrl"] as? String ?: "",
            isSerialEnabled = data["isSerialEnabled"] as? Boolean ?: false,
        )
    }
}

