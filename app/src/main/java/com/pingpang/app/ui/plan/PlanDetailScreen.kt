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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.JsonUtils
import com.pingpang.app.data.TrainingTemplates
import com.pingpang.app.data.model.WeekPlan
import kotlinx.coroutines.launch

/**
 * 阶段计划详情（F02/F04）：周切换 + 该周训练课 + 打卡入口 + 添加训练课。
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

    var stage by remember { mutableStateOf<com.pingpang.app.data.model.StagePlan?>(null) }
    var weeks by remember { mutableStateOf<List<WeekPlan>>(emptyList()) }
    var weekIndex by remember { mutableStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(stageId) {
        stage = db.stagePlanDao().getById(stageId)
        weeks = db.weekPlanDao().forStage(stageId)
    }

    val currentWeek = weeks.getOrNull(weekIndex)

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

        Text(
            currentWeek?.theme ?: "暂无周计划",
            style = MaterialTheme.typography.titleMedium,
        )

        val sessions = currentWeek?.let { JsonUtils.sessionsFromJson(it.sessionsJson) } ?: emptyList()
        if (sessions.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("本周还没有训练课")
                    Text("点击下方按钮添加训练内容，或在球场直接打卡", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions) { (type, content) ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("[$type] $content", style = MaterialTheme.typography.bodyMedium)
                            }
                            Button(
                                onClick = { onCheckin(stageId, content) },
                            ) {
                                Text("打卡")
                            }
                        }
                    }
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
                    weeks = db.weekPlanDao().forStage(stageId)
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
                        db.stagePlanDao().deleteById(stageId)
                        db.weekPlanDao().deleteByStage(stageId)
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
