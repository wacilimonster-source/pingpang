package com.pingpang.app.ui.plan

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.DataCleaner
import com.pingpang.app.data.JsonUtils
import com.pingpang.app.data.TrainingTemplates
import com.pingpang.app.data.model.TrainingSession
import com.pingpang.app.data.model.WeekPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 阶段计划详情（F02/F04）：周切换 + 训练课（状态徽标）+ 快速打卡面板 + 添加训练课。
 */
@Composable
fun PlanDetailScreen(
    stageId: Long,
    onEdit: () -> Unit,
    onCheckin: (planId: Long, content: String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as PingPangApp).database

    var weekIndex by remember { mutableStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var quickCheckin by remember { mutableStateOf<Pair<String, String>?>(null) } // (type, content)

    // Flow 响应式：DB 变化自动刷新
    val stage by db.stagePlanDao().observeById(stageId).collectAsState(initial = null)
    val weeks by db.weekPlanDao().observeForStage(stageId).collectAsState(initial = emptyList())
    val allSessions by db.trainingSessionDao().observeAll().collectAsState(initial = emptyList())

    LaunchedEffect(weeks.size, weeks.lastOrNull()?.id) {
        if (weekIndex >= weeks.size) weekIndex = (weeks.size - 1).coerceAtLeast(0)
    }

    val currentWeek = weeks.getOrNull(weekIndex)

    // 当前周日期范围（阶段开始日 + weekNo 推算）
    val weekRange = remember(stage, currentWeek) {
        if (stage == null || currentWeek == null) null
        else {
            val start = try {
                LocalDate.parse(stage!!.startDate).plusWeeks((currentWeek.weekNo - 1).toLong())
            } catch (e: Exception) { LocalDate.now() }
            start to start.plusDays(6)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            Text(
                stage?.title ?: "阶段计划",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "编辑") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
        }

        stage?.let { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.goal, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${s.startDate} ~ ${s.endDate} · 每周 ${s.weeklyTimes} 次 · " +
                            if (s.source == "AI") "AI 生成" else "手动",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (weeks.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weeks.forEachIndexed { i, w ->
                    FilterChip(
                        selected = i == weekIndex,
                        onClick = { weekIndex = i },
                        label = { Text("第 ${w.weekNo} 周") },
                    )
                }
            }
        }

        HorizontalDivider()

        Text(currentWeek?.theme ?: "暂无周计划", style = MaterialTheme.typography.titleMedium)

        val sessions = currentWeek?.let { JsonUtils.sessionsFromJson(it.sessionsJson) } ?: emptyList()
        if (sessions.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("本周还没有训练课")
                    Text("点击下方按钮添加训练内容，点击卡片即可快速打卡", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions) { (type, content) ->
                    SessionRow(
                        type = type,
                        content = content,
                        status = findStatus(allSessions, content, weekRange),
                        onClick = { quickCheckin = type to content },
                        onDetail = { onCheckin(stageId, content) },
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("添加训练课（用模板）")
        }
    }

    // 快速打卡面板
    quickCheckin?.let { (type, content) ->
        QuickCheckinSheet(
            stageId = stageId,
            type = type,
            content = content,
            existing = findExisting(allSessions, content, weekRange),
            onDismiss = { quickCheckin = null },
            onFull = {
                quickCheckin = null
                onCheckin(stageId, content)
            },
        )
    }

    if (showAddDialog) {
        AddSessionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { type, content ->
                scope.launch {
                    val w = currentWeek ?: return@launch
                    val list = JsonUtils.sessionsFromJson(w.sessionsJson).toMutableList()
                    list.add(type to content)
                    val arr = org.json.JSONArray()
                    list.forEach { (t, c) ->
                        arr.put(org.json.JSONObject().put("type", t).put("content", c))
                    }
                    val updated = w.copy(sessionsJson = arr.toString())
                    if (updated.id > 0) {
                        db.weekPlanDao().update(updated)
                    } else {
                        db.weekPlanDao().insert(updated)
                    }
                    showAddDialog = false
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除阶段计划？") },
            text = { Text("将同时删除该计划下的周计划和训练记录，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            db.trainingSessionDao().forPlan(stageId).forEach { session ->
                                DataCleaner.deleteSessionPhotos(session)
                            }
                        }
                        db.trainingSessionDao().deleteByPlan(stageId)
                        db.weekPlanDao().deleteByStage(stageId)
                        db.stagePlanDao().deleteById(stageId)
                        onBack()
                    }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }
}

/** 训练课卡片：内容 + 状态徽标 + 点击快速打卡 */
@Composable
private fun SessionRow(
    type: String,
    content: String,
    status: TrainingSession?,
    onClick: () -> Unit,
    onDetail: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("[$type] $content", style = MaterialTheme.typography.bodyMedium)
                Text(
                    statusText(status),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor(status),
                )
            }
            if (status != null) {
                TextButton(onClick = onDetail) { Text("详情") }
            } else {
                TextButton(onClick = onDetail) { Text("完整记录") }
            }
        }
    }
}

/** 快速打卡底部面板：状态 + 时长 + 命中率，一步保存 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCheckinSheet(
    stageId: Long,
    type: String,
    content: String,
    existing: TrainingSession?,
    onDismiss: () -> Unit,
    onFull: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as PingPangApp).database
    val sheetState = rememberModalBottomSheetState()

    var completed by remember { mutableStateOf(existing?.completed ?: "DONE") }
    var duration by remember { mutableStateOf(existing?.durationMin?.toString() ?: "") }
    var hitRate by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (existing != null) {
            hitRate = JsonUtils.stringToStats(existing.statsJson)["hitRate"]?.toString() ?: ""
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("快速打卡 · $content", style = MaterialTheme.typography.titleMedium)
            if (existing != null) {
                Text("已有记录，保存将更新（${existing.date}）", style = MaterialTheme.typography.bodySmall)
            }

            Text("完成情况", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("DONE" to "完成", "PARTIAL" to "部分", "SKIPPED" to "跳过").forEach { (v, label) ->
                    FilterChip(selected = completed == v, onClick = { completed = v }, label = { Text(label) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("时长(分)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = hitRate,
                    onValueChange = { hitRate = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("命中率%") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (saving) return@Button
                        saving = true
                        scope.launch {
                            val stats = mutableMapOf<String, Int>()
                            duration.toIntOrNull()?.let { stats["durationMin"] = it }
                            hitRate.toIntOrNull()?.let { stats["hitRate"] = it }
                            val base = existing ?: TrainingSession(
                                planId = stageId,
                                date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                                type = type,
                                content = content,
                                durationMin = 0,
                                completed = completed,
                                statsJson = "{}",
                                notes = "",
                                photosJson = "[]",
                                videosJson = "[]",
                            )
                            val updated = base.copy(
                                completed = completed,
                                durationMin = duration.toIntOrNull() ?: 0,
                                statsJson = JsonUtils.statsToString(stats),
                            )
                            if (existing != null) {
                                db.trainingSessionDao().update(updated)
                            } else {
                                db.trainingSessionDao().insert(updated)
                            }
                            saving = false
                            Toast.makeText(context, "已打卡", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (saving) "保存中…" else "保存")
                }
                OutlinedButton(onClick = onFull, modifier = Modifier.weight(1f)) {
                    Text("完整记录")
                }
            }
        }
    }
}

/** 找到当前周内同内容的打卡记录 */
private fun findStatus(
    sessions: List<TrainingSession>,
    content: String,
    weekRange: Pair<LocalDate, LocalDate>?,
): TrainingSession? = findExisting(sessions, content, weekRange)

private fun findExisting(
    sessions: List<TrainingSession>,
    content: String,
    weekRange: Pair<LocalDate, LocalDate>?,
): TrainingSession? {
    if (weekRange == null) return null
    return sessions.firstOrNull { s ->
        s.content == content && try {
            val d = LocalDate.parse(s.date)
            !d.isBefore(weekRange.first) && !d.isAfter(weekRange.second)
        } catch (e: Exception) { false }
    }
}

private fun statusText(s: TrainingSession?): String = when (s?.completed) {
    "DONE" -> "已完成 ✓"
    "PARTIAL" -> "部分完成 ◐"
    "SKIPPED" -> "已跳过"
    else -> "未打卡"
}

private fun statusColor(s: TrainingSession?): Color = when (s?.completed) {
    "DONE" -> Color(0xFF3B6D11)
    "PARTIAL" -> Color(0xFF854F0B)
    "SKIPPED" -> Color(0xFF5F5E5A)
    else -> Color(0xFF888780)
}

/** 添加训练课：模板列表 + 自定义 */
@Composable
private fun AddSessionDialog(
    onDismiss: () -> Unit,
    onAdd: (type: String, content: String) -> Unit,
) {
    val templates = TrainingTemplates.all
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加训练课") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(templates) { t ->
                    TextButton(onClick = { onAdd(t.type, t.content) }) {
                        Text("${TrainingTemplates.typeLabel(t.type)} · ${t.content}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}
