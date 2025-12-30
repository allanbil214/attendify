package com.allan.attendify.di

import com.allan.attendify.data.repository.AttendanceRepositoryImpl
import com.allan.attendify.data.repository.AuthRepositoryImpl
import com.allan.attendify.data.repository.LocationRepositoryImpl
import com.allan.attendify.domain.repository.AttendanceRepository
import com.allan.attendify.domain.repository.AuthRepository
import com.allan.attendify.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(
        attendanceRepositoryImpl: AttendanceRepositoryImpl
    ): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository
}
