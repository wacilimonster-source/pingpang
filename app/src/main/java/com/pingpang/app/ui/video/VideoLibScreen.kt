package com.pingpang.app.ui.video

import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.VideoThumbnailer
import com.pingpang.app.data.model.VideoClip
import com.pingpang.app.ui.common.PingPangViewModelFactory
import com.pingpang.app.util.LocalImage
import com.pingpang.app.util.MediaHelper
import kotlinx.coroutines.launch

/**
 * 视频库（F08）：网格展示、相册导入、录制入口、比对入口。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VideoLibScreen(
    onOpenVideo: (Long) -> Unit,
    onRecord: () -> Unit,
    onCompare: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = (context.applicationContext as PingPangApp).database
    val dao = db.videoClipDao()

    val vm: VideoViewModel = viewModel(factory = PingPangViewModelFactory(videoClipDao = dao))
    val clips by vm.clips.collectAsState(initial = emptyList())
    var importing by remember { mutableStateOf(false) }

    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            try {
                uris.forEach { uri ->
                    val path = MediaHelper.copyToInternal(context, uri, "videos") ?: return@forEach
                    val duration = try {
                        val r = MediaMetadataRetriever()
                        r.setDataSource(path)
                        val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        r.release()
                        d
                    } catch (e: Exception) { 0L }
                    val thumb = VideoThumbnailer.generate(context, path)
                    dao.insert(
                        VideoClip(
                            filePath = path,
                            date = java.time.LocalDate.now().toString(),
                            source = "IMPORTED",
                            tagsJson = "[]",
                            durationMs = duration,
                            thumbPath = thumb,
                        )
                    )
                }
                Toast.makeText(context, "导入完成", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导入失败：${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                importing = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("视频库", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onCompare) { Text("比对工作台") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { importer.launch("video/*") }, modifier = Modifier.weight(1f)) {
                Text(if (importing) "导入中…" else "从相册导入")
            }
            OutlinedButton(onClick = onRecord, modifier = Modifier.weight(1f)) { Text("录制视频") }
        }

        if (clips.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("视频库为空")
                    Text("导入训练/教学视频后，可慢放回放并与他人动作并排比对", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(clips, key = { it.id }) { clip: VideoClip ->
                    Card(
                        onClick = { onOpenVideo(clip.id) },
                        modifier = Modifier.aspectRatio(1f),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                            if (clip.thumbPath != null) {
                                LocalImage(path = clip.thumbPath, modifier = Modifier.fillMaxSize(), contentDescription = null)
                            }
                            Text(
                                (clip.durationMs / 1000).toString() + "s",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(4.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
