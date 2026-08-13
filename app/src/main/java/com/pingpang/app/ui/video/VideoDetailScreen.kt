package com.pingpang.app.ui.video

import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.DataCleaner
import kotlinx.coroutines.launch
import java.io.File

/** 倍速档位：支持 0.1× 超慢速（部分机型音频可能失真，视频帧仍平滑） */
private val speeds = listOf(0.1f, 0.15f, 0.25f, 0.5f, 1f, 1.5f, 2f)

/**
 * 视频详情/播放（F09 增强）：
 * 全屏（横屏）、0.1×~2× 变速、暂停时逐帧步进（±1 帧）、删除、加入比对。
 */
@Composable
fun VideoDetailScreen(
    videoId: Long,
    onBack: () -> Unit,
    onAddToCompare: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as PingPangApp).database

    var speed by remember { mutableStateOf(1f) }
    var confirmDelete by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    val clip by db.videoClipDao().observeById(videoId).collectAsState(initial = null)

    val player = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(clip) {
        clip?.let {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(it.filePath))))
            player.prepare()
            player.playWhenReady = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> if (player.isPlaying) player.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    // 全屏：切横屏 + 系统栏隐藏；退出恢复
    val activity = context as? android.app.Activity
    fun applyFullscreen(full: Boolean) {
        activity?.requestedOrientation = if (full) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        if (full) {
            androidx.core.view.WindowInsetsControllerCompat(
                activity!!.window,
                activity!!.window.decorView,
            ).apply { hide(androidx.core.view.WindowInsetsCompat.Type.systemBars()) }
        } else {
            androidx.core.view.WindowInsetsControllerCompat(
                activity!!.window,
                activity!!.window.decorView,
            ).apply { show(androidx.core.view.WindowInsetsCompat.Type.systemBars()) }
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
        applyFullscreen(false)
    }

    val c = clip
    if (c == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedButton(onClick = onBack) { Text("‹ 返回") }
            Text("视频不存在或已删除")
        }
        return
    }

    // 全屏模式：只显示播放器 + 悬浮控制
    if (isFullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = {
                    isFullscreen = false
                    applyFullscreen(false)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color(0x88000000)),
            ) {
                Icon(Icons.Filled.FullscreenExit, contentDescription = "退出全屏", tint = Color.White)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("‹ 返回") }
            Text("视频播放", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            )
            IconButton(
                onClick = {
                    isFullscreen = true
                    applyFullscreen(true)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            ) {
                Icon(Icons.Filled.Fullscreen, contentDescription = "全屏")
            }
        }

        // 倍速档位（含 0.1× / 0.15× 超慢速）
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            speeds.forEach { s ->
                FilterChip(
                    selected = speed == s,
                    onClick = {
                        speed = s
                        player.setPlaybackSpeed(s)
                    },
                    label = { Text(if (s == 1f) "1×" else s.toString() + "×") },
                )
            }
        }

        // 逐帧步进（暂停时可用）：按视频帧率 ±1 帧
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("逐帧", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = {
                if (player.isPlaying) player.pause()
                val frameMs = frameDurationMs(player)
                player.seekTo((player.currentPosition - frameMs).coerceAtLeast(0L))
            }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "上一帧")
            }
            IconButton(onClick = {
                if (player.isPlaying) player.pause()
                val frameMs = frameDurationMs(player)
                val max = if (player.duration > 0) player.duration - 1 else Long.MAX_VALUE
                player.seekTo((player.currentPosition + frameMs).coerceAtMost(max))
            }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "下一帧")
            }
            Text("暂停后点击步进", style = MaterialTheme.typography.bodySmall)
        }

        Text(
            "${c.date} · ${if (c.source == "RECORDED") "录制" else "导入"} · ${c.durationMs / 1000} 秒",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAddToCompare(videoId) }, modifier = Modifier.weight(1f)) {
                Text("加入比对工作台")
            }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                Text("删除")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除视频？") },
            text = { Text("将从本地删除该视频文件及其记录。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        DataCleaner.removeVideoRefs(db, videoId)
                        db.videoClipDao().delete(c)
                        File(c.filePath).delete()
                        c.thumbPath?.let { File(it).delete() }
                        onBack()
                    }
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

/** 根据视频帧率计算单帧时长（毫秒），取不到帧率时按 30fps 兜底 */
private fun frameDurationMs(player: ExoPlayer): Long {
    val fps = player.videoFormat?.frameRate
    if (fps != null && fps > 0f) {
        return (1000f / fps).toLong().coerceAtLeast(10L)
    }
    return 33L
}
