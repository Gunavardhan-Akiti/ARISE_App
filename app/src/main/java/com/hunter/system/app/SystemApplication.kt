package com.hunter.system.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Solo Leveling SYSTEM Application
 *
 * Hilt entry point for dependency injection.
 * Initializes notification channels and Health Connect on app start.
 */
@HiltAndroidApp
class SystemApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Notification channels will be created here in Phase 2
        // Health Connect availability check will be done here
    }
}
