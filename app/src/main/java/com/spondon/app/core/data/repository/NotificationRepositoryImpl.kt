package com.spondon.app.core.data.repository

import com.google.firebase.Timestamp
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.local.dao.NotificationDao
import com.spondon.app.core.data.local.entity.NotificationEntity
import com.spondon.app.core.data.remote.FirestoreService
import com.spondon.app.core.domain.model.AppNotification
import com.spondon.app.core.domain.model.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val firestore: FirebaseFirestore,
    private val notificationDao: NotificationDao,
) : NotificationRepository {

    override suspend fun getNotifications(userId: String): Resource<List<AppNotification>> {
        return when (val result = firestoreService.getNotifications(userId)) {
            is Resource.Success -> {
                val notifications = result.data.map { it.toAppNotification() }
                // Sync to local DB
                notificationDao.insertAll(notifications.map { it.toEntity(userId) })
                Resource.Success(notifications)
            }
            is Resource.Error -> {
                // Fall back to local data
                val local = notificationDao.getAllForUser(userId).map { it.toAppNotification() }
                if (local.isNotEmpty()) Resource.Success(local)
                else Resource.Error(result.message)
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun markAsRead(notificationId: String): Resource<Unit> {
        // Update local DB immediately
        notificationDao.markAsRead(notificationId)
        // Then update Firebase
        return firestoreService.markNotificationRead(notificationId)
    }

    override suspend fun markAllAsRead(userId: String): Resource<Unit> {
        // Update local DB immediately
        notificationDao.markAllAsRead(userId)
        // Then update Firebase
        return firestoreService.markAllNotificationsRead(userId)
    }

    override suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        // Delete from local DB immediately
        notificationDao.delete(notificationId)
        // Then delete from Firebase
        return firestoreService.deleteNotification(notificationId)
    }

    override fun observeUnreadCount(userId: String): Flow<Int> {
        if (userId.isBlank()) return callbackFlow {
            trySend(0)
            awaitClose()
        }
        // Use local DB for unread count (more reliable, works offline)
        return notificationDao.observeUnreadCount(userId)
    }

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        // Observe from local DB — the single source of truth.
        // Sync happens via getNotifications() called from the ViewModel init,
        // and from SpondonFCMService / NotificationObserver when new data arrives.
        return notificationDao.observeNotifications(userId).map { entities ->
            entities.map { it.toAppNotification() }
        }
    }

    /**
     * Creates a notification in the top-level `notifications` collection.
     * The `userId` field is used by [getNotifications] and [observeUnreadCount]
     * to query notifications for a specific user.
     *
     * Used by [SpondonFCMService] and for in-app trigger events.
     */
    override suspend fun createNotification(
        userId: String,
        type: NotificationType,
        title: String,
        body: String,
        deepLink: String,
    ): Resource<String> {
        return try {
            val now = Timestamp.now()
            val data = hashMapOf(
                "userId" to userId,
                "type" to type.name,
                "title" to title,
                "body" to body,
                "deepLink" to deepLink,
                "isRead" to false,
                "createdAt" to now,
            )
            val docRef = firestore.collection(Constants.NOTIFICATIONS_COLLECTION)
                .add(data)
                .await()

            // Also save to local DB immediately
            notificationDao.insert(
                NotificationEntity(
                    id = docRef.id,
                    type = type.name,
                    title = title,
                    body = body,
                    deepLink = deepLink,
                    isRead = false,
                    createdAt = now.toDate().time,
                    userId = userId,
                )
            )

            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create notification", e)
        }
    }


    /**
     * Send notification to a list of specific users.
     * Used for blood group targeted requests and admin broadcasts.
     */
    override suspend fun sendNotificationToUsers(
        userIds: List<String>,
        type: NotificationType,
        title: String,
        body: String,
        deepLink: String,
    ): Resource<Unit> {
        return try {
            userIds.forEach { userId ->
                createNotification(
                    userId = userId,
                    type = type,
                    title = title,
                    body = body,
                    deepLink = deepLink,
                )
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send notifications", e)
        }
    }


    private fun Map<String, Any>.toAppNotification(): AppNotification {
        return AppNotification(
            id = this["id"] as? String ?: "",
            type = try {
                NotificationType.valueOf(this["type"] as? String ?: "REQUEST")
            } catch (_: Exception) {
                NotificationType.REQUEST
            },
            title = this["title"] as? String ?: "",
            body = this["body"] as? String ?: "",
            deepLink = this["deepLink"] as? String ?: "",
            isRead = this["isRead"] as? Boolean ?: false,
            createdAt = when (val d = this["createdAt"]) {
                is Timestamp -> d.toDate()
                is Date -> d
                else -> null
            },
        )
    }

    private fun NotificationEntity.toAppNotification(): AppNotification {
        return AppNotification(
            id = id,
            type = try {
                NotificationType.valueOf(type)
            } catch (_: Exception) {
                NotificationType.REQUEST
            },
            title = title,
            body = body,
            deepLink = deepLink,
            isRead = isRead,
            createdAt = if (createdAt > 0) Date(createdAt) else null,
        )
    }

    private fun AppNotification.toEntity(userId: String): NotificationEntity {
        return NotificationEntity(
            id = id,
            type = type.name,
            title = title,
            body = body,
            deepLink = deepLink,
            isRead = isRead,
            createdAt = createdAt?.time ?: 0L,
            userId = userId,
        )
    }

    /**
     * Deletes all notifications older than 30 days from Firestore.
     * Notifications remain in the local Room database so users keep them
     * on device until the app is uninstalled.
     */
    override suspend fun deleteOldNotifications(userId: String) {
        try {
            val thirtyDaysAgo = Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
            val cutoff = Timestamp(thirtyDaysAgo)

            val docs = firestore.collection(Constants.NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereLessThan("createdAt", cutoff)
                .get()
                .await()

            if (docs.isEmpty) return

            // Firestore batch supports max 500 writes at a time
            docs.documents.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
        } catch (_: Exception) {
            // Silently fail — cleanup is best-effort
        }
    }
}
