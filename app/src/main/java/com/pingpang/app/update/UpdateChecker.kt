package com.pingpang.app.update

import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * GitHub Releases 更新检查。
 *
 * 发布新版本流程（见 README）：
 *  1. 修改 app/build.gradle.kts 的 versionName（如 0.1.1）
 *  2. git tag v0.1.1 && git push origin v0.1.1
 *  3. GitHub Releases 页面：基于该 tag 新建 Release，上传 APK（assets）
 *
 * App 侧通过 GitHub API 获取 latest release，比对版本号后提示更新。
 */
object UpdateChecker {

    /** 发布仓库（GitHub Releases 所在仓库） */
    const val GITHUB_REPO = "wacilimonster-source/pingpang"

    private val client = OkHttpClient()

    /** GitHub Releases 最新版信息 */
    data class ReleaseInfo(
        val tagName: String,      // 如 v0.1.1
        val versionName: String,  // 去掉 v 前缀，如 0.1.1
        val apkName: String,
        val apkUrl: String,
        val notes: String,
    )

    /** 请求 latest release；失败或无 APK 资产返回 null */
    suspend fun checkLatest(): ReleaseInfo? =
        withContextSafe {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "PingPang-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContextSafe null
                val json = JSONObject(response.body?.string() ?: return@withContextSafe null)
                val tagName = json.optString("tag_name") ?: return@withContextSafe null
                val notes = json.optString("body") ?: ""

                val assets = json.optJSONArray("assets") ?: JSONObject.NULL
                if (assets == JSONObject.NULL) return@withContextSafe null

                var apkName: String? = null
                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name") ?: ""
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkName = name
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                val name = apkName ?: return@withContextSafe null
                val url = apkUrl ?: return@withContextSafe null

                ReleaseInfo(
                    tagName = tagName,
                    versionName = tagName.removePrefix("v"),
                    apkName = name,
                    apkUrl = url,
                    notes = notes,
                )
            }
        }

    /**
     * 版本号比较：-1 表示 a 更旧，0 相同，1 表示 a 更新。
     * 支持 "1.2" / "1.2.3" / "1.2.3-beta" 等常见格式（按数字段比较，忽略后缀）。
     */
    fun compareVersions(a: String, b: String): Int {
        fun parse(v: String): List<Int> =
            v.trim().removePrefix("v")
                .split('.', '-', '+')
                .mapNotNull { it.toIntOrNull() }

        val pa = parse(a)
        val pb = parse(b)
        val max = maxOf(pa.size, pb.size)
        for (i in 0 until max) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return if (x > y) 1 else -1
        }
        return 0
    }

    /** 是否有新版本：远程版本 > 本地版本 */
    fun hasNewVersion(latest: String, current: String): Boolean =
        compareVersions(latest, current) > 0
}

/** 轻量协程包装，避免顶层导入混乱 */
private suspend fun <T> withContextSafe(block: () -> T): T =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }
