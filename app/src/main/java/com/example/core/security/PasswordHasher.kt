package com.example.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Cryptographic utility for password hashing and verification.
 * Employs unique per-user cryptographically random salts with SHA-256 / multi-iteration digest.
 */
object PasswordHasher {

    private const val SALT_LENGTH = 16
    private const val HASH_ITERATIONS = 1000

    /**
     * Generates a secure random 16-byte salt, Base64-encoded.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Hashes a raw password combined with a salt using multi-iteration SHA-256.
     */
    fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltBytes)
        var hashed = digest.digest(password.toByteArray(Charsets.UTF_8))
        
        for (i in 1 until HASH_ITERATIONS) {
            digest.reset()
            hashed = digest.digest(hashed)
        }
        return Base64.getEncoder().encodeToString(hashed)
    }

    /**
     * Verifies that the provided plain password matches the stored hash given the salt.
     */
    fun verifyPassword(providedPassword: String, storedSalt: String, storedHash: String): Boolean {
        val computedHash = hashPassword(providedPassword, storedSalt)
        return slowEquals(computedHash.toByteArray(Charsets.UTF_8), storedHash.toByteArray(Charsets.UTF_8))
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private fun slowEquals(a: ByteArray, b: ByteArray): Boolean {
        var diff = a.size xor b.size
        val minLen = minOf(a.size, b.size)
        for (i in 0 until minLen) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }
}
