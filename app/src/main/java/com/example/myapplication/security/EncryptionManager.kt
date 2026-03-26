package com.example.myapplication.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptionManager(private val context: Context) {
    private val keyAlias = "GuardianHubKey"
    private val keystoreName = "AndroidKeyStore"

    init {
        generateKeyIfNeeded()
    }

    private fun generateKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(keystoreName)
        keyStore.load(null)

        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setIsStrongBoxBacked(true)
                .build()

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                keystoreName
            )
            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
    }

    fun getEncryptedOutputStream(filename: String): OutputStream {
        val file = File(context.filesDir, filename)
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileOutput()
    }

    fun getEncryptedInputStream(filename: String): InputStream? {
        return try {
            val file = File(context.filesDir, filename)
            if (!file.exists()) return null

            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileInput()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteEncryptedFile(filename: String): Boolean {
        return File(context.filesDir, filename).delete()
    }

    fun getEncryptedFilesSize(): Long {
        return context.filesDir
            .listFiles() { file ->
                file.isFile && file.name.endsWith(".enc")
            }
            ?.sumOf { it.length() } ?: 0L
    }

    fun getAllEncryptedFiles(): List<File> {
        return context.filesDir
            .listFiles { file ->
                file.isFile && file.name.endsWith(".enc")
            }
            ?.sortedBy { it.lastModified() } ?: emptyList()
    }
}
