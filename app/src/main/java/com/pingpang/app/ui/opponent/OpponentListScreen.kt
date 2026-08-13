package com.pingpang.app.ui.opponent

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import com.pingpang.app.data.JsonUtils
import com.pingpang.app.data.model.Opponent
import kotlinx.coroutines.launch

/** 对手列表（多个对手档案） */
@Composable
fun OpponentListScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = (context.applicationContext as PingPangApp).database.opponentDao()
    val opponents by dao.observeAll().collectAsState(initial = emptyList())

    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("‹ 返回") }
                Text("对手档案", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "新建对手")
                }
            }
            Text(
                "维护每个对手的技术特长，交手后补充执行反馈，AI 会结合你的特长给出应对建议",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (opponents.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("还没有对手档案")
                        Text("点击右上角 + 添加一位对手", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        items(opponents, key = { it.id }) { opp ->
            Card(
                onClick = { onOpen(opp.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(opp.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            buildString {
                                append("特长 ${JsonUtils.stringToProblems(opp.skillsJson).size} 项")
                                append(" · 反馈 ${JsonUtils.stringToProblems(opp.feedbackJson).size} 条")
                                if (opp.notes.isNotBlank()) append(" · ${opp.notes}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false; newName = "" },
            title = { Text("新建对手") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("对手称呼（如：老张 / 李教练）") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        dao.insert(Opponent(name = newName.trim().ifBlank { "未命名对手" }))
                        showAdd = false
                        newName = ""
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false; newName = "" }) { Text("取消") }
            },
        )
    }
}
