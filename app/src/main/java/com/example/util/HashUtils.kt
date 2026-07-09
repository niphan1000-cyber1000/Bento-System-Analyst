package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object HashUtils {

    /**
     * Generates a secure random 16-byte salt, Base64 encoded.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    /**
     * Hashes the password with the salt using SHA-256, returning the Base64-encoded hash.
     */
    fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = password + salt
        val hashBytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
}
