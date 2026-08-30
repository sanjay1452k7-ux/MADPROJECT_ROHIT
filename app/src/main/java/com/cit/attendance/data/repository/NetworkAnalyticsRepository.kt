package com.cit.attendance.data.repository

import android.content.Context
import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.common.map
import com.cit.attendance.core.network.ApiErrorMapper
import com.cit.attendance.core.network.AttendanceApi
import com.cit.attendance.core.network.dto.GenerateReportRequest
import com.cit.attendance.data.mapper.toDomain
import com.cit.attendance.domain.model.AdminAnalytics
import com.cit.attendance.domain.model.GeneratedReport
import com.cit.attendance.domain.model.ReportFormat
import com.cit.attendance.domain.model.ReportType
import com.cit.attendance.domain.model.StudentAnalytics
import com.cit.attendance.domain.model.TeacherAnalytics
import com.cit.attendance.domain.repository.AnalyticsRepository
import com.cit.attendance.domain.repository.ReportsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkAnalyticsRepository @Inject constructor(
    private val api: AttendanceApi,
    private val errors: ApiErrorMapper,
) : AnalyticsRepository {

    override suspend fun studentAnalytics(): Outcome<StudentAnalytics> =
        errors.execute { api.studentAnalytics() }.map { it.toDomain() }

    override suspend fun teacherAnalytics(): Outcome<TeacherAnalytics> =
        errors.execute { api.teacherAnalytics() }.map { it.toDomain() }

    override suspend fun adminAnalytics(): Outcome<AdminAnalytics> =
        errors.execute { api.adminAnalytics() }.map { it.toDomain() }
}

/**
 * Downloads a rendered report and writes it where the user can open it
 * (Section 33).
 *
 * Files go to the app's external files directory so a viewer app can be handed
 * a content URI for them; the streaming copy happens off the main thread.
 */
@Singleton
class NetworkReportsRepository @Inject constructor(
    private val api: AttendanceApi,
    private val errors: ApiErrorMapper,
    @param:ApplicationContext private val context: Context,
) : ReportsRepository {

    override suspend fun generate(
        type: ReportType,
        format: ReportFormat,
        subjectId: String?,
        classSectionId: String?,
    ): Outcome<GeneratedReport> {
        val response = errors.execute {
            api.generateReport(
                GenerateReportRequest(
                    type = type.name,
                    format = format.name,
                    subjectId = subjectId,
                    classSectionId = classSectionId,
                ),
            )
        }

        return when (response) {
            is Outcome.Failure -> response
            is Outcome.Success -> {
                val raw = response.data
                if (!raw.isSuccessful) {
                    return Outcome.Failure(AppError.Unexpected("report-${raw.code()}"))
                }
                val body = raw.body()
                    ?: return Outcome.Failure(AppError.Unexpected("report-empty"))

                // Prefer the server's filename; fall back to a dated one so the
                // file is still identifiable if the header is missing.
                val fileName = raw.headers()["X-Report-Filename"]
                    ?: "${type.name.lowercase()}-${LocalDate.now()}.${format.extension}"

                withContext(Dispatchers.IO) {
                    runCatching {
                        val directory = File(context.getExternalFilesDir(null), "reports")
                        directory.mkdirs()
                        val file = File(directory, fileName)
                        body.byteStream().use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                        Outcome.Success(
                            GeneratedReport(
                                fileName = fileName,
                                absolutePath = file.absolutePath,
                                sizeBytes = file.length(),
                                format = format,
                            ),
                        )
                    }.getOrElse { Outcome.Failure(AppError.Unexpected("report-write")) }
                }
            }
        }
    }
}
