package com.cit.attendance.core.network

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serves a cached copy when the network is unreachable (Section 36).
 *
 * The server decides what may be cached at all: analytics and profile reads
 * carry a short private max-age, while anything describing live attendance
 * state is marked `no-store` and therefore never enters the cache. That
 * division is deliberate - a stale "session active" or "already marked" would
 * be worse than an honest error, because the user would act on it.
 *
 * On a failed request this retries once against the cache alone. If nothing
 * usable is stored the original network failure is rethrown, so the interface
 * still says "no internet connection" rather than showing a blank screen.
 */
@Singleton
class OfflineCacheInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return try {
            chain.proceed(request)
        } catch (networkFailure: IOException) {
            // Only reads can be satisfied from a cache; a write must reach the
            // server or fail loudly.
            if (request.method != "GET") {
                throw networkFailure
            }

            val cachedOnly = request.newBuilder()
                .cacheControl(CacheControl.FORCE_CACHE)
                .build()

            val cached = chain.proceed(cachedOnly)
            if (cached.code == GATEWAY_TIMEOUT_NO_CACHED_RESPONSE) {
                cached.close()
                throw networkFailure
            }
            cached
        }
    }

    private companion object {
        /** OkHttp's response code when FORCE_CACHE finds nothing stored. */
        const val GATEWAY_TIMEOUT_NO_CACHED_RESPONSE = 504
    }
}
