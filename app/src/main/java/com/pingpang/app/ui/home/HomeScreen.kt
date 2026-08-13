package com.pingpang.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.TrainingTemplates
import com.pingpang.app.ui.common.PingPangViewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val gray = Color(0xFFD3D1C7)
private val blueLight = Color(0xFFB5D4F4)
private val blueMid = Color(0xFF378ADD)
private val blueDeep = Color(0xFF185FA5)

/**
 * 首页 · 训练看板：统计卡 / 阶段进度 / 14 天热力图 / 类型分布 / 待办 / 快捷操作。
 */
@Composable
fun HomeScreen(
    onCheckin: () -> Unit,
    onRecord: () -> Unit,
    onCompare: () -> Unit,
    onHistory: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as PingPangApp
    val vm: DashboardViewModel = viewModel(
        factory = PingPangViewModelFactory(
            stagePlanDao = app.database.stagePlanDao(),
            trainingDao = app.database.trainingSessionDao(),
            weekPlanDao = app.database.weekPlanDao(),
            videoClipDao = app.database.videoClipDao(),
        )
    )
    val month by vm.month.collectAsState()
    val heatmap by vm.heatmap.collectAsState()
    val types by vm.types.collectAsState()
    val stage by vm.stage.collectAsState()
    val todos by vm.todos.collectAsState()

    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.CHINA))

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 标题
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("乒乓训练助手", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text(today, style = MaterialTheme.typography.bodySmall)
        }

        // 统计卡
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("${month.cnt}", "本月训练", Modifier.weight(1f))
            StatCard("${month.mins / 60}h${month.mins % 60}m", "总时长", Modifier.weight(1f))
            StatCard("${month.rate}%", "完成率", Modifier.weight(1f))
            StatCard("${month.streak}", "连续天数", Modifier.weight(1f))
        }

        // 阶段计划进度
        stage?.let { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(s.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("剩 ${s.daysLeft} 天", style = MaterialTheme.typography.bodySmall)
                    }
                    Box(
                        Modifier.fillMaxWidth().height(8.dp)
                            .background(gray, RoundedCornerShape(4.dp)),
                    ) {
                        val pct = if (s.planned > 0) (s.done.toFloat() / s.planned).coerceIn(0f, 1f) else 0f
                        Box(
                            Modifier.fillMaxWidth(pct).fillMaxHeight()
                                .background(blueDeep, RoundedCornerShape(4.dp)),
                        )
                    }
                    Text(
                        "已完成 ${s.done}/${s.planned} 次 · ${s.weekInfo}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // 近 14 天热力图
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("近 14 天打卡", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    heatmap.forEach { mins ->
                        val color = when {
                            mins <= 0 -> gray
                            mins < 30 -> blueLight
                            mins < 60 -> blueMid
                            else -> blueDeep
                        }
                        Box(
                            Modifier.weight(1f).height(32.dp)
                                .background(color, RoundedCornerShape(6.dp)),
                        )
                    }
                }
                Text(
                    heatmap.sum().let {
                        if (it > 0) "14 天共训练 ${it} 分钟" else "最近 14 天还没有训练记录"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // 训练类型分布
        if (types.isNotEmpty()) {
            val total = types.sumOf { it.cnt }.coerceAtLeast(1)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("训练类型分布", style = MaterialTheme.typography.titleSmall)
                    types.forEach { t ->
                        val label = TrainingTemplates.typeLabel(t.type)
                        val pct = t.cnt * 100 / total
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(56.dp))
                            Box(
                                Modifier.weight(1f).height(10.dp)
                                    .background(gray, RoundedCornerShape(5.dp)),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(pct / 100f).fillMaxHeight()
                                        .background(barColor(t.type), RoundedCornerShape(5.dp)),
                                )
                            }
                            Text("$pct%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }

        // 待办提醒
        if (todos.pendingSessions > 0 || todos.unlinkedVideos > 0) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("待办提醒", style = MaterialTheme.typography.titleSmall)
                    if (todos.pendingSessions > 0) {
                        Text("· ${todos.pendingSessions} 节计划训练课未打卡", style = MaterialTheme.typography.bodySmall)
                    }
                    if (todos.unlinkedVideos > 0) {
                        Text("· ${todos.unlinkedVideos} 个视频未关联复盘", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        HorizontalDivider()

        // 快捷操作
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCheckin, modifier = Modifier.weight(1f)) { Text("记录训练") }
            OutlinedButton(onClick = onRecord, modifier = Modifier.weight(1f)) { Text("拍视频") }
        }
        OutlinedButton(onClick = onCompare, modifier = Modifier.fillMaxWidth()) {
            Text("多视频比对工作台")
        }
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
            Text("全部训练记录")
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun barColor(type: String): Color = when (type) {
    "MULTI_BALL" -> blueDeep
    "SINGLE_BALL" -> Color(0xFF0F6E56)
    else -> Color(0xFFBA7517)
}
