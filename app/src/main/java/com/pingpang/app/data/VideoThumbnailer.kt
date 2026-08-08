package com.pingpang.app.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File

/** 视频缩略图：取首帧存 jpg 到私有目录 */
object VideoThumbnailer {

    /** 生成缩略图；失败返回 null */
    fun generate(context: Context, videoPath: String, thumbDir: File? = null): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val frame: Bitmap? = retriever.getFrameAtTime(0)
            retriever.release()
            if (frame == null) return null

            val dir = thumbDir ?: File(context.filesDir, "thumbs").apply { mkdirs() }
            val name = "thumb_${System.currentTimeMillis()}.jpg"
            val out = File(dir, name)
            out.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
