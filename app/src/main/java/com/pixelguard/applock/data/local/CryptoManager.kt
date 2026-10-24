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

    private val keyLock = Any()

    private val keyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    private fun getOrCreateKey(): SecretKey = synchronized(keyLock) {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        // Titan M2 / StrongBox Hardware Security Module (SEC-007):
        // Attempt dedicated StrongBox hardware-isolated key generation; gracefully fallback to TEE
        return try {
            val strongBoxSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .setIsStrongBoxBacked(true)
                .build()
            keyGenerator.init(strongBoxSpec)
            keyGenerator.generateKey()
        } catch (_: Exception) {
            // Standard TEE fallback when StrongBox is unavailable (e.g. emulators)
            val teeSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(teeSpec)
            keyGenerator.generateKey()
        }
    }

    /**
     * Hashes a PIN using PBKDF2WithHmacSHA256 with 200,000 iterations.
     * Memory Hygiene (SEC-006 & PG-SEC-13): Zeroes out PBEKeySpec, caller's CharArray buffer,
     * and intermediate derived key byte arrays.
     */
    fun hashPin(pin: CharArray, salt: ByteArray): String {
        val pinCopy = pin.clone()
        val spec = PBEKeySpec(
            pinCopy,
            salt,
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword() // Zero out the PBEKeySpec internal buffer
        pinCopy.fill('0')
        pin.fill('0') // Zero out caller's char buffer
        val hexString = hash.joinToString("") { "%02x".format(it) }
        hash.fill(0) // Zero out intermediate derived key bytes from memory (PG-SEC-13)
        return hexString
    }

    fun hashPin(pin: String, salt: ByteArray): String {
        val chars = pin.toCharArray()
        return hashPin(chars, salt)
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
     * PG-SEC-07: Validates buffer length and IV boundaries to eliminate BufferUnderflowException.
     */
    fun decryptFromFileBytes(fileBytes: ByteArray): ByteArray {
        // Minimal valid AES-GCM envelope: 1 byte (ivLen) + 12 bytes (IV) + 16 bytes (auth tag) = 29 bytes
        if (fileBytes.size < 1 + 12 + 16) {
            throw IllegalArgumentException("Truncated or corrupted ciphertext container: size=${fileBytes.size}")
        }
        val buffer = java.nio.ByteBuffer.wrap(fileBytes)
        val ivLength = buffer.get().toInt() and 0xFF
        if (ivLength != 12 || buffer.remaining() < ivLength + 16) {
            throw IllegalArgumentException("Invalid IV length or insufficient ciphertext payload: ivLength=$ivLength, remaining=${buffer.remaining()}")
        }
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)
        return decryptData(encrypted, iv)
    }

    /**
     * Encrypts a string (e.g. pin_hash, pin_salt) using the Titan M2 Keystore key
     * and returns a base64-encoded encrypted container with format prefix "enc:".
     */
    fun encryptString(plaintext: String): String {
        val bytes = plaintext.toByteArray(Charsets.UTF_8)
        val encContainer = encryptToFileBytes(bytes)
        return "enc:" + base64Encode(encContainer)
    }

    /**
     * Decrypts a string produced by [encryptString].
     * If the string does not have the "enc:" prefix, it is treated as a legacy plaintext string.
     */
    fun decryptString(storedValue: String): String {
        if (!storedValue.startsWith("enc:")) {
            return storedValue // Legacy plaintext fallback
        }
        val base64Payload = storedValue.removePrefix("enc:")
        val encBytes = base64Decode(base64Payload)
        val decBytes = decryptFromFileBytes(encBytes)
        return String(decBytes, Charsets.UTF_8)
    }

    private fun base64Encode(bytes: ByteArray): String {
        return try {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun base64Decode(str: String): ByteArray {
        return try {
            android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(str)
        }
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
