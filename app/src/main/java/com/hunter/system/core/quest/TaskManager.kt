package com.hunter.system.core.quest

import android.util.Log
import com.hunter.system.core.database.TaskDao
import com.hunter.system.core.database.TaskEntity
import com.hunter.system.core.database.TaskStatus
import com.hunter.system.core.datastore.SystemPreferences
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class TaskManager @Inject constructor(
    private val taskDao: TaskDao,
    private val preferences: SystemPreferences,
) {
    companion object {
        private const val TAG = "TaskManager"
    }

    suspend fun ensureTodayReady() {
        val today = LocalDate.now()
        carryOverPendingTasks(today)
        populateDefaultTasks(today)
    }

    private suspend fun carryOverPendingTasks(today: LocalDate) {
        val stale = taskDao.getCarryoverTasks(today)
        for (task in stale) {
            taskDao.update(task.copy(createdDate = today))
            Log.i(TAG, "Carried over task: \"${task.description}\" (originally ${task.originalDate})")
        }
    }

    private suspend fun populateDefaultTasks(today: LocalDate) {
        val dailyStepGoal = preferences.stepGoal.first()

        val existingStepTask = taskDao.getDefaultStepTaskForDate(today)
        if (existingStepTask == null) {
            val yesterday = today.minusDays(1)
            val yesterdayStepTask = taskDao.getDefaultStepTaskForDate(yesterday)
            val deficit = if (yesterdayStepTask != null && yesterdayStepTask.status != TaskStatus.COMPLETED) {
                (yesterdayStepTask.stepGoal!! - yesterdayStepTask.stepsCompleted).coerceAtLeast(0)
            } else 0

            val todayGoal = dailyStepGoal + deficit
            val desc = if (deficit > 0) {
                "Walk %,d steps (%,dK + %,dK carry)".format(todayGoal, dailyStepGoal / 1000, deficit / 1000)
            } else {
                "Walk %,d steps".format(todayGoal)
            }

            taskDao.insert(
                TaskEntity(
                    description = desc,
                    createdDate = today,
                    originalDate = today,
                    isDefault = true,
                    stepGoal = todayGoal,
                    sortOrder = 0,
                )
            )
            Log.i(TAG, "Created default step task: goal=$todayGoal (deficit=$deficit)")
        }
    }

    suspend fun addTask(description: String) {
        val today = LocalDate.now()
        val maxOrder = taskDao.getTasksForDate(today).maxOfOrNull { it.sortOrder } ?: -1
        taskDao.insert(
            TaskEntity(
                description = description,
                createdDate = today,
                originalDate = today,
                sortOrder = maxOrder + 1,
            )
        )
    }

    suspend fun completeTask(taskId: Long) {
        val tasks = taskDao.getTasksForDate(LocalDate.now())
        val task = tasks.find { it.id == taskId } ?: return
        taskDao.update(task.copy(status = TaskStatus.COMPLETED, completedDate = LocalDate.now()))
    }

    suspend fun discardTask(taskId: Long) {
        val tasks = taskDao.getTasksForDate(LocalDate.now())
        val task = tasks.find { it.id == taskId } ?: return
        taskDao.update(task.copy(status = TaskStatus.DISCARDED))
    }

    suspend fun editTask(taskId: Long, newDescription: String) {
        val tasks = taskDao.getTasksForDate(LocalDate.now())
        val task = tasks.find { it.id == taskId } ?: return
        if (task.isDefault) return
        taskDao.update(task.copy(description = newDescription.trim()))
    }

    suspend fun deleteTask(taskId: Long) {
        val tasks = taskDao.getTasksForDate(LocalDate.now())
        val task = tasks.find { it.id == taskId } ?: return
        if (task.isDefault) return
        taskDao.delete(task)
    }

    suspend fun reorderTasks(reorderedIds: List<Long>) {
        val today = LocalDate.now()
        val tasks = taskDao.getTasksForDate(today).associateBy { it.id }
        reorderedIds.forEachIndexed { index, id ->
            tasks[id]?.let { taskDao.update(it.copy(sortOrder = index)) }
        }
    }

    suspend fun updateStepProgress(steps: Int) {
        val today = LocalDate.now()
        val stepTask = taskDao.getDefaultStepTaskForDate(today) ?: return
        if (stepTask.status == TaskStatus.COMPLETED) return

        val updated = stepTask.copy(stepsCompleted = steps)
        if (steps >= (stepTask.stepGoal ?: Int.MAX_VALUE)) {
            taskDao.update(updated.copy(status = TaskStatus.COMPLETED, completedDate = today))
            Log.i(TAG, "Step task auto-completed: $steps / ${stepTask.stepGoal}")
        } else {
            taskDao.update(updated)
        }
    }

    fun carryoverDays(task: TaskEntity): Int =
        ChronoUnit.DAYS.between(task.originalDate, task.createdDate).toInt()
}
