package com.pingpang.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.db.TrainingSessionDao
import com.pingpang.app.ui.common.PingPangViewModelFactory

@Composable
fun HomeScreen() {
    val dao: TrainingSessionDao =
        (LocalContext.current.applicationContext as PingPangApp).database.trainingSessionDao()
    val vm: HomeViewModel = viewModel(factory = PingPangViewModelFactory(trainingDao = dao))
    val sessions by vm.sessions.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("今日训练", style = MaterialTheme.typography.headlineSmall)
            Text("记录训练 → 打开「计划」选择训练课快速打卡", style = MaterialTheme.typography.bodyMedium)
        }
        item { HorizontalDivider() }
        item {
            Text("最近训练", style = MaterialTheme.typography.titleMedium)
        }
        if (sessions.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("还没有训练记录")
                        Text(
                            "在「计划」中创建阶段计划并开始第一次训练吧",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        } else {
            items(sessions.take(10)) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(s.content.ifBlank { s.type }, style = MaterialTheme.typography.titleSmall)
                            Text(s.date, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            when (s.completed) {
                                "DONE" -> "已完成"
                                "PARTIAL" -> "部分完成"
                                else -> "未完成"
                            },
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
