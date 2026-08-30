package com.cit.attendance.di

import com.cit.attendance.data.local.SessionStorage
import com.cit.attendance.data.local.SessionStore
import com.cit.attendance.data.repository.NetworkAdminRepository
import com.cit.attendance.data.repository.NetworkAnalyticsRepository
import com.cit.attendance.data.repository.NetworkAttendanceRepository
import com.cit.attendance.data.repository.NetworkAuthRepository
import com.cit.attendance.data.repository.NetworkNotificationRepository
import com.cit.attendance.data.repository.NetworkReportsRepository
import com.cit.attendance.data.repository.NetworkStudentRepository
import com.cit.attendance.data.repository.NetworkTeacherRepository
import com.cit.attendance.domain.repository.AdminRepository
import com.cit.attendance.domain.repository.AnalyticsRepository
import com.cit.attendance.domain.repository.AttendanceRepository
import com.cit.attendance.domain.repository.AuthRepository
import com.cit.attendance.domain.repository.NotificationRepository
import com.cit.attendance.domain.repository.ReportsRepository
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
 * Milestone 1 bound in-memory fakes here. Milestone 2 rebinds the same
 * interfaces to their Retrofit-backed implementations - and that was the whole
 * change: no use case, view model or screen was touched to move the app onto a
 * real backend (Section 12).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSessionStorage(impl: SessionStore): SessionStorage

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: NetworkAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindStudentRepository(impl: NetworkStudentRepository): StudentRepository

    @Binds
    @Singleton
    abstract fun bindTeacherRepository(impl: NetworkTeacherRepository): TeacherRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(impl: NetworkAdminRepository): AdminRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NetworkNotificationRepository,
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(
        impl: NetworkAttendanceRepository,
    ): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        impl: NetworkAnalyticsRepository,
    ): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindReportsRepository(impl: NetworkReportsRepository): ReportsRepository
}
