package com.pingpang.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 加载本地图片文件（路径）并显示；加载中显示圈形进度。
 * 按 [targetMaxSize] 降采样解码，避免全尺寸 Bitmap（约 46MB/张）导致 OOM。
 */
@Composable
fun LocalImage(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    targetMaxSize: Int = 1024,
) {
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            decodeSampled(path, targetMaxSize)
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.fillMaxSize(0.2f))
        }
    }
}

/** 按目标最大边长降采样解码，节省内存（原图 4000px 只为 100dp 缩略图是完全浪费） */
fun decodeSampled(path: String, targetMaxSize: Int = 1024): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / (sample * 2) > targetMaxSize) sample *= 2

        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (e: Exception) {
        null
    }
}

/** 全屏查看大图（点击任意位置关闭） */
@Composable
fun FullscreenPhotoPreview(
    path: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            LocalImage(
                path = path,
                targetMaxSize = 2048,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "关闭")
            }
        }
    }
}