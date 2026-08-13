package com.pingpang.app.ui.skill

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
import androidx.compose.material3.Button
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
import com.pingpang.app.data.model.MySkill
import kotlinx.coroutines.launch

/** 我的技术特长管理（列表 + 增删改，持久保存） */
@Composable
fun SkillListScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = (context.applicationContext as PingPangApp).database.mySkillDao()
    val skills by dao.observeAll().collectAsState(initial = emptyList())

    var editing by remember { mutableStateOf<MySkill?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onBack) { Text("‹ 返回") }
                Text("我的技术特长", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加")
                }
            }
            Text(
                "维护自己的打法特长（如：正手弧圈、反手拧拉、发球抢攻），AI 应对建议会参考这些信息",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (skills.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("还没有技术特长")
                        Text("点击右上角 + 添加你的第一项特长", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        items(skills, key = { it.id }) { skill ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(skill.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editing = skill }) { Text("编辑") }
                    }
                    if (skill.description.isNotBlank()) {
                        Text(skill.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        val target = editing
        SkillEditDialog(
            initial = target,
            onDismiss = { showAdd = false; editing = null },
            onSave = { name, desc ->
                scope.launch {
                    if (target == null) {
                        dao.insert(MySkill(name = name, description = desc))
                    } else {
                        dao.update(target.copy(name = name, description = desc, updatedAt = System.currentTimeMillis()))
                    }
                    showAdd = false; editing = null
                }
            },
            onDelete = if (target == null) null else {
                {
                    scope.launch {
                        dao.delete(target)
                        editing = null
                    }
                }
            },
        )
    }
}

@Composable
private fun SkillEditDialog(
    initial: MySkill?,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加技术特长" else "编辑技术特长") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("特长名称（如：正手弧圈）") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述（如：旋转强、落点深）") },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(name.trim(), desc.trim())
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                onDelete?.let {
                    TextButton(onClick = it) { Text("删除") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
