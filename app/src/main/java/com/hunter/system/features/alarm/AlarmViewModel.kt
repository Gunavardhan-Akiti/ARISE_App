package com.hunter.system.features.alarm

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunter.system.core.datastore.SystemPreferences
import com.hunter.system.core.quest.ActiveQuestState
import com.hunter.system.core.quest.QuestManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlarmUiState(
        val alarmTime: String = "05:30",
        val alarmEnabled: Boolean = true,
        val alarmDays: Set<Int> = (1..7).toSet(),
        val stepGoal: Int = 10_000,
        val streak: Int = 0,
        val isQuestActive: Boolean = false,
        val showTimePicker: Boolean = false,
        val needsExactAlarmPermission: Boolean = false,
        val needsFullScreenPermission: Boolean = false
)

@HiltViewModel
class AlarmViewModel
@Inject
constructor(
        @ApplicationContext private val appContext: Context,
        private val preferences: SystemPreferences,
        private val alarmScheduler: AlarmScheduler,
        private val questManager: QuestManager
) : ViewModel() {

  private val _showTimePicker = MutableStateFlow(false)
  private val _needsPermission = MutableStateFlow(!alarmScheduler.canScheduleExactAlarms())
  private val _needsFullScreenPermission = MutableStateFlow(checkNeedsFullScreenPermission())

  val uiState: StateFlow<AlarmUiState> =
          combine(
                          preferences.alarmTime,
                          preferences.alarmEnabled,
                          preferences.stepGoal,
                          preferences.alarmDays,
                          questManager.state,
                  ) { alarmTime, alarmEnabled, stepGoal, alarmDays, questState ->
                    AlarmUiState(
                            alarmTime = alarmTime,
                            alarmEnabled = alarmEnabled,
                            alarmDays = alarmDays,
                            stepGoal = stepGoal,
                            isQuestActive = questState is ActiveQuestState.Active,
                    )
                  }
                  .combine(_showTimePicker) { state, showPicker ->
                    state.copy(showTimePicker = showPicker)
                  }
                  .combine(_needsPermission) { state, needsPerm ->
                    state.copy(needsExactAlarmPermission = needsPerm)
                  }
                  .combine(_needsFullScreenPermission) { state, needsFs ->
                    state.copy(needsFullScreenPermission = needsFs)
                  }
                  .stateIn(
                          scope = viewModelScope,
                          started = SharingStarted.WhileSubscribed(5000),
                          initialValue = AlarmUiState()
                  )

  fun setAlarmTime(hour: Int, minute: Int) {
    val timeStr = "%02d:%02d".format(hour, minute)
    viewModelScope.launch {
      preferences.setAlarmTime(timeStr)
      if (uiState.value.alarmEnabled) {
        reschedule(LocalTime.of(hour, minute))
      }
    }
  }

  fun toggleAlarm(enabled: Boolean) {
    viewModelScope.launch {
      preferences.setAlarmEnabled(enabled)
      if (enabled) {
        reschedule()
      } else {
        alarmScheduler.cancel()
      }
    }
  }

  fun toggleAlarmDay(dayOfWeek: Int) {
    viewModelScope.launch {
      val current = uiState.value.alarmDays.toMutableSet()
      if (dayOfWeek in current) {
        if (current.size > 1) current.remove(dayOfWeek)
      } else {
        current.add(dayOfWeek)
      }
      preferences.setAlarmDays(current)
      if (uiState.value.alarmEnabled) {
        reschedule()
      }
    }
  }

  fun onExactAlarmPermissionResult() {
    _needsPermission.value = !alarmScheduler.canScheduleExactAlarms()
    _needsFullScreenPermission.value = checkNeedsFullScreenPermission()
  }

  private fun checkNeedsFullScreenPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
    val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return !nm.canUseFullScreenIntent()
  }

  private suspend fun reschedule(time: LocalTime? = null) {
    val t = time ?: LocalTime.parse(uiState.value.alarmTime)
    val days = preferences.alarmDays.first()
    val scheduled = alarmScheduler.schedule(t, days)
    if (!scheduled) _needsPermission.value = true
  }

  fun showTimePicker() { _showTimePicker.value = true }
  fun hideTimePicker() { _showTimePicker.value = false }

  fun startQuestNow() {
    viewModelScope.launch {
      val goal = uiState.value.stepGoal
      questManager.startQuest(goal)
    }
  }
}
