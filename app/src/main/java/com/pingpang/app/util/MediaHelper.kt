package com.pingpang.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 媒体文件复制：相册/系统选择器 URI → 应用私有目录 */
object MediaHelper {

    /** 复制 uri 到私有目录子目录，返回新文件绝对路径 */
    suspend fun copyToInternal(
        context: Context,
        uri: Uri,
        subDir: String,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, subDir).apply { mkdirs() }
            val name = queryName(context, uri) ?: "${System.currentTimeMillis()}"
            val safeName = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val target = File(dir, "${System.currentTimeMillis()}_$safeName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            target.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun queryName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
