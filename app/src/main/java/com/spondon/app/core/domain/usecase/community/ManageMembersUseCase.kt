package com.spondon.app.core.domain.usecase.community

import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.CommunityRepository
import com.spondon.app.core.domain.model.CommunityRole
import javax.inject.Inject

/** Use case for managing community members — join, leave, approve, reject, promote, remove. */
class ManageMembersUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend fun joinCommunity(communityId: String, userId: String): Resource<Unit> {
        return repository.joinCommunity(communityId, userId)
    }

    suspend fun requestToJoin(
        communityId: String,
        userId: String,
        message: String,
        serialId: String? = null,
    ): Resource<Unit> {
        return repository.requestToJoin(communityId, userId, message, serialId)
    }

    suspend fun approveMember(communityId: String, userId: String): Resource<Unit> {
        return repository.approveMember(communityId, userId)
    }

    suspend fun rejectMember(communityId: String, userId: String): Resource<Unit> {
        return repository.rejectMember(communityId, userId)
    }

    suspend fun removeMember(communityId: String, userId: String): Resource<Unit> {
        return repository.removeMember(communityId, userId)
    }

    suspend fun promoteMember(
        communityId: String,
        userId: String,
        role: CommunityRole,
    ): Resource<Unit> {
        return repository.promoteMember(communityId, userId, role)
    }
}