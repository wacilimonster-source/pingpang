package com.pingpang.app.ui.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.model.VideoClip
import com.pingpang.app.ui.common.PingPangViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

private val speeds = listOf(0.25f, 0.5f, 1f)

/**
 * 多视频比对工作台（F10，核心亮点）：
 * 同屏 2-3 路播放、每路独立双指缩放/平移对齐、同步播放、全局倍速、截图。
 */
@Composable
fun CompareScreen(
    initialIds: List<Long>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val db = (context.applicationContext as PingPangApp).database
    val dao = db.videoClipDao()
    val vm: VideoViewModel = viewModel(factory = PingPangViewModelFactory(videoClipDao = dao))
    val allClips by vm.clips.collectAsState(initial = emptyList())

    var selectedIds by remember { mutableStateOf(initialIds.distinct().take(3)) }
    var showAdd by remember { mutableStateOf(false) }
    var syncEnabled by remember { mutableStateOf(true) }
    var speed by remember { mutableStateOf(1f) }

    val clips = selectedIds.mapNotNull { id -> allClips.find { it.id == id } }

    // 每路播放器实例：选中集变化时重建并释放旧实例；媒体数据异步到达后再绑定，避免时序 bug
    var players by remember { mutableStateOf(emptyList<ExoPlayer>()) }
    LaunchedEffect(selectedIds) {
        players.forEach { it.release() }
        players = selectedIds.map { ExoPlayer.Builder(context).build() }
    }
    LaunchedEffect(players) {
        players.forEachIndexed { i, p ->
            val id = selectedIds.getOrNull(i) ?: return@forEachIndexed
            val path = allClips.find { it.id == id }?.filePath
            if (path != null && p.currentMediaItem == null) {
                p.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
                p.prepare()
            }
        }
    }

    // 绑定 Activity Lifecycle：后台暂停、前台恢复（不随页面重建释放）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> players.forEach { it.pause() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            players.forEach { it.release() }
        }
    }

    // 同步播放：以第 1 路为基准，仅当偏差过大时对齐（避免频繁 seekTo 抖动）
    LaunchedEffect(syncEnabled, players) {
        if (!syncEnabled || players.size <= 1) return@LaunchedEffect
        while (isActive) {
            val master = players[0]
            if (master.isPlaying && master.duration > 0) {
                val pos = master.currentPosition
                players.drop(1).forEach { p ->
                    if (abs(p.currentPosition - pos) > 500) {
                        p.seekTo(pos.coerceIn(0, master.duration.coerceAtLeast(pos)))
                    }
                    if (!p.isPlaying) p.play()
                }
            } else if (!master.isPlaying) {
                players.drop(1).forEach { p -> if (p.isPlaying) p.pause() }
            }
            delay(250)
        }
    }

    // 全局倍速
    LaunchedEffect(speed, players) {
        players.forEach { it.setPlaybackSpeed(speed) }
    }

    // 各路的 PlayerView 引用（截图用）
    val playerViews = remember { mutableMapOf<Int, PlayerView>() }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("‹ 返回") }
            Text("比对工作台", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (clips.size < 3) {
                OutlinedButton(onClick = { showAdd = true }) { Text("＋添加") }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("同步播放", style = MaterialTheme.typography.bodySmall)
            Switch(checked = syncEnabled, onCheckedChange = { syncEnabled = it })
            speeds.forEach { s ->
                FilterChip(
                    selected = speed == s,
                    onClick = { speed = s },
                    label = { Text(if (s == 1f) "1×" else s.toString() + "×") },
                )
            }
            OutlinedButton(onClick = {
                playerViews[0]?.let { view ->
                    val bmp = captureView(view)
                    val dir = File(context.filesDir, "screenshots").apply { mkdirs() }
                    val f = File(dir, "cmp_${System.currentTimeMillis()}.png")
                    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    Toast.makeText(context, "已保存截图", Toast.LENGTH_SHORT).show()
                }
            }) { Text("截图") }
        }

        // 分屏播放区
        when {
            clips.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("请先添加视频（最多 3 段）")
                }
            }
            clips.size == 1 -> {
                ComparePane(
                    clips[0], players.getOrNull(0), playerViews, 0,
                    onRemove = { selectedIds = selectedIds - clips[0].id },
                    Modifier.fillMaxWidth().weight(1f),
                )
            }
            clips.size == 2 -> {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComparePane(
                        clips[0], players.getOrNull(0), playerViews, 0,
                        onRemove = { selectedIds = selectedIds - clips[0].id },
                        Modifier.weight(1f).fillMaxSize(),
                    )
                    ComparePane(
                        clips[1], players.getOrNull(1), playerViews, 1,
                        onRemove = { selectedIds = selectedIds - clips[1].id },
                        Modifier.weight(1f).fillMaxSize(),
                    )
                }
            }
            else -> {
                Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComparePane(
                        clips[0], players.getOrNull(0), playerViews, 0,
                        onRemove = { selectedIds = selectedIds - clips[0].id },
                        Modifier.fillMaxWidth().weight(1f),
                    )
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ComparePane(
                            clips[1], players.getOrNull(1), playerViews, 1,
                            onRemove = { selectedIds = selectedIds - clips[1].id },
                            Modifier.weight(1f).fillMaxSize(),
                        )
                        ComparePane(
                            clips[2], players.getOrNull(2), playerViews, 2,
                            onRemove = { selectedIds = selectedIds - clips[2].id },
                            Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }

        Text(
            "双指缩放、单指平移，对齐到同一部位（如手腕/引拍点）",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (showAdd) {
        val candidates = allClips.filter { it.id !in selectedIds }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加视频（最多 3 段）") },
            text = {
                if (candidates.isEmpty()) {
                    Text("视频库中没有更多视频")
                } else {
                    LazyColumn {
                        items(candidates.size) { i ->
                            val c = candidates[i]
                            TextButton(onClick = {
                                selectedIds = (selectedIds + c.id).take(3)
                                showAdd = false
                            }) {
                                Text("${c.date} · ${c.durationMs / 1000}s")
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAdd = false }) { Text("关闭") } },
        )
    }
}

/** 一路视频面板：播放器 + 独立缩放平移；顶部可移除该路视频 */
@Composable
private fun ComparePane(
    clip: VideoClip,
    player: ExoPlayer?,
    views: MutableMap<Int, PlayerView>,
    index: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 8f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Card(modifier) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
                .transformable(transformState),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        views[index] = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                Modifier.align(Alignment.TopStart).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(clip.date, style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "移除该视频", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** 把 PlayerView 内容绘制为 Bitmap（截图） */
private fun captureView(view: View): Bitmap {
    val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bmp))
    return bmp
}
