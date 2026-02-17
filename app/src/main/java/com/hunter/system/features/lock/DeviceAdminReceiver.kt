package com.hunter.system.features.lock

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device Admin Receiver for kiosk mode (Section 5.2.1 — Primary Layer).
 *
 * Enables screen pinning and lock enforcement.
 * User grants Device Admin permission during onboarding.
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Device admin enabled — screen pinning now available
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Device admin disabled — kiosk mode no longer available
    }
}
