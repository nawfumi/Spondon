package com.spondon.app.core.data.repository

import com.spondon.app.core.common.Resource
import com.spondon.app.core.domain.model.Community
import com.spondon.app.core.domain.model.CommunityRole

interface CommunityRepository {
    suspend fun getCommunities(): Resource<List<Community>>
    suspend fun getMyCommunities(userId: String): Resource<List<Community>>
    suspend fun getCommunity(communityId: String): Resource<Community>
    suspend fun createCommunity(community: Community): Resource<String>
    suspend fun updateCommunity(community: Community): Resource<Unit>
    suspend fun joinCommunity(communityId: String, userId: String): Resource<Unit>
    suspend fun requestToJoin(communityId: String, userId: String, message: String, serialId: String? = null): Resource<Unit>
    suspend fun approveMember(communityId: String, userId: String): Resource<Unit>
    suspend fun rejectMember(communityId: String, userId: String): Resource<Unit>
    suspend fun removeMember(communityId: String, userId: String): Resource<Unit>
    suspend fun promoteMember(communityId: String, userId: String, role: CommunityRole): Resource<Unit>
}