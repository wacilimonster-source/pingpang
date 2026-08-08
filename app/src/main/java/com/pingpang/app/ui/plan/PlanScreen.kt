package com.pingpang.app.ui.plan

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.model.StagePlan
import com.pingpang.app.ui.common.PingPangViewModelFactory

/**
 * 计划列表（F02）：进行中/已结束 Tab。
 */
@Composable
fun PlanScreen(
    onNew: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val context = LocalContext.current
    val dao = (context.applicationContext as PingPangApp).database.stagePlanDao()
    val vm: PlanViewModel = viewModel(factory = PingPangViewModelFactory(stagePlanDao = dao))
    val plans by vm.plans.collectAsState(initial = emptyList())

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("训练计划", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Button(onClick = onNew) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("新建")
                }
            }
        }
        if (plans.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("还没有进行中的阶段计划")
                        Text("创建一个阶段计划（支持 AI 生成），把目标拆解到每周", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
                            Text("创建第一个阶段计划")
                        }
                    }
                }
            }
        }
        items(plans, key = { it.id }) { plan: StagePlan ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text(plan.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(if (plan.source == "AI") "AI" else "手动", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(plan.goal, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${plan.startDate} ~ ${plan.endDate} · 每周 ${plan.weeklyTimes} 次",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = { onOpen(plan.id) }) { Text("查看详情") }
                }
            }
        }
        item { HorizontalDivider() }
    }
}
