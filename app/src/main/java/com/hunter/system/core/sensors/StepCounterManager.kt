package com.hunter.system.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.hunter.system.core.quest.CriteriaProgress
import com.hunter.system.core.quest.QuestCriteriaTracker
import com.hunter.system.core.quest.QuestCriteriaType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Step counter using the phone's built-in TYPE_STEP_COUNTER sensor.
 *
 * TYPE_STEP_COUNTER returns total steps since last reboot. We record a baseline when tracking
 * starts and calculate progress as: progress = current_sensor_value - baseline
 *
 * This is the Phase 1 implementation. Health Connect integration will be a separate
 * QuestCriteriaTracker implementation added later.
 */
@Singleton
class StepCounterManager @Inject constructor(@ApplicationContext private val context: Context) :
        QuestCriteriaTracker, SensorEventListener {

  companion object {
    private const val TAG = "StepCounterManager"
  }

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

  override val criteriaType: QuestCriteriaType = QuestCriteriaType.Steps()

  private val _progress = MutableStateFlow(CriteriaProgress())
  override val progress: StateFlow<CriteriaProgress> = _progress.asStateFlow()

  private var _isTracking = false
  override val isTracking: Boolean
    get() = _isTracking

  /** Baseline sensor value recorded when tracking starts */
  private var baselineSensorValue: Long = -1L

  /** Current goal for this tracking session */
  private var currentGoal: Int = 10_000

  /** Debug step accumulator (for testing without walking) */
  private var debugStepOffset: Int = 0

  // ─── Sensor availability ──────────────────────────────────────
  val isSensorAvailable: Boolean
    get() = stepSensor != null

  // ─── QuestCriteriaTracker implementation ──────────────────────

  override fun start(goal: Int?) {
    if (_isTracking) {
      Log.w(TAG, "Already tracking — ignoring start()")
      return
    }

    currentGoal = goal ?: (criteriaType as QuestCriteriaType.Steps).goal
    baselineSensorValue = -1L // Will be set on first sensor event
    debugStepOffset = 0

    _progress.value =
            CriteriaProgress(
                    current = 0,
                    goal = currentGoal,
                    criteriaType = QuestCriteriaType.Steps(currentGoal)
            )

    if (stepSensor != null) {
      val registered =
              sensorManager.registerListener(
                      this,
                      stepSensor,
                      SensorManager.SENSOR_DELAY_UI // ~60ms, good for UI updates
              )
      if (registered) {
        _isTracking = true
        Log.i(TAG, "Step counter started — goal: $currentGoal")
      } else {
        Log.e(TAG, "Failed to register step counter sensor")
      }
    } else {
      // No hardware sensor — tracking still works via debug steps
      _isTracking = true
      Log.w(TAG, "No step counter sensor available — debug mode only")
    }
  }

  override fun stop() {
    if (!_isTracking) return

    sensorManager.unregisterListener(this)
    _isTracking = false
    baselineSensorValue = -1L
    debugStepOffset = 0
    Log.i(TAG, "Step counter stopped")
  }

  override fun addDebugSteps(count: Int) {
    if (!_isTracking) return
    debugStepOffset += count
    updateProgress(debugStepOffset)
    Log.d(TAG, "Debug steps added: +$count (total offset: $debugStepOffset)")
  }

  // ─── SensorEventListener ──────────────────────────────────────

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

    val totalStepsSinceReboot = event.values[0].toLong()

    // Record baseline on first event
    if (baselineSensorValue < 0) {
      baselineSensorValue = totalStepsSinceReboot
      Log.i(TAG, "Baseline recorded: $baselineSensorValue")
    }

    val sensorSteps = (totalStepsSinceReboot - baselineSensorValue).toInt()
    updateProgress(sensorSteps + debugStepOffset)
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    Log.d(TAG, "Sensor accuracy changed: $accuracy")
  }

  // ─── Internal ─────────────────────────────────────────────────

  private fun updateProgress(stepsTaken: Int) {
    _progress.value =
            CriteriaProgress(
                    current = stepsTaken.coerceAtLeast(0),
                    goal = currentGoal,
                    criteriaType = QuestCriteriaType.Steps(currentGoal)
            )
  }
}
