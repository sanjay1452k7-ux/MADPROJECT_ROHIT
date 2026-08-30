package com.cit.attendance.data.local

/**
 * Where a session is persisted between launches.
 *
 * Repositories depend on this rather than on the DataStore implementation, so
 * session behaviour can be tested on the JVM without an Android context, and so
 * the storage mechanism can change without touching auth logic.
 */
interface SessionStorage {
    suspend fun save(session: PersistedSession)
    suspend fun read(): PersistedSession?
    suspend fun clear()
}
