package com.pingpang.app.ui.opponent

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.pingpang.app.ai.AiPlanService
import com.pingpang.app.data.JsonUtils
import com.pingpang.app.data.model.Opponent
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 对手详情：技术特长维护 + 执行反馈补充 + AI 应对建议。
 */
@Composable
fun OpponentDetailScreen(
    opponentId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as PingPangApp).database
    val dao = db.opponentDao()
    val mySkillDao = db.mySkillDao()

    val opp by dao.observeById(opponentId).collectAsState(initial = null)
    val mySkills by mySkillDao.observeAll().collectAsState(initial = emptyList())

    var editNotes by remember { mutableStateOf<String?>(null) }
    var editSkill by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showAddSkill by remember { mutableStateOf(false) }
    var addFeedbackText by remember { mutableStateOf<String?>(null) }
    var showTactics by remember { mutableStateOf(false) }
    var tacticsText by remember { mutableStateOf<String?>(null) }
    var tacticsLoading by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val o = opp
    if (o == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedButton(onClick = onBack) { Text("‹ 返回") }
            Text("对手不存在或已删除")
        }
        return
    }

    val skills = JsonUtils.stringToProblems(o.skillsJson)
    val feedbacks = JsonUtils.stringToProblems(o.feedbackJson)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("‹ 返回") }
            Text(o.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "删除对手") }
        }

        // 备注
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("打法备注", style = MaterialTheme.typography.titleSmall)
                Text(
                    o.notes.ifBlank { "（未填写，点击编辑补充打法风格、发球习惯等）" },
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { editNotes = o.notes }) { Text("编辑") }
            }
        }

        // 技术特长
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("技术特长", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showAddSkill = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加特长")
                    }
                }
                if (skills.isEmpty()) {
                    Text("还没有特长记录", style = MaterialTheme.typography.bodySmall)
                }
                skills.forEach { (name, desc) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("· $name", style = MaterialTheme.typography.bodyMedium)
                            if (desc.isNotBlank()) {
                                Text("  $desc", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = { editSkill = name to desc }) { Text("编辑") }
                    }
                }
            }
        }

        // 执行反馈
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("执行反馈", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { addFeedbackText = "" }) {
                        Icon(Icons.Filled.Add, contentDescription = "添加反馈")
                    }
                }
                if (feedbacks.isEmpty()) {
                    Text("还没有反馈记录", style = MaterialTheme.typography.bodySmall)
                }
                feedbacks.forEach { (date, content) ->
                    Column {
                        Text("[$date] $content", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        HorizontalDivider()

        // AI 应对建议
        if (tacticsLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp))
                Text("AI 正在分析应对策略…")
            }
        } else {
            Button(
                onClick = {
                    val config = AiPlanService.configOrNull(context)
                    if (config == null) {
                        Toast.makeText(context, "AI 未配置，请到「我的 → AI 设置」检查", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    tacticsLoading = true
                    showTactics = true
                    scope.launch {
                        try {
                            tacticsText = AiPlanService.generateTactics(
                                config = config,
                                mySkills = mySkills.map { it.name to it.description },
                                opponentName = o.name,
                                opponentSkills = skills,
                                opponentNotes = o.notes,
                                feedbacks = feedbacks,
                            )
                        } catch (e: Exception) {
                            tacticsText = "AI 请求失败：${e.message ?: "网络错误"}"
                        } finally {
                            tacticsLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("AI 分析如何应对（结合我的特长与执行反馈）")
            }
        }
    }

    // ---- 备注编辑 ----
    editNotes?.let { current ->
        var value by remember { mutableStateOf(current) }
        AlertDialog(
            onDismissRequest = { editNotes = null },
            title = { Text("编辑打法备注") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("打法风格、发球习惯、注意事项") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.update(o.copy(notes = value.trim(), updatedAt = System.currentTimeMillis()))
                        editNotes = null
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editNotes = null }) { Text("取消") } },
        )
    }

    // ---- 特长编辑/添加 ----
    if (showAddSkill || editSkill != null) {
        val target = editSkill
        var name by remember { mutableStateOf(target?.first ?: "") }
        var desc by remember { mutableStateOf(target?.second ?: "") }
        AlertDialog(
            onDismissRequest = { showAddSkill = false; editSkill = null },
            title = { Text(if (target == null) "添加技术特长" else "编辑技术特长") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("特长（如：正手爆冲）") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("描述（如：侧身抢攻威胁大）") },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) return@TextButton
                    scope.launch {
                        val list = skills.toMutableList()
                        if (target != null) list.remove(target)
                        list.add(name.trim() to desc.trim())
                        dao.update(o.copy(skillsJson = JsonUtils.problemsToString(list), updatedAt = System.currentTimeMillis()))
                        showAddSkill = false; editSkill = null
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSkill = false; editSkill = null }) { Text("取消") }
            },
        )
    }

    // ---- 添加反馈 ----
    addFeedbackText?.let { _ ->
        var value by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addFeedbackText = null },
            title = { Text("补充执行反馈") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("交手后复盘：对方表现、我用的策略效果如何") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (value.isBlank()) return@TextButton
                    scope.launch {
                        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val list = feedbacks.toMutableList()
                        list.add(date to value.trim())
                        dao.update(o.copy(feedbackJson = JsonUtils.problemsToString(list), updatedAt = System.currentTimeMillis()))
                        addFeedbackText = null
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { addFeedbackText = null }) { Text("取消") }
            },
        )
    }

    // ---- AI 建议结果 ----
    if (showTactics) {
        AlertDialog(
            onDismissRequest = { showTactics = false },
            title = { Text("AI 应对建议 · ${o.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (tacticsLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.padding(end = 8.dp))
                            Text("正在分析…")
                        }
                    } else {
                        Text(tacticsText ?: "无结果", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTactics = false }) { Text("关闭") }
            },
        )
    }

    // ---- 删除 ----
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除对手「${o.name}」？") },
            text = { Text("将删除该对手的特长与反馈记录，不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.delete(o)
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
