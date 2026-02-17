package com.hunter.system.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Preferences for app settings (Section 8.2 of tech spec).
 *
 * Lightweight key-value configuration for alarm time, step goal,
 * strict mode, and whitelisted apps.
 */

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "system_settings"
)

@Singleton
class SystemPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    companion object {
        val ALARM_TIME = stringPreferencesKey("alarm_time")
        val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val STRICT_MODE = booleanPreferencesKey("strict_mode")
        val WHITELISTED_APPS = stringSetPreferencesKey("whitelisted_apps")
        val EMERGENCY_PIN = stringPreferencesKey("emergency_pin")

        // Defaults per tech spec
        const val DEFAULT_ALARM_TIME = "05:30"
        const val DEFAULT_STEP_GOAL = 10_000
        const val DEFAULT_STRICT_MODE = true
        const val DEFAULT_ALARM_ENABLED = true
    }

    // ─── Alarm Time ───
    val alarmTime: Flow<String> = dataStore.data.map { prefs ->
        prefs[ALARM_TIME] ?: DEFAULT_ALARM_TIME
    }

    suspend fun setAlarmTime(time: String) {
        dataStore.edit { it[ALARM_TIME] = time }
    }

    // ─── Alarm Enabled ───
    val alarmEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ALARM_ENABLED] ?: DEFAULT_ALARM_ENABLED
    }

    suspend fun setAlarmEnabled(enabled: Boolean) {
        dataStore.edit { it[ALARM_ENABLED] = enabled }
    }

    // ─── Step Goal ───
    val stepGoal: Flow<Int> = dataStore.data.map { prefs ->
        prefs[STEP_GOAL] ?: DEFAULT_STEP_GOAL
    }

    suspend fun setStepGoal(goal: Int) {
        dataStore.edit { it[STEP_GOAL] = goal }
    }

    // ─── Strict Mode ───
    val strictMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[STRICT_MODE] ?: DEFAULT_STRICT_MODE
    }

    suspend fun setStrictMode(enabled: Boolean) {
        dataStore.edit { it[STRICT_MODE] = enabled }
    }

    // ─── Whitelisted Apps ───
    val whitelistedApps: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[WHITELISTED_APPS] ?: emptySet()
    }

    suspend fun setWhitelistedApps(apps: Set<String>) {
        dataStore.edit { it[WHITELISTED_APPS] = apps }
    }

    // ─── Emergency PIN ───
    val emergencyPin: Flow<String?> = dataStore.data.map { prefs ->
        prefs[EMERGENCY_PIN]
    }

    suspend fun setEmergencyPin(pin: String) {
        dataStore.edit { it[EMERGENCY_PIN] = pin }
    }
}
