package io.github.wjy.meditate.network

import android.content.Context
import io.github.wjy.meditate.data.AppDatabase
import io.github.wjy.meditate.data.SettingsManager
import io.github.wjy.meditate.data.WebDavConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestorePreview(
    val entryCount: Int,
    val lastEntryContent: String,
    val lastEntryDate: String,
)

class SyncManager(private val context: Context) {
    private val settingsManager = SettingsManager(context)

    suspend fun testConnection(config: WebDavConfig): Result<Unit> = withContext(Dispatchers.IO) {
        if (config.url.isBlank()) return@withContext Result.failure(Exception("URL 不能为空"))
        WebDavClient(config.url, config.user, config.pass, config.ignoreCert).testConnection()
    }

    suspend fun syncToWebDav(config: WebDavConfig): Result<Unit> = withContext(Dispatchers.IO) {
        if (config.url.isBlank()) return@withContext Result.failure(Exception("WebDAV 未配置"))

        try {
            val client = WebDavClient(config.url, config.user, config.pass, config.ignoreCert)
            AppDatabase.checkpoint(context)
            
            val slot = settingsManager.activeSlot.first()
            val dbFile = context.getDatabasePath("paper_database_$slot")
            
            if (!dbFile.exists()) return@withContext Result.failure(Exception("数据库文件不存在"))
            
            client.uploadFile("paper-slip-backup.db", dbFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndPreview(config: WebDavConfig): Result<RestorePreview> = withContext(Dispatchers.IO) {
        try {
            val client = WebDavClient(config.url, config.user, config.pass, config.ignoreCert)
            val currentSlot = settingsManager.activeSlot.first()
            val inactiveSlot = if (currentSlot == "a") "b" else "a"
            val targetDbFile = context.getDatabasePath("paper_database_$inactiveSlot")
            val tempFile = File(context.cacheDir, "temp_restore.db")

            val downloadResult = client.downloadFile("paper-slip-backup.db", tempFile)
            if (downloadResult.isSuccess) {
                tempFile.copyTo(targetDbFile, overwrite = true)
                File(targetDbFile.path + "-wal").delete()
                File(targetDbFile.path + "-shm").delete()
                tempFile.delete()

                val previewDb = AppDatabase.getPreviewDatabase(context, inactiveSlot)
                val entries = previewDb.journalDao().getAllEntries().first()
                val count = entries.size
                val lastEntry = entries.firstOrNull()
                
                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                val preview = RestorePreview(
                    entryCount = count,
                    lastEntryContent = lastEntry?.content?.take(30) ?: "无内容",
                    lastEntryDate = lastEntry?.let { dateFormat.format(Date(it.timestamp)) } ?: "N/A"
                )
                previewDb.close()
                Result.success(preview)
            } else {
                Result.failure(Exception("下载失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun switchActiveSlot(): Result<Unit> {
        return try {
            AppDatabase.closeDatabase()
            settingsManager.switchSlot()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
