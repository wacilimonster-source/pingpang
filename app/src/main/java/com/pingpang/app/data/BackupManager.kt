package com.pingpang.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 备份导出（PRD F12）：打包 SQLite 数据库 + 视频目录 + manifest.json 为 zip，
 * 经 SAF 写入用户选择的位置。manifest 记录版本/时间/条目清单，便于校验恢复。
 */
object BackupManager {

    fun suggestFileName(): String {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "pingpang_backup_$ts.zip"
    }

    /**
     * 打包并写入 uri（SAF CreateDocument 返回的 content uri）。
     * @param onProgress 进度回调 (已处理条目, 总条目)
     * @return 打包的条目数
     */
    suspend fun export(
        context: Context,
        uri: Uri,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        versionName: String = "",
        versionCode: Int = 0,
    ): Int = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("pingpang.db")
        // 导出前将 WAL 日志合并回主库，保证备份数据完整
        try {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                .use { it.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { c -> c.moveToFirst() } }
        } catch (e: Exception) {
        }
        val videosDir = File(context.filesDir, "videos")
        val videoFiles = videosDir.listFiles()?.filter { it.isFile } ?: emptyList()
        var total = (if (dbFile.exists()) 1 else 0) + videoFiles.size + 1
        var done = 0

        val manifest = JSONObject().apply {
            put("app", "pingpang")
            put("exportedAt", System.currentTimeMillis())
            put("versionName", versionName)
            put("versionCode", versionCode)
            put("hasDb", dbFile.exists())
            put("dbSize", dbFile.length())
            put("videoCount", videoFiles.size)
            put("entryCount", total)
            put("totalSize", videoFiles.sumOf { it.length() } + if (dbFile.exists()) dbFile.length() else 0)
        }

        val resolver = context.contentResolver
        resolver.openOutputStream(uri)?.use { os ->
            ZipOutputStream(os).use { zip ->
                fun addEntry(name: String, file: File) {
                    if (!file.exists() || !file.isFile) return
                    zip.putNextEntry(ZipEntry(name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    done++
                    onProgress(done, total)
                }
                if (dbFile.exists()) addEntry("pingpang.db", dbFile)
                videoFiles.forEach { f -> addEntry("videos/${f.name}", f) }
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray())
                zip.closeEntry()
                done++
                onProgress(done, total)
            }
        }
        total
    }
}