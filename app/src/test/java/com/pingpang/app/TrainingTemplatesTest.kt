package com.pingpang.app

import com.pingpang.app.data.TrainingTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** TrainingTemplates 训练模板库 单元测试 */
class TrainingTemplatesTest {

    @Test
    fun typesCovered() {
        val types = TrainingTemplates.all.map { it.type }.distinct()
        assertEquals(listOf("MULTI_BALL", "SERVE_RECEIVE", "SINGLE_BALL"), types.sorted())
    }

    @Test
    fun templateCounts() {
        // 每个类别都有模板，且多球为主
        val byType = TrainingTemplates.all.groupBy { it.type }
        assertTrue(byType["MULTI_BALL"]!!.size >= 5)
        assertTrue(byType["SINGLE_BALL"]!!.size >= 3)
        assertTrue(byType["SERVE_RECEIVE"]!!.size >= 3)
        assertTrue(TrainingTemplates.all.all { it.content.isNotBlank() })
    }

    @Test
    fun groupCountsValid() {
        // 预设组数要么为 0（不预设）要么 >= 6，避免过小
        assertTrue(TrainingTemplates.all.all { it.defaultGroups == 0 || it.defaultGroups >= 6 })
    }

    @Test
    fun typeLabels() {
        assertEquals("多球", TrainingTemplates.typeLabel("MULTI_BALL"))
        assertEquals("单球", TrainingTemplates.typeLabel("SINGLE_BALL"))
        assertEquals("发接发", TrainingTemplates.typeLabel("SERVE_RECEIVE"))
        assertEquals("自定义", TrainingTemplates.typeLabel("自定义"))
    }

    @Test
    fun completedLabels() {
        assertEquals("已完成", TrainingTemplates.completedLabel("DONE"))
        assertEquals("部分完成", TrainingTemplates.completedLabel("PARTIAL"))
        assertEquals("跳过", TrainingTemplates.completedLabel("SKIPPED"))
        assertEquals("未完成", TrainingTemplates.completedLabel("PENDING"))
        assertEquals("未完成", TrainingTemplates.completedLabel(""))
    }
}