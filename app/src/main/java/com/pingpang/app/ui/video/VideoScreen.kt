package com.pingpang.app.ui.video

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
import com.pingpang.app.data.db.VideoClipDao
import com.pingpang.app.data.model.VideoClip
import com.pingpang.app.ui.common.PingPangViewModelFactory

/**
 * 视频复盘页（MVP 核心②）。
 * 骨架：视频库列表；"多视频比对工作台"入口待实现（Media3 多实例 + 缩放对齐）。
 */
@Composable
fun VideoScreen() {
    val dao: VideoClipDao =
        (LocalContext.current.applicationContext as PingPangApp).database.videoClipDao()
    val vm: VideoViewModel = viewModel(factory = PingPangViewModelFactory(videoClipDao = dao))
    val clips by vm.clips.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("视频复盘", style = MaterialTheme.typography.headlineSmall)
            Text("本地视频库 · 慢放 · 多视频同屏比对", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Button(
                onClick = { /* TODO V1.0：比对工作台（Media3 多实例 + 双指缩放对齐） */ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("比对工作台（自己 vs 标准动作）")
            }
        }
        item {
            Button(
                onClick = { /* TODO V1.0：相册导入 / APP 内录制（Photo Picker / CameraX） */ },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("导入 / 录制视频")
            }
        }
        item { HorizontalDivider() }
        if (clips.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("视频库为空")
                        Text(
                            "导入训练视频后，可在这里慢放回放、与其他视频并排比对",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        } else {
            items(clips) { clip: VideoClip ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(clip.date, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${clip.durationMs / 1000} 秒 · ${if (clip.source == "RECORDED") "录制" else "导入"}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
