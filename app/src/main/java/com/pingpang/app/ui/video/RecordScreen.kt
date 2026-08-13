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
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.core.content.ContextCompat
import com.pingpang.app.PingPangApp
import com.pingpang.app.data.VideoThumbnailer
import com.pingpang.app.data.model.VideoClip
import com.pingpang.app.util.MediaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val speeds = listOf(0.25f, 0.5f, 1f)

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
    var stopping by remember { mutableStateOf(false) }   // 已点停止、等待 finalize
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
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

    // 绑定实时预览（仅在未录制时调用；绑定 Preview 会先解除绑定，确保干净）
    fun bindPreview() {
        ProcessCameraProvider.getInstance(context).addListener({
            try {
                val provider = ProcessCameraProvider.getInstance(context).get()
                val preview = androidx.camera.core.Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            } catch (e: Exception) { /* ignore */ }
        }, ContextCompat.getMainExecutor(context))
    }

    // 进入页面（已授权）即显示实时画面
    LaunchedEffect(granted, cameraSelector) {
        if (granted && !recording) {
            bindPreview()
        }
    }

    fun toggleRecording() {
        if (!granted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
            return
        }
        // 等待 finalize 期间忽略再次点击，避免重复 stop / 误触开始
        if (stopping) return
        if (recording) {
            // 优雅停止：用 Recording.stop() 触发 finalize（保存文件），不要 unbindAll，
            // 否则录制器在活跃状态被强拆会崩溃。
            try {
                activeRecording?.stop()
            } catch (e: Exception) { /* ignore */ }
            activeRecording = null
            recording = false
            stopping = true
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

        ProcessCameraProvider.getInstance(context).addListener({
            val provider = ProcessCameraProvider.getInstance(context).get()
            try {
                val recorder = Recorder.Builder().build()
                val videoCapture = VideoCapture.withOutput(recorder)
                val preview = androidx.camera.core.Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                provider.unbindAll()
                // 预览 + 录制 同时绑定（录制过程中画面持续可见）
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
                recording = true

                val options = FileOutputOptions.Builder(file).build()
                val pending = recorder.prepareRecording(context, options).withAudioEnabled()
                val rec = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Finalize -> {
                            stopping = false
                            recording = false
                            activeRecording = null
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
                if (rec == null) {
                    recording = false
                    Toast.makeText(context, "相机启动失败", Toast.LENGTH_SHORT).show()
                } else {
                    activeRecording = rec
                }
            } catch (e: Exception) {
                recording = false
                Toast.makeText(context, "相机启动失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            // 退出页面：若仍在录制先优雅停止，再解除绑定（避免强拆相机崩溃）
            try {
                activeRecording?.stop()
            } catch (e: Exception) { /* ignore */ }
            activeRecording = null
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
        // 底部控制条：返回(左) / 录制(中) / 切换镜头(右)，单行排布减少对画面遮挡
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = !recording && !stopping,
            ) { Text("返回") }

            Button(
                onClick = { toggleRecording() },
                modifier = Modifier.size(72.dp),
            ) {
                Text(if (recording) "停止" else "录制")
            }

            OutlinedButton(
                onClick = {
                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                },
                enabled = !recording && !stopping,
            ) { Text("切换镜头") }
        }
    }
}
