package com.cit.attendance.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cit.attendance.core.security.KeystoreCipher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cit_session",
)

/** What is persisted about a signed-in user between launches. */
data class PersistedSession(
    val userId: String,
    val role: String,
    val token: String,
)

/**
 * Persists the session token at rest, encrypted (Section 34).
 *
 * Only the token is treated as a secret; the user id and role are stored in
 * clear because they carry no authority on their own - the server decides what
 * a token may do. Milestone 2 stores a real JWT here without changing this API.
 */
@Singleton
class SessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cipher: KeystoreCipher,
) : SessionStorage {

    override suspend fun save(session: PersistedSession) {
        val encryptedToken = cipher.encrypt(session.token) ?: return
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_ROLE] = session.role
            prefs[KEY_TOKEN] = encryptedToken
        }
    }

    /** Returns the stored session, or null when absent or undecryptable. */
    override suspend fun read(): PersistedSession? {
        val prefs = context.sessionDataStore.data.first()
        val userId = prefs[KEY_USER_ID] ?: return null
        val role = prefs[KEY_ROLE] ?: return null
        val token = prefs[KEY_TOKEN]?.let(cipher::decrypt) ?: return null
        return PersistedSession(userId = userId, role = role, token = token)
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_ROLE = stringPreferencesKey("role")
        val KEY_TOKEN = stringPreferencesKey("token")
    }
}
