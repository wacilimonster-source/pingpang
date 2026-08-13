package com.pingpang.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pingpang.app.data.model.MatchRecord
import com.pingpang.app.data.model.OpponentType
import com.pingpang.app.data.model.SkillCard
import com.pingpang.app.data.model.StagePlan
import com.pingpang.app.data.model.TrainingSession
import com.pingpang.app.data.model.VideoClip
import com.pingpang.app.data.model.WeekPlan
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** v1→v2：训练记录增加 AI 复盘草稿字段（PRD §8.1），并为高频查询列建索引 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE training_session ADD COLUMN aiReviewJson TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_training_session_date ON training_session(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_training_session_planId ON training_session(planId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_week_plan_stageId ON week_plan(stageId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_video_clip_date ON video_clip(date)")
    }
}

@Database(
    entities = [
        StagePlan::class,
        WeekPlan::class,
        TrainingSession::class,
        SkillCard::class,
        VideoClip::class,
        MatchRecord::class,
        OpponentType::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stagePlanDao(): StagePlanDao
    abstract fun weekPlanDao(): WeekPlanDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun skillCardDao(): SkillCardDao
    abstract fun videoClipDao(): VideoClipDao
    abstract fun matchRecordDao(): MatchRecordDao
    abstract fun opponentTypeDao(): OpponentTypeDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun build(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: run {
                    recoverCorruptDatabase(context)
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "pingpang.db",
                    )
                        .addMigrations(MIGRATION_1_2)
                        .build()
                        .also { instance = it }
                }
            }

        /**
         * 数据库自愈（PRD §4.4）：打开前用 PRAGMA quick_check 体检，
         * 若库文件损坏则先备份为 pingpang_<时间戳>.db.corrupt 再删除，
         * 让 Room 重建新库，避免整 App 崩溃。
         */
        private fun recoverCorruptDatabase(context: Context) {
            val dbFile = context.getDatabasePath("pingpang.db")
            if (!dbFile.exists()) return
            var healthy = false
            try {
                SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                    .use { db ->
                        db.rawQuery("PRAGMA quick_check(1)", null).use { c ->
                            healthy = c.moveToFirst() && c.getString(0) == "ok"
                        }
                    }
            } catch (_: Exception) {
                healthy = false
            }
            if (healthy) return

            // 备份损坏库 → 删除 → 下次打开时重建
            try {
                val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val backup = File(backupDir, "pingpang_$stamp.db.corrupt")
                listOf(dbFile, File(dbFile.absolutePath + "-wal"), File(dbFile.absolutePath + "-shm"))
                    .filter { it.exists() }
                    .forEach { it.copyTo(backup, overwrite = true) }
                dbFile.delete()
                File(dbFile.absolutePath + "-wal").let { if (it.exists()) it.delete() }
                File(dbFile.absolutePath + "-shm").let { if (it.exists()) it.delete() }
            } catch (_: Exception) {
            }
        }
    }
}