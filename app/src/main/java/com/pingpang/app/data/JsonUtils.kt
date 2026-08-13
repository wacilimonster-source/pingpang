package com.pingpang.app.data

import org.json.JSONArray

/** 简单 JSON 数组工具（存 Room 的 String 列） */
object JsonUtils {

    fun listToString(items: List<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    fun stringToList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.optString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 问题痛点 [{q,a}] */
    fun problemsToString(problems: List<Pair<String, String>>): String {
        val arr = JSONArray()
        problems.forEach { (q, a) ->
            arr.put(JSONArray().put(q).put(a))
        }
        return arr.toString()
    }

    fun stringToProblems(json: String?): List<Pair<String, String>> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val item = arr.optJSONArray(i) ?: return@mapNotNull null
                item.optString(0) to item.optString(1)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 训练量化数据 map */
    fun statsToString(stats: Map<String, Int>): String {
        val obj = org.json.JSONObject()
        stats.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    fun stringToStats(json: String?): Map<String, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = org.json.JSONObject(json)
            val keys = obj.keys()
            val map = LinkedHashMap<String, Int>()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optInt(k, 0)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** 周计划训练课 [{type, content}] → (type, content) 列表 */
    fun sessionsFromJson(json: String?): List<Pair<String, String>> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                (obj.optString("type") to obj.optString("content")).takeIf { it.second.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** AI 复盘草稿序列化（PRD §8.1 持久化） */
    fun aiReviewToJson(content: String, generatedAt: Long = System.currentTimeMillis()): String {
        val obj = org.json.JSONObject()
        obj.put("content", content)
        obj.put("generatedAt", generatedAt)
        return obj.toString()
    }

    /** 解析 AI 复盘草稿正文，无草稿返回 null */
    fun aiReviewContent(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            org.json.JSONObject(json).optString("content").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
