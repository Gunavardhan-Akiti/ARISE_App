# ARISE

**Solo Leveling SYSTEM Alarm App** — A gamified wake-up alarm inspired by *Solo Leveling*. The alarm can only be dismissed by walking steps or proving you made your bed (verified by on-device AI). Starts every morning with a rotating motivational quote from the System.

> *"Arise, Hunter."*

## Overview

| Spec | Value |
|------|-------|
| Platform | Android 16 (API 36) |
| Language | Kotlin 2.2.20 |
| UI | Jetpack Compose (BOM 2025.12.00) with Material 3 |
| Architecture | Single-screen, MVVM, Hilt DI |
| AI | Gemini Nano on-device (ML Kit GenAI Prompt API) |
| Min SDK | 36 |
| Build System | Gradle 8.13, AGP 8.8.0, KSP |

## Features

| Feature | Description |
|---------|-------------|
| **Alarm** | Daily alarm with configurable time, sound, volume, and vibration. Weekday selection (run on specific days). Doze-exempt via `AlarmManager.setAlarmClock()`. Persists across reboots. |
| **Dismiss by Steps** | Alarm can only be silenced after walking a configurable number of steps (uses `TYPE_STEP_COUNTER` sensor). |
| **Dismiss by Bed Photo** | Take a photo of your bed; Gemini Nano (on-device) analyzes the image and determines if the bed is made. If not, you must retry. No cloud calls, no API key. |
| **Daily Motivation** | Rotating Solo Leveling themed quotes that change every day. "[ SYSTEM MESSAGE ]" displayed prominently on the main screen. |
| **Solo Leveling Theme** | Dark immersive UI with cyan-blue glow panels, particle background, glitch text effects, and pulsing animations. |

## Single-Screen Layout

The entire app is a single scrollable screen:

1. **Permission banners** (if needed)
2. **Daily motivational quote** (changes each day, Solo Leveling themed)
3. **Current time and date**
4. **WAKE-UP ALARM** — time picker, on/off toggle, weekday chips (S M T W T F S)
5. **ALARM SOUND** — sound picker, volume slider, vibrate toggle
6. **ALARM DISMISS** — dismiss by walking (step count), dismiss by bed photo (AI)

## Tech Stack

- **Jetpack Compose BOM 2025.12.00** — UI framework
- **Material 3** — Theming and components
- **ML Kit GenAI Prompt API** (`genai-prompt:1.0.0-beta1`) — On-device Gemini Nano for bed classification
- **Health Connect** (`connect-client:1.1.0-alpha12`) — Step sync from Samsung Health / Google Fit (Phase 2)
- **Room 2.7.0** — Local database (KSP)
- **Hilt 2.56.2** — Dependency injection (KSP)
- **DataStore 1.1.4** — Preferences storage
- **Coroutines 1.10.1** — Async operations and Flows

## Alarm Dismiss Flow

```
Alarm fires
  → AlarmRingingActivity shown (full-screen, over lock screen)
  → Sound plays, vibration starts
  → User must satisfy at least one enabled condition:
      • Walk N steps (configurable, uses hardware step counter)
      • Take photo of made bed (Gemini Nano on-device classification)
  → Once satisfied, slide-up gesture dismisses alarm
  → Next alarm auto-scheduled for the next enabled weekday
```

## Permissions

| Permission | Purpose |
|-----------|---------|
| `SCHEDULE_EXACT_ALARM` | Doze-exempt alarm scheduling |
| `RECEIVE_BOOT_COMPLETED` | Re-register alarm after reboot |
| `USE_FULL_SCREEN_INTENT` | Alarm UI over lock screen |
| `WAKE_LOCK` | Keep screen on during alarm |
| `VIBRATE` | Alarm vibration |
| `FOREGROUND_SERVICE` | Background services |
| `FOREGROUND_SERVICE_HEALTH` | Step tracking service type |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Alarm ringing service type |
| `POST_NOTIFICATIONS` | Alarm and progress notifications |
| `ACTIVITY_RECOGNITION` | Hardware step counter sensor |
| `health.READ_STEPS` | Health Connect step data (Phase 2) |
| `CAMERA` | Capture bed photo for AI classification |

## Building

### Prerequisites
- JDK 21+
- Android SDK with API Level 36 (Android 16)
- Android Studio Ladybug or newer

### Commands

```bash
./gradlew assembleDebug    # Build debug APK
./gradlew installDebug     # Install on connected device
```

## Device Requirements

- **Gemini Nano (bed classification):** Requires a device with AICore support (Pixel 8+, Galaxy S24+, select Xiaomi/vivo). On unsupported devices, bed dismiss fails safe to false and the user can rely on step dismiss instead.
- **Step counter:** Requires hardware `TYPE_STEP_COUNTER` sensor. Falls back to immediate dismiss if sensor is unavailable.

## Roadmap

- **Phase 2:** Daily quest task board with custom tasks, step tracking via Health Connect (Samsung Health sync), carryover system, and goal review notifications. Code is written and stubbed — ready to enable.
- **Phase 3:** Home screen widget, hunter stats, streaks, and ranks.

## License

See [LICENSE](LICENSE) for details.
