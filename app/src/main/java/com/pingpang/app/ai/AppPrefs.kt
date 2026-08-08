package com.pingpang.app.ai

import android.content.Context

/** 简单键值存储：AI 配置、更新相关偏好等 */
object AppPrefs {

    private const val NAME = "pingpang_prefs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getAiBaseUrl(context: Context): String =
        prefs(context).getString("ai_base_url", "") ?: ""

    fun getAiApiKey(context: Context): String =
        prefs(context).getString("ai_api_key", "") ?: ""

    fun getAiModel(context: Context): String =
        prefs(context).getString("ai_model", "deepseek-chat") ?: "deepseek-chat"

    fun setAiConfig(context: Context, baseUrl: String, apiKey: String, model: String) {
        prefs(context).edit()
            .putString("ai_base_url", baseUrl)
            .putString("ai_api_key", apiKey)
            .putString("ai_model", model)
            .apply()
    }

    fun isAiConfigured(context: Context): Boolean =
        getAiBaseUrl(context).isNotBlank() && getAiApiKey(context).isNotBlank()

    /** 上次检查更新的时间戳，避免频繁请求 GitHub API（未认证限 60 次/小时） */
    fun lastCheckTime(context: Context): Long =
        prefs(context).getLong("last_check_time", 0L)

    fun setLastCheckTime(context: Context, time: Long) {
        prefs(context).edit().putLong("last_check_time", time).apply()
    }
}
