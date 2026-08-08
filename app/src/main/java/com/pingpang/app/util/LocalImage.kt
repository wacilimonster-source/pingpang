package com.pingpang.app.util

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 加载本地图片文件（路径）并显示；加载中显示圈形进度 */
@Composable
fun LocalImage(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    var bitmap by remember(path) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                null
            }
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
