package com.spondon.app.core.data.repository

import com.google.firebase.Timestamp
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.remote.FirestoreService
import com.spondon.app.core.domain.model.BloodRequest
import com.spondon.app.core.domain.model.RequestStatus
import com.spondon.app.core.domain.model.Urgency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
) : RequestRepository {

    override suspend fun createRequest(request: BloodRequest): Resource<String> {
        val data = mapOf(
            "communityIds" to request.communityIds,
            "requesterId" to request.requesterId,
            "bloodGroup" to request.bloodGroup,
            "urgency" to request.urgency.name,
            "unitsNeeded" to request.unitsNeeded,
            "patientName" to request.patientName,
            "requesterName" to request.requesterName,
            "communityName" to request.communityName,
            "hospital" to request.hospital,
            "address" to request.address,
            "donationDateTime" to request.donationDateTime?.let { Timestamp(it) },
            "contactNumber" to request.contactNumber,
            "respondents" to request.respondents,
            "status" to request.status.name,
            "isPinned" to request.isPinned,
            "createdAt" to Timestamp.now(),
            "expiresAt" to request.expiresAt?.let { Timestamp(it) },
        )
        return firestoreService.createRequest(data)
    }

    override suspend fun getRequest(requestId: String): Resource<BloodRequest> {
        return when (val result = firestoreService.getRequest(requestId)) {
            is Resource.Success -> Resource.Success(mapToBloodRequest(result.data))
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getRequestsForCommunities(
        communityIds: List<String>,
    ): Resource<List<BloodRequest>> {
        return when (val result = firestoreService.getRequestsForCommunities(communityIds)) {
            is Resource.Success -> Resource.Success(result.data.map { mapToBloodRequest(it) })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getMyRequests(userId: String): Resource<List<BloodRequest>> {
        return when (val result = firestoreService.getRequestsByUser(userId)) {
            is Resource.Success -> Resource.Success(result.data.map { mapToBloodRequest(it) })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun respondToRequest(requestId: String, donorId: String): Resource<Unit> {
        return firestoreService.addRespondent(requestId, donorId)
    }

    override suspend fun updateRequestStatus(
        requestId: String,
        status: RequestStatus,
    ): Resource<Unit> {
        return firestoreService.updateRequestStatus(requestId, status.name)
    }

    override fun observeRequests(communityIds: List<String>): Flow<List<BloodRequest>> {
        return firestoreService.observeRequestsForCommunities(communityIds).map { list ->
            list.map { mapToBloodRequest(it) }
        }
    }

    private fun mapToBloodRequest(data: Map<String, Any>): BloodRequest {
        val donationDt = when (val d = data["donationDateTime"]) {
            is Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        val createdAt = when (val d = data["createdAt"]) {
            is Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        val expiresAt = when (val d = data["expiresAt"]) {
            is Timestamp -> d.toDate()
            is Date -> d
            else -> null
        }
        return BloodRequest(
            id = data["id"] as? String ?: "",
            communityIds = (data["communityIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            requesterId = data["requesterId"] as? String ?: "",
            bloodGroup = data["bloodGroup"] as? String ?: "",
            urgency = try {
                Urgency.valueOf(data["urgency"] as? String ?: "NORMAL")
            } catch (_: Exception) { Urgency.NORMAL },
            unitsNeeded = (data["unitsNeeded"] as? Number)?.toInt() ?: 1,
            patientName = data["patientName"] as? String,
            requesterName = data["requesterName"] as? String ?: "",
            communityName = data["communityName"] as? String ?: "",
            hospital = data["hospital"] as? String ?: "",
            address = data["address"] as? String ?: "",
            donationDateTime = donationDt,
            contactNumber = data["contactNumber"] as? String ?: "",
            respondents = (data["respondents"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            status = try {
                RequestStatus.valueOf(data["status"] as? String ?: "ACTIVE")
            } catch (_: Exception) { RequestStatus.ACTIVE },
            isPinned = data["isPinned"] as? Boolean ?: false,
            createdAt = createdAt,
            expiresAt = expiresAt,
        )
    }
}
