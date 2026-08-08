package com.pingpang.app.ui.plan

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.JsonUtils
import com.pingpang.app.data.model.TrainingSession
import com.pingpang.app.util.LocalImage
import com.pingpang.app.util.MediaHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 训练打卡（F06）。planId = -1 表示临时训练。
 */
@Composable
fun CheckinScreen(
    planId: Long,
    prefillContent: String,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as PingPangApp).database

    var content by remember { mutableStateOf(prefillContent) }
    var type by remember { mutableStateOf("MULTI_BALL") }
    var completed by remember { mutableStateOf("DONE") }
    var duration by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var hitRate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        scope.launch {
            val copied = uris.mapNotNull { MediaHelper.copyToInternal(context, it, "photos") }
            photos = (photos + copied).take(6)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedButton(onClick = onDone) { Text("‹ 返回") }
            Text(
                "训练打卡",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("训练内容") },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("训练类型", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MULTI_BALL" to "多球", "SINGLE_BALL" to "单球", "SERVE_RECEIVE" to "发接发").forEach { (v, label) ->
                FilterChip(selected = type == v, onClick = { type = v }, label = { Text(label) })
            }
        }

        HorizontalDivider()

        Text("完成情况", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("DONE" to "完成", "PARTIAL" to "部分完成", "SKIPPED" to "跳过").forEach { (v, label) ->
                FilterChip(selected = completed == v, onClick = { completed = v }, label = { Text(label) })
            }
        }

        HorizontalDivider()

        Text("量化数据（可选）", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = duration, onValueChange = { duration = it.filter { c -> c.isDigit() } },
                label = { Text("时长(分)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = groups, onValueChange = { groups = it.filter { c -> c.isDigit() } },
                label = { Text("组数") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = reps, onValueChange = { reps = it.filter { c -> c.isDigit() } },
                label = { Text("次数") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = hitRate, onValueChange = { hitRate = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text("命中率%") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        HorizontalDivider()

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("问题与收获") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("照片（最多 6 张）", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos.size) { i ->
                LocalImage(
                    path = photos[i],
                    modifier = Modifier.size(80.dp).aspectRatio(1f),
                    contentDescription = null,
                )
            }
            item {
                OutlinedButton(onClick = { photoPicker.launch("image/*") }) { Text("+") }
            }
        }

        Button(
            onClick = {
                if (content.isBlank()) {
                    Toast.makeText(context, "请填写训练内容", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val stats = mutableMapOf<String, Int>()
                duration.toIntOrNull()?.let { stats["durationMin"] = it }
                groups.toIntOrNull()?.let { stats["groups"] = it }
                reps.toIntOrNull()?.let { stats["reps"] = it }
                hitRate.toIntOrNull()?.let { stats["hitRate"] = it }
                val session = TrainingSession(
                    planId = if (planId > 0) planId else null,
                    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    type = type,
                    content = content,
                    durationMin = duration.toIntOrNull() ?: 0,
                    completed = completed,
                    statsJson = JsonUtils.statsToString(stats),
                    notes = notes,
                    photosJson = JsonUtils.listToString(photos),
                    videosJson = "[]",
                )
                scope.launch {
                    db.trainingSessionDao().insert(session)
                    Toast.makeText(context, "已记录", Toast.LENGTH_SHORT).show()
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存打卡记录")
        }
    }
}
