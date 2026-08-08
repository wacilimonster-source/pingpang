package com.pingpang.app.ui.video

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pingpang.app.PingPangApp
import kotlinx.coroutines.launch
import java.io.File

private val speeds = listOf(0.25f, 0.5f, 1f, 1.5f, 2f)

/**
 * 视频详情/播放（F09）：ExoPlayer + 倍速 + 删除 + 加入比对。
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

    var clip by remember { mutableStateOf<com.pingpang.app.data.model.VideoClip?>(null) }
    var speed by remember { mutableStateOf(1f) }
    var confirmDelete by remember { mutableStateOf(false) }

    val player = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(videoId) {
        clip = db.videoClipDao().getById(videoId)
        clip?.let {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(it.filePath))))
            player.prepare()
            player.playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    val c = clip
    if (c == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedButton(onClick = onBack) { Text("‹ 返回") }
            Text("视频不存在或已删除")
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("‹ 返回") }
            Text("视频播放", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
