package com.hunter.system.features.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hunter.system.R
import com.hunter.system.core.datastore.SystemPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that plays the alarm sound and vibrates until dismissed.
 *
 * This runs separately from QuestForegroundService so we can control the alarm media independently.
 * Uses USAGE_ALARM AudioAttributes so volume follows the alarm stream.
 */
@AndroidEntryPoint
class AlarmRingingService : Service() {

  @Inject lateinit var preferences: SystemPreferences

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var mediaPlayer: MediaPlayer? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private var vibrator: Vibrator? = null
  private var isRinging = false

  companion object {
    private const val TAG = "AlarmRingingService"
    const val CHANNEL_ID = "alarm_ringing"
    const val NOTIFICATION_ID = 3001
    const val ACTION_DISMISS = "com.hunter.system.ACTION_DISMISS_ALARM"
    const val ACTION_SNOOZE = "com.hunter.system.ACTION_SNOOZE_ALARM"

    // Max ringing duration (5 minutes) as a safety net
    private const val MAX_RING_DURATION_MS = 5 * 60 * 1000L

    fun start(context: Context) {
      val intent = Intent(context, AlarmRingingService::class.java)
      context.startForegroundService(intent)
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, AlarmRingingService::class.java))
    }
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    acquireWakeLock()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Show foreground notification (required within 10s of startForegroundService)
    startForeground(NOTIFICATION_ID, buildRingingNotification())

    // Guard: only start sound/vibration once even if onStartCommand is called multiple times
    if (!isRinging) {
      isRinging = true

      // Start playing alarm sound
      serviceScope.launch {
        startAlarmSound()
        startVibration()
      }

      // Safety timeout — stop after MAX_RING_DURATION_MS
      serviceScope.launch {
        kotlinx.coroutines.delay(MAX_RING_DURATION_MS)
        Log.w(TAG, "Alarm ringing timed out after ${MAX_RING_DURATION_MS / 1000}s")
        stopSelf()
      }
    }

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    isRinging = false
    stopAlarmSound()
    stopVibration()
    releaseWakeLock()
    serviceScope.cancel()
    Log.i(TAG, "Alarm ringing stopped")
  }

  // ─── Media Player ─────────────────────────────────────────────────

  private suspend fun startAlarmSound() {
    try {
      val soundUriStr = preferences.alarmSoundUri.first()
      val volume = preferences.alarmVolume.first()

      // Resolve the alarm sound URI
      val soundUri: Uri =
              if (soundUriStr.isNotEmpty()) {
                Uri.parse(soundUriStr)
              } else {
                // Use system default alarm sound, fallback to notification, then ringtone
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
              }

      Log.i(TAG, "Playing alarm sound: $soundUri at volume $volume%")

      // Set the alarm stream volume
      val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
      val maxStreamVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
      val targetVolume = (maxStreamVolume * volume / 100f).toInt().coerceIn(0, maxStreamVolume)
      audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)

      mediaPlayer =
              MediaPlayer().apply {
                setAudioAttributes(
                        AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                )
                setDataSource(this@AlarmRingingService, soundUri)
                isLooping = true
                prepare()
                start()
              }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to play alarm sound", e)
      // Fallback: try system default
      tryFallbackAlarmSound()
    }
  }

  private fun tryFallbackAlarmSound() {
    try {
      val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
      mediaPlayer =
              MediaPlayer().apply {
                setAudioAttributes(
                        AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                )
                setDataSource(this@AlarmRingingService, fallbackUri)
                isLooping = true
                prepare()
                start()
              }
    } catch (e: Exception) {
      Log.e(TAG, "Fallback alarm sound also failed", e)
    }
  }

  private fun stopAlarmSound() {
    mediaPlayer?.let {
      try {
        if (it.isPlaying) it.stop()
        it.release()
      } catch (e: Exception) {
        Log.e(TAG, "Error stopping media player", e)
      }
    }
    mediaPlayer = null
  }

  // ─── Vibration ─────────────────────────────────────────────────────

  private suspend fun startVibration() {
    val shouldVibrate = preferences.alarmVibrate.first()
    if (!shouldVibrate) return

    vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
              vm.defaultVibrator
            } else {
              @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

    // Pattern: wait 0ms, vibrate 500ms, pause 500ms, vibrate 500ms, pause 500ms...
    val pattern = longArrayOf(0, 500, 500, 500, 500, 800, 400)
    vibrator?.vibrate(
            VibrationEffect.createWaveform(pattern, 0) // repeat from index 0
    )
  }

  private fun stopVibration() {
    vibrator?.cancel()
    vibrator = null
  }

  // ─── Wake Lock ─────────────────────────────────────────────────────

  private fun acquireWakeLock() {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock =
            pm.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "ARISE:AlarmRinging"
                    )
                    .apply {
                      acquire(MAX_RING_DURATION_MS) // auto-release after timeout
                    }
  }

  private fun releaseWakeLock() {
    wakeLock?.let { if (it.isHeld) it.release() }
    wakeLock = null
  }

  // ─── Notification ──────────────────────────────────────────────────

  private fun createNotificationChannel() {
    val channel =
            NotificationChannel(CHANNEL_ID, "Alarm Ringing", NotificationManager.IMPORTANCE_HIGH)
                    .apply {
                      description = "Active alarm ringing"
                      setBypassDnd(true)
                      lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                      setSound(null, null) // We handle sound ourselves via MediaPlayer
                      enableVibration(false) // We handle vibration ourselves
                    }
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(channel)
  }

  private fun buildRingingNotification(): Notification {
    // Full-screen intent to show the ringing activity
    val fullScreenIntent =
            Intent(this, AlarmRingingActivity::class.java).apply {
              addFlags(
                      Intent.FLAG_ACTIVITY_NEW_TASK or
                              Intent.FLAG_ACTIVITY_CLEAR_TOP or
                              Intent.FLAG_ACTIVITY_SINGLE_TOP
              )
            }
    val fullScreenPendingIntent =
            PendingIntent.getActivity(
                    this,
                    0,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

    return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚔ ARISE, HUNTER")
            .setContentText("Complete your steps to dismiss the alarm!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
  }
}
