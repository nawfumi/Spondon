package com.spondon.app.core.data.repository

import com.spondon.app.core.common.Resource
import com.spondon.app.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(userId: String): Resource<User>
    /** Batch-fetches multiple users in a single Firestore operation. */
    suspend fun getUsers(userIds: List<String>): Resource<List<User>>
    suspend fun updateUser(user: User): Resource<Unit>
    suspend fun updateFcmToken(token: String): Resource<Unit>
    suspend fun deleteAccount(): Resource<Unit>
    fun observeUser(userId: String): Flow<User>
}
