package com.jarvis.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jarvis.autonomy.Goal
import com.jarvis.autonomy.GoalStatus
import com.jarvis.autonomy.PlanStep
import com.jarvis.autonomy.Trigger
import com.jarvis.logging.JarvisLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [MemoryEntity::class, MessageEntity::class, KvEntity::class,
        GoalEntity::class, TaskEventEntity::class],
    version = 2, exportSchema = false
)
abstract class JarvisDb : RoomDatabase() { abstract fun dao(): MemoryDao }

@Singleton
class MemoryStore @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val logger: JarvisLogger
) {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var db: JarvisDb

    @Synchronized
    fun open() {
        if (::db.isInitialized) return
        db = Room.databaseBuilder(ctx, JarvisDb::class.java, "jarvis.db")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        logger.i(TAG, "memory opened")
    }

    suspend fun putMemory(key: String, value: String, category: String) =
        withContext(Dispatchers.IO) {
            db.dao().upsertMemory(MemoryEntity(key, value, category))
        }

    suspend fun getMemory(key: String): String? = withContext(Dispatchers.IO) {
        db.dao().findMemory(key)?.value
    }

    suspend fun search(query: String, limit: Int = 20) = withContext(Dispatchers.IO) {
        db.dao().search(query, limit)
    }

    suspend fun recordMistake(
        tool: String,
        context: String,
        errorCode: String,
        errorMessage: String,
        correction: String = ""
    ) = withContext(Dispatchers.IO) {
        val normalized = listOf(tool, context, errorCode).joinToString("|") {
            it.trim().lowercase().take(256)
        }
        val fingerprint = java.security.MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val existing = db.dao().searchLearning(tool, 100)
            .firstOrNull { it.fingerprint == fingerprint }
        if (existing == null) {
            db.dao().upsertLearningLesson(
                LearningLessonEntity(
                    fingerprint = fingerprint,
                    tool = tool.take(128),
                    context = context.take(512),
                    errorCode = errorCode.take(128),
                    errorMessage = errorMessage.take(1024),
                    correction = correction.take(1024)
                )
            )
        } else {
            db.dao().incrementLearningFailure(
                fingerprint = fingerprint,
                decrement = 0.05f,
                now = System.currentTimeMillis()
            )
            if (correction.isNotBlank()) {
                db.dao().setLearningCorrection(
                    fingerprint = fingerprint,
                    correction = correction.take(1024),
                    increment = 0.15f,
                    now = System.currentTimeMillis()
                )
            }
        }
        db.dao().pruneLearningLessons(
            System.currentTimeMillis() - 90L * 86_400_000L
        )
    }

    suspend fun recordLessonSuccess(fingerprint: String) = withContext(Dispatchers.IO) {
        db.dao().incrementLearningSuccess(
            fingerprint = fingerprint,
            increment = 0.10f,
            now = System.currentTimeMillis()
        )
    }

    suspend fun relevantLessons(query: String, limit: Int = 5): List<LearningLessonEntity> =
        withContext(Dispatchers.IO) {
            db.dao().searchLearning(query.take(256), limit)
        }

    suspend fun appendMessage(role: String, content: String, lang: String) =
        withContext(Dispatchers.IO) {
            db.dao().insertMessage(MessageEntity(role = role, content = content, language = lang))
            db.dao().compactMessages(keep = 200)
        }

    suspend fun recentContext(limit: Int = 12): List<String> = withContext(Dispatchers.IO) {
        db.dao().recentMessages(limit).reversed().map { "${it.role}: ${it.content}" }
    }

    suspend fun putGoal(goal: Goal) = withContext(Dispatchers.IO) {
        db.dao().upsertGoal(GoalEntity(
            id = goal.id,
            title = goal.title,
            description = goal.description,
            language = goal.language,
            priority = goal.priority,
            deadline = goal.deadline,
            status = goal.status.name,
            planJson = json.encodeToString(ListSerializer(PlanStep.serializer()), goal.plan),
            evidenceJson = json.encodeToString(ListSerializer(String.serializer()), goal.evidence),
            triggersJson = json.encodeToString(ListSerializer(Trigger.serializer()), goal.triggers),
            createdAt = goal.createdAt,
            updatedAt = goal.updatedAt,
            checkIntervalSeconds = goal.checkIntervalSeconds,
            maxRetries = goal.maxRetries,
            maxRuntimeSeconds = goal.maxRuntimeSeconds,
            confirmationRequired = goal.confirmationRequired
        ))
    }

    suspend fun updateGoalStatus(id: String, status: GoalStatus) =
        withContext(Dispatchers.IO) {
            db.dao().setGoalStatus(id, status.name, System.currentTimeMillis())
        }

    suspend fun getGoal(id: String): Goal? = withContext(Dispatchers.IO) {
        db.dao().goal(id)?.toGoal()
    }

    suspend fun runningGoals(): List<Goal> = withContext(Dispatchers.IO) {
        db.dao().goalsByStatus(GoalStatus.RUNNING.name).map { it.toGoal() }
    }

    suspend fun resumableGoals(): List<Goal> = withContext(Dispatchers.IO) {
        db.dao().resumableGoals().map { it.toGoal() }
    }

    suspend fun allGoals(): List<Goal> = withContext(Dispatchers.IO) {
        db.dao().allGoals().map { it.toGoal() }
    }

    private fun GoalEntity.toGoal(): Goal = Goal(
        id = id,
        title = title,
        description = description,
        language = language,
        priority = priority,
        deadline = deadline,
        status = GoalStatus.valueOf(status),
        plan = json.decodeFromString(ListSerializer(PlanStep.serializer()), planJson),
        evidence = json.decodeFromString(ListSerializer(String.serializer()), evidenceJson),
        triggers = json.decodeFromString(ListSerializer(Trigger.serializer()), triggersJson),
        createdAt = createdAt,
        updatedAt = updatedAt,
        checkIntervalSeconds = checkIntervalSeconds,
        maxRetries = maxRetries,
        maxRuntimeSeconds = maxRuntimeSeconds,
        confirmationRequired = confirmationRequired
    )

    companion object {
        private const val TAG = "MemoryStore"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goal_records ADD COLUMN checkIntervalSeconds INTEGER")
                db.execSQL("ALTER TABLE goal_records ADD COLUMN maxRetries INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE goal_records ADD COLUMN maxRuntimeSeconds INTEGER NOT NULL DEFAULT 86400")
                db.execSQL("ALTER TABLE goal_records ADD COLUMN confirmationRequired INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS learning_lessons (
                        fingerprint TEXT NOT NULL PRIMARY KEY,
                        tool TEXT NOT NULL,
                        context TEXT NOT NULL,
                        errorCode TEXT NOT NULL,
                        errorMessage TEXT NOT NULL,
                        correction TEXT NOT NULL,
                        failures INTEGER NOT NULL DEFAULT 1,
                        successes INTEGER NOT NULL DEFAULT 0,
                        confidence REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        lastSuccessAt INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_lessons_tool ON learning_lessons(tool)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_lessons_updatedAt ON learning_lessons(updatedAt)")
            }
        }
    }
}
