package com.hunter.system.core.permissions

/**
 * Permission tracking for all required system permissions (Section 7 of tech spec).
 *
 * Android 16 permissions required:
 * - health.READ_STEPS — Health Connect step data
 * - READ_HEALTH_DATA_IN_BACKGROUND — background step reads
 * - DEVICE_ADMIN — screen pinning
 * - ACCESSIBILITY_SERVICE — app blocking
 * - NOTIFICATION_LISTENER — notification suppression
 * - SYSTEM_ALERT_WINDOW — overlay blocker
 * - USE_FULL_SCREEN_INTENT — alarm on lock screen
 * - POST_NOTIFICATIONS — Live Updates progress notification
 */
data class PermissionState(
    val healthConnectGranted: Boolean = false,
    val backgroundHealthGranted: Boolean = false,
    val deviceAdminEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val fullScreenIntentGranted: Boolean = false,
    val postNotificationsGranted: Boolean = false
) {
    /** All critical permissions needed for quest activation */
    val canActivateQuest: Boolean
        get() = healthConnectGranted && postNotificationsGranted

    /** All permissions needed for full kiosk mode */
    val canEnableKiosk: Boolean
        get() = deviceAdminEnabled && accessibilityEnabled &&
                notificationListenerEnabled && overlayPermissionGranted

    /** All permissions granted */
    val allGranted: Boolean
        get() = canActivateQuest && canEnableKiosk &&
                backgroundHealthGranted && fullScreenIntentGranted
}
