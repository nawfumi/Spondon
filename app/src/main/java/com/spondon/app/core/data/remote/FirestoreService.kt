package com.spondon.app.core.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    // ─── Users ───────────────────────────────────────────────────

    suspend fun createUser(userId: String, data: Map<String, Any?>): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .set(data)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create user", e)
        }
    }

    suspend fun getUser(userId: String): Resource<Map<String, Any>> {
        return try {
            val doc = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            if (doc.exists()) {
                Resource.Success(doc.data ?: emptyMap())
            } else {
                Resource.Error("User not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user", e)
        }
    }

    suspend fun updateUser(userId: String, data: Map<String, Any?>): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .set(data, SetOptions.merge())
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update user", e)
        }
    }

    /**
     * Removes a user from all their communities and deletes their user document.
     * Used by [UserRepositoryImpl] for admin-initiated deletion from the SA panel.
     *
     * See also [deleteUserAccount] which additionally deletes the user's requests
     * and is used for self-service account deletion from Settings.
     */
    suspend fun deleteUser(userId: String): Resource<Unit> {
        return try {
            // Fetch user's communityIds before deleting
            val userDoc = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId).get().await()
            val communityIds = (userDoc.data?.get("communityIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()

            // Remove user from all communities and decrement memberCount
            for (communityId in communityIds) {
                val cRef = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                    .document(communityId)
                val cDoc = cRef.get().await()
                val data = cDoc.data ?: continue

                val memberIds = (data["memberIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
                val adminIds = (data["adminIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
                val moderatorIds = (data["moderatorIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()

                val wasMember = userId in memberIds
                val wasAdmin = userId in adminIds
                val wasModerator = userId in moderatorIds

                if (wasMember || wasAdmin || wasModerator) {
                    val updates = mutableMapOf<String, Any>()
                    if (wasMember) updates["memberIds"] = FieldValue.arrayRemove(userId)
                    if (wasAdmin) updates["adminIds"] = FieldValue.arrayRemove(userId)
                    if (wasModerator) updates["moderatorIds"] = FieldValue.arrayRemove(userId)
                    updates["memberCount"] = FieldValue.increment(-1)
                    cRef.update(updates).await()
                }
            }

            // Delete user document
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete user", e)
        }
    }

    fun observeUser(userId: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener = firestore.collection(Constants.USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.data)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateFcmToken(userId: String, token: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update("fcmToken", token)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update FCM token", e)
        }
    }

    /**
     * Fetches multiple users by their IDs.
     */
    suspend fun getUsers(userIds: List<String>): Resource<List<Map<String, Any>>> {
        if (userIds.isEmpty()) return Resource.Success(emptyList())
        return try {
            // Firestore `whereIn` supports max 30 items at a time
            val results = mutableListOf<Map<String, Any>>()
            userIds.chunked(30).forEach { chunk ->
                val docs = firestore.collection(Constants.USERS_COLLECTION)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                docs.documents.forEach { doc ->
                    if (doc.exists()) {
                        results.add((doc.data ?: emptyMap()) + ("uid" to doc.id))
                    }
                }
            }
            Resource.Success(results)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get users", e)
        }
    }

    // ─── Communities ──────────────────────────────────────────────

    suspend fun createCommunity(data: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create community", e)
        }
    }

    suspend fun getCommunity(communityId: String): Resource<Map<String, Any>> {
        return try {
            val doc = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .get()
                .await()
            if (doc.exists()) {
                Resource.Success((doc.data ?: emptyMap()) + ("id" to doc.id))
            } else {
                Resource.Error("Community not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get community", e)
        }
    }

    suspend fun updateCommunity(communityId: String, data: Map<String, Any?>): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .set(data, SetOptions.merge())
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update community", e)
        }
    }

    /**
     * Updates specific fields on a community document using Firestore's `.update()`.
     * Unlike [updateCommunity] (which uses `set/merge`), this correctly interprets
     * dot-notation keys (e.g. `"memberSerials.userId123"`) as nested field paths.
     */
    suspend fun updateCommunityFields(communityId: String, data: Map<String, Any?>): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .update(data)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update community fields", e)
        }
    }

    /**
     * Fetches all public communities, ordered by creation date.
     */
    suspend fun getAllCommunities(): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }
            Resource.Success(list)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get communities", e)
        }
    }

    /**
     * Fetches communities that the user is a member, admin, or moderator of.
     * Firestore `whereArrayContains` only supports a single field per query,
     * so we run three queries and merge + deduplicate the results.
     */
    suspend fun getMyCommunities(userId: String): Resource<List<Map<String, Any>>> {
        return try {
            val results = mutableMapOf<String, Map<String, Any>>()

            // Query 1: memberIds
            val memberDocs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .whereArrayContains("memberIds", userId)
                .get()
                .await()
            memberDocs.documents.forEach { doc ->
                if (doc.exists()) results[doc.id] = (doc.data ?: emptyMap()) + ("id" to doc.id)
            }

            // Query 2: adminIds
            val adminDocs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .whereArrayContains("adminIds", userId)
                .get()
                .await()
            adminDocs.documents.forEach { doc ->
                if (doc.exists()) results[doc.id] = (doc.data ?: emptyMap()) + ("id" to doc.id)
            }

            // Query 3: moderatorIds
            val modDocs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .whereArrayContains("moderatorIds", userId)
                .get()
                .await()
            modDocs.documents.forEach { doc ->
                if (doc.exists()) results[doc.id] = (doc.data ?: emptyMap()) + ("id" to doc.id)
            }

            Resource.Success(results.values.toList())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user communities", e)
        }
    }

    /**
     * Adds a user to a public community.
     */
    suspend fun joinCommunity(communityId: String, userId: String): Resource<Unit> {
        return try {
            val ref = firestore.collection(Constants.COMMUNITIES_COLLECTION).document(communityId)
            ref.update(
                mapOf(
                    "memberIds" to FieldValue.arrayUnion(userId),
                    "memberCount" to FieldValue.increment(1),
                )
            ).await()
            // Also add community to user's communityIds
            firestore.collection(Constants.USERS_COLLECTION).document(userId)
                .update("communityIds", FieldValue.arrayUnion(communityId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to join community", e)
        }
    }

    /**
     * Removes a user from a community.
     */
    suspend fun leaveCommunity(communityId: String, userId: String): Resource<Unit> {
        return try {
            val ref = firestore.collection(Constants.COMMUNITIES_COLLECTION).document(communityId)
            ref.update(
                mapOf(
                    "memberIds" to FieldValue.arrayRemove(userId),
                    "adminIds" to FieldValue.arrayRemove(userId),
                    "moderatorIds" to FieldValue.arrayRemove(userId),
                    "memberCount" to FieldValue.increment(-1),
                )
            ).await()
            firestore.collection(Constants.USERS_COLLECTION).document(userId)
                .update("communityIds", FieldValue.arrayRemove(communityId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to leave community", e)
        }
    }

    /**
     * Adds a user to the pending list for a private community.
     */
    suspend fun addPendingMember(communityId: String, userId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .update("pendingIds", FieldValue.arrayUnion(userId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add pending member", e)
        }
    }

    /**
     * Removes a user from the pending list.
     */
    suspend fun removePendingMember(communityId: String, userId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .update("pendingIds", FieldValue.arrayRemove(userId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove pending member", e)
        }
    }

    // ─── Join Requests (subcollection) ─────────────────────────────

    /**
     * Creates a join request document in communities/{id}/joinRequests
     */
    suspend fun createJoinRequest(
        communityId: String,
        data: Map<String, Any?>,
    ): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .collection(Constants.JOIN_REQUESTS_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create join request", e)
        }
    }

    /**
     * Fetches pending join requests for a community.
     */
    suspend fun getPendingJoinRequests(communityId: String): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .collection(Constants.JOIN_REQUESTS_COLLECTION)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }.sortedByDescending { map ->
                (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
            }
            Resource.Success(list)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get join requests", e)
        }
    }

    /**
     * Updates a join request's status (APPROVED / REJECTED).
     */
    suspend fun updateJoinRequestStatus(
        communityId: String,
        requestId: String,
        status: String,
        rejectionNote: String? = null,
    ): Resource<Unit> {
        return try {
            val data = mutableMapOf<String, Any>("status" to status)
            if (rejectionNote != null) data["rejectionNote"] = rejectionNote
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .collection(Constants.JOIN_REQUESTS_COLLECTION)
                .document(requestId)
                .update(data)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update join request", e)
        }
    }

    /**
     * Promotes a member to a role (admin or moderator).
     */
    suspend fun promoteMember(
        communityId: String,
        userId: String,
        roleField: String,
    ): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .update(roleField, FieldValue.arrayUnion(userId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to promote member", e)
        }
    }

    /**
     * Demotes a member back to regular member by removing them from
     * both adminIds and moderatorIds.
     */
    suspend fun demoteMember(
        communityId: String,
        userId: String,
    ): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .update(
                    mapOf(
                        "adminIds" to FieldValue.arrayRemove(userId),
                        "moderatorIds" to FieldValue.arrayRemove(userId),
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to demote member", e)
        }
    }

    /**
     * Updates a member's donation status and last donation date.
     */
    suspend fun updateMemberDonationStatus(
        userId: String,
        lastDonationDate: com.google.firebase.Timestamp,
        totalDonations: Int,
    ): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "lastDonationDate" to lastDonationDate,
                        "totalDonations" to totalDonations,
                        "availabilityOverride" to false,
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update donation status", e)
        }
    }

    /**
     * Overrides a member's availability (admin sets available early at ≥90 days).
     */
    suspend fun overrideMemberAvailability(userId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update("availabilityOverride", true)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to override availability", e)
        }
    }

    // ─── Blood Requests ───────────────────────────────────────────

    suspend fun createRequest(data: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.REQUESTS_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create request", e)
        }
    }

    suspend fun getRequest(requestId: String): Resource<Map<String, Any>> {
        return try {
            val doc = firestore.collection(Constants.REQUESTS_COLLECTION)
                .document(requestId)
                .get()
                .await()
            if (doc.exists()) {
                Resource.Success((doc.data ?: emptyMap()) + ("id" to doc.id))
            } else {
                Resource.Error("Request not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get request", e)
        }
    }

    /**
     * Fetches requests for the given community IDs.
     * Firestore `whereArrayContainsAny` supports max 30 values.
     */
    suspend fun getRequestsForCommunities(
        communityIds: List<String>,
    ): Resource<List<Map<String, Any>>> {
        if (communityIds.isEmpty()) return Resource.Success(emptyList())
        return try {
            val results = mutableListOf<Map<String, Any>>()
            // Avoid combining whereArrayContainsAny with orderBy
            // as it requires a composite index that may not exist.
            // Sort in memory instead.
            communityIds.chunked(30).forEach { chunk ->
                val docs = firestore.collection(Constants.REQUESTS_COLLECTION)
                    .whereArrayContainsAny("communityIds", chunk)
                    .get()
                    .await()
                docs.documents.forEach { doc ->
                    if (doc.exists()) {
                        results.add((doc.data ?: emptyMap()) + ("id" to doc.id))
                    }
                }
            }
            // Deduplicate (a request may belong to multiple communities)
            val deduplicated = results.distinctBy { it["id"] }
            // Sort by createdAt descending in memory
            val sorted = deduplicated.sortedByDescending { data ->
                when (val d = data["createdAt"]) {
                    is com.google.firebase.Timestamp -> d.toDate().time
                    is java.util.Date -> d.time
                    else -> 0L
                }
            }
            Resource.Success(sorted)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get requests", e)
        }
    }

    /**
     * Fetches requests created by a specific user.
     */
    suspend fun getRequestsByUser(userId: String): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.REQUESTS_COLLECTION)
                .whereEqualTo("requesterId", userId)
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }
            // Sort by createdAt descending in memory
            val sorted = list.sortedByDescending { data ->
                when (val d = data["createdAt"]) {
                    is com.google.firebase.Timestamp -> d.toDate().time
                    is java.util.Date -> d.time
                    else -> 0L
                }
            }
            Resource.Success(sorted)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user requests", e)
        }
    }

    /**
     * Observes requests from the given communities in real-time.
     */
    fun observeRequestsForCommunities(communityIds: List<String>): Flow<List<Map<String, Any>>> =
        callbackFlow {
            if (communityIds.isEmpty()) {
                trySend(emptyList())
                awaitClose()
                return@callbackFlow
            }
            // Use the first 10 communities for the listener (Firestore limit)
            val limitedIds = communityIds.take(10)
            val listener = firestore.collection(Constants.REQUESTS_COLLECTION)
                .whereArrayContainsAny("communityIds", limitedIds)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
                    } ?: emptyList()
                    // Sort in memory
                    val sorted = list.sortedByDescending { data ->
                        when (val d = data["createdAt"]) {
                            is com.google.firebase.Timestamp -> d.toDate().time
                            is java.util.Date -> d.time
                            else -> 0L
                        }
                    }
                    trySend(sorted)
                }
            awaitClose { listener.remove() }
        }

    /**
     * Adds a respondent (donor) to a request.
     */
    suspend fun addRespondent(requestId: String, donorId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.REQUESTS_COLLECTION)
                .document(requestId)
                .update("respondents", FieldValue.arrayUnion(donorId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to respond to request", e)
        }
    }

    /**
     * Updates request status.
     */
    suspend fun updateRequestStatus(requestId: String, status: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", status)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update request status", e)
        }
    }

    // ─── Notifications ───────────────────────────────────────────

    // ─── Donations ───────────────────────────────────────────────

    suspend fun createDonation(data: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.DONATIONS_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to record donation", e)
        }
    }

    /**
     * Fetches donation history for a specific user.
     */
    suspend fun getDonationsByUser(userId: String): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.DONATIONS_COLLECTION)
                .whereEqualTo("donorId", userId)
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }
            // Sort by date descending in memory to avoid requiring composite index
            val sorted = list.sortedByDescending { data ->
                when (val d = data["date"]) {
                    is com.google.firebase.Timestamp -> d.toDate().time
                    is java.util.Date -> d.time
                    else -> 0L
                }
            }
            Resource.Success(sorted)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get donations", e)
        }
    }

    /**
     * Searches donors by blood group, community, district, and availability.
     * Filters are applied progressively.
     */
    suspend fun searchDonors(
        bloodGroup: String?,
        communityId: String?,
        district: String?,
        availableOnly: Boolean,
    ): Resource<List<Map<String, Any>>> {
        return try {
            var query: Query =
                firestore.collection(Constants.USERS_COLLECTION)
                    .whereEqualTo("isDonor", true)

            if (!bloodGroup.isNullOrBlank()) {
                query = query.whereEqualTo("bloodGroup", bloodGroup)
            }
            if (!district.isNullOrBlank()) {
                query = query.whereEqualTo("district", district)
            }

            val docs = query.get().await()
            var results = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("uid" to doc.id) else null
            }

            // Filter by community membership in memory (Firestore can't combine
            // whereEqualTo on different array fields in a single query)
            if (!communityId.isNullOrBlank()) {
                results = results.filter { data ->
                    val ids = (data["communityIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    ids.contains(communityId)
                }
            }

            // Filter by availability in memory
            if (availableOnly) {
                val now = System.currentTimeMillis()
                results = results.filter { data ->
                    val lastDonation = when (val d = data["lastDonationDate"]) {
                        is com.google.firebase.Timestamp -> d.toDate().time
                        is java.util.Date -> d.time
                        else -> null
                    }
                    if (lastDonation == null) {
                        true // Never donated, available
                    } else {
                        val daysSince = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now - lastDonation)
                        val interval = (data["donationInterval"] as? Number)?.toInt() ?: 120
                        val overridden = data["availabilityOverride"] as? Boolean ?: false
                        val requiredDays = if (overridden) Constants.MIN_OVERRIDE_DAYS else interval
                        daysSince >= requiredDays
                    }
                }
            }

            Resource.Success(results)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to search donors", e)
        }
    }

    // ─── Notifications ──────────────────────────────────────────

    suspend fun getNotifications(userId: String): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }
            val sorted = list.sortedByDescending { data ->
                when (val d = data["createdAt"]) {
                    is com.google.firebase.Timestamp -> d.toDate().time
                    is java.util.Date -> d.time
                    else -> 0L
                }
            }
            Resource.Success(sorted)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get notifications", e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }
        val listener = firestore.collection(Constants.NOTIFICATIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
                } ?: emptyList()
                val sorted = list.sortedByDescending { data ->
                    when (val d = data["createdAt"]) {
                        is com.google.firebase.Timestamp -> d.toDate().time
                        is java.util.Date -> d.time
                        else -> 0L
                    }
                }
                trySend(sorted)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markNotificationRead(notificationId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update("isRead", true)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark notification as read", e)
        }
    }

    suspend fun markAllNotificationsRead(userId: String): Resource<Unit> {
        return try {
            val docs = firestore.collection(Constants.NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            // Firestore batch supports max 500 writes at a time
            docs.documents.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark all as read", e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete notification", e)
        }
    }

    suspend fun deleteUserAccount(userId: String): Resource<Unit> {
        return try {
            // Fetch user's communityIds before deleting
            val userDoc = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId).get().await()
            val communityIds = (userDoc.data?.get("communityIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()

            // Remove user from all communities and decrement memberCount
            for (communityId in communityIds) {
                val cRef = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                    .document(communityId)
                val cDoc = cRef.get().await()
                val data = cDoc.data ?: continue

                val memberIds = (data["memberIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
                val adminIds = (data["adminIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
                val moderatorIds = (data["moderatorIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()

                val wasMember = userId in memberIds
                val wasAdmin = userId in adminIds
                val wasModerator = userId in moderatorIds

                if (wasMember || wasAdmin || wasModerator) {
                    val updates = mutableMapOf<String, Any>()
                    if (wasMember) updates["memberIds"] = FieldValue.arrayRemove(userId)
                    if (wasAdmin) updates["adminIds"] = FieldValue.arrayRemove(userId)
                    if (wasModerator) updates["moderatorIds"] = FieldValue.arrayRemove(userId)
                    updates["memberCount"] = FieldValue.increment(-1)
                    cRef.update(updates).await()
                }
            }

            // Delete user's requests
            val reqSnapshot = firestore.collection(Constants.REQUESTS_COLLECTION)
                .whereEqualTo("requesterId", userId)
                .get().await()
            for (doc in reqSnapshot.documents) {
                doc.reference.delete().await()
            }

            // Delete user document
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete account", e)
        }
    }

    /**
     * Adds donor IDs to the confirmedDonors array field on a request document.
     */
    suspend fun confirmDonors(requestId: String, donorIds: List<String>): Resource<Unit> {
        return try {
            firestore.collection(Constants.REQUESTS_COLLECTION)
                .document(requestId)
                .update("confirmedDonors", FieldValue.arrayUnion(*donorIds.toTypedArray()))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to confirm donors", e)
        }
    }

    suspend fun deleteRequest(requestId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.REQUESTS_COLLECTION)
                .document(requestId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete request", e)
        }
    }

    // ─── Spondon Global Community ─────────────────────────────────

    /**
     * Reads the Spondon community ID from the config document.
     * Returns null if the config doc doesn't exist yet.
     */
    suspend fun getSpondonCommunityId(): String? {
        return try {
            val doc = firestore.document(Constants.SPONDON_CONFIG_DOC).get().await()
            if (doc.exists()) doc.getString("communityId") else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates the Spondon community if it doesn't already exist,
     * and stores its ID in the config document.
     */
    suspend fun ensureSpondonCommunity(): Resource<String> {
        return try {
            // Check if already created
            val existingId = getSpondonCommunityId()
            if (existingId != null) return Resource.Success(existingId)

            // Fetch super admin ID
            val saDoc = firestore.document("config/superadmin").get().await()
            val superAdminId =
                saDoc.getString("uid") ?: return Resource.Error("Super Admin not registered yet")

            // Create the community
            val data = mapOf<String, Any?>(
                "name" to Constants.SPONDON_COMMUNITY_NAME,
                "description" to "স্পন্দন — the official community of the Spondon platform. Every user is a member. Admin posts announcements, news, and updates here.",
                "coverUrl" to "",
                "type" to "SPONDON",
                "adminIds" to listOf(superAdminId),
                "moderatorIds" to emptyList<String>(),
                "memberIds" to listOf(superAdminId),
                "pendingIds" to emptyList<String>(),
                "district" to "",
                "upazila" to "",
                "bloodGroups" to emptyList<String>(),
                "memberCount" to 1,
                "donationCount" to 0,
                "isVerified" to true,
                "isSpondon" to true,
                "createdAt" to com.google.firebase.Timestamp.now(),
            )
            val result = createCommunity(data)
            if (result is Resource.Success) {
                val communityId = result.data
                // Store the ID in config
                firestore.document(Constants.SPONDON_CONFIG_DOC)
                    .set(mapOf("communityId" to communityId))
                    .await()
                // Add to super admin's communityIds
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(superAdminId)
                    .update("communityIds", FieldValue.arrayUnion(communityId))
                    .await()
                Resource.Success(communityId)
            } else {
                Resource.Error("Failed to create Spondon community")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to ensure Spondon community", e)
        }
    }

    // ─── Community Posts ───────────────────────────────────────────

    /**
     * Creates a general-purpose community post.
     */
    suspend fun createCommunityPost(data: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.COMMUNITY_POSTS_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create post", e)
        }
    }

    /**
     * Fetches all posts for a community, sorted by creation date descending.
     */
    suspend fun getCommunityPosts(communityId: String): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.COMMUNITY_POSTS_COLLECTION)
                .whereEqualTo("communityId", communityId)
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }
            val sorted = list.sortedByDescending { data ->
                when (val d = data["createdAt"]) {
                    is com.google.firebase.Timestamp -> d.toDate().time
                    is java.util.Date -> d.time
                    else -> 0L
                }
            }
            Resource.Success(sorted)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get community posts", e)
        }
    }

    /**
     * Deletes a community post.
     */
    suspend fun deleteCommunityPost(postId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITY_POSTS_COLLECTION)
                .document(postId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete post", e)
        }
    }
}
