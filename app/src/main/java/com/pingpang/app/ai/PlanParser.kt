package com.pingpang.app.ai

import org.json.JSONArray
import org.json.JSONObject

/** AI 生成计划的解析结果 */
data class AiPlan(
    val title: String,
    val weeks: List<AiWeek>,
)

data class AiWeek(
    val weekNo: Int,
    val theme: String,
    val sessions: List<AiSession>,
)

data class AiSession(
    val type: String,    // 多球 / 单球 / 发接发
    val content: String,
)

/** 解析 AI 输出（容忍 ```json 围栏），失败返回 null */
object PlanParser {

    fun parse(raw: String): AiPlan? {
        val jsonText = stripFences(raw) ?: return null
        return try {
            val obj = JSONObject(jsonText)
            val title = obj.optString("title").ifBlank { "AI 生成计划" }
            val weeksArr = obj.optJSONArray("weeks") ?: return AiPlan(title, emptyList())
            val weeks = mutableListOf<AiWeek>()
            for (i in 0 until weeksArr.length()) {
                val w = weeksArr.optJSONObject(i) ?: continue
                val weekNo = w.optInt("week", i + 1)
                val theme = w.optString("theme").ifBlank { "第 $weekNo 周" }
                val sessions = mutableListOf<AiSession>()
                val sArr = w.optJSONArray("sessions")
                if (sArr != null) {
                    for (j in 0 until sArr.length()) {
                        val s = sArr.optJSONObject(j) ?: continue
                        sessions.add(
                            AiSession(
                                type = s.optString("type", "多球"),
                                content = s.optString("content", ""),
                            )
                        )
                    }
                }
                weeks.add(AiWeek(weekNo, theme, sessions))
            }
            AiPlan(title, weeks)
        } catch (e: Exception) {
            null
        }
    }

    private fun stripFences(raw: String): String? {
        var t = raw.trim()
        if (t.startsWith("```")) {
            val idx = t.indexOf('\n')
            if (idx in 1..40) t = t.substring(idx + 1)
        }
        if (t.endsWith("```")) t = t.removeSuffix("```").trim()
        return t.ifBlank { null }
    }
}
