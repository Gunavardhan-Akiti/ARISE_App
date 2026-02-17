package com.hunter.system.core.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service for quest tracking (Section 4.1 of tech spec).
 *
 * Keeps the alarm and step tracking alive while the quest is active.
 * Uses Android 16 Live Updates notification with ProgressStyle for
 * real-time step progress in the status bar.
 *
 * Service type: health|specialUse (declared in manifest)
 */
@AndroidEntryPoint
class QuestForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Will be fully implemented in Phase 1/2
        // - Start foreground with Live Updates notification
        // - Begin Health Connect polling (every 15 seconds)
        // - Update quest state via ViewModel/Repository
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up polling, release Health Connect resources
    }
}
