package com.jarvis.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_lessons")
data class LearningLessonEntity(
    @PrimaryKey val fingerprint: String,
    val tool: String,
    val context: String,
    val errorCode: String,
    val errorMessage: String,
    val correction: String,
    val failures: Int = 1,
    val successes: Int = 0,
    val confidence: Float = 0.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSuccessAt: Long? = null
)
