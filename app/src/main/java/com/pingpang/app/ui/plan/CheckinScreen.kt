package com.pingpang.app.ui.plan

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.JsonUtils
import com.pingpang.app.data.TrainingTemplates
import com.pingpang.app.data.model.TrainingSession
import com.pingpang.app.data.model.VideoClip
import com.pingpang.app.util.LocalImage
import com.pingpang.app.util.MediaHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 训练打卡（F06）。planId = -1 表示临时训练；editSessionId > 0 表示编辑已有记录。
 * 交互优化：未保存返回需确认；照片可预览删除；可关联视频库视频。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckinScreen(
    planId: Long,
    prefillContent: String,
    editSessionId: Long = -1L,
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
    var videoIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var dirty by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var showVideoPicker by remember { mutableStateOf(false) }
    var allVideos by remember { mutableStateOf<List<VideoClip>>(emptyList()) }
    var previewPhoto by remember { mutableStateOf<String?>(null) }
    var templateCategory by remember { mutableStateOf<String?>(null) }

    // 编辑模式：预填已有记录（不置 dirty，避免误判未保存）
    LaunchedEffect(editSessionId) {
        if (editSessionId > 0) {
            db.trainingSessionDao().getById(editSessionId)?.let { s ->
                content = s.content
                type = s.type
                completed = s.completed
                duration = if (s.durationMin > 0) s.durationMin.toString() else ""
                val stats = JsonUtils.stringToStats(s.statsJson)
                groups = stats["groups"]?.toString() ?: ""
                reps = stats["reps"]?.toString() ?: ""
                hitRate = stats["hitRate"]?.toString() ?: ""
                notes = s.notes
                photos = JsonUtils.stringToList(s.photosJson)
                videoIds = JsonUtils.stringToList(s.videosJson).mapNotNull { it.toLongOrNull() }
            }
        }
    }

    LaunchedEffect(Unit) {
        allVideos = db.videoClipDao().observeAll().first()
    }

    // 未保存修改时，返回需二次确认（PRD §5.5 边界：未保存直接返回 → 确认丢弃）
    BackHandler(enabled = dirty) { showDiscard = true }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        scope.launch {
            val copied = uris.mapNotNull { MediaHelper.copyToInternal(context, it, "photos") }
            photos = (photos + copied).take(6)
            if (copied.isNotEmpty()) dirty = true
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (dirty) showDiscard = true else onDone() }) { Text("‹ 返回") }
            Text(
                if (editSessionId > 0) "编辑训练记录" else "训练打卡",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        // 训练模板：按分类选择，点击自动填入内容 + 训练类型 + 预设组数
        Text("训练模板（点击选用，也可手动填写）", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "全部", "MULTI_BALL" to "多球", "SINGLE_BALL" to "单球", "SERVE_RECEIVE" to "发接发").forEach { (v, label) ->
                FilterChip(selected = templateCategory == v, onClick = { templateCategory = v }, label = { Text(label) })
            }
        }
        val visibleTemplates = remember(templateCategory) {
            TrainingTemplates.all.filter { templateCategory == null || it.type == templateCategory }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            visibleTemplates.forEach { t ->
                FilterChip(
                    selected = content == t.content,
                    onClick = {
                        content = t.content
                        type = t.type
                        if (t.defaultGroups > 0) groups = t.defaultGroups.toString()
                        dirty = true
                    },
                    label = { Text(t.content, maxLines = 1) },
                )
            }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = content,
            onValueChange = { content = it; dirty = true },
            label = { Text("训练内容（可自定义，如：正手起下旋 20 个）") },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("训练类型", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MULTI_BALL" to "多球", "SINGLE_BALL" to "单球", "SERVE_RECEIVE" to "发接发").forEach { (v, label) ->
                FilterChip(selected = type == v, onClick = { type = v; dirty = true }, label = { Text(label) })
            }
        }

        HorizontalDivider()

        Text("完成情况", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("DONE" to "完成", "PARTIAL" to "部分完成", "SKIPPED" to "跳过").forEach { (v, label) ->
                FilterChip(selected = completed == v, onClick = { completed = v; dirty = true }, label = { Text(label) })
            }
        }

        HorizontalDivider()

        Text("量化数据（可选）", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = duration, onValueChange = { duration = it.filter { c -> c.isDigit() }; dirty = true },
                label = { Text("时长(分)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = groups, onValueChange = { groups = it.filter { c -> c.isDigit() }; dirty = true },
                label = { Text("组数") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = reps, onValueChange = { reps = it.filter { c -> c.isDigit() }; dirty = true },
                label = { Text("次数") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = hitRate, onValueChange = { hitRate = it.filter { c -> c.isDigit() }.take(3); dirty = true },
                label = { Text("命中率%") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        HorizontalDivider()

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it; dirty = true },
            label = { Text("问题与收获") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("照片（最多 6 张，点击 × 可删除）", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos.size) { i ->
                Box {
                    LocalImage(
                        path = photos[i],
                        modifier = Modifier.size(80.dp).aspectRatio(1f).clickable { previewPhoto = photos[i] },
                        contentDescription = null,
                    )
                    IconButton(
                        onClick = {
                            photos = photos.filterIndexed { idx, _ -> idx != i }
                            dirty = true
                        },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "删除照片", modifier = Modifier.size(16.dp))
                    }
                }
            }
            item {
                OutlinedButton(onClick = { photoPicker.launch("image/*") }) { Text("+") }
            }
        }

        Text("关联视频（观看记录用）", style = MaterialTheme.typography.titleSmall)
        if (videoIds.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(videoIds.size) { i ->
                    val v = allVideos.find { it.id == videoIds[i] }
                    FilterChip(
                        selected = true,
                        onClick = {
                            videoIds = videoIds.filterIndexed { idx, _ -> idx != i }
                            dirty = true
                        },
                        label = { Text("${v?.date ?: videoIds[i]} ×") },
                    )
                }
            }
        }
        OutlinedButton(
            onClick = { if (allVideos.isEmpty()) Toast.makeText(context, "视频库为空，请先导入/录制视频", Toast.LENGTH_SHORT).show() else showVideoPicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (videoIds.isEmpty()) "＋ 关联视频" else "＋ 添加更多视频") }

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
                scope.launch {
                    val existing = if (editSessionId > 0) db.trainingSessionDao().getById(editSessionId) else null
                    if (existing != null) {
                        db.trainingSessionDao().update(
                            existing.copy(
                                type = type,
                                content = content,
                                durationMin = duration.toIntOrNull() ?: 0,
                                completed = completed,
                                statsJson = JsonUtils.statsToString(stats),
                                notes = notes,
                                photosJson = JsonUtils.listToString(photos),
                                videosJson = JsonUtils.listToString(videoIds.map { it.toString() }),
                            )
                        )
                        Toast.makeText(context, "已更新", Toast.LENGTH_SHORT).show()
                    } else {
                        db.trainingSessionDao().insert(
                            TrainingSession(
                                planId = if (planId > 0) planId else null,
                                date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                                type = type,
                                content = content,
                                durationMin = duration.toIntOrNull() ?: 0,
                                completed = completed,
                                statsJson = JsonUtils.statsToString(stats),
                                notes = notes,
                                photosJson = JsonUtils.listToString(photos),
                                videosJson = JsonUtils.listToString(videoIds.map { it.toString() }),
                            )
                        )
                        Toast.makeText(context, "已记录", Toast.LENGTH_SHORT).show()
                    }
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存打卡记录")
        }
    }

    // 未保存修改确认
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("放弃本次打卡？") },
            text = { Text("返回将丢失尚未保存的填写内容。") },
            confirmButton = {
                TextButton(onClick = { onDone() }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text("继续填写") }
            },
        )
    }

    // 视频选择
    // 视频选择
    if (showVideoPicker) {
        AlertDialog(
            onDismissRequest = { showVideoPicker = false },
            title = { Text("选择关联视频") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(allVideos.size) { i ->
                        val v = allVideos[i]
                        val selected = v.id in videoIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                videoIds = if (selected) videoIds - v.id else videoIds + v.id
                                dirty = true
                            },
                            label = { Text("${v.date} · ${v.durationMs / 1000}s") },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVideoPicker = false }) { Text("完成") } },
            dismissButton = { TextButton(onClick = { showVideoPicker = false }) { Text("关闭") } },
        )
    }

    previewPhoto?.let { path ->
        com.pingpang.app.util.FullscreenPhotoPreview(path = path, onDismiss = { previewPhoto = null })
    }
}