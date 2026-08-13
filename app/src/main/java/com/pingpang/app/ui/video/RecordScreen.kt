package com.pingpang.app.ui.video

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.foundation.background
import androidx.core.content.ContextCompat
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.VideoThumbnailer
import com.pingpang.app.data.model.VideoClip
import com.pingpang.app.util.MediaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

private val speeds = listOf(0.25f, 0.5f, 1f)

/** 录制暂存用的固定执行器（进程级单例，避免每次录制泄漏一个线程） */
private val recorderExecutor = Executors.newSingleThreadExecutor { r ->
    Thread(r, "pingpang-recorder").apply { isDaemon = true }
}

/**
 * 视频录制（F08）：CameraX 全屏预览 + 录制/停止（CameraX 1.3 video API）。
 */
@Composable
fun RecordScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context) }
    var recording by remember { mutableStateOf(false) }
    var outputPath by remember { mutableStateOf<String?>(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    // 录制计时：每秒递增，展示在预览顶部
    LaunchedEffect(recording) {
        recordSeconds = 0
        while (recording) {
            kotlinx.coroutines.delay(1000)
            recordSeconds++
        }
    }

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result[Manifest.permission.CAMERA] == true &&
            (result[Manifest.permission.RECORD_AUDIO] ?: true)
    }

    LaunchedEffect(Unit) {
        if (!granted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    fun saveClip(path: String) {
        scope.launch {
            val thumb = withContext(Dispatchers.IO) { VideoThumbnailer.generate(context, path) }
            val duration = withContext(Dispatchers.IO) {
                try {
                    val r = android.media.MediaMetadataRetriever()
                    r.setDataSource(path)
                    val d = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    r.release()
                    d
                } catch (e: Exception) { 0L }
            }
            val db = (context.applicationContext as PingPangApp).database
            db.videoClipDao().insert(
                VideoClip(
                    filePath = path,
                    date = java.time.LocalDate.now().toString(),
                    source = "RECORDED",
                    tagsJson = "[]",
                    durationMs = duration,
                    thumbPath = thumb,
                )
            )
            Toast.makeText(context, "录制已保存", Toast.LENGTH_SHORT).show()
            onSaved()
        }
    }

    fun toggleRecording() {
        if (!granted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            return
        }
        if (recording) {
            // 停止：通过解除绑定触发录制 finalize（简化方案）
            ProcessCameraProvider.getInstance(context).addListener({
                try {
                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                } catch (e: Exception) { /* ignore */ }
                recording = false
            }, ContextCompat.getMainExecutor(context))
            return
        }

        // 存储空间检查（PRD §4.4）：不足 200MB 禁止开始录制
        if (!MediaHelper.ensureStorage(context, 200L * 1024 * 1024)) {
            Toast.makeText(context, "存储空间不足，请清理后再录制", Toast.LENGTH_SHORT).show()
            return
        }

        val dir = File(context.filesDir, "videos").apply { mkdirs() }
        val file = File(dir, "rec_${System.currentTimeMillis()}.mp4")
        outputPath = file.absolutePath
        val executor = recorderExecutor

        ProcessCameraProvider.getInstance(context).addListener({
            val provider = ProcessCameraProvider.getInstance(context).get()
            try {
                val recorder = Recorder.Builder().build()
                val videoCapture = VideoCapture.withOutput(recorder)
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, videoCapture)
                recording = true

                val options = FileOutputOptions.Builder(file).build()
                val pending = recorder.prepareRecording(context, options).withAudioEnabled()
                pending.start(executor) { event ->
                    when (event) {
                        is VideoRecordEvent.Finalize -> {
                            recording = false
                            if (event.hasError()) {
                                Toast.makeText(
                                    context,
                                    "录制出错：${event.cause?.message ?: "error=${event.error}"}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                val path = event.outputResults.outputUri.path
                                if (path != null) {
                                    saveClip(path)
                                } else {
                                    Toast.makeText(context, "录制保存失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "相机启动失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            ProcessCameraProvider.getInstance(context).addListener({
                try {
                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                } catch (e: Exception) { /* ignore */ }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        // 录制计时（PRD P-RECORD：顶部显示已录时长）
        Text(
            text = if (recording) {
                val m = recordSeconds / 60
                val s = recordSeconds % 60
                "● ${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
            } else "",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = { toggleRecording() }, modifier = Modifier.size(72.dp)) {
                Text(if (recording) "停止" else "录制")
            }
            OutlinedButton(onClick = {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            }) { Text("切换镜头") }
            OutlinedButton(onClick = onBack) { Text("返回") }
        }
    }
}
