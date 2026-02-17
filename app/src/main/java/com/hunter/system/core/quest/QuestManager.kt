package com.hunter.system.core.quest

import com.hunter.system.core.database.QuestDao
import com.hunter.system.core.database.QuestEntity
import com.hunter.system.core.database.QuestStatus
import com.hunter.system.core.datastore.SystemPreferences
import com.hunter.system.core.sensors.StepCounterManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Active quest UI state — represents what the UI layer sees. */
sealed interface ActiveQuestState {
  /** No quest running */
  data object Idle : ActiveQuestState

  /** Quest is in progress */
  data class Active(
          val questId: Long,
          val progress: CriteriaProgress,
          val startTime: Instant,
          val elapsedMinutes: Long
  ) : ActiveQuestState

  /** Quest just completed */
  data class Completed(val questId: Long, val totalSteps: Int, val durationMinutes: Long) :
          ActiveQuestState
}

/**
 * Central quest lifecycle manager.
 *
 * Manages the quest state machine: Idle → Active → Completed/Failed. Coordinates between the step
 * counter tracker and the Room database. This is a singleton shared between the foreground service
 * and UI.
 */
@Singleton
class QuestManager
@Inject
constructor(
        private val questDao: QuestDao,
        private val stepCounter: StepCounterManager,
        private val preferences: SystemPreferences
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val _state = MutableStateFlow<ActiveQuestState>(ActiveQuestState.Idle)
  val state: StateFlow<ActiveQuestState> = _state.asStateFlow()

  /** The Room entity ID for the currently active quest */
  private var activeQuestId: Long = -1L
  private var questStartTime: Instant = Instant.now()

  /** Access the step counter tracker (also usable by foreground service) */
  val tracker: StepCounterManager
    get() = stepCounter

  /**
   * Start a new quest. Records baseline and begins step tracking.
   * @param goal Step goal (default from DataStore preferences)
   */
  suspend fun startQuest(goal: Int = 10_000) {
    // Don't start if already active
    if (_state.value is ActiveQuestState.Active) return

    questStartTime = Instant.now()

    // Insert quest record into Room
    val entity =
            QuestEntity(
                    date = LocalDate.now(),
                    alarmTime = LocalTime.now(),
                    baselineSteps = 0, // For inbuilt sensor, baseline is 0 (we track delta)
                    goalSteps = goal,
                    status = QuestStatus.ACTIVE
            )
    activeQuestId = questDao.insertQuest(entity)

    // Start the step counter tracker
    stepCounter.start(goal)

    // Set initial state
    _state.value =
            ActiveQuestState.Active(
                    questId = activeQuestId,
                    progress = stepCounter.progress.value,
                    startTime = questStartTime,
                    elapsedMinutes = 0
            )

    // Observe step counter progress and update quest state
    scope.launch {
      stepCounter.progress.collect { progress ->
        val currentState = _state.value
        if (currentState is ActiveQuestState.Active) {
          val elapsed = ChronoUnit.MINUTES.between(questStartTime, Instant.now())
          _state.value = currentState.copy(progress = progress, elapsedMinutes = elapsed)

          // Check for completion
          if (progress.isComplete) {
            completeQuest()
          }
        }
      }
    }
  }

  /** Complete the active quest. Stops tracking and persists results. */
  suspend fun completeQuest() {
    val currentState = _state.value
    if (currentState !is ActiveQuestState.Active) return

    val now = Instant.now()
    val duration = ChronoUnit.MINUTES.between(questStartTime, now)
    val finalSteps = currentState.progress.current

    // Stop tracking
    stepCounter.stop()

    // Update Room entity
    val entity = questDao.getQuestByDate(LocalDate.now())
    if (entity != null) {
      questDao.updateQuest(
              entity.copy(
                      status = QuestStatus.COMPLETE,
                      finalSteps = finalSteps,
                      completedAt = now,
                      durationMin = duration.toInt()
              )
      )
    }

    _state.value =
            ActiveQuestState.Completed(
                    questId = activeQuestId,
                    totalSteps = finalSteps,
                    durationMinutes = duration
            )
  }

  /** Fail the active quest (e.g., midnight rollover without completion). */
  suspend fun failQuest() {
    if (_state.value !is ActiveQuestState.Active) return

    stepCounter.stop()

    val entity = questDao.getQuestByDate(LocalDate.now())
    if (entity != null) {
      questDao.updateQuest(entity.copy(status = QuestStatus.FAILED))
    }

    _state.value = ActiveQuestState.Idle
    activeQuestId = -1L
  }

  /** Reset back to idle state (after viewing completion screen). */
  fun resetToIdle() {
    _state.value = ActiveQuestState.Idle
    activeQuestId = -1L
  }

  /**
   * Restore quest state after process restart (e.g., app killed by system). Called from foreground
   * service on startup.
   */
  suspend fun restoreIfNeeded() {
    val activeEntity = questDao.getActiveQuest()
    if (activeEntity != null) {
      activeQuestId = activeEntity.id
      questStartTime = activeEntity.completedAt ?: Instant.now()
      // Restart tracking from where we left off (sensor baseline resets)
      stepCounter.start(activeEntity.goalSteps)

      _state.value =
              ActiveQuestState.Active(
                      questId = activeEntity.id,
                      progress = stepCounter.progress.value,
                      startTime = questStartTime,
                      elapsedMinutes = 0
              )
    }
  }

  /** Calculate current streak (consecutive completed quest days). */
  suspend fun getStreak(): Int {
    var streak = 0
    var checkDate = LocalDate.now()

    // Check today first (if quest completed today)
    while (true) {
      val quest = questDao.getQuestByDate(checkDate)
      if (quest?.status == QuestStatus.COMPLETE) {
        streak++
        checkDate = checkDate.minusDays(1)
      } else if (checkDate == LocalDate.now() && quest?.status == QuestStatus.ACTIVE) {
        // Today's quest is still active — check yesterday
        checkDate = checkDate.minusDays(1)
      } else {
        break
      }
    }
    return streak
  }
}
