package com.jarvis.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val key: String,
    val value: String,
    val category: String,
    val language: String = "hi",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val embedding: ByteArray? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val language: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "kv")
data class KvEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "goal_records")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val language: String,
    val priority: Int,
    val deadline: Long?,
    val status: String,
    val planJson: String,
    val evidenceJson: String,
    val triggersJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val checkIntervalSeconds: Long?,
    val maxRetries: Int,
    val maxRuntimeSeconds: Long,
    val confirmationRequired: Boolean
)

@Entity(tableName = "task_events")
data class TaskEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: String,
    val stepId: String?,
    val event: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
