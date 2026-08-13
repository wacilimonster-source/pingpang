package com.pingpang.app.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File

/** 视频缩略图：取首帧、缩放后存 jpg 到私有目录（避免原分辨率大文件/大位图） */
object VideoThumbnailer {

    private const val THUMB_MAX_SIZE = 360

    /** 生成缩略图；失败返回 null */
    fun generate(context: Context, videoPath: String, thumbDir: File? = null): String? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            val frame: Bitmap? = retriever.getFrameAtTime(0) ?: retriever.getFrameAtTime(1_000_000)
            if (frame == null) return null

            val scaled = scaleFrame(frame, THUMB_MAX_SIZE)
            if (scaled !== frame) frame.recycle()

            val dir = thumbDir ?: File(context.filesDir, "thumbs").apply { mkdirs() }
            val name = "thumb_${System.currentTimeMillis()}.jpg"
            val out = File(dir, name)
            out.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            scaled.recycle()
            out.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            retriever?.release()
        }
    }

    private fun scaleFrame(src: Bitmap, maxSize: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (maxOf(w, h) <= maxSize) return src
        val scale = maxSize.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}