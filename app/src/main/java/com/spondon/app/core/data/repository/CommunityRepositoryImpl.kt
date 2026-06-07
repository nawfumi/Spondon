package com.spondon.app.core.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.remote.FirestoreService
import com.spondon.app.core.data.remote.StorageService
import com.spondon.app.core.domain.model.*
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestoreService: FirestoreService,
    private val storageService: StorageService,
) : CommunityRepository {

    override suspend fun getCommunities(): Resource<List<Community>> {
        return when (val result = firestoreService.getAllCommunities()) {
            is Resource.Success -> Resource.Success(result.data.map { mapToCommunity(it) })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getMyCommunities(userId: String): Resource<List<Community>> {
        return when (val result = firestoreService.getMyCommunities(userId)) {
            is Resource.Success -> Resource.Success(result.data.map { mapToCommunity(it) })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getCommunity(communityId: String): Resource<Community> {
        return when (val result = firestoreService.getCommunity(communityId)) {
            is Resource.Success -> Resource.Success(mapToCommunity(result.data))
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun createCommunity(community: Community): Resource<String> {
        val data = mapOf<String, Any?>(
            "name" to community.name,
            "description" to community.description,
            "coverUrl" to community.coverUrl,
            "type" to community.type.name,
            "adminIds" to community.adminIds,
            "moderatorIds" to community.moderatorIds,
            "memberIds" to community.memberIds,
            "pendingIds" to community.pendingIds,
            "district" to community.district,
            "upazila" to community.upazila,
            "bloodGroups" to community.bloodGroups,
            "memberCount" to community.memberCount,
            "donationCount" to community.donationCount,
            "isVerified" to community.isVerified,
            "createdAt" to Timestamp.now(),
        )
        val result = firestoreService.createCommunity(data)
        // Add the new community ID to the creator's communityIds in user doc
        if (result is Resource.Success) {
            val communityId = result.data
            community.adminIds.forEach { adminId ->
                firestoreService.updateUser(adminId, mapOf(
                    "communityIds" to com.google.firebase.firestore.FieldValue.arrayUnion(communityId)
                ))
            }
        }
        return result
    }

    override suspend fun updateCommunity(community: Community): Resource<Unit> {
        val data = mapOf<String, Any?>(
            "name" to community.name,
            "description" to community.description,
            "coverUrl" to community.coverUrl,
            "type" to community.type.name,
            "district" to community.district,
            "upazila" to community.upazila,
            "bloodGroups" to community.bloodGroups,
        )
        return firestoreService.updateCommunity(community.id, data)
    }

    override suspend fun joinCommunity(communityId: String, userId: String): Resource<Unit> {
        return firestoreService.joinCommunity(communityId, userId)
    }

    override suspend fun requestToJoin(
        communityId: String,
        userId: String,
        message: String,
    ): Resource<Unit> {
        // Add user to pending list
        val pendingResult = firestoreService.addPendingMember(communityId, userId)
        if (pendingResult is Resource.Error) return pendingResult

        // Also create a join request document
        val currentUser = auth.currentUser
        val userResult = firestoreService.getUser(userId)
        val userData = (userResult as? Resource.Success)?.data ?: emptyMap()

        val data = mapOf<String, Any?>(
            "userId" to userId,
            "userName" to (userData["name"] as? String ?: currentUser?.displayName ?: ""),
            "userBloodGroup" to (userData["bloodGroup"] as? String ?: ""),
            "userDistrict" to (userData["district"] as? String ?: ""),
            "userUpazila" to (userData["upazila"] as? String ?: ""),
            "message" to message,
            "status" to "PENDING",
            "createdAt" to Timestamp.now(),
        )
        return when (val createResult = firestoreService.createJoinRequest(communityId, data)) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error -> Resource.Error(createResult.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun approveMember(communityId: String, userId: String): Resource<Unit> {
        // Remove from pending
        firestoreService.removePendingMember(communityId, userId)
        // Add as member
        return firestoreService.joinCommunity(communityId, userId)
    }

    override suspend fun rejectMember(communityId: String, userId: String): Resource<Unit> {
        return firestoreService.removePendingMember(communityId, userId)
    }

    override suspend fun removeMember(communityId: String, userId: String): Resource<Unit> {
        return firestoreService.leaveCommunity(communityId, userId)
    }

    override suspend fun promoteMember(
        communityId: String,
        userId: String,
        role: CommunityRole,
    ): Resource<Unit> {
        val roleField = when (role) {
            CommunityRole.ADMIN -> "adminIds"
            CommunityRole.MODERATOR -> "moderatorIds"
            CommunityRole.MEMBER -> return Resource.Success(Unit) // No action needed
        }
        return firestoreService.promoteMember(communityId, userId, roleField)
    }

    /**
     * Fetches pending join requests for the admin panel.
     */
    suspend fun getPendingJoinRequests(communityId: String): Resource<List<JoinRequest>> {
        return when (val result = firestoreService.getPendingJoinRequests(communityId)) {
            is Resource.Success -> Resource.Success(result.data.map { mapToJoinRequest(it) })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    /**
     * Approves a join request — updates the request status and adds user as member.
     */
    suspend fun approveJoinRequest(
        communityId: String,
        requestId: String,
        userId: String,
    ): Resource<Unit> {
        // Step 1: Update the join request document status
        val statusResult = firestoreService.updateJoinRequestStatus(communityId, requestId, "APPROVED")
        if (statusResult is Resource.Error) {
            return Resource.Error("Failed to update request status: ${statusResult.message}")
        }

        // Step 2: Remove user from the pending list
        val pendingResult = firestoreService.removePendingMember(communityId, userId)
        if (pendingResult is Resource.Error) {
            return Resource.Error("Failed to remove from pending: ${pendingResult.message}")
        }

        // Step 3: Add user as a member
        val joinResult = firestoreService.joinCommunity(communityId, userId)
        if (joinResult is Resource.Error) {
            return Resource.Error("Failed to add member: ${joinResult.message}")
        }

        return Resource.Success(Unit)
    }

    /**
     * Rejects a join request with an optional note.
     */
    suspend fun rejectJoinRequest(
        communityId: String,
        requestId: String,
        userId: String,
        note: String?,
    ): Resource<Unit> {
        val statusResult = firestoreService.updateJoinRequestStatus(communityId, requestId, "REJECTED", note)
        if (statusResult is Resource.Error) {
            return Resource.Error("Failed to update request status: ${statusResult.message}")
        }

        val pendingResult = firestoreService.removePendingMember(communityId, userId)
        if (pendingResult is Resource.Error) {
            return Resource.Error("Failed to remove from pending: ${pendingResult.message}")
        }

        return Resource.Success(Unit)
    }

    /**
     * Uploads a community cover image.
     */
    suspend fun uploadCoverImage(communityId: String, uri: Uri): Resource<String> {
        return storageService.uploadCommunityCover(communityId, uri)
    }

    /**
     * Gets all members of a community as User objects.
     */
    suspend fun getCommunityMembers(memberIds: List<String>): Resource<List<User>> {
        return when (val result = firestoreService.getUsers(memberIds)) {
            is Resource.Success -> Resource.Success(result.data.map { mapToUser(it) })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    /**
     * Updates member donation status (admin action).
     */
    suspend fun updateMemberDonationStatus(
        userId: String,
        donationDate: Date,
        totalDonations: Int,
    ): Resource<Unit> {
        return firestoreService.updateMemberDonationStatus(
            userId,
            Timestamp(donationDate),
            totalDonations,
        )
    }

    /**
     * Admin overrides member availability at ≥90 days.
     */
    suspend fun overrideMemberAvailability(userId: String): Resource<Unit> {
        return firestoreService.overrideMemberAvailability(userId)
    }

    /**
     * Leaves a community.
     */
    suspend fun leaveCommunity(communityId: String, userId: String): Resource<Unit> {
        return firestoreService.leaveCommunity(communityId, userId)
    }

    /**
     * Cancels a pending join request.
     */
    suspend fun cancelJoinRequest(communityId: String, userId: String): Resource<Unit> {
        return firestoreService.removePendingMember(communityId, userId)
    }

    // ─── Mappers ─────────────────────────────────────────────────

    private fun mapToCommunity(data: Map<String, Any>): Community {
        val createdAt = when (val ts = data["createdAt"]) {
            is Timestamp -> ts.toDate()
            is Date -> ts
            else -> null
        }
        return Community(
            id = data["id"] as? String ?: "",
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            coverUrl = data["coverUrl"] as? String ?: "",
            type = try {
                CommunityType.valueOf(data["type"] as? String ?: "PUBLIC")
            } catch (_: Exception) {
                CommunityType.PUBLIC
            },
            adminIds = (data["adminIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            moderatorIds = (data["moderatorIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            pendingIds = (data["pendingIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            district = data["district"] as? String ?: "",
            upazila = data["upazila"] as? String ?: "",
            bloodGroups = (data["bloodGroups"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            memberCount = (data["memberCount"] as? Number)?.toInt() ?: 0,
            donationCount = (data["donationCount"] as? Number)?.toInt() ?: 0,
            isVerified = data["isVerified"] as? Boolean ?: false,
            isSpondon = data["isSpondon"] as? Boolean ?: false,
            createdAt = createdAt,
        )
    }

    private fun mapToJoinRequest(data: Map<String, Any>): JoinRequest {
        val createdAt = when (val ts = data["createdAt"]) {
            is Timestamp -> ts.toDate()
            is Date -> ts
            else -> null
        }
        return JoinRequest(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            userName = data["userName"] as? String ?: "",
            userBloodGroup = data["userBloodGroup"] as? String ?: "",
            userDistrict = data["userDistrict"] as? String ?: "",
            userUpazila = data["userUpazila"] as? String ?: "",
            message = data["message"] as? String ?: "",
            status = try {
                JoinRequestStatus.valueOf(data["status"] as? String ?: "PENDING")
            } catch (_: Exception) {
                JoinRequestStatus.PENDING
            },
            rejectionNote = data["rejectionNote"] as? String,
            createdAt = createdAt,
        )
    }

    private fun mapToUser(data: Map<String, Any>): User {
        val dob = when (val d = data["dob"]) {
            is Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        val lastDonation = when (val d = data["lastDonationDate"]) {
            is Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        return User(
            uid = data["uid"] as? String ?: "",
            name = data["name"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            email = data["email"] as? String ?: "",
            avatarUrl = data["avatarUrl"] as? String ?: "",
            bloodGroup = data["bloodGroup"] as? String ?: "",
            dob = dob,
            weight = (data["weight"] as? Number)?.toFloat() ?: 0f,
            isDonor = data["isDonor"] as? Boolean ?: false,
            lastDonationDate = lastDonation,
            donationInterval = (data["donationInterval"] as? Number)?.toInt() ?: 120,
            availabilityOverride = data["availabilityOverride"] as? Boolean ?: false,
            totalDonations = (data["totalDonations"] as? Number)?.toInt() ?: 0,
            district = data["district"] as? String ?: "",
            upazila = data["upazila"] as? String ?: "",
            isPhoneVisible = data["isPhoneVisible"] as? Boolean ?: true,
            communityIds = (data["communityIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            badges = (data["badges"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        )
    }

    private fun mapToCommunityPost(data: Map<String, Any>): CommunityPost {
        val createdAt = when (val ts = data["createdAt"]) {
            is Timestamp -> ts.toDate()
            is Date -> ts
            else -> null
        }
        return CommunityPost(
            id = data["id"] as? String ?: "",
            communityId = data["communityId"] as? String ?: "",
            authorId = data["authorId"] as? String ?: "",
            authorName = data["authorName"] as? String ?: "",
            authorAvatarUrl = data["authorAvatarUrl"] as? String ?: "",
            content = data["content"] as? String ?: "",
            imageUrl = data["imageUrl"] as? String,
            createdAt = createdAt,
        )
    }

    // ─── Spondon Community ────────────────────────────────────────

    /**
     * Returns the Spondon community ID from config, or null if not created yet.
     */
    suspend fun getSpondonCommunityId(): String? {
        val id = firestoreService.getSpondonCommunityId()
        if (id != null) return id
        
        // Auto-create Spondon community if not exists
        val result = firestoreService.ensureSpondonCommunity()
        return if (result is Resource.Success) result.data else null
    }

    /**
     * Ensures the current user is a member of the Spondon community.
     * If the Spondon community exists and the user is not yet a member,
     * they are silently added.
     */
    suspend fun ensureUserInSpondonCommunity(userId: String): Resource<Unit> {
        val spondonId = firestoreService.getSpondonCommunityId() ?: return Resource.Success(Unit)
        return try {
            // Check if user is already a member
            val communityResult = firestoreService.getCommunity(spondonId)
            if (communityResult is Resource.Success) {
                val memberIds = (communityResult.data["memberIds"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
                if (!memberIds.contains(userId)) {
                    firestoreService.joinCommunity(spondonId, userId)
                } else {
                    Resource.Success(Unit)
                }
            } else {
                Resource.Success(Unit)
            }
        } catch (_: Exception) {
            Resource.Success(Unit) // fail silently
        }
    }

    // ─── Community Posts ──────────────────────────────────────────

    /**
     * Creates a community post with optional image upload.
     */
    suspend fun createCommunityPost(
        communityId: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String,
        content: String,
        imageUri: Uri?,
    ): Resource<String> {
        // Upload image first if provided
        var imageUrl: String? = null
        if (imageUri != null) {
            val tempId = System.currentTimeMillis().toString()
            when (val uploadResult = storageService.uploadPostImage(tempId, imageUri)) {
                is Resource.Success -> imageUrl = uploadResult.data
                is Resource.Error -> return Resource.Error("Image upload failed: ${uploadResult.message}")
                is Resource.Loading -> {}
            }
        }

        val data = mapOf<String, Any?>(
            "communityId" to communityId,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorAvatarUrl" to authorAvatarUrl,
            "content" to content,
            "imageUrl" to imageUrl,
            "createdAt" to Timestamp.now(),
        )
        return firestoreService.createCommunityPost(data)
    }

    /**
     * Fetches all posts for a community.
     */
    suspend fun getCommunityPosts(communityId: String): Resource<List<CommunityPost>> {
        return when (val result = firestoreService.getCommunityPosts(communityId)) {
            is Resource.Success -> Resource.Success(result.data.map { mapToCommunityPost(it) })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    /**
     * Deletes a community post.
     */
    suspend fun deleteCommunityPost(postId: String): Resource<Unit> {
        return firestoreService.deleteCommunityPost(postId)
    }
}

