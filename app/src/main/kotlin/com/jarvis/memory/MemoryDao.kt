package com.jarvis.memory

import androidx.room.*

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE `key` = :key LIMIT 1")
    fun findMemory(key: String): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertMemory(m: MemoryEntity)

    @Query("DELETE FROM memories WHERE `key` = :key")
    fun deleteMemory(key: String)

    @Query("SELECT * FROM memories WHERE category = :cat ORDER BY updatedAt DESC LIMIT :limit")
    fun listByCategory(cat: String, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE value LIKE '%' || :q || '%' ORDER BY updatedAt DESC LIMIT :limit")
    fun search(q: String, limit: Int): List<MemoryEntity>

    @Insert
    fun insertMessage(m: MessageEntity): Long

    @Query("SELECT * FROM messages ORDER BY id DESC LIMIT :limit")
    fun recentMessages(limit: Int): List<MessageEntity>

    @Query("DELETE FROM messages WHERE id NOT IN (SELECT id FROM messages ORDER BY id DESC LIMIT :keep)")
    fun compactMessages(keep: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putKv(kv: KvEntity)

    @Query("SELECT * FROM kv WHERE `key` = :key LIMIT 1")
    fun getKv(key: String): KvEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertGoal(g: GoalEntity)

    @Query("UPDATE goal_records SET status = :status, updatedAt = :now WHERE id = :id")
    fun setGoalStatus(id: String, status: String, now: Long)

    @Query("SELECT * FROM goal_records WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    fun goalsByStatus(status: String): List<GoalEntity>

    @Query("SELECT * FROM goal_records WHERE status IN ('RUNNING', 'PENDING') ORDER BY priority DESC, createdAt ASC")
    fun resumableGoals(): List<GoalEntity>

    @Query("SELECT * FROM goal_records ORDER BY priority DESC, createdAt ASC")
    fun allGoals(): List<GoalEntity>

    @Query("SELECT * FROM goal_records WHERE id = :id")
    fun goal(id: String): GoalEntity?

    @Insert
    fun insertEvent(e: TaskEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertLearningLesson(lesson: LearningLessonEntity)

    @Query("SELECT * FROM learning_lessons WHERE tool = :tool ORDER BY confidence DESC, updatedAt DESC LIMIT :limit")
    fun learningForTool(tool: String, limit: Int): List<LearningLessonEntity>

    @Query("""
        SELECT * FROM learning_lessons
        WHERE context LIKE '%' || :query || '%'
           OR errorMessage LIKE '%' || :query || '%'
           OR correction LIKE '%' || :query || '%'
        ORDER BY confidence DESC, updatedAt DESC
        LIMIT :limit
    """)
    fun searchLearning(query: String, limit: Int): List<LearningLessonEntity>

    @Query("""
        UPDATE learning_lessons
        SET failures = failures + 1,
            confidence = MAX(0.0, confidence - :decrement),
            updatedAt = :now
        WHERE fingerprint = :fingerprint
    """)
    fun incrementLearningFailure(fingerprint: String, decrement: Float, now: Long)

    @Query("""
        UPDATE learning_lessons
        SET successes = successes + 1,
            confidence = MIN(1.0, confidence + :increment),
            updatedAt = :now,
            lastSuccessAt = :now
        WHERE fingerprint = :fingerprint
    """)
    fun incrementLearningSuccess(fingerprint: String, increment: Float, now: Long)

    @Query("""
        UPDATE learning_lessons
        SET correction = :correction,
            updatedAt = :now
        WHERE fingerprint = :fingerprint
    """)
    fun setLearningCorrection(
        fingerprint: String,
        correction: String,
        increment: Float,
        now: Long
    )

    @Query("DELETE FROM learning_lessons WHERE updatedAt < :cutoff")
    fun pruneLearningLessons(cutoff: Long): Int
}
