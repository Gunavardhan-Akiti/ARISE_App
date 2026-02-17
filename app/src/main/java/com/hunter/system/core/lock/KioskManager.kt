package com.hunter.system.core.lock

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the alarm dismiss screen should be shown (alarm fired but not yet dismissed).
 * Used so AlarmRingingActivity and MainActivity can redirect to the alarm screen until the user
 * properly dismisses it. No device lock or kiosk mode.
 */
object KioskManager {

  private const val TAG = "KioskManager"

  private val _isAlarmRinging = MutableStateFlow(false)

  /** Whether the alarm has fired and has not yet been dismissed. */
  val isAlarmRinging: StateFlow<Boolean> = _isAlarmRinging.asStateFlow()

  /** Call when the alarm fires. */
  fun setAlarmRinging() {
    _isAlarmRinging.value = true
    Log.i(TAG, "Alarm ringing")
  }

  /** Call when the user dismisses the alarm (slide-up). Alarm screen no longer required. */
  fun onAlarmDismissed() {
    _isAlarmRinging.value = false
    Log.i(TAG, "Alarm dismissed")
  }
}
