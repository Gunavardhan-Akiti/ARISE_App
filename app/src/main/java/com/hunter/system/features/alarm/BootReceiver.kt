package com.hunter.system.features.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hunter.system.core.datastore.SystemPreferences
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "BootReceiver"
  }

  @Inject lateinit var alarmScheduler: AlarmScheduler
  @Inject lateinit var preferences: SystemPreferences

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

    Log.i(TAG, "Boot completed — restoring alarm schedule")

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val enabled = preferences.alarmEnabled.first()
        if (enabled) {
          val timeStr = preferences.alarmTime.first()
          val time = LocalTime.parse(timeStr)
          val days = preferences.alarmDays.first()
          alarmScheduler.schedule(time, days)
          Log.i(TAG, "Alarm restored: $timeStr on days=$days")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to restore alarm", e)
      } finally {
        pendingResult.finish()
      }
    }
  }
}
