package com.hunter.system.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val createdDate: LocalDate,
    val completedDate: LocalDate? = null,
    val isDefault: Boolean = false,
    val originalDate: LocalDate,
    val sortOrder: Int = 0,
    val stepGoal: Int? = null,
    val stepsCompleted: Int = 0,
)

enum class TaskStatus { PENDING, COMPLETED, DISCARDED }
