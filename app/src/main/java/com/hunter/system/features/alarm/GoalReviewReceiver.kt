package com.hunter.system.features.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hunter.system.R
import com.hunter.system.core.database.SystemDatabase
import com.hunter.system.features.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GoalReviewReceiver : BroadcastReceiver() {

  companion object {
    private const val TAG = "GoalReviewReceiver"
    private const val CHANNEL_ID = "goal_review"
    private const val NOTIFICATION_ID = 2001
  }

  override fun onReceive(context: Context, intent: Intent) {
    Log.i(TAG, "Goal review triggered")

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = androidx.room.Room.databaseBuilder(
            context, SystemDatabase::class.java, SystemDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

        val pending = db.taskDao().getPendingTasksForDate(LocalDate.now())
        if (pending.isNotEmpty()) {
          postNotification(context, pending.size)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to check goals", e)
      } finally {
        pendingResult.finish()
      }
    }
  }

  private fun postNotification(context: Context, pendingCount: Int) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (nm.getNotificationChannel(CHANNEL_ID) == null) {
      nm.createNotificationChannel(
          NotificationChannel(CHANNEL_ID, "Goal Review", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Daily goal review reminder"
          }
      )
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pi = PendingIntent.getActivity(context, 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    val notif = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Hunter, review your quests")
        .setContentText("You have $pendingCount quest${if (pendingCount > 1) "s" else ""} remaining today.")
        .setContentIntent(pi)
        .setAutoCancel(true)
        .build()

    nm.notify(NOTIFICATION_ID, notif)
    Log.i(TAG, "Goal review notification posted: $pendingCount pending")
  }
}
