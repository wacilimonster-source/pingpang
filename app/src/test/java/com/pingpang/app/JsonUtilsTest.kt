package com.pingpang.app

import com.pingpang.app.data.JsonUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JsonUtils 序列化/容错 单元测试 */
class JsonUtilsTest {

    @Test
    fun listRoundTrip() {
        val json = JsonUtils.listToString(listOf("a", "b", "中文"))
        assertEquals(listOf("a", "b", "中文"), JsonUtils.stringToList(json))
    }

    @Test
    fun listEmptyHandling() {
        assertEquals(emptyList<String>(), JsonUtils.stringToList(null))
        assertEquals(emptyList<String>(), JsonUtils.stringToList(""))
        assertEquals(emptyList<String>(), JsonUtils.stringToList("not-json"))
        assertTrue(JsonUtils.stringToList(JsonUtils.listToString(emptyList())).isEmpty())
    }

    @Test
    fun problemsRoundTrip() {
        val problems = listOf("正手" to "拉球稳定性", "步法" to "并步")
        val json = JsonUtils.problemsToString(problems)
        assertEquals(problems, JsonUtils.stringToProblems(json))
    }

    @Test
    fun statsRoundTrip() {
        val stats = mapOf("groups" to 10, "reps" to 30, "hitRate" to 85)
        val json = JsonUtils.statsToString(stats)
        assertEquals(stats, JsonUtils.stringToStats(json))
    }

    @Test
    fun statsEmptyHandling() {
        assertTrue(JsonUtils.stringToStats(null).isEmpty())
        assertTrue(JsonUtils.stringToStats("bad-json").isEmpty())
    }

    @Test
    fun sessionsFromJsonParsesContent() {
        val json = """[{"type":"MULTI_BALL","content":"正手定点拉球 10 组"},{"type":"SERVE_RECEIVE","content":""}]"""
        val sessions = JsonUtils.sessionsFromJson(json)
        assertEquals(listOf("MULTI_BALL" to "正手定点拉球 10 组"), sessions)
    }

    @Test
    fun aiReviewRoundTrip() {
        val content = "要点：重心压低，蹬转发力"
        val json = JsonUtils.aiReviewToJson(content)
        assertEquals(content, JsonUtils.aiReviewContent(json))
    }

    @Test
    fun aiReviewEmptyHandling() {
        assertNull(JsonUtils.aiReviewContent(null))
        assertNull(JsonUtils.aiReviewContent(""))
        assertNull(JsonUtils.aiReviewContent("not-json"))
        assertNull(JsonUtils.aiReviewContent("""{"content":""}"""))
    }
}