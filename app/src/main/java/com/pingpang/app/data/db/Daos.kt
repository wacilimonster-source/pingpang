package com.pingpang.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pingpang.app.data.model.MatchRecord
import com.pingpang.app.data.model.OpponentType
import com.pingpang.app.data.model.SkillCard
import com.pingpang.app.data.model.StagePlan
import com.pingpang.app.data.model.TrainingSession
import com.pingpang.app.data.model.VideoClip
import com.pingpang.app.data.model.WeekPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface StagePlanDao {
    @Insert suspend fun insert(plan: StagePlan): Long
    @Update suspend fun update(plan: StagePlan)
    @Delete suspend fun delete(plan: StagePlan)
    @Query("SELECT * FROM stage_plan WHERE status != 'DONE' ORDER BY startDate DESC")
    fun observeActive(): Flow<List<StagePlan>>
}

@Dao
interface WeekPlanDao {
    @Insert suspend fun insert(plan: WeekPlan): Long
    @Query("SELECT * FROM week_plan WHERE stageId = :stageId ORDER BY weekNo")
    suspend fun forStage(stageId: Long): List<WeekPlan>
}

@Dao
interface TrainingSessionDao {
    @Insert suspend fun insert(session: TrainingSession): Long
    @Update suspend fun update(session: TrainingSession)
    @Query("SELECT * FROM training_session ORDER BY date DESC")
    fun observeAll(): Flow<List<TrainingSession>>
    @Query("SELECT * FROM training_session WHERE date = :date ORDER BY id DESC")
    fun observeByDate(date: String): Flow<List<TrainingSession>>
}

@Dao
interface SkillCardDao {
    @Insert suspend fun insert(card: SkillCard): Long
    @Update suspend fun update(card: SkillCard)
    @Query("SELECT * FROM skill_card ORDER BY category, name")
    fun observeAll(): Flow<List<SkillCard>>
}

@Dao
interface VideoClipDao {
    @Insert suspend fun insert(clip: VideoClip): Long
    @Delete suspend fun delete(clip: VideoClip)
    @Query("SELECT * FROM video_clip ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<VideoClip>>
}

@Dao
interface MatchRecordDao {
    @Insert suspend fun insert(record: MatchRecord): Long
    @Query("SELECT * FROM match_record ORDER BY date DESC")
    fun observeAll(): Flow<List<MatchRecord>>
}

@Dao
interface OpponentTypeDao {
    @Insert suspend fun insert(type: OpponentType): Long
    @Query("SELECT * FROM opponent_type ORDER BY id")
    fun observeAll(): Flow<List<OpponentType>>
}
