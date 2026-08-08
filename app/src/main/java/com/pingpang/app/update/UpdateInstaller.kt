package com.pingpang.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * APK 下载与安装。
 * 下载到 cache/updates/ 目录（FileProvider 已声明该路径），
 * 完成后通过系统安装器拉起安装（Android 8+ 需"安装未知应用"授权）。
 */
object UpdateInstaller {

    private val client = OkHttpClient()

    /** 下载 APK 到缓存目录，返回文件；[onProgress] 回调 0-100 */
    suspend fun downloadApk(
        context: Context,
        url: String,
        fileName: String,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, fileName)

        val request = Request.Builder().url(url)
            .header("User-Agent", "PingPang-App")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("empty body")
            val total = body.contentLength()
            val input = body.byteStream()
            val output = target.outputStream()
            try {
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read = 0L
                var progress = 0
                while (true) {
                    val n = input.read(buffer)
                    if (n == -1) break
                    output.write(buffer, 0, n)
                    read += n
                    if (total > 0) {
                        val p = ((read * 100) / total).toInt()
                        if (p != progress) {
                            progress = p
                            onProgress(p)
                        }
                    }
                }
                output.flush()
            } finally {
                input.close()
                output.close()
            }
        }
        target
    }

    /**
     * 通过系统安装器安装 APK。
     * 注意：Android 8+ 首次安装前需引导用户到设置页授权"安装未知应用"。
     */
    fun installApk(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /** 跳转系统设置页，授权"安装未知应用" */
    fun openInstallPermissionSettings(context: Context, packageName: String) {
        val intent = Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:$packageName"),
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
