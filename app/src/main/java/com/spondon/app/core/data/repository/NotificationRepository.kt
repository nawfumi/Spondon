package com.spondon.app.core.data.repository

import com.spondon.app.core.common.Resource
import com.spondon.app.core.domain.model.AppNotification
import com.spondon.app.core.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun getNotifications(userId: String): Resource<List<AppNotification>>
    suspend fun markAsRead(notificationId: String): Resource<Unit>
    suspend fun markAllAsRead(userId: String): Resource<Unit>
    suspend fun deleteNotification(notificationId: String): Resource<Unit>
    fun observeUnreadCount(userId: String): Flow<Int>
    fun observeNotifications(userId: String): Flow<List<AppNotification>>
    suspend fun sendNotificationToUsers(
        userIds: List<String>,
        type: NotificationType,
        title: String,
        body: String,
        deepLink: String = "",
    ): Resource<Unit>
    suspend fun createNotification(
        userId: String,
        type: NotificationType,
        title: String,
        body: String,
        deepLink: String = "",
    ): Resource<String>
    suspend fun deleteOldNotifications(userId: String)
}
