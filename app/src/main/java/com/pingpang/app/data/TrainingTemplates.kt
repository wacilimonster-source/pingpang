package com.pingpang.app.data

/** 训练模板库（MVP 内置，见 PRD F05） */
object TrainingTemplates {

    data class Template(
        val type: String,          // MULTI_BALL / SINGLE_BALL / SERVE_RECEIVE
        val content: String,       // 训练内容
        val defaultGroups: Int = 0,// 默认组数（0 表示不预设）
    )

    val all: List<Template> = listOf(
        // 多球
        Template("MULTI_BALL", "正手定点拉球 10 组", 10),
        Template("MULTI_BALL", "反手定点拨球 10 组", 10),
        Template("MULTI_BALL", "正手两点摆速 10 组", 10),
        Template("MULTI_BALL", "反手两点摆速 10 组", 10),
        Template("MULTI_BALL", "两点正反手衔接 8 组", 8),
        Template("MULTI_BALL", "三点位步法衔接 8 组", 8),
        Template("MULTI_BALL", "反手拧拉定点 8 组", 8),
        // 单球
        Template("SINGLE_BALL", "发球抢攻（发抢衔接）6 组", 6),
        Template("SINGLE_BALL", "正手对拉/相持 8 组", 8),
        Template("SINGLE_BALL", "反手相持 8 组", 8),
        Template("SINGLE_BALL", "左右摆速对练 6 组", 6),
        Template("SINGLE_BALL", "搓攻套路（搓转拉）6 组", 6),
        // 发接发
        Template("SERVE_RECEIVE", "发球落点变化（左中右）", 0),
        Template("SERVE_RECEIVE", "发球旋转变化（上下侧旋）", 0),
        Template("SERVE_RECEIVE", "接发球摆短", 0),
        Template("SERVE_RECEIVE", "接发球挑打/拧拉", 0),
    )

    fun typeLabel(type: String): String = when (type) {
        "MULTI_BALL" -> "多球"
        "SINGLE_BALL" -> "单球"
        "SERVE_RECEIVE" -> "发接发"
        else -> type
    }
}
