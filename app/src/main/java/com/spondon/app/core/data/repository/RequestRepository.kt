package com.spondon.app.core.data.repository

import com.spondon.app.core.common.Resource
import com.spondon.app.core.domain.model.BloodRequest
import com.spondon.app.core.domain.model.RequestStatus
import kotlinx.coroutines.flow.Flow

interface RequestRepository {
    suspend fun createRequest(request: BloodRequest): Resource<String>
    suspend fun getRequest(requestId: String): Resource<BloodRequest>
    suspend fun getRequestsForCommunities(communityIds: List<String>): Resource<List<BloodRequest>>
    suspend fun getMyRequests(userId: String): Resource<List<BloodRequest>>
    suspend fun respondToRequest(requestId: String, donorId: String): Resource<Unit>
    suspend fun updateRequestStatus(requestId: String, status: RequestStatus): Resource<Unit>
    suspend fun deleteRequest(requestId: String): Resource<Unit>
    fun observeRequests(communityIds: List<String>): Flow<List<BloodRequest>>
}