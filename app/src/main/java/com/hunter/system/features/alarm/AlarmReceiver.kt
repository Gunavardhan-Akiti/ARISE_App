package com.hunter.system.features.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hunter.system.R
import com.hunter.system.core.datastore.SystemPreferences
import com.hunter.system.core.lock.KioskManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Alarm BroadcastReceiver triggered by AlarmManager.setAlarmClock().
 * Posts the alarm notification with fullScreenIntent immediately, starts AlarmRingingService,
 * and re-schedules the next occurrence for the configured days.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "AlarmReceiver"
  }

  @Inject lateinit var alarmScheduler: AlarmScheduler
  @Inject lateinit var preferences: SystemPreferences

  override fun onReceive(context: Context, intent: Intent) {
    Log.i(TAG, "Alarm fired! Action: ${intent.action}")

    KioskManager.setAlarmRinging()

    ensureNotificationChannel(context)

    val notification = buildAlarmNotification(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(AlarmRingingService.NOTIFICATION_ID, notification)
    Log.i(TAG, "Alarm notification posted")

    AlarmRingingService.start(context)

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val enabled = preferences.alarmEnabled.first()
        if (enabled) {
          val timeStr = preferences.alarmTime.first()
          val time = LocalTime.parse(timeStr)
          val days = preferences.alarmDays.first()
          alarmScheduler.schedule(time, days)
          Log.i(TAG, "Next alarm re-scheduled for days=$days")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to re-schedule alarm", e)
      } finally {
        pendingResult.finish()
      }
    }
  }

  private fun ensureNotificationChannel(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (nm.getNotificationChannel(AlarmRingingService.CHANNEL_ID) != null) return

    val channel =
            NotificationChannel(
                            AlarmRingingService.CHANNEL_ID,
                            "Alarm Ringing",
                            NotificationManager.IMPORTANCE_HIGH
                    )
                    .apply {
                      description = "Active alarm ringing"
                      setBypassDnd(true)
                      lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                      setSound(null, null)
                      enableVibration(false)
                    }
    nm.createNotificationChannel(channel)
  }

  private fun buildAlarmNotification(context: Context): Notification {
    val fullScreenIntent =
            Intent(context, AlarmRingingActivity::class.java).apply {
              addFlags(
                      Intent.FLAG_ACTIVITY_NEW_TASK or
                              Intent.FLAG_ACTIVITY_CLEAR_TOP or
                              Intent.FLAG_ACTIVITY_SINGLE_TOP
              )
            }
    val fullScreenPendingIntent =
            PendingIntent.getActivity(
                    context,
                    0,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

    return NotificationCompat.Builder(context, AlarmRingingService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚔ ARISE, HUNTER")
            .setContentText("Complete your challenge to dismiss the alarm!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
  }
}
