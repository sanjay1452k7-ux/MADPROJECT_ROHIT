package com.cit.attendance.di

import android.content.Context
import com.cit.attendance.BuildConfig
import com.cit.attendance.core.network.AttendanceApi
import com.cit.attendance.core.network.AuthInterceptor
import com.cit.attendance.core.network.AuthTokenStore
import com.cit.attendance.core.network.OfflineCacheInterceptor
import com.cit.attendance.core.network.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Networking setup.
 *
 * The base URL is build configuration, not a literal: debug builds point at a
 * local service, release builds at HTTPS (Section 46).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // The server may add fields ahead of the client; that must not break it.
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokens: AuthTokenStore,
        json: Json,
    ): TokenAuthenticator = TokenAuthenticator(tokens, json, BuildConfig.API_BASE_URL)

    /**
     * A small disk cache. Only responses the server marks cacheable land here;
     * live attendance state is sent `no-store` and never does (Section 36).
     */
    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http"), HTTP_CACHE_BYTES)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
        offlineCache: OfflineCacheInterceptor,
        cache: Cache,
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(cache)
        // Outermost, so it can retry against the cache after any inner
        // interceptor has failed to reach the network.
        .addInterceptor(offlineCache)
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .apply {
            if (BuildConfig.DEBUG) {
                // Headers only: bodies carry tokens and student records, which
                // have no business in logcat even during development.
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    },
                )
            }
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAttendanceApi(retrofit: Retrofit): AttendanceApi =
        retrofit.create(AttendanceApi::class.java)

    /** Large enough for the cacheable reads, small enough to be polite. */
    private const val HTTP_CACHE_BYTES = 8L * 1024 * 1024
}
