package com.example.data.crypto

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.SecureRandom

object ChecksumUtil {

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun generateSecurePassword(length: Int = 16): String {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val symbols = "!@#%&*+=-"
        val allChars = uppercase + lowercase + digits + symbols

        val random = SecureRandom()
        val password = StringBuilder(length)

        // Ensure at least one from each character set
        password.append(uppercase[random.nextInt(uppercase.length)])
        password.append(lowercase[random.nextInt(lowercase.length)])
        password.append(digits[random.nextInt(digits.length)])
        password.append(symbols[random.nextInt(symbols.length)])

        for (i in 4 until length) {
            password.append(allChars[random.nextInt(allChars.length)])
        }

        // Shuffle characters
        val charList = password.toString().toList().shuffled(random)
        return charList.joinToString("")
    }
}
