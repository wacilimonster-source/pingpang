package com.pingpang.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.TrainingTemplates
import com.pingpang.app.data.model.TrainingSession
import com.pingpang.app.ui.common.HomeViewModel
import com.pingpang.app.ui.common.PingPangViewModelFactory

/**
 * 首页：快捷打卡 / 当前阶段计划 / 快捷操作 / 最近训练。
 */
@Composable
fun HomeScreen(
    onCheckin: () -> Unit,
    onRecord: () -> Unit,
    onCompare: () -> Unit,
    onHistory: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val context = LocalContext.current
    val dao = (context.applicationContext as PingPangApp).database.trainingSessionDao()
    val vm: HomeViewModel = viewModel(factory = PingPangViewModelFactory(trainingDao = dao))
    val sessions by vm.sessions.collectAsState(initial = emptyList())

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("乒乓训练助手", style = MaterialTheme.typography.headlineSmall)
            Text("进阶 · 每周 3-4 次 · 数据全在本地", style = MaterialTheme.typography.bodySmall)
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("快捷操作", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onCheckin, modifier = Modifier.weight(1f)) { Text("记录训练") }
                        OutlinedButton(onClick = onRecord, modifier = Modifier.weight(1f)) { Text("拍视频") }
                    }
                    OutlinedButton(onClick = onCompare, modifier = Modifier.fillMaxWidth()) {
                        Text("多视频比对工作台")
                    }
                }
            }
        }

        item { HorizontalDivider() }

        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("最近训练", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onHistory) { Text("全部") }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("还没有训练记录")
                        Text("在「计划」中创建阶段计划，或在球场直接打卡", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(sessions.take(5), key = { it.id }) { s: TrainingSession ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${TrainingTemplates.typeLabel(s.type)} · ${s.content}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${s.date} · " + when (s.completed) {
                                    "DONE" -> "已完成"
                                    "PARTIAL" -> "部分完成"
                                    else -> "未完成"
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        OutlinedButton(onClick = { onOpenSession(s.id) }) { Text("详情") }
                    }
                }
            }
        }
    }
}
