package com.pingpang.app.data

import android.content.Context
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
 * 经 SAF 写入用户选择的位置。
 */
object BackupManager {

    fun suggestFileName(): String {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "pingpang_backup_$ts.zip"
    }

    /**
     * 打包并写入 uri（SAF CreateDocument 返回的 content uri）。
     * 返回打包的条目数。
     */
    suspend fun export(
        context: Context,
        uri: Uri,
        onProgress: (Int) -> Unit = {},
    ): Int = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("pingpang.db")
        val videosDir = File(context.filesDir, "videos")

        val manifest = JSONObject().apply {
            put("app", "pingpang")
            put("exportedAt", System.currentTimeMillis())
            put("hasDb", dbFile.exists())
            put("dbSize", dbFile.length())
            put("videoCount", videosDir.listFiles()?.count { it.isFile } ?: 0)
        }

        val resolver = context.contentResolver
        resolver.openOutputStream(uri)?.use { os ->
            ZipOutputStream(os).use { zip ->
                fun addEntry(name: String, file: File) {
                    if (!file.exists() || !file.isFile) return
                    zip.putNextEntry(ZipEntry(name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                if (dbFile.exists()) addEntry("pingpang.db", dbFile)
                videosDir.listFiles()?.forEach { f ->
                    if (f.isFile) addEntry("videos/${f.name}", f)
                }
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray())
                zip.closeEntry()
            }
        }
        val count = 1 + (if (dbFile.exists()) 1 else 0) +
            (videosDir.listFiles()?.count { it.isFile } ?: 0)
        count
    }
}
