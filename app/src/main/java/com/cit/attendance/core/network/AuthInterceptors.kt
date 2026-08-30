package com.cit.attendance.core.network

import com.cit.attendance.core.network.dto.AuthTokensDto
import com.cit.attendance.core.network.dto.RefreshRequest
import com.cit.attendance.data.local.PersistedSession
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/** Attaches the bearer token to every request that has one. */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokens: AuthTokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.accessToken
        val request = if (token == null) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}

/**
 * Refreshes an expired access token once, transparently (Section 34).
 *
 * OkHttp calls this only after a 401. A dedicated bare client is used for the
 * refresh call so it cannot recurse back through this authenticator. If the
 * refresh itself fails the session is cleared, which the application observes
 * and turns into a return to the sign-in screen.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokens: AuthTokenStore,
    private val json: Json,
    private val baseUrl: String,
) : Authenticator {

    private val bareClient = OkHttpClient.Builder().build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Only retry once: if the retried request also 401s, give up rather
        // than looping.
        if (response.priorResponse != null) {
            return null
        }

        val refresh = tokens.refreshToken ?: return null
        val userId = tokens.userId ?: return null
        val role = response.request.header(HEADER_ROLE)

        val refreshed = runCatching { requestNewTokens(refresh) }.getOrNull()
        if (refreshed == null) {
            tokens.clearBlocking()
            return null
        }

        tokens.saveBlocking(
            PersistedSession(
                userId = refreshed.user.id.ifEmpty { userId },
                role = role ?: refreshed.user.role,
                token = refreshed.accessToken,
                refreshToken = refreshed.refreshToken,
            ),
        )

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${refreshed.accessToken}")
            .build()
    }

    private fun requestNewTokens(refreshToken: String): AuthTokensDto? {
        val payload = json.encodeToString(RefreshRequest(refreshToken))
        val request = Request.Builder()
            .url(baseUrl + "auth/refresh")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        bareClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body.string()
            if (body.isEmpty()) return null
            return json.decodeFromString<AuthTokensDto>(body)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val HEADER_ROLE = "X-Cit-Role"
    }
}
