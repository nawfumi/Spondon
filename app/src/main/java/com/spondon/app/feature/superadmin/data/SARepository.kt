package com.spondon.app.feature.superadmin.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.spondon.app.core.common.Resource
import com.spondon.app.feature.superadmin.broadcast.*
import com.spondon.app.feature.superadmin.community.*
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
        type: String = "INFO",
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
    ): Resource<Unit> {
        return try {
            val broadcast = hashMapOf(
                "title" to title,
                "body" to body,
                "type" to type,
                "target" to target,
                "sentBy" to (auth.currentUser?.uid ?: ""),
                "sentAt" to FieldValue.serverTimestamp(),
                "status" to "SENT",
            )
            firestore.collection("broadcasts").add(broadcast).await()
            auditLogger.log(
                SAAction.BROADCAST,
                metadata = mapOf("title" to title, "target" to target, "type" to type),
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
                SABroadcastItem(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    body = data["body"] as? String ?: "",
                    type = data["type"] as? String ?: "",
                    target = data["target"] as? String ?: "",
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
        )
    }
}

