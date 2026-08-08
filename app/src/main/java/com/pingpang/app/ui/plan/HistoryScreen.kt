package com.pingpang.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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

/** 训练记录列表（全部历史） */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val context = LocalContext.current
    val dao = (context.applicationContext as PingPangApp).database.trainingSessionDao()
    val vm: HomeViewModel = viewModel(factory = PingPangViewModelFactory(trainingDao = dao))
    val sessions by vm.sessions.collectAsState(initial = emptyList())

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("‹ 返回") }
                Text("全部训练记录", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (sessions.isEmpty()) {
            item {
                Text("还没有训练记录", style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(sessions, key = { it.id }) { s: TrainingSession ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                    OutlinedButton(onClick = { onOpen(s.id) }) { Text("详情") }
                }
            }
        }
    }
}
