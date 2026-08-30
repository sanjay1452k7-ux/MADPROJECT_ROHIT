package com.cit.attendance.data.repository

import com.cit.attendance.data.local.PersistedSession
import com.cit.attendance.data.local.SessionStorage

/**
 * A test double for session persistence.
 *
 * The production implementation encrypts through the Android Keystore, which is
 * unavailable on the JVM; this stands in so auth behaviour can be tested
 * without an emulator.
 */
class InMemorySessionStorage(
    private var stored: PersistedSession? = null,
) : SessionStorage {

    var saveCount: Int = 0
        private set
    var clearCount: Int = 0
        private set

    override suspend fun save(session: PersistedSession) {
        stored = session
        saveCount++
    }

    override suspend fun read(): PersistedSession? = stored

    override suspend fun clear() {
        stored = null
        clearCount++
    }
}
