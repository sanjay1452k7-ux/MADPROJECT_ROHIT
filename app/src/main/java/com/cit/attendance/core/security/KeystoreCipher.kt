package com.cit.attendance.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-GCM encryption backed by the Android Keystore (Section 34).
 *
 * The key is generated inside the Keystore and is never exportable, so the
 * session token is not recoverable from a stolen backup or from a rooted read
 * of the preferences file. `EncryptedSharedPreferences` was the alternative;
 * it is deprecated in current AndroidX, so the Keystore is used directly and
 * the ciphertext is kept in DataStore.
 *
 * The GCM IV is generated per encryption and stored alongside the ciphertext,
 * which is required for correctness - reusing an IV with GCM is a real break,
 * not a theoretical one.
 */
@Singleton
class KeystoreCipher @Inject constructor() {

    private val keyStore: KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** Returns base64(iv):base64(ciphertext), or null if encryption is unavailable. */
    fun encrypt(plainText: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        encoder.encodeToString(cipher.iv) + SEPARATOR + encoder.encodeToString(cipherText)
    }.getOrNull()

    /**
     * Reverses [encrypt]. Returns null when the payload is malformed or the key
     * has been invalidated - for example after the device lock is reset - which
     * the caller treats as "no stored session" rather than as a crash.
     */
    fun decrypt(payload: String): String? = runCatching {
        val parts = payload.split(SEPARATOR)
        if (parts.size != 2) return null
        val decoder = Base64.getDecoder()
        val iv = decoder.decode(parts[0])
        val cipherText = decoder.decode(parts[1])

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "cit_attendance_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = ":"
        const val TAG_LENGTH_BITS = 128
        const val KEY_SIZE_BITS = 256
    }
}
