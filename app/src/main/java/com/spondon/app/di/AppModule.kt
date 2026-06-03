package com.spondon.app.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.spondon.app.core.data.local.SpondonDatabase
import com.spondon.app.core.data.local.dao.NotificationDao
import com.spondon.app.core.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides @Singleton
    fun provideSpondonDatabase(@ApplicationContext context: Context): SpondonDatabase {
        return Room.databaseBuilder(
                context,
                SpondonDatabase::class.java,
                "spondon_database"
            ).fallbackToDestructiveMigration(false).build()
    }

    @Provides @Singleton
    fun provideUserDao(database: SpondonDatabase): UserDao = database.userDao()

    @Provides @Singleton
    fun provideNotificationDao(database: SpondonDatabase): NotificationDao = database.notificationDao()
}