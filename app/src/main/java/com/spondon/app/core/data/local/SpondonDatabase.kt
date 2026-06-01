package com.spondon.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spondon.app.core.data.local.dao.NotificationDao
import com.spondon.app.core.data.local.dao.UserDao
import com.spondon.app.core.data.local.entity.NotificationEntity
import com.spondon.app.core.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, NotificationEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SpondonDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun notificationDao(): NotificationDao
}