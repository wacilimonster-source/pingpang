package com.pingpang.app.ui.plan

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pingpang.app.PingPangApp
import com.pingpang.app.ai.AiPlan
import com.pingpang.app.ai.AiPlanService
import com.pingpang.app.ai.PlanParser
import com.pingpang.app.data.JsonUtils
import com.pingpang.app.data.model.StagePlan
import com.pingpang.app.data.model.WeekPlan
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val typeOptions = listOf("多球", "单球", "发接发")

/**
 * 新建/编辑阶段计划（F02/F03）。stageId = -1 表示新建。
 */
@Composable
fun PlanEditScreen(
    stageId: Long,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as PingPangApp).database

    var title by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var endDate by remember {
        mutableStateOf(LocalDate.now().plusWeeks(8).format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
    var weeklyTimes by remember { mutableStateOf("3") }
    var level by remember { mutableStateOf("进阶提高") }
    var selectedTypes by remember { mutableStateOf(setOf("多球", "单球")) }
    var loaded by remember { mutableStateOf(false) }

    // AI 状态
    var aiLoading by remember { mutableStateOf(false) }
    var aiPlan by remember { mutableStateOf<AiPlan?>(null) }
    var aiError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(stageId) {
        if (stageId > 0 && !loaded) {
            val plan = db.stagePlanDao().getById(stageId)
            if (plan != null) {
                title = plan.title
                goal = plan.goal
                startDate = plan.startDate
                endDate = plan.endDate
                weeklyTimes = plan.weeklyTimes.toString()
            }
            loaded = true
        }
    }

    /**
     * 保存：编辑时保留既有周计划内容（数量变化时增删）；
     * [replaceWeeks] = true 表示 AI 确认启用，用生成内容整体替换。
     */
    fun save(plan: StagePlan, weeks: List<WeekPlan>, replaceWeeks: Boolean = false) {
        scope.launch {
            if (stageId > 0) {
                db.stagePlanDao().update(plan)
                val existing = if (replaceWeeks) emptyList() else db.weekPlanDao().forStage(stageId)
                db.weekPlanDao().deleteByStage(stageId)
                val merged = (1..plan.weeklyTimes).map { weekNo ->
                    existing.find { it.weekNo == weekNo }?.copy(stageId = stageId)
                        ?: weeks.find { it.weekNo == weekNo }?.copy(stageId = stageId)
                        ?: WeekPlan(stageId = stageId, weekNo = weekNo, theme = "第 $weekNo 周")
                }
                db.weekPlanDao().insertAll(merged)
            } else {
                val id = db.stagePlanDao().insert(plan)
                weeks.forEach { w ->
                    db.weekPlanDao().insert(w.copy(stageId = id))
                }
            }
            onDone()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (stageId > 0) "编辑阶段计划" else "新建阶段计划",
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            label = { Text("目标（如：三个月内比赛中稳定使用反手拧拉）") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("开始日期") },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("结束日期") },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weeklyTimes,
                onValueChange = { weeklyTimes = it.filter { c -> c.isDigit() }.take(1) },
                label = { Text("每周次数 (1-7)") },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = level,
                onValueChange = { level = it },
                label = { Text("水平描述") },
                modifier = Modifier.weight(1f),
            )
        }

        Text("训练类型偏好", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            typeOptions.forEach { t ->
                FilterChip(
                    selected = t in selectedTypes,
                    onClick = {
                        selectedTypes = if (t in selectedTypes) selectedTypes - t
                        else selectedTypes + t
                    },
                    label = { Text(t) },
                )
            }
        }

        // AI 生成
        if (aiLoading) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp))
                Text("AI 正在制定计划…")
            }
        } else {
            Button(
                onClick = {
                    if (title.isBlank() || goal.isBlank()) {
                        Toast.makeText(context, "请先填写名称和目标", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val config = AiPlanService.configOrNull(context)
                    if (config == null) {
                        aiError = "尚未配置 AI，请先在「我的 → AI 设置」中配置"
                        return@Button
                    }
                    aiLoading = true
                    scope.launch {
                        try {
                            val raw = AiPlanService.generateStagePlan(
                                config,
                                goal = goal,
                                weeklyTimes = weeklyTimes.toIntOrNull() ?: 3,
                                types = selectedTypes.joinToString("、"),
                                level = level,
                            )
                            val parsed = PlanParser.parse(raw)
                            if (parsed == null || parsed.weeks.isEmpty()) {
                                aiError = "AI 返回格式无法解析，请重试"
                            } else {
                                aiPlan = parsed
                            }
                        } catch (e: Exception) {
                            aiError = "AI 请求失败：${e.message ?: "网络错误"}"
                        } finally {
                            aiLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("✨ 让 AI 帮我生成计划")
            }
        }

        // 保存
        Button(
            onClick = {
                if (title.isBlank() || goal.isBlank()) {
                    Toast.makeText(context, "请填写名称和目标", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val times = (weeklyTimes.toIntOrNull() ?: 3).coerceIn(1, 7)
                val now = StagePlan(
                    id = if (stageId > 0) stageId else 0,
                    title = title,
                    goal = goal,
                    startDate = startDate,
                    endDate = endDate,
                    weeklyTimes = times,
                    source = if (aiPlan != null) "AI" else "MANUAL",
                    status = "ACTIVE",
                )
                val weeks = (1..times).map { weekNo ->
                    WeekPlan(stageId = stageId, weekNo = weekNo, theme = "第 $weekNo 周")
                }
                save(now, weeks)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存计划")
        }

        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("取消")
        }
    }

    // AI 生成结果预览
    aiPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { aiPlan = null },
            title = { Text("AI 生成结果（草稿）") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("标题：${plan.title}", style = MaterialTheme.typography.titleSmall)
                    Text("共 ${plan.weeks.size} 周", style = MaterialTheme.typography.bodySmall)
                    plan.weeks.take(6).forEach { w ->
                        Column {
                            Text("第 ${w.weekNo} 周 · ${w.theme}", style = MaterialTheme.typography.titleSmall)
                            w.sessions.take(3).forEach { s ->
                                Text("  · ${s.type}：${s.content}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (w.sessions.size > 3) {
                                Text("  · 等 ${w.sessions.size} 节", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val times = (weeklyTimes.toIntOrNull() ?: 3).coerceIn(1, 7)
                    val stage = StagePlan(
                        id = if (stageId > 0) stageId else 0,
                        title = plan.title,
                        goal = goal,
                        startDate = startDate,
                        endDate = endDate,
                        weeklyTimes = times,
                        source = "AI",
                        status = "ACTIVE",
                    )
                    val weeks = plan.weeks.map { w ->
                        val arr = org.json.JSONArray()
                        w.sessions.forEach { s ->
                            arr.put(org.json.JSONObject().put("type", s.type).put("content", s.content))
                        }
                        WeekPlan(
                            stageId = stageId,
                            weekNo = w.weekNo,
                            theme = w.theme,
                            sessionsJson = arr.toString(),
                        )
                    }
                    save(stage, weeks, replaceWeeks = true)
                    aiPlan = null
                }) { Text("确认启用") }
            },
            dismissButton = {
                TextButton(onClick = { aiPlan = null }) { Text("返回修改") }
            },
        )
    }

    // AI 错误 / 引导
    aiError?.let { err ->
        AlertDialog(
            onDismissRequest = { aiError = null },
            title = { Text("提示") },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = { aiError = null }) { Text("知道了") }
            },
        )
    }
}
