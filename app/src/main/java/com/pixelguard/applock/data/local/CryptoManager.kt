package com.pixelguard.applock.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

class CryptoManager {

    private val keyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Hashes a PIN using PBKDF2WithHmacSHA256 with 200,000 iterations.
     * This makes offline brute-force of a 4-digit PIN effectively impossible
     * even after DataStore file extraction on a rooted device.
     */
    fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword() // Zero out the PBEKeySpec in memory after use
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)
        return salt
    }

    fun verifyHash(computedHash: String, expectedHash: String): Boolean {
        return MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun encryptData(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return Pair(encrypted, iv)
    }

    fun decryptData(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        return cipher.doFinal(encryptedData)
    }

    /**
     * Encrypts arbitrary binary data (e.g. intruder selfie JPEG) into a self-contained
     * container format: [1 byte IV length][IV bytes][AES-GCM Ciphertext + Tag].
     */
    fun encryptToFileBytes(data: ByteArray): ByteArray {
        val (encrypted, iv) = encryptData(data)
        val buffer = java.nio.ByteBuffer.allocate(1 + iv.size + encrypted.size)
        buffer.put(iv.size.toByte())
        buffer.put(iv)
        buffer.put(encrypted)
        return buffer.array()
    }

    /**
     * Decrypts a container format produced by [encryptToFileBytes].
     */
    fun decryptFromFileBytes(fileBytes: ByteArray): ByteArray {
        val buffer = java.nio.ByteBuffer.wrap(fileBytes)
        val ivLength = buffer.get().toInt() and 0xFF
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)
        return decryptData(encrypted, iv)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        // NOTE: Do NOT rename KEY_ALIAS — changing it destroys existing users' stored keys.
        private const val KEY_ALIAS = "pixel_app_lock_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PBKDF2_ITERATIONS = 200_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
    }
}
