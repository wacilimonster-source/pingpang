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
 * AI 辅助：制定训练计划 / 训练复盘（F03 / F07）。
 * OpenAI 兼容协议（DeepSeek / 通义千问 / 智谱），用户自配 baseUrl/apiKey/model。
 * 所有输出为草稿，用户确认后才入库。
 */
object AiPlanService {

    private val client = OkHttpClient()

    data class Config(
        val baseUrl: String,
        val apiKey: String,
        val model: String,
    )

    /** 从本地偏好构造配置；未配置返回 null */
    fun configOrNull(context: Context): Config? {
        if (!AppPrefs.isAiConfigured(context)) return null
        return Config(
            baseUrl = AppPrefs.getAiBaseUrl(context),
            apiKey = AppPrefs.getAiApiKey(context),
            model = AppPrefs.getAiModel(context),
        )
    }

    /** 生成阶段计划草稿（结构化 JSON），失败抛异常 */
    suspend fun generateStagePlan(
        config: Config,
        goal: String,
        weeklyTimes: Int,
        types: String,
        level: String,
    ): String = chat(config, systemPlanPrompt, "目标：$goal\n每周训练次数：$weeklyTimes\n训练类型偏好：$types\n当前水平：$level")

    /** 训练复盘（F07），返回 Markdown 风格建议文本 */
    suspend fun generateReview(
        config: Config,
        sessionInfo: String,
    ): String = chat(config, systemReviewPrompt, "训练记录：\n$sessionInfo")

    /**
     * 对战应对建议：结合自己的技术特长、对手技术特长与历史执行反馈，
     * 输出针对性战术建议（Markdown）。
     */
    suspend fun generateTactics(
        config: Config,
        mySkills: List<Pair<String, String>>,
        opponentName: String,
        opponentSkills: List<Pair<String, String>>,
        opponentNotes: String,
        feedbacks: List<Pair<String, String>>,
    ): String {
        val my = mySkills.joinToString("\n") { (n, d) -> "- $n：$d" }.ifBlank { "- （未填写）" }
        val opp = opponentSkills.joinToString("\n") { (n, d) -> "- $n：$d" }.ifBlank { "- （未填写）" }
        val fb = feedbacks.joinToString("\n") { (d, c) -> "- $d：$c" }.ifBlank { "- （暂无）" }
        val user = buildString {
            append("我的技术特长：\n$my\n\n")
            append("对手（$opponentName）技术特长：\n$opp\n\n")
            if (opponentNotes.isNotBlank()) append("对手备注：$opponentNotes\n\n")
            append("与该对手交手的执行反馈：\n$fb")
        }
        return chat(config, systemTacticsPrompt, user)
    }

    private val systemTacticsPrompt = """
        你是一名专业乒乓球教练兼战术分析师。请根据球员自身技术特长、对手的技术特长，
        以及与该对手交手的执行反馈，给出针对性的比赛应对策略。
        输出用简洁中文 Markdown，分四部分：
        1. 对手威胁点分析（1-2 条）
        2. 发球与接发球策略（针对对手弱点）
        3. 相持与落点策略（结合自己的特长扬长避短）
        4. 临场注意事项（结合执行反馈中的教训）
        要求：每条策略具体可执行（如"多发反手位短球，迫使对手正手起板质量下降"），
        避免空话；300 字以内。
    """.trimIndent()

    private val systemPlanPrompt = """
        你是一名专业乒乓球教练。请根据球员目标制定一份阶段训练计划。
        输出必须是严格的 JSON，不要输出其他文字或代码围栏。格式：
        {"title":"计划标题","weeks":[{"week":1,"theme":"本周主题","sessions":[{"type":"多球","content":"具体训练内容"}]}]}
        要求：总周数 4-12 周；每周训练次数不超过用户要求；每周至少 1 次训练；训练内容具体可执行。
    """.trimIndent()

    private val systemReviewPrompt = """
        你是一名专业乒乓球教练。请根据球员的本次训练记录给出复盘，包含三部分：
        1. 训练质量评估（简短）
        2. 发现的问题（结合记录中的问题与量化数据）
        3. 下次训练建议（1-3 条，具体可执行）
        用简洁的中文，Markdown 列表，200 字以内。
    """.trimIndent()

    private suspend fun chat(config: Config, system: String, user: String): String =
        withContext(Dispatchers.IO) {
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
                    error("AI 请求失败：HTTP ${response.code}")
                }
                val json = JSONObject(response.body?.string() ?: error("空响应"))
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        }
}
