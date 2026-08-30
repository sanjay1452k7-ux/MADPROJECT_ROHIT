package com.cit.attendance.di

import com.cit.attendance.BuildConfig
import com.cit.attendance.domain.config.InstitutionConfig
import com.cit.attendance.domain.model.AttendancePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-wide configuration.
 *
 * Institution identity and the attendance threshold come from BuildConfig, so
 * they are build-time configuration rather than literals in feature code
 * (Sections 9 and 30).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideInstitutionConfig(): InstitutionConfig = InstitutionConfig(
        name = BuildConfig.INSTITUTION_NAME,
        emailDomain = BuildConfig.INSTITUTION_EMAIL_DOMAIN,
        attendanceThresholdPercent = BuildConfig.ATTENDANCE_THRESHOLD_PERCENT,
    )

    @Provides
    @Singleton
    fun provideAttendancePolicy(config: InstitutionConfig): AttendancePolicy =
        AttendancePolicy(thresholdPercent = config.attendanceThresholdPercent)
}
