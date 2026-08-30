package com.cit.attendance.core.network

import com.cit.attendance.core.network.dto.ActiveSessionDto
import com.cit.attendance.core.network.dto.AdminDashboardDto
import com.cit.attendance.core.network.dto.AttendanceRecordDto
import com.cit.attendance.core.network.dto.AuthTokensDto
import com.cit.attendance.core.network.dto.AuthUserDto
import com.cit.attendance.core.network.dto.LoginRequest
import com.cit.attendance.core.network.dto.ManualAttendanceRequest
import com.cit.attendance.core.network.dto.MarkAttendanceRequest
import com.cit.attendance.core.network.dto.MarkResultDto
import com.cit.attendance.core.network.dto.MintedTokenDto
import com.cit.attendance.core.network.dto.NotificationDto
import com.cit.attendance.core.network.dto.RefreshRequest
import com.cit.attendance.core.network.dto.SessionSummaryDto
import com.cit.attendance.core.network.dto.StartSessionRequest
import com.cit.attendance.core.network.dto.StudentDashboardDto
import com.cit.attendance.core.network.dto.StudentDto
import com.cit.attendance.core.network.dto.TeacherDashboardDto
import com.cit.attendance.core.network.dto.VerifiedStudentDto
import com.cit.attendance.core.network.dto.VerifyTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * The attendance service contract.
 *
 * No endpoint takes a student or teacher identifier: the server derives the
 * caller from the access token, so a client cannot ask for someone else's data
 * by changing a path (Section 34).
 */
interface AttendanceApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthTokensDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthTokensDto

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    /** Re-reads the identity behind the stored token, validating it in passing. */
    @GET("auth/me")
    suspend fun me(): AuthUserDto

    // ------------------------------------------------------------ student

    @GET("students/me/dashboard")
    suspend fun studentDashboard(): StudentDashboardDto

    @GET("students/me/profile")
    suspend fun studentProfile(): StudentDto

    @GET("students/me/attendance")
    suspend fun studentAttendance(): List<AttendanceRecordDto>

    /** Returns an empty body when no session is open, which Retrofit maps to null. */
    @GET("students/me/active-session")
    suspend fun activeSession(): ActiveSessionDto?

    /** Section 19: the QR credential is always minted by the server. */
    @POST("attendance-sessions/{id}/token")
    suspend fun mintAttendanceToken(@Path("id") sessionId: String): MintedTokenDto

    // ------------------------------------------------------------ teacher

    @GET("teachers/me/dashboard")
    suspend fun teacherDashboard(): TeacherDashboardDto

    @POST("attendance-sessions")
    suspend fun startSession(@Body body: StartSessionRequest): SessionSummaryDto

    @GET("attendance-sessions/{id}")
    suspend fun sessionDetail(@Path("id") sessionId: String): SessionSummaryDto

    @POST("attendance-sessions/{id}/close")
    suspend fun closeSession(@Path("id") sessionId: String): SessionSummaryDto

    /** Scan step: validates and identifies, but records nothing (Section 17). */
    @POST("attendance-sessions/{id}/verify")
    suspend fun verifyToken(
        @Path("id") sessionId: String,
        @Body body: VerifyTokenRequest,
    ): VerifiedStudentDto

    /** Confirmation step, after the teacher has looked at the student. */
    @POST("attendance-sessions/{id}/mark")
    suspend fun markAttendance(
        @Path("id") sessionId: String,
        @Body body: MarkAttendanceRequest,
    ): MarkResultDto

    @POST("attendance-sessions/{id}/manual")
    suspend fun markManually(
        @Path("id") sessionId: String,
        @Body body: ManualAttendanceRequest,
    ): Response<Unit>

    // -------------------------------------------------------------- admin

    @GET("admin/dashboard")
    suspend fun adminDashboard(): AdminDashboardDto

    // ------------------------------------------------------ notifications

    @GET("notifications")
    suspend fun notifications(): List<NotificationDto>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>
}
