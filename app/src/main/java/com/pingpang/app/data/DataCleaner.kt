package com.pingpang.app.data

import com.pingpang.app.data.db.AppDatabase
import com.pingpang.app.data.model.TrainingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 删除/清理场景的文件与引用一致性处理（PRD §8.2 关联规则） */
object DataCleaner {

    /** 删除训练记录引用的照片文件（无文件时静默忽略） */
    suspend fun deleteSessionPhotos(session: TrainingSession) {
        withContext(Dispatchers.IO) {
            JsonUtils.stringToList(session.photosJson).forEach { path ->
                runCatching { File(path).delete() }
            }
        }
    }

    /** 删除视频后，从所有训练记录的 videosJson 中移除引用（记录不删除，只解除关联） */
    suspend fun removeVideoRefs(db: AppDatabase, videoId: Long) {
        db.trainingSessionDao().all().forEach { s ->
            val ids = JsonUtils.stringToList(s.videosJson)
            if (videoId.toString() in ids) {
                val rest = ids.filterNot { it == videoId.toString() }
                db.trainingSessionDao().update(s.copy(videosJson = JsonUtils.listToString(rest)))
            }
        }
    }
}