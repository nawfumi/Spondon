package com.spondon.app.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.local.dao.NotificationDao
import com.spondon.app.core.data.repository.NotificationRepository
import com.spondon.app.core.data.repository.NotificationRepositoryImpl
import com.spondon.app.core.domain.model.AppNotification
import com.spondon.app.core.domain.model.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class NotificationState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationRepositoryImpl: NotificationRepositoryImpl,
    private val notificationDao: NotificationDao,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val currentUserId get() = auth.currentUser?.uid ?: ""

    private val _state = MutableStateFlow(NotificationState())
    val state: StateFlow<NotificationState> = _state.asStateFlow()

    init {
        observeUnread()
        observeNotificationsList()
        syncFirebaseToLocal()
        // Clean up notifications older than 30 days from Firestore
        // (they remain in local Room database on device)
        cleanupOldNotifications()
    }

    private fun cleanupOldNotifications() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            notificationRepositoryImpl.deleteOldNotifications(currentUserId)
        }
    }

    /**
     * Syncs Firebase notifications to local Room database.
     * This ensures that notifications received while the app was closed
     * are captured locally before Firebase's 30-day cleanup removes them.
     */
    private fun syncFirebaseToLocal() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch {
            try {
                val result = notificationRepository.getNotifications(currentUserId)
                // getNotifications already syncs to local DB in the impl
            } catch (_: Exception) {
                // Best-effort sync
            }
        }
    }

    private fun observeUnread() {
        viewModelScope.launch {
            notificationRepository.observeUnreadCount(currentUserId)
                .collect { count ->
                    _state.update { it.copy(unreadCount = count) }
                }
        }
    }

    private fun observeNotificationsList() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            notificationRepository.observeNotifications(currentUserId)
                .collect { notifications ->
                    _state.update { it.copy(notifications = notifications, isLoading = false, error = null) }
                }
        }
    }

    fun loadNotifications() { /* real-time observer via observeNotificationsList handles updates */ }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
            _state.update { s ->
                s.copy(notifications = s.notifications.map { n ->
                    if (n.id == notificationId) n.copy(isRead = true) else n
                })
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead(currentUserId)
            _state.update { s ->
                s.copy(notifications = s.notifications.map { it.copy(isRead = true) })
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
            _state.update { s ->
                s.copy(notifications = s.notifications.filter { it.id != notificationId })
            }
        }
    }
}
