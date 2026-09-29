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
            .addMigrations(MIGRATION_1_2)
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

    suspend fun appendMessage(role: String, content: String, lang: String) =
        withContext(Dispatchers.IO) {
            db.dao().insertMessage(MessageEntity(role = role, content = content, language = lang))
            db.dao().compactMessages(keep = 200)
        }

    suspend fun recentContext(limit: Int = 12): List<String> = withContext(Dispatchers.IO) {
        db.dao().recentMessages(limit).reversed().map { "\${it.role}: \${it.content}" }
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
    }
}
