package com.pingpang.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 阶段计划（如"三个月练好反手拧拉"） */
@Entity(tableName = "stage_plan")
data class StagePlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val goal: String,
    val startDate: String,      // yyyy-MM-dd
    val endDate: String,        // yyyy-MM-dd
    val weeklyTimes: Int,       // 每周训练次数
    val source: String,         // AI / MANUAL
    val status: String,         // ACTIVE / DONE / DRAFT
)

/** 周计划：隶属于某个阶段计划 */
@Entity(tableName = "week_plan")
data class WeekPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stageId: Long,
    val weekNo: Int,
    val theme: String,
    /** 该周训练课内容列表 JSON：[{"type":"多球","content":"..."}] */
    val sessionsJson: String = "[]",
)

/** 单次训练课 / 训练记录 */
@Entity(tableName = "training_session")
data class TrainingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long?,          // 关联周计划，可空（临时训练）
    val date: String,           // yyyy-MM-dd
    val type: String,           // MULTI_BALL / SINGLE_BALL / SERVE_RECEIVE
    val content: String,        // 训练内容描述
    val durationMin: Int,       // 训练时长（分钟）
    val completed: String,      // DONE / PARTIAL / SKIPPED
    val statsJson: String,      // 量化数据 {groups, reps, hitRate...}
    val notes: String,          // 问题与收获
    val photosJson: String,     // 照片路径数组 JSON
    val videosJson: String,     // 关联视频 id 数组 JSON
)

/** 技术档案卡 */
@Entity(tableName = "skill_card")
data class SkillCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,       // 正手/反手/发球/接发球/台内/步法
    val name: String,           // 如"反手拧拉"
    val keyPoints: String,      // 动作要点
    val progress: String,       // 入门/巩固/熟练/稳定
    val photosJson: String,     // 照片路径数组 JSON
    val problemsJson: String,   // 问题痛点数组 JSON [{q, a}]
)

/** 视频片段 */
@Entity(tableName = "video_clip")
data class VideoClip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,       // 应用私有目录内绝对路径
    val date: String,           // yyyy-MM-dd
    val source: String,         // RECORDED / IMPORTED
    val tagsJson: String,       // 标签数组 JSON
    val linkedType: String? = null,    // TRAINING / SKILL / MATCH
    val linkedId: Long? = null,
    val durationMs: Long,
    val thumbPath: String?,     // 缩略图路径
)

/** 对战记录 */
@Entity(tableName = "match_record")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,           // yyyy-MM-dd
    val opponentTypeId: Long,   // 对手类型 id
    val statsJson: String,      // 技战术数据 {serveWin, serveLose, receiveErr, rallyWin, ...}
    val summary: String,        // 赛后总结
    val videosJson: String,     // 关联视频 id 数组 JSON
)

/** 对手类型档案（按类型，不按个人） */
@Entity(tableName = "opponent_type")
data class OpponentType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,           // 左撇子/削球手/颗粒胶/直板快攻/横板弧圈...
    val notes: String,          // 特点描述
    val tactics: String,        // 应对战术
    val historyJson: String,    // 历史交手汇总 JSON
)
