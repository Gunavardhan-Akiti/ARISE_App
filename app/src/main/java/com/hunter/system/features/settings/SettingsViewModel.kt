package com.hunter.system.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunter.system.core.datastore.SystemPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
        val stepGoal: Int = SystemPreferences.DEFAULT_STEP_GOAL,
        val alarmSoundUri: String = SystemPreferences.DEFAULT_ALARM_SOUND_URI,
        val alarmSoundTitle: String = SystemPreferences.DEFAULT_ALARM_SOUND_TITLE,
        val alarmVolume: Int = SystemPreferences.DEFAULT_ALARM_VOLUME,
        val alarmVibrate: Boolean = SystemPreferences.DEFAULT_ALARM_VIBRATE,
        val dismissBySteps: Boolean = SystemPreferences.DEFAULT_DISMISS_BY_STEPS,
        val dismissStepCount: Int = SystemPreferences.DEFAULT_DISMISS_STEP_COUNT,
        val dismissByBed: Boolean = SystemPreferences.DEFAULT_DISMISS_BY_BED,
        val goalReviewEnabled: Boolean = SystemPreferences.DEFAULT_GOAL_REVIEW_ENABLED,
        val goalReviewTime: String = SystemPreferences.DEFAULT_GOAL_REVIEW_TIME,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val prefs: SystemPreferences) : ViewModel() {

  val uiState: StateFlow<SettingsUiState> =
          combine(
                          prefs.stepGoal,
                          prefs.alarmSoundUri,
                          prefs.alarmSoundTitle,
                  ) { goal, soundUri, soundTitle ->
                    SettingsUiState(
                            stepGoal = goal,
                            alarmSoundUri = soundUri,
                            alarmSoundTitle = soundTitle
                    )
                  }
                  .combine(
                          combine(prefs.alarmVolume, prefs.alarmVibrate) { volume, vibrate ->
                            Pair(volume, vibrate)
                          }
                  ) { state, (volume, vibrate) ->
                    state.copy(alarmVolume = volume, alarmVibrate = vibrate)
                  }
                  .combine(
                          combine(prefs.dismissBySteps, prefs.dismissStepCount, prefs.dismissByBed) { enabled, count, bed ->
                            Triple(enabled, count, bed)
                          }
                  ) { state, (enabled, count, bed) ->
                    state.copy(dismissBySteps = enabled, dismissStepCount = count, dismissByBed = bed)
                  }
                  .combine(
                          combine(prefs.goalReviewEnabled, prefs.goalReviewTime) { enabled, time ->
                            Pair(enabled, time)
                          }
                  ) { state, (enabled, time) ->
                    state.copy(goalReviewEnabled = enabled, goalReviewTime = time)
                  }
                  .stateIn(
                          scope = viewModelScope,
                          started = SharingStarted.WhileSubscribed(5000),
                          initialValue = SettingsUiState()
                  )

  fun setStepGoal(goal: Int) {
    viewModelScope.launch { prefs.setStepGoal(goal.coerceIn(100, 100_000)) }
  }

  fun setAlarmSound(uri: String, title: String) {
    viewModelScope.launch {
      prefs.setAlarmSoundUri(uri)
      prefs.setAlarmSoundTitle(title)
    }
  }

  fun setAlarmVolume(volume: Int) {
    viewModelScope.launch { prefs.setAlarmVolume(volume) }
  }

  fun setAlarmVibrate(enabled: Boolean) {
    viewModelScope.launch { prefs.setAlarmVibrate(enabled) }
  }

  fun setDismissBySteps(enabled: Boolean) {
    viewModelScope.launch { prefs.setDismissBySteps(enabled) }
  }

  fun setDismissStepCount(count: Int) {
    viewModelScope.launch { prefs.setDismissStepCount(count) }
  }

  fun setDismissByBed(enabled: Boolean) {
    viewModelScope.launch { prefs.setDismissByBed(enabled) }
  }

  fun setGoalReviewEnabled(enabled: Boolean) {
    viewModelScope.launch { prefs.setGoalReviewEnabled(enabled) }
  }
}
