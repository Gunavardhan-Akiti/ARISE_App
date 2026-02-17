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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore Preferences for app settings (Section 8.2 of tech spec).
 *
 * Lightweight key-value configuration for alarm time, step goal, strict mode, and whitelisted apps.
 */
private val Context.settingsDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "system_settings")

@Singleton
class SystemPreferences @Inject constructor(@ApplicationContext private val context: Context) {
  private val dataStore = context.settingsDataStore

  companion object {
    val ALARM_TIME = stringPreferencesKey("alarm_time")
    val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
    val ALARM_SOUND_URI = stringPreferencesKey("alarm_sound_uri")
    val ALARM_SOUND_TITLE = stringPreferencesKey("alarm_sound_title")
    val ALARM_VOLUME = intPreferencesKey("alarm_volume")
    val ALARM_VIBRATE = booleanPreferencesKey("alarm_vibrate")
    val DISMISS_BY_STEPS = booleanPreferencesKey("dismiss_by_steps")
    val DISMISS_STEP_COUNT = intPreferencesKey("dismiss_step_count")
    val DISMISS_BY_BED = booleanPreferencesKey("dismiss_by_bed")
    val STEP_GOAL = intPreferencesKey("step_goal")
    val ALARM_DAYS = stringSetPreferencesKey("alarm_days")
    val GOAL_REVIEW_ENABLED = booleanPreferencesKey("goal_review_enabled")
    val GOAL_REVIEW_TIME = stringPreferencesKey("goal_review_time")

    // Defaults
    const val DEFAULT_ALARM_TIME = "05:30"
    const val DEFAULT_STEP_GOAL = 10_000
    const val DEFAULT_GOAL_REVIEW_ENABLED = false
    const val DEFAULT_GOAL_REVIEW_TIME = "21:00"
    const val DEFAULT_DISMISS_BY_BED = false
    val DEFAULT_ALARM_DAYS: Set<String> = setOf("1", "2", "3", "4", "5", "6", "7")
    const val DEFAULT_ALARM_ENABLED = true
    const val DEFAULT_ALARM_VOLUME = 80
    const val DEFAULT_ALARM_VIBRATE = true
    // Empty string means "use system default alarm sound"
    const val DEFAULT_ALARM_SOUND_URI = ""
    const val DEFAULT_ALARM_SOUND_TITLE = "Default alarm"
    const val DEFAULT_DISMISS_BY_STEPS = true
    const val DEFAULT_DISMISS_STEP_COUNT = 9
  }

  // ─── Alarm Time ───
  val alarmTime: Flow<String> =
          dataStore.data.map { prefs -> prefs[ALARM_TIME] ?: DEFAULT_ALARM_TIME }

  suspend fun setAlarmTime(time: String) {
    dataStore.edit { it[ALARM_TIME] = time }
  }

  // ─── Alarm Enabled ───
  val alarmEnabled: Flow<Boolean> =
          dataStore.data.map { prefs -> prefs[ALARM_ENABLED] ?: DEFAULT_ALARM_ENABLED }

  suspend fun setAlarmEnabled(enabled: Boolean) {
    dataStore.edit { it[ALARM_ENABLED] = enabled }
  }

  // ─── Step Goal ───
  val stepGoal: Flow<Int> = dataStore.data.map { prefs -> prefs[STEP_GOAL] ?: DEFAULT_STEP_GOAL }

  suspend fun setStepGoal(goal: Int) {
    dataStore.edit { it[STEP_GOAL] = goal }
  }

  // ─── Alarm Sound URI ───
  val alarmSoundUri: Flow<String> =
          dataStore.data.map { prefs -> prefs[ALARM_SOUND_URI] ?: DEFAULT_ALARM_SOUND_URI }

  suspend fun setAlarmSoundUri(uri: String) {
    dataStore.edit { it[ALARM_SOUND_URI] = uri }
  }

  // ─── Alarm Sound Title ───
  val alarmSoundTitle: Flow<String> =
          dataStore.data.map { prefs -> prefs[ALARM_SOUND_TITLE] ?: DEFAULT_ALARM_SOUND_TITLE }

  suspend fun setAlarmSoundTitle(title: String) {
    dataStore.edit { it[ALARM_SOUND_TITLE] = title }
  }

  // ─── Alarm Volume ───
  val alarmVolume: Flow<Int> =
          dataStore.data.map { prefs -> prefs[ALARM_VOLUME] ?: DEFAULT_ALARM_VOLUME }

  suspend fun setAlarmVolume(volume: Int) {
    dataStore.edit { it[ALARM_VOLUME] = volume.coerceIn(0, 100) }
  }

  // ─── Alarm Vibrate ───
  val alarmVibrate: Flow<Boolean> =
          dataStore.data.map { prefs -> prefs[ALARM_VIBRATE] ?: DEFAULT_ALARM_VIBRATE }

  suspend fun setAlarmVibrate(enabled: Boolean) {
    dataStore.edit { it[ALARM_VIBRATE] = enabled }
  }

  // ─── Dismiss by Steps ───
  val dismissBySteps: Flow<Boolean> =
          dataStore.data.map { prefs -> prefs[DISMISS_BY_STEPS] ?: DEFAULT_DISMISS_BY_STEPS }

  suspend fun setDismissBySteps(enabled: Boolean) {
    dataStore.edit { it[DISMISS_BY_STEPS] = enabled }
  }

  // ─── Dismiss Step Count ───
  val dismissStepCount: Flow<Int> =
          dataStore.data.map { prefs -> prefs[DISMISS_STEP_COUNT] ?: DEFAULT_DISMISS_STEP_COUNT }

  suspend fun setDismissStepCount(count: Int) {
    dataStore.edit { it[DISMISS_STEP_COUNT] = count.coerceIn(1, 100) }
  }

  // ─── Dismiss by Bed ───
  val dismissByBed: Flow<Boolean> =
          dataStore.data.map { prefs -> prefs[DISMISS_BY_BED] ?: DEFAULT_DISMISS_BY_BED }

  suspend fun setDismissByBed(enabled: Boolean) {
    dataStore.edit { it[DISMISS_BY_BED] = enabled }
  }

  // ─── Alarm Days (1=Sun … 7=Sat, Calendar.DAY_OF_WEEK convention) ───
  val alarmDays: Flow<Set<Int>> =
          dataStore.data.map { prefs ->
            (prefs[ALARM_DAYS] ?: DEFAULT_ALARM_DAYS).mapNotNull { it.toIntOrNull() }.toSet()
          }

  suspend fun setAlarmDays(days: Set<Int>) {
    dataStore.edit { it[ALARM_DAYS] = days.map { d -> d.toString() }.toSet() }
  }

  // ─── Goal Review ───
  val goalReviewEnabled: Flow<Boolean> =
          dataStore.data.map { prefs -> prefs[GOAL_REVIEW_ENABLED] ?: DEFAULT_GOAL_REVIEW_ENABLED }

  suspend fun setGoalReviewEnabled(enabled: Boolean) {
    dataStore.edit { it[GOAL_REVIEW_ENABLED] = enabled }
  }

  val goalReviewTime: Flow<String> =
          dataStore.data.map { prefs -> prefs[GOAL_REVIEW_TIME] ?: DEFAULT_GOAL_REVIEW_TIME }

  suspend fun setGoalReviewTime(time: String) {
    dataStore.edit { it[GOAL_REVIEW_TIME] = time }
  }
}
