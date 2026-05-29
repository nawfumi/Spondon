package com.spondon.app.di

import com.spondon.app.core.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindCommunityRepository(impl: CommunityRepositoryImpl): CommunityRepository

    @Binds @Singleton
    abstract fun bindRequestRepository(impl: RequestRepositoryImpl): RequestRepository

    @Binds @Singleton
    abstract fun bindDonorRepository(impl: DonorRepositoryImpl): DonorRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds @Singleton
    abstract fun bindTipsRepository(impl: TipsRepositoryImpl): TipsRepository

    @Binds @Singleton
    abstract fun bindEligibilityRepository(impl: EligibilityRepositoryImpl): EligibilityRepository
}