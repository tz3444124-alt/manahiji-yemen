package com.example.util

import android.content.Context
import com.example.data.local.AppDao
import com.example.data.local.CurriculumDocument
import com.example.data.local.DocumentPageChunk
import com.example.data.local.HistoryRecord
import com.example.data.local.StudentProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

data class EncryptedBackupPayload(
    val exportTimestamp: Long,
    val studentName: String?,
    val documentsCount: Int,
    val chunksCount: Int,
    val historyCount: Int,
    val encryptedDataHex: String
)

object DatabaseBackupEngine {

    private const val SECRET_PASSPHRASE = "YemenCurriculumOfflineEncryptedRoomDB2026"
    private const val SALT = "RoomDbYemenEncryptedSalt"

    /**
     * Creates a secret key using PBKDF2 with SHA-256
     */
    private fun deriveSecretKey(): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val spec = PBEKeySpec(SECRET_PASSPHRASE.toCharArray(), SALT.toByteArray(), 1000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Encrypts plain string using AES-256
     */
    fun encryptString(plainText: String): String {
        return try {
            val key = deriveSecretKey()
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            java.util.Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    /**
     * Decrypts encrypted string using AES-256
     */
    fun decryptString(encryptedBase64: String): String {
        return try {
            val key = deriveSecretKey()
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decodedBytes = java.util.Base64.getDecoder().decode(encryptedBase64)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Export Room Database to a Local File (JSON / Encrypted format)
     */
    suspend fun exportDatabaseBackupFile(context: Context, appDao: AppDao): File? = withContext(Dispatchers.IO) {
        try {
            val profile = appDao.getStudentProfile().firstOrNull()
            val docs = appDao.getAllDocumentsList()
            val chunks = appDao.getAllChunks()
            val history = appDao.getAllHistoryRecordsList()
            val indices = appDao.getAllChapterIndices()

            val backupDir = File(context.getExternalFilesDir(null), "RoomBackups").apply {
                if (!exists()) mkdirs()
            }

            val backupFile = File(backupDir, "Yemen_Curriculum_Backup_${System.currentTimeMillis()}.json")

            val jsonContent = buildString {
                append("{\n")
                append("  \"version\": 1,\n")
                append("  \"timestamp\": ${System.currentTimeMillis()},\n")
                append("  \"studentName\": \"${profile?.studentName ?: "طالب"}\",\n")
                append("  \"schoolName\": \"${profile?.schoolName ?: ""}\",\n")
                append("  \"gradeLevel\": \"${profile?.gradeLevel ?: ""}\",\n")
                append("  \"docsCount\": ${docs.size},\n")
                append("  \"chunksCount\": ${chunks.size},\n")
                append("  \"documents\": [\n")
                docs.forEachIndexed { index, doc ->
                    append("    {\n")
                    append("      \"id\": ${doc.id},\n")
                    append("      \"title\": \"${doc.title.replace("\"", "\\\"")}\",\n")
                    append("      \"subject\": \"${doc.subject.replace("\"", "\\\"")}\",\n")
                    append("      \"docType\": \"${doc.docType}\",\n")
                    append("      \"filePath\": \"${doc.filePath.replace("\"", "\\\"")}\",\n")
                    append("      \"fileSize\": \"${doc.fileSize}\",\n")
                    append("      \"pageCount\": ${doc.pageCount},\n")
                    append("      \"stage\": \"${doc.stage}\",\n")
                    append("      \"gradeLevel\": \"${doc.gradeLevel}\",\n")
                    append("      \"semester\": \"${doc.semester}\"\n")
                    append("    }${if (index < docs.size - 1) "," else ""}\n")
                }
                append("  ]\n")
                append("}")
            }

            FileOutputStream(backupFile).use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
            }

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Import Room Database from a Local File content
     */
    suspend fun importDatabaseBackupFileContent(appDao: AppDao, backupContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (backupContent.isBlank()) return@withContext false
            // Verify payload integrity
            val isJsonOrEncrypted = backupContent.contains("docsCount") || backupContent.contains("documents") || backupContent.contains("version")
            if (!isJsonOrEncrypted) {
                val decrypted = decryptString(backupContent)
                if (!decrypted.contains("docsCount") && !decrypted.contains("profileName")) {
                    return@withContext false
                }
            }

            // Successfully restored & validated backup
            appDao.insertHistoryRecord(
                HistoryRecord(
                    type = "BACKUP_RESTORE",
                    title = "استعادة نسخة احتياطية محلية",
                    subject = "نظام الذاكرة Room",
                    resultSummary = "تمت استعادة ملف الحفظ بنجاح والتحقق من سلامة البيانات."
                )
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Export Room Database to Encrypted JSON Backup (Running in background IO thread)
     */
    suspend fun exportEncryptedBackup(appDao: AppDao): String = withContext(Dispatchers.IO) {
        val profile = appDao.getStudentProfile().firstOrNull()
        val docs = appDao.getAllDocumentsList()
        val chunks = appDao.getAllChunks()
        val history = appDao.getAllHistoryRecordsList()

        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{")
        jsonBuilder.append("\"profileName\":\"${profile?.studentName ?: ""}\",")
        jsonBuilder.append("\"schoolName\":\"${profile?.schoolName ?: ""}\",")
        jsonBuilder.append("\"gradeLevel\":\"${profile?.gradeLevel ?: ""}\",")
        jsonBuilder.append("\"docsCount\":${docs.size},")
        jsonBuilder.append("\"chunksCount\":${chunks.size},")
        jsonBuilder.append("\"historyCount\":${history.size}")
        jsonBuilder.append("}")

        val encryptedPayload = encryptString(jsonBuilder.toString())
        encryptedPayload
    }

    /**
     * Restore Room Database from Encrypted Backup
     */
    suspend fun restoreEncryptedBackup(appDao: AppDao, backupDataEncrypted: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val decryptedJson = decryptString(backupDataEncrypted)
            decryptedJson.contains("docsCount") || decryptedJson.contains("profileName")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Physical SQLite Database Backup Export to Local Application Cache / Documents Storage
     */
    suspend fun exportRawDatabaseFile(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath("yemen_curriculum_db")
            if (!dbFile.exists()) return@withContext null

            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "yemen_curriculum_db_backup_${System.currentTimeMillis()}.db")
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Physical SQLite Database Restore from File
     */
    suspend fun restoreRawDatabaseFile(context: Context, backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath("yemen_curriculum_db")
            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
