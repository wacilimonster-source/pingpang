package com.pingpang.app.ai

import android.content.Context

/**
 * 简单键值存储：AI 配置、更新相关偏好等。
 *
 * AI 配置支持两种来源：
 *  - BUILTIN：内置 opencode MiMo-V2.5（baseUrl/model/密钥内置，UI 不显示密钥）
 *  - CUSTOM：用户自定义 OpenAI 兼容源（baseUrl/API Key/模型名）
 */
object AppPrefs {

    private const val NAME = "pingpang_prefs"

    // ---------- 内置 AI 源（opencode Go · MiMo-V2.5，OpenAI 兼容） ----------
    const val BUILTIN_AI_LABEL = "opencode MiMo-V2.5"
    const val BUILTIN_AI_BASE_URL = "https://opencode.ai/zen/go/v1/chat/completions"
    const val BUILTIN_AI_MODEL = "mimo-v2.5"
    private const val BUILTIN_AI_KEY = "sk-fi67mgiFVX2Yzv1eCpe3Af6VqFgAfTUWwpnNK8jWg6W1dVFQrjHJWrAoqX2pdJam"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    /** AI 来源：BUILTIN（默认）/ CUSTOM */
    fun getAiSource(context: Context): String =
        prefs(context).getString("ai_source", "BUILTIN") ?: "BUILTIN"

    fun setAiSource(context: Context, source: String) {
        prefs(context).edit().putString("ai_source", source).apply()
    }

    fun getAiBaseUrl(context: Context): String =
        if (getAiSource(context) == "CUSTOM") {
            prefs(context).getString("ai_base_url", "") ?: ""
        } else {
            BUILTIN_AI_BASE_URL
        }

    fun getAiApiKey(context: Context): String =
        if (getAiSource(context) == "CUSTOM") {
            prefs(context).getString("ai_api_key", "") ?: ""
        } else {
            BUILTIN_AI_KEY
        }

    fun getAiModel(context: Context): String =
        if (getAiSource(context) == "CUSTOM") {
            prefs(context).getString("ai_model", "") ?: ""
        } else {
            BUILTIN_AI_MODEL
        }

    fun setCustomAiConfig(context: Context, baseUrl: String, apiKey: String, model: String) {
        prefs(context).edit()
            .putString("ai_base_url", baseUrl)
            .putString("ai_api_key", apiKey)
            .putString("ai_model", model)
            .putString("ai_source", "CUSTOM")
            .apply()
    }

    /** 切换回内置模型 */
    fun useBuiltinAi(context: Context) {
        prefs(context).edit().putString("ai_source", "BUILTIN").apply()
    }

    /** AI 是否可用：内置恒可用；自定义需填齐 baseUrl 与 Key */
    fun isAiConfigured(context: Context): Boolean =
        if (getAiSource(context) == "CUSTOM") {
            getAiBaseUrl(context).isNotBlank() && getAiApiKey(context).isNotBlank()
        } else {
            true
        }

    fun currentAiLabel(context: Context): String =
        if (getAiSource(context) == "CUSTOM") getAiModel(context).ifBlank { "自定义" }
        else BUILTIN_AI_LABEL

    /** 上次检查更新的时间戳，避免频繁请求 GitHub API（未认证限 60 次/小时） */
    fun lastCheckTime(context: Context): Long =
        prefs(context).getLong("last_check_time", 0L)

    fun setLastCheckTime(context: Context, time: Long) {
        prefs(context).edit().putLong("last_check_time", time).apply()
    }
}
