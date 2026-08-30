package com.cit.attendance.core.network

import com.cit.attendance.data.local.PersistedSession
import com.cit.attendance.data.local.SessionStorage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the live credentials for the signed-in user.
 *
 * OkHttp interceptors run off the main thread and are not suspending, so the
 * tokens are cached in memory here and only written through to encrypted
 * storage when they change. That keeps the request path free of blocking reads.
 */
@Singleton
class AuthTokenStore @Inject constructor(
    private val storage: SessionStorage,
) {
    private val mutex = Mutex()

    @Volatile
    private var cached: PersistedSession? = null

    val accessToken: String?
        get() = cached?.token

    val refreshToken: String?
        get() = cached?.refreshToken

    val userId: String?
        get() = cached?.userId

    /** Loads persisted credentials into memory on cold start. */
    suspend fun restore(): PersistedSession? {
        val persisted = storage.read()
        cached = persisted
        return persisted
    }

    suspend fun save(session: PersistedSession) {
        mutex.withLock {
            cached = session
            storage.save(session)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            cached = null
            storage.clear()
        }
    }

    /**
     * Blocking variant used by the OkHttp authenticator, which has no coroutine
     * context of its own. It runs on OkHttp's own background thread.
     */
    fun saveBlocking(session: PersistedSession) {
        runBlocking { save(session) }
    }

    fun clearBlocking() {
        runBlocking { clear() }
    }
}
