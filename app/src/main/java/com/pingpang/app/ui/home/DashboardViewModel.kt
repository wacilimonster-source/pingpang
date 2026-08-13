package com.pingpang.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingpang.app.data.JsonUtils
import com.pingpang.app.data.db.StagePlanDao
import com.pingpang.app.data.db.TrainingSessionDao
import com.pingpang.app.data.db.VideoClipDao
import com.pingpang.app.data.db.WeekPlanDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 本月统计 */
data class MonthView(
    val cnt: Int = 0,
    val mins: Int = 0,
    val rate: Int = 0,     // 完成率 %
    val streak: Int = 0,   // 连续打卡天数
)

/** 阶段进度卡 */
data class StageCard(
    val title: String = "",
    val done: Int = 0,
    val planned: Int = 0,
    val weekInfo: String = "",
    val daysLeft: Int = 0,
)

/** 待办提醒 */
data class TodoView(
    val pendingSessions: Int = 0,
    val unlinkedVideos: Int = 0,
)

/** 首页看板聚合（F-看板） */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    stageDao: StagePlanDao,
    sessionDao: TrainingSessionDao,
    weekDao: WeekPlanDao,
    videoDao: VideoClipDao,
) : ViewModel() {

    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private fun monthStart(): String = LocalDate.now().withDayOfMonth(1).format(dateFmt)
    private fun dateAt(daysAgo: Int): String = LocalDate.now().minusDays(daysAgo.toLong()).format(dateFmt)

    /** 本月统计：次数 / 时长 / 完成率 / 连续天数 */
    val month: StateFlow<MonthView> = combine(
        sessionDao.observeSinceDate(monthStart()),
        sessionDao.observeCompletion(monthStart()),
        sessionDao.observeDistinctDates(),
    ) { stat, comp, dates ->
        val total = comp.done + comp.partial + comp.skipped
        val rate = if (total > 0) ((comp.done + comp.partial) * 100 / total) else 0
        MonthView(stat.cnt, stat.mins, rate, calcStreak(dates))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthView())

    /** 近 14 天打卡热力图（每天训练分钟数，0 表示未训练） */
    val heatmap: StateFlow<List<Int>> = sessionDao.observeDateMinutes(dateAt(13))
        .map { list ->
            (0 until 14).map { i ->
                list.firstOrNull { it.date == dateAt(13 - i) }?.mins ?: 0
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(14) { 0 })

    /** 训练类型分布 */
    val types: StateFlow<List<TrainingSessionDao.TypeCount>> = sessionDao.observeTypeCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 当前阶段计划卡 */
    val stage: StateFlow<StageCard?> = stageDao.observeActive()
        .flatMapLatest { stages ->
            val s = stages.firstOrNull()
            if (s == null) flowOf(null)
            else combine(
                sessionDao.observeAll(),
                weekDao.observeForStage(s.id),
            ) { sessions, weeks ->
                val done = sessions.count { it.planId == s.id }
                val planned = weeks.sumOf { JsonUtils.sessionsFromJson(it.sessionsJson).size }
                val today = LocalDate.now()
                val daysLeft = try {
                    java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.parse(s.endDate)).toInt().coerceAtLeast(0)
                } catch (e: Exception) { 0 }
                StageCard(
                    title = s.title,
                    done = done,
                    planned = planned,
                    weekInfo = "每周 ${s.weeklyTimes} 次 · 进行中",
                    daysLeft = daysLeft,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 待办提醒 */
    val todos: StateFlow<TodoView> = combine(stage, videoDao.observeAll()) { s, videos ->
        val pending = if (s != null) (s.planned - s.done).coerceAtLeast(0) else 0
        val limit = LocalDate.now().minusDays(30).format(dateFmt)
        val unlinked = videos.count { it.linkedType == null && it.date >= limit }
        TodoView(pending, unlinked)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoView())

    private fun calcStreak(dates: List<String>): Int {
        var streak = 0
        var day = LocalDate.now()
        val set = dates.toSet()
        while (set.contains(day.format(dateFmt))) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }
}
