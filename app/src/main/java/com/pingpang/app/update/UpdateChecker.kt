package com.pingpang.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * 检查更新（version.txt 方案，见 PRD §10）。
 *
 * 发版流程：
 *  1. app/build.gradle.kts 修改 versionCode(+1)/versionName
 *  2. 构建 APK 上传到仓库（如 0.1.1/pingpang_v0.1.1.apk）
 *  3. 更新仓库根 version.txt 的 versionCode/versionName/updateMessage/apkDownloadUrl
 *  4. 推送 GitHub（raw CDN 约 5 分钟缓存）
 *
 * 判更新唯一依据：远程 versionCode > 本地 BuildConfig.VERSION_CODE。
 */
object UpdateChecker {

    /** 发布仓库（version.txt 所在仓库） */
    const val GITHUB_REPO = "wacilimonster-source/pingpang"
    const val GITHUB_BRANCH = "main"

    /** version.txt 直链 */
    val VERSION_TXT_URL: String
        get() = "https://raw.githubusercontent.com/$GITHUB_REPO/$GITHUB_BRANCH/version.txt"

    private val client = OkHttpClient()

    data class VersionInfo(
        val versionCode: Int,
        val versionName: String,
        val updateMessage: String,
        val apkDownloadUrl: String,
    )

    /** 拉取远程 version.txt；失败或格式错误返回 null */
    suspend fun fetchVersion(): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(VERSION_TXT_URL)
                .header("User-Agent", "PingPang-App")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                VersionInfo(
                    versionCode = json.optInt("versionCode", -1),
                    versionName = json.optString("versionName", ""),
                    updateMessage = json.optString("updateMessage", ""),
                    apkDownloadUrl = json.optString("apkDownloadUrl", ""),
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 是否有新版本：远程 versionCode > 本地 versionCode */
    fun hasNewVersion(remote: VersionInfo, localVersionCode: Int): Boolean =
        remote.versionCode > localVersionCode
}
