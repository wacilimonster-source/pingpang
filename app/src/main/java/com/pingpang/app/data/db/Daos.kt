package com.pingpang.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pingpang.app.data.model.MatchRecord
import com.pingpang.app.data.model.MySkill
import com.pingpang.app.data.model.Opponent
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
    @Query("SELECT * FROM stage_plan WHERE id = :id")
    suspend fun getById(id: Long): StagePlan?
    @Query("DELETE FROM stage_plan WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("SELECT * FROM stage_plan WHERE status != 'DONE' ORDER BY startDate DESC")
    fun observeActive(): Flow<List<StagePlan>>
    @Query("SELECT * FROM stage_plan WHERE status = 'DONE' ORDER BY endDate DESC")
    fun observeDone(): Flow<List<StagePlan>>
    @Query("SELECT * FROM stage_plan WHERE id = :id")
    fun observeById(id: Long): Flow<StagePlan?>
}

@Dao
interface WeekPlanDao {
    @Insert suspend fun insert(plan: WeekPlan): Long
    @Insert suspend fun insertAll(plans: List<WeekPlan>)
    @Update suspend fun update(plan: WeekPlan)
    @Query("SELECT * FROM week_plan WHERE stageId = :stageId ORDER BY weekNo")
    suspend fun forStage(stageId: Long): List<WeekPlan>
    @Query("SELECT * FROM week_plan WHERE stageId = :stageId ORDER BY weekNo")
    fun observeForStage(stageId: Long): Flow<List<WeekPlan>>
    @Query("DELETE FROM week_plan WHERE stageId = :stageId")
    suspend fun deleteByStage(stageId: Long)
}

@Dao
interface TrainingSessionDao {
    @Insert suspend fun insert(session: TrainingSession): Long
    @Update suspend fun update(session: TrainingSession)
    @Query("SELECT * FROM training_session WHERE id = :id")
    suspend fun getById(id: Long): TrainingSession?
    @Query("SELECT * FROM training_session WHERE id = :id")
    fun observeById(id: Long): Flow<TrainingSession?>
    @Query("SELECT * FROM training_session ORDER BY date DESC")
    fun observeAll(): Flow<List<TrainingSession>>
    @Query("SELECT * FROM training_session WHERE date = :date ORDER BY id DESC")
    fun observeByDate(date: String): Flow<List<TrainingSession>>
    @Query("SELECT * FROM training_session WHERE planId = :planId ORDER BY date DESC")
    suspend fun forPlan(planId: Long): List<TrainingSession>
    @Query("SELECT COUNT(*) FROM training_session WHERE planId = :planId")
    suspend fun countFor(planId: Long): Int
    @Query("SELECT * FROM training_session")
    suspend fun all(): List<TrainingSession>
    @Query("DELETE FROM training_session WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("DELETE FROM training_session WHERE planId = :planId")
    suspend fun deleteByPlan(planId: Long)

    // ---------- 看板聚合查询 ----------
    data class MonthStat(val cnt: Int, val mins: Int)
    @Query("SELECT COUNT(*) AS cnt, COALESCE(SUM(durationMin),0) AS mins FROM training_session WHERE date >= :startDate")
    fun observeSinceDate(startDate: String): Flow<MonthStat>

    data class DateMinutes(val date: String, val mins: Int)
    @Query("SELECT date, SUM(durationMin) AS mins FROM training_session WHERE date >= :startDate GROUP BY date")
    fun observeDateMinutes(startDate: String): Flow<List<DateMinutes>>

    data class TypeCount(val type: String, val cnt: Int)
    @Query("SELECT type, COUNT(*) AS cnt FROM training_session GROUP BY type")
    fun observeTypeCounts(): Flow<List<TypeCount>>

    data class CompletionStat(val done: Int, val partial: Int, val skipped: Int)
    @Query("SELECT SUM(CASE WHEN completed='DONE' THEN 1 ELSE 0 END) AS done, SUM(CASE WHEN completed='PARTIAL' THEN 1 ELSE 0 END) AS partial, SUM(CASE WHEN completed='SKIPPED' THEN 1 ELSE 0 END) AS skipped FROM training_session WHERE date >= :startDate")
    fun observeCompletion(startDate: String): Flow<CompletionStat>

    @Query("SELECT DISTINCT date FROM training_session ORDER BY date DESC")
    fun observeDistinctDates(): Flow<List<String>>
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
    @Update suspend fun update(clip: VideoClip)
    @Delete suspend fun delete(clip: VideoClip)
    @Query("SELECT * FROM video_clip WHERE id = :id")
    suspend fun getById(id: Long): VideoClip?
    @Query("SELECT * FROM video_clip WHERE id = :id")
    fun observeById(id: Long): Flow<VideoClip?>
    @Query("SELECT * FROM video_clip ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<VideoClip>>
    @Query("SELECT * FROM video_clip")
    suspend fun all(): List<VideoClip>
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

@Dao
interface MySkillDao {
    @Insert suspend fun insert(skill: MySkill): Long
    @Update suspend fun update(skill: MySkill)
    @Delete suspend fun delete(skill: MySkill)
    @Query("SELECT * FROM my_skill ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MySkill>>
}

@Dao
interface OpponentDao {
    @Insert suspend fun insert(opponent: Opponent): Long
    @Update suspend fun update(opponent: Opponent)
    @Delete suspend fun delete(opponent: Opponent)
    @Query("SELECT * FROM opponent ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Opponent>>
    @Query("SELECT * FROM opponent WHERE id = :id")
    fun observeById(id: Long): Flow<Opponent?>
    @Query("SELECT * FROM opponent WHERE id = :id")
    suspend fun getById(id: Long): Opponent?
}
