package com.cit.attendance.di

import com.cit.attendance.data.local.SessionStorage
import com.cit.attendance.data.local.SessionStore
import com.cit.attendance.data.repository.FakeAdminRepository
import com.cit.attendance.data.repository.FakeAuthRepository
import com.cit.attendance.data.repository.FakeNotificationRepository
import com.cit.attendance.data.repository.FakeStudentRepository
import com.cit.attendance.data.repository.FakeTeacherRepository
import com.cit.attendance.domain.repository.AdminRepository
import com.cit.attendance.domain.repository.AuthRepository
import com.cit.attendance.domain.repository.NotificationRepository
import com.cit.attendance.domain.repository.StudentRepository
import com.cit.attendance.domain.repository.TeacherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The seam between the domain contracts and their implementations.
 *
 * This module is the *only* place that knows Milestone 1 runs on in-memory
 * data. Milestone 2 rebinds each interface to its Retrofit-backed
 * implementation here and nothing else in the application changes (Section 12).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSessionStorage(impl: SessionStore): SessionStorage

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindStudentRepository(impl: FakeStudentRepository): StudentRepository

    @Binds
    @Singleton
    abstract fun bindTeacherRepository(impl: FakeTeacherRepository): TeacherRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(impl: FakeAdminRepository): AdminRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: FakeNotificationRepository,
    ): NotificationRepository
}
