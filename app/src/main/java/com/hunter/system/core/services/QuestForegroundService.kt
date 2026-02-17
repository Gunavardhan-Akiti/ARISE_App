package com.hunter.system.core.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hunter.system.R
import com.hunter.system.core.quest.TaskManager
import com.hunter.system.core.sensors.StepCounterManager
import com.hunter.system.core.sensors.StepProvider
import com.hunter.system.features.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service for step tracking. Polls step count every 30s and updates
 * the step task in Room. Also keeps the step counter sensor alive.
 */
@AndroidEntryPoint
class QuestForegroundService : Service() {

  @Inject lateinit var taskManager: TaskManager
  @Inject lateinit var stepProvider: StepProvider
  @Inject lateinit var stepCounterManager: StepCounterManager

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var trackingJob: Job? = null

  companion object {
    private const val TAG = "QuestFgService"
    const val CHANNEL_ID = "quest_progress"
    const val NOTIFICATION_ID = 1001
    const val ACTION_ALARM_TRIGGERED = "com.hunter.system.ACTION_ALARM_TRIGGERED"
    const val ACTION_STOP = "com.hunter.system.ACTION_STOP_QUEST"

    fun start(context: Context) {
      val intent = Intent(context, QuestForegroundService::class.java).apply {
        action = ACTION_ALARM_TRIGGERED
      }
      context.startForegroundService(intent)
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, QuestForegroundService::class.java))
    }
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopSelf()
        return START_NOT_STICKY
      }
    }

    startForeground(NOTIFICATION_ID, buildNotification(0, 0))
    Log.i(TAG, "Service started — tracking steps")

    if (!stepCounterManager.isTracking) {
      stepCounterManager.start(10_000)
    }

    serviceScope.launch { taskManager.ensureTodayReady() }

    trackingJob?.cancel()
    trackingJob = serviceScope.launch {
      while (true) {
        try {
          val steps = stepProvider.getTodaySteps()
          taskManager.updateStepProgress(steps)

          val notif = buildNotification(steps, 0)
          getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notif)
        } catch (e: Exception) {
          Log.w(TAG, "Step poll failed: ${e.message}")
        }
        delay(30_000)
      }
    }

    return START_STICKY
  }

  override fun onDestroy() {
    trackingJob?.cancel()
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun createNotificationChannel() {
    val channel = NotificationChannel(CHANNEL_ID, "Quest Progress", NotificationManager.IMPORTANCE_LOW).apply {
      description = "Shows step progress during an active quest"
      setShowBadge(false)
    }
    getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
  }

  private fun buildNotification(current: Int, goal: Int): Notification {
    val contentIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Quest Active — %,d steps today".format(current))
        .setContentText("Keep walking, Hunter!")
        .setContentIntent(contentIntent)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()
  }
}
