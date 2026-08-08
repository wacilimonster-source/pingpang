package com.pingpang.app.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * AI 辅助：制定训练计划（MVP）。
 *
 * 使用 OpenAI 兼容协议（DeepSeek / 通义千问 / 智谱等均支持），
 * 用户在"我的 → AI 设置"中自配 baseUrl / apiKey / model。
 * 所有 AI 输出均为草稿，用户确认后才入库。
 */
object AiPlanService {

    private val client = OkHttpClient()

    data class Config(
        val baseUrl: String,   // 如 https://api.deepseek.com/v1/chat/completions
        val apiKey: String,
        val model: String,     // 如 deepseek-chat
    )

    /** 生成阶段计划草稿（结构化 JSON 文本），失败抛异常 */
    suspend fun generateStagePlan(
        config: Config,
        goal: String,
        weeklyTimes: Int,
        types: String,         // 多球/单球/发接发 偏好
        level: String,         // 水平描述
    ): String = withContext(Dispatchers.IO) {
        val system = """
            你是一名专业乒乓球教练。请根据球员目标制定一份阶段训练计划。
            输出必须是严格的 JSON，格式：
            {"title":"计划标题","weeks":[{"week":1,"theme":"本周主题","sessions":[{"type":"多球|单球|发接发","content":"具体训练内容"}]}]}
            只输出 JSON，不要其他文字。
        """.trimIndent()
        val user = "目标：$goal\n每周训练次数：$weeklyTimes\n训练类型偏好：$types\n当前水平：$level"

        val body = JSONObject()
            .put("model", config.model)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", system))
                .put(JSONObject().put("role", "user").put("content", user)))
            .put("temperature", 0.7)
            .put("stream", false)

        val request = Request.Builder()
            .url(config.baseUrl)
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("AI 请求失败：HTTP ${response.code} ${response.body?.string()?.take(200)}")
            }
            val json = JSONObject(response.body?.string() ?: error("空响应"))
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            content
        }
    }
}
