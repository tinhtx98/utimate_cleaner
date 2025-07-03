package com.mars.ultimatecleaner.data.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HashUtils @Inject constructor() {

    suspend fun computeMD5Hash(file: File): String = withContext(Dispatchers.IO) {
        computeHash(file, "MD5")
    }

    suspend fun computeSHA1Hash(file: File): String = withContext(Dispatchers.IO) {
        computeHash(file, "SHA-1")
    }

    suspend fun computeSHA256Hash(file: File): String = withContext(Dispatchers.IO) {
        computeHash(file, "SHA-256")
    }

    private fun computeHash(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun computeQuickHash(file: File): String = withContext(Dispatchers.IO) {
        // Quick hash for large files - hash first and last 8KB + file size
        val digest = MessageDigest.getInstance("MD5")
        val fileSize = file.length()

        FileInputStream(file).use { input ->
            // Hash first 8KB
            val buffer = ByteArray(8192)
            val firstBytes = input.read(buffer)
            if (firstBytes > 0) {
                digest.update(buffer, 0, firstBytes)
            }

            // Hash last 8KB if file is large enough
            if (fileSize > 16384) {
                input.skip(fileSize - 16384)
                val lastBytes = input.read(buffer)
                if (lastBytes > 0) {
                    digest.update(buffer, 0, lastBytes)
                }
            }

            // Include file size in hash
            digest.update(fileSize.toString().toByteArray())
        }

        digest.digest().joinToString("") { "%02x".format(it) }
    }
}