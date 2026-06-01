package com.spondon.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String = "REQUEST",
    val title: String = "",
    val body: String = "",
    val deepLink: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
    val userId: String = "",
)
