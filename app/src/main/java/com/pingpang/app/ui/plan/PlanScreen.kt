package com.pingpang.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.pingpang.app.data.db.StagePlanDao
import com.pingpang.app.data.model.StagePlan
import com.pingpang.app.ui.common.PingPangViewModelFactory

/**
 * 训练计划页（MVP 核心①）。
 * 骨架：展示阶段计划列表；"新建计划（含 AI 生成）"入口待 V1.0 完善。
 */
@Composable
fun PlanScreen() {
    val dao: StagePlanDao =
        (LocalContext.current.applicationContext as PingPangApp).database.stagePlanDao()
    val vm: PlanViewModel = viewModel(factory = PingPangViewModelFactory(stagePlanDao = dao))
    val plans by vm.plans.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("训练计划", style = MaterialTheme.typography.headlineSmall)
            Text("阶段计划 → 周计划，支持 AI 辅助生成", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Button(
                onClick = { /* TODO V1.0：新建阶段计划（手动 / AI 生成） */ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("＋ 新建阶段计划（支持 AI 生成）")
            }
        }
        item { HorizontalDivider() }
        if (plans.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("还没有阶段计划")
                        Text(
                            "点击上方按钮创建，可输入目标让 AI 帮你拆解到每周",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        } else {
            items(plans) { plan: StagePlan ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(plan.title, style = MaterialTheme.typography.titleMedium)
                        Text(plan.goal, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${plan.startDate} ~ ${plan.endDate} · 每周 ${plan.weeklyTimes} 次 · " +
                                if (plan.source == "AI") "AI 生成" else "手动",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
