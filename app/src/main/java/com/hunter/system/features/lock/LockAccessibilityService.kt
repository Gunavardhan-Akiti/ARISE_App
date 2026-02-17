package com.hunter.system.features.lock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service for lock enforcement (Section 5.2.1 — Secondary Layer).
 *
 * Detects and blocks unauthorized app launches during active quest.
 * Only active when a quest is in ACTIVE state.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Phase 2: Detect foreground app changes
        // Check against whitelist (phone, Bluetooth, selected music apps)
        // Block unauthorized apps by navigating back to quest screen
    }

    override fun onInterrupt() {
        // Service interrupted — handle gracefully
    }
}
