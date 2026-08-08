package com.pingpang.app.ui.plan

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.pingpang.app.ai.AiPlanService
import com.pingpang.app.data.JsonUtils
import com.pingpang.app.data.TrainingTemplates
import com.pingpang.app.data.model.TrainingSession
import com.pingpang.app.util.LocalImage
import kotlinx.coroutines.launch

/**
 * 训练记录详情（F06 查看 + F07 AI 复盘）。
 */
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as PingPangApp).database

    var session by remember { mutableStateOf<TrainingSession?>(null) }
    var reviewText by remember { mutableStateOf<String?>(null) }
    var reviewLoading by remember { mutableStateOf(false) }
    var reviewError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        session = db.trainingSessionDao().getById(sessionId)
    }

    val s = session
    if (s == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedButton(onClick = onBack) { Text("‹ 返回") }
            Text("记录不存在或已删除")
        }
        return
    }

    val stats = JsonUtils.stringToStats(s.statsJson)
    val photos = JsonUtils.stringToList(s.photosJson)

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("‹ 返回") }
                Text("训练记录", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${TrainingTemplates.typeLabel(s.type)} · ${s.content}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${s.date} · " + when (s.completed) {
                            "DONE" -> "已完成"
                            "PARTIAL" -> "部分完成"
                            else -> "未完成"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (s.durationMin > 0 || stats.isNotEmpty()) {
                        Text(
                            buildString {
                                if (s.durationMin > 0) append("时长 ${s.durationMin} 分 ")
                                stats.forEach { (k, v) ->
                                    append("$k=$v ")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (s.notes.isNotBlank()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("问题与收获", style = MaterialTheme.typography.titleSmall)
                        Text(s.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (photos.isNotEmpty()) {
            item {
                Text("照片", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos.size) { i ->
                        LocalImage(path = photos[i], modifier = Modifier.size(100.dp).aspectRatio(1f), contentDescription = null)
                    }
                }
            }
        }
        item { HorizontalDivider() }
        item {
            Text("AI 复盘", style = MaterialTheme.typography.titleSmall)
            when {
                reviewLoading -> Row {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Text("AI 正在复盘…", Modifier.padding(start = 8.dp))
                }
                reviewText != null -> Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(reviewText!!, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    db.trainingSessionDao().update(s.copy(notes = s.notes + "\n\n【AI 复盘】\n" + reviewText!!))
                                    session = db.trainingSessionDao().getById(sessionId)
                                    Toast.makeText(context, "已采纳到笔记", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("采纳为笔记") }
                            OutlinedButton(onClick = { reviewText = null }) { Text("关闭") }
                        }
                    }
                }
                else -> Button(onClick = {
                    val config = AiPlanService.configOrNull(context)
                    if (config == null) {
                        reviewError = "尚未配置 AI，请先在「我的 → AI 设置」中配置"
                        return@Button
                    }
                    reviewLoading = true
                    scope.launch {
                        try {
                            val info = buildString {
                                append("日期：${s.date}\n")
                                append("内容：${s.content}\n")
                                append("完成情况：${s.completed}\n")
                                if (s.durationMin > 0) append("时长：${s.durationMin} 分\n")
                                stats.forEach { (k, v) -> append("$k：$v\n") }
                                if (s.notes.isNotBlank()) append("问题与收获：${s.notes}")
                            }
                            reviewText = AiPlanService.generateReview(config, info)
                        } catch (e: Exception) {
                            reviewError = "AI 请求失败：${e.message ?: "网络错误"}"
                        } finally {
                            reviewLoading = false
                        }
                    }
                }) { Text("生成 AI 复盘") }
            }
        }
        item {
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("删除记录")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除记录？") },
            text = { Text("删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        db.trainingSessionDao().deleteById(sessionId)
                        onBack()
                    }
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }

    reviewError?.let { err ->
        AlertDialog(
            onDismissRequest = { reviewError = null },
            title = { Text("提示") },
            text = { Text(err) },
            confirmButton = { TextButton(onClick = { reviewError = null }) { Text("知道了") } },
        )
    }
}
