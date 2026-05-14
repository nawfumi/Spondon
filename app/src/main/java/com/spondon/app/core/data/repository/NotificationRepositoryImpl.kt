package com.spondon.app.core.data.repository

import com.google.firebase.Timestamp
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
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
) : NotificationRepository {

    override suspend fun getNotifications(userId: String): Resource<List<AppNotification>> {
        return when (val result = firestoreService.getNotifications(userId)) {
            is Resource.Success -> Resource.Success(result.data.map { it.toAppNotification() })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return firestoreService.markNotificationRead(notificationId)
    }

    override suspend fun markAllAsRead(userId: String): Resource<Unit> {
        return firestoreService.markAllNotificationsRead(userId)
    }

    override suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return firestoreService.deleteNotification(notificationId)
    }

    override fun observeUnreadCount(userId: String): Flow<Int> = callbackFlow {
        if (userId.isBlank()) {
            trySend(0)
            awaitClose()
            return@callbackFlow
        }
        val listener = firestore.collection(Constants.NOTIFICATIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> {
        return firestoreService.observeNotifications(userId).map { list ->
            list.map { it.toAppNotification() }
        }
    }

    /**
     * Creates a notification in the top-level `notifications` collection.
     * The `userId` field is used by [getNotifications] and [observeUnreadCount]
     * to query notifications for a specific user.
     *
     * Used by [SpondonFCMService] and for in-app trigger events.
     */
    suspend fun createNotification(
        userId: String,
        type: NotificationType,
        title: String,
        body: String,
        deepLink: String = "",
    ): Resource<String> {
        return try {
            val data = hashMapOf(
                "userId" to userId,
                "type" to type.name,
                "title" to title,
                "body" to body,
                "deepLink" to deepLink,
                "isRead" to false,
                "createdAt" to Timestamp.now(),
            )
            val docRef = firestore.collection(Constants.NOTIFICATIONS_COLLECTION)
                .add(data)
                .await()
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

    /**
     * Deletes all notifications older than 30 days from Firestore.
     * This is a server-side cleanup — notifications are removed from the database,
     * not just hidden in the app. Runs silently; errors are swallowed.
     */
    suspend fun deleteOldNotifications(userId: String) {
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
