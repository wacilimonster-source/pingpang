package com.pingpang.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pingpang.app.data.model.MatchRecord
import com.pingpang.app.data.model.OpponentType
import com.pingpang.app.data.model.SkillCard
import com.pingpang.app.data.model.StagePlan
import com.pingpang.app.data.model.TrainingSession
import com.pingpang.app.data.model.VideoClip
import com.pingpang.app.data.model.WeekPlan

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
    version = 1,
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
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pingpang.db",
                ).build().also { instance = it }
            }
    }
}
