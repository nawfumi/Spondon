package com.spondon.app.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.remote.FirestoreService
import com.spondon.app.core.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestoreService: FirestoreService,
) : UserRepository {

    override suspend fun getUser(userId: String): Resource<User> {
        return when (val result = firestoreService.getUser(userId)) {
            is Resource.Success -> Resource.Success(mapToUser(userId, result.data))
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getUsers(userIds: List<String>): Resource<List<User>> {
        if (userIds.isEmpty()) return Resource.Success(emptyList())
        return when (val result = firestoreService.getUsers(userIds)) {
            is Resource.Success -> Resource.Success(
                result.data.map { data -> mapToUser(data["uid"] as? String ?: "", data) }
            )
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun updateUser(user: User): Resource<Unit> {
        val data = mapOf<String, Any?>(
            "name" to user.name,
            "phone" to user.phone,
            "email" to user.email,
            "avatarUrl" to user.avatarUrl,
            "bloodGroup" to user.bloodGroup,
            "dob" to user.dob?.let { com.google.firebase.Timestamp(it) },
            "weight" to user.weight,
            "isDonor" to user.isDonor,
            "lastDonationDate" to user.lastDonationDate?.let { com.google.firebase.Timestamp(it) },
            "donationInterval" to user.donationInterval,
            "totalDonations" to user.totalDonations,
            "availabilityOverride" to user.availabilityOverride,
            "district" to user.district,
            "upazila" to user.upazila,
            "isPhoneVisible" to user.isPhoneVisible,
            "badges" to user.badges,
        )
        return firestoreService.updateUser(user.uid, data)
    }

    override suspend fun updateFcmToken(token: String): Resource<Unit> {
        val uid = auth.currentUser?.uid ?: return Resource.Error("Not logged in")
        return firestoreService.updateFcmToken(uid, token)
    }

    override suspend fun deleteAccount(): Resource<Unit> {
        val uid = auth.currentUser?.uid ?: return Resource.Error("Not logged in")
        return try {
            firestoreService.deleteUser(uid)
            auth.currentUser?.delete()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Account deletion failed", e)
        }
    }

    override fun observeUser(userId: String): Flow<User> {
        return firestoreService.observeUser(userId).map { data ->
            if (data != null) mapToUser(userId, data) else User(uid = userId)
        }
    }

    private fun mapToUser(uid: String, data: Map<String, Any>): User {
        val dob = when (val d = data["dob"]) {
            is com.google.firebase.Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        val lastDonation = when (val d = data["lastDonationDate"]) {
            is com.google.firebase.Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        val bannedAt = when (val d = data["bannedAt"]) {
            is com.google.firebase.Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        return User(
            uid = uid,
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
            role = try {
                com.spondon.app.core.domain.model.UserRole.valueOf(
                    data["role"] as? String ?: "USER"
                )
            } catch (_: Exception) {
                com.spondon.app.core.domain.model.UserRole.USER
            },
            isBanned = data["isBanned"] as? Boolean ?: false,
            banReason = data["banReason"] as? String,
            bannedAt = bannedAt,
        )
    }
}
