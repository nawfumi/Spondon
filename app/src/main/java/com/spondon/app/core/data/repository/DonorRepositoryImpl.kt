package com.spondon.app.core.data.repository

import com.google.firebase.Timestamp
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.remote.FirestoreService
import com.spondon.app.core.domain.model.Donation
import com.spondon.app.core.domain.model.DonationStatus
import com.spondon.app.core.domain.model.User
import com.spondon.app.core.domain.model.UserRole
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DonorRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
) : DonorRepository {

    override suspend fun searchDonors(
        bloodGroup: String?,
        communityId: String?,
        district: String?,
        availableOnly: Boolean,
    ): Resource<List<User>> {
        return when (val result = firestoreService.searchDonors(bloodGroup, communityId, district, availableOnly)) {
            is Resource.Success -> Resource.Success(result.data.map { mapToUser(it) })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getDonorProfile(userId: String): Resource<User> {
        return when (val result = firestoreService.getUser(userId)) {
            is Resource.Success -> Resource.Success(mapToUser(result.data + ("uid" to userId)))
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getDonationHistory(userId: String): Resource<List<Donation>> {
        return when (val result = firestoreService.getDonationsByUser(userId)) {
            is Resource.Success -> Resource.Success(result.data.map { mapToDonation(it) })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun recordDonation(donation: Donation): Resource<Unit> {
        val data = mapOf(
            "requestId" to donation.requestId,
            "donorId" to donation.donorId,
            "hospital" to donation.hospital,
            "bloodGroup" to donation.bloodGroup,
            "date" to donation.date?.let { Timestamp(it) },
            "status" to donation.status.name,
            "confirmedBy" to donation.confirmedBy,
        )
        return when (firestoreService.createDonation(data)) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error -> Resource.Error("Failed to record donation")
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun overrideAvailability(userId: String, adminId: String): Resource<Unit> {
        return firestoreService.overrideMemberAvailability(userId)
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
        val createdAt = when (val d = data["createdAt"]) {
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
            role = try {
                val roleStr = (data["role"] as? String)?.uppercase()?.trim() ?: "USER"
                UserRole.valueOf(roleStr)
            } catch (_: Exception) {
                UserRole.USER
            },
            createdAt = createdAt,
        )
    }

    private fun mapToDonation(data: Map<String, Any>): Donation {
        val date = when (val d = data["date"]) {
            is Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        return Donation(
            id = data["id"] as? String ?: "",
            requestId = data["requestId"] as? String ?: "",
            donorId = data["donorId"] as? String ?: "",
            hospital = data["hospital"] as? String ?: "",
            bloodGroup = data["bloodGroup"] as? String ?: "",
            date = date,
            status = try {
                DonationStatus.valueOf(data["status"] as? String ?: "PENDING")
            } catch (_: Exception) {
                DonationStatus.PENDING
            },
            confirmedBy = data["confirmedBy"] as? String,
        )
    }
}
