package com.hunter.system.features.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alarm scheduling using AlarmManager.setAlarmClock(). Doze-exempt, survives battery optimization.
 * Supports weekday filtering: the alarm is scheduled for the next matching day at the given time.
 * BootReceiver and AlarmReceiver re-register after reboot / fire.
 */
@Singleton
class AlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) {
  companion object {
    private const val TAG = "AlarmScheduler"
    const val ALARM_REQUEST_CODE = 1001
    const val ACTION_ALARM_FIRED = "com.hunter.system.ACTION_ALARM_FIRED"
  }

  private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  fun canScheduleExactAlarms(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      alarmManager.canScheduleExactAlarms()
    } else {
      true
    }
  }

  /**
   * Schedule alarm at [time] only on [enabledDays] (Calendar.DAY_OF_WEEK: 1=Sun … 7=Sat).
   * Finds the next future instant that matches both the time and an enabled day.
   */
  fun schedule(time: LocalTime, enabledDays: Set<Int> = (1..7).toSet()): Boolean {
    if (enabledDays.isEmpty()) {
      cancel()
      return true
    }
    if (!canScheduleExactAlarms()) {
      Log.w(TAG, "Cannot schedule exact alarm — permission not granted")
      return false
    }

    val triggerCalendar = nextTrigger(time, enabledDays)
    val pendingIntent = createPendingIntent()
    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerCalendar.timeInMillis, createShowIntent())

    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    Log.i(TAG, "Alarm scheduled for ${triggerCalendar.time} (day ${triggerCalendar.get(Calendar.DAY_OF_WEEK)})")
    return true
  }

  fun cancel() {
    alarmManager.cancel(createPendingIntent())
    Log.i(TAG, "Alarm cancelled")
  }

  fun isScheduled(): Boolean {
    val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_ALARM_FIRED }
    val existing =
            PendingIntent.getBroadcast(
                    context,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
    return existing != null
  }

  /**
   * Find the next Calendar instant at [time] that falls on one of [enabledDays].
   * Tries today first (if the time is still in the future), then iterates up to 7 days ahead.
   */
  private fun nextTrigger(time: LocalTime, enabledDays: Set<Int>): Calendar {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, time.hour)
      set(Calendar.MINUTE, time.minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    if (cal.timeInMillis <= System.currentTimeMillis()) {
      cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    for (i in 0 until 7) {
      if (cal.get(Calendar.DAY_OF_WEEK) in enabledDays) return cal
      cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    return cal
  }

  private fun createPendingIntent(): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_ALARM_FIRED }
    return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun createShowIntent(): PendingIntent {
    val intent =
            Intent(context, com.hunter.system.features.MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }
}
