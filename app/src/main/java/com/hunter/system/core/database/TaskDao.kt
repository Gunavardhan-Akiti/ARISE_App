package com.hunter.system.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE createdDate = :date ORDER BY sortOrder ASC, id ASC")
    fun observeTasksForDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE createdDate = :date ORDER BY sortOrder ASC, id ASC")
    suspend fun getTasksForDate(date: LocalDate): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' AND createdDate < :today AND stepGoal IS NULL")
    suspend fun getCarryoverTasks(today: LocalDate): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE createdDate = :date AND isDefault = 1 AND description = :desc LIMIT 1")
    suspend fun getDefaultTaskForDate(date: LocalDate, desc: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE createdDate = :date AND isDefault = 1 AND stepGoal IS NOT NULL LIMIT 1")
    suspend fun getDefaultStepTaskForDate(date: LocalDate): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' AND completedDate >= :since ORDER BY completedDate DESC, id DESC")
    fun observeCompletedTasksSince(since: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' AND createdDate = :date")
    suspend fun getPendingTasksForDate(date: LocalDate): List<TaskEntity>
}
