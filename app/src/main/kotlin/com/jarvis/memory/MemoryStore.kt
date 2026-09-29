package com.jarvis.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1, exportSchema = false
)
abstract class JarvisDb : RoomDatabase() { abstract fun dao(): MemoryDao }

/**
 * High-level memory façade: facts, messages, goals, KV. WAL is enabled for
 * multi-process safety. All access is off the main thread.
 */
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
        db.dao().recentMessages(limit).reversed().map { "${it.role}: ${it.content}" }
    }

    suspend fun putGoal(goal: Goal) = withContext(Dispatchers.IO) {
        db.dao().upsertGoal(GoalEntity(
            id = goal.id, title = goal.title, description = goal.description,
            language = goal.language, priority = goal.priority, deadline = goal.deadline,
            status = goal.status.name,
            planJson = json.encodeToString(ListSerializer(PlanStep.serializer()), goal.plan),
            evidenceJson = json.encodeToString(ListSerializer(String.serializer()), goal.evidence),
            triggersJson = json.encodeToString(ListSerializer(Trigger.serializer()), goal.triggers),
            createdAt = goal.createdAt, updatedAt = goal.updatedAt
        ))
    }

    suspend fun updateGoalStatus(id: String, status: GoalStatus) =
        withContext(Dispatchers.IO) {
            db.dao().setGoalStatus(id, status.name, System.currentTimeMillis())
        }

    suspend fun getGoal(id: String): Goal? = withContext(Dispatchers.IO) {
        db.dao().goal(id)?.let {
            Goal(
                id = it.id, title = it.title, description = it.description,
                language = it.language, priority = it.priority, deadline = it.deadline,
                status = GoalStatus.valueOf(it.status),
                plan = json.decodeFromString(ListSerializer(PlanStep.serializer()), it.planJson),
                evidence = json.decodeFromString(ListSerializer(String.serializer()), it.evidenceJson),
                triggers = json.decodeFromString(ListSerializer(Trigger.serializer()), it.triggersJson),
                createdAt = it.createdAt, updatedAt = it.updatedAt
            )
        }
    }

    suspend fun runningGoals(): List<Goal> = withContext(Dispatchers.IO) {
        db.dao().goalsByStatus(GoalStatus.RUNNING.name).map {
            Goal(
                id = it.id, title = it.title, description = it.description,
                language = it.language, priority = it.priority, deadline = it.deadline,
                status = GoalStatus.valueOf(it.status),
                plan = json.decodeFromString(ListSerializer(PlanStep.serializer()), it.planJson),
                createdAt = it.createdAt, updatedAt = it.updatedAt
            )
        }
    }

    companion object { private const val TAG = "MemoryStore" }
}
