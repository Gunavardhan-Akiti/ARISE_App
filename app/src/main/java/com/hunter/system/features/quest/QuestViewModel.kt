package com.hunter.system.features.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunter.system.core.database.TaskDao
import com.hunter.system.core.database.TaskEntity
import com.hunter.system.core.database.TaskStatus
import com.hunter.system.core.quest.TaskManager
import com.hunter.system.core.sensors.StepProvider
import com.hunter.system.core.sensors.StepSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class QuestViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val taskManager: TaskManager,
    private val stepProvider: StepProvider,
) : ViewModel() {

  private val today = LocalDate.now()

  val tasks: StateFlow<List<TaskEntity>> =
      taskDao.observeTasksForDate(today)
          .map { list ->
            val pending = list.filter { it.status == TaskStatus.PENDING }.sortedBy { it.sortOrder }
            val completed = list.filter { it.status == TaskStatus.COMPLETED }.sortedByDescending { it.completedDate }
            val discarded = list.filter { it.status == TaskStatus.DISCARDED }.sortedByDescending { it.id }
            pending + completed + discarded
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val completedHistory: StateFlow<Map<LocalDate, List<TaskEntity>>> =
      taskDao.observeCompletedTasksSince(today.minusDays(30))
          .map { list -> list.groupBy { it.completedDate ?: it.createdDate } }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  val stepSource: StateFlow<StepSource> = stepProvider.source
  val hasHealthConnectPermission: StateFlow<Boolean> = stepProvider.hasHealthConnectPermission

  init {
    viewModelScope.launch {
      taskManager.ensureTodayReady()
      stepProvider.checkPermission()
    }
  }

  fun addTask(description: String) {
    if (description.isBlank()) return
    viewModelScope.launch { taskManager.addTask(description.trim()) }
  }

  fun completeTask(taskId: Long) {
    viewModelScope.launch { taskManager.completeTask(taskId) }
  }

  fun discardTask(taskId: Long) {
    viewModelScope.launch { taskManager.discardTask(taskId) }
  }

  fun editTask(taskId: Long, newDescription: String) {
    if (newDescription.isBlank()) return
    viewModelScope.launch { taskManager.editTask(taskId, newDescription) }
  }

  fun deleteTask(taskId: Long) {
    viewModelScope.launch { taskManager.deleteTask(taskId) }
  }

  fun moveTaskUp(taskId: Long) {
    viewModelScope.launch {
      val pending = tasks.value.filter { it.status == TaskStatus.PENDING }
      val idx = pending.indexOfFirst { it.id == taskId }
      if (idx > 0) {
        val reordered = pending.map { it.id }.toMutableList()
        reordered.removeAt(idx)
        reordered.add(idx - 1, taskId)
        taskManager.reorderTasks(reordered)
      }
    }
  }

  fun moveTaskDown(taskId: Long) {
    viewModelScope.launch {
      val pending = tasks.value.filter { it.status == TaskStatus.PENDING }
      val idx = pending.indexOfFirst { it.id == taskId }
      if (idx >= 0 && idx < pending.size - 1) {
        val reordered = pending.map { it.id }.toMutableList()
        reordered.removeAt(idx)
        reordered.add(idx + 1, taskId)
        taskManager.reorderTasks(reordered)
      }
    }
  }

  fun forceSync() {
    viewModelScope.launch {
      stepProvider.checkPermission()
      val steps = stepProvider.getTodaySteps()
      taskManager.updateStepProgress(steps)
    }
  }

  fun carryoverDays(task: TaskEntity): Int = taskManager.carryoverDays(task)
}
