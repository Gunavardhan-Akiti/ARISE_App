package com.hunter.system.features.alarm

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.hunter.system.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hunter.system.core.theme.GlitchText
import com.hunter.system.core.theme.ParticleBackground
import com.hunter.system.core.theme.SystemAccent
import com.hunter.system.core.theme.SystemBackground
import com.hunter.system.core.theme.SystemPrimary
import com.hunter.system.core.theme.SystemTextMuted
import com.hunter.system.core.theme.SystemTextPrimary
import com.hunter.system.core.theme.SystemTextSecondary
import com.hunter.system.core.theme.SystemWindow
import com.hunter.system.core.theme.SystemBlue
import com.hunter.system.features.settings.SettingsViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.onExactAlarmPermissionResult()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // Wallpaper background
    Image(
        painter = painterResource(R.drawable.wallpaper_bg),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
    // Dark overlay so text is readable
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Empty space on top so only time + quote + alarm box visible initially
      Spacer(Modifier.height(120.dp))

      // ─── Current Time ───
      var currentTime by remember { mutableStateOf(LocalTime.now()) }
      LaunchedEffect(Unit) {
        while (true) {
          currentTime = LocalTime.now()
          delay(1000L - (System.currentTimeMillis() % 1000L))
        }
      }

      Text(
          text = "%02d:%02d".format(currentTime.hour, currentTime.minute),
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
          color = SystemTextPrimary, letterSpacing = 4.sp
      )
      Text(
          text = LocalDate.now().let { date ->
            "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, " +
            "${date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${date.dayOfMonth}"
          },
          style = MaterialTheme.typography.bodyMedium, color = SystemTextSecondary
      )

      Spacer(Modifier.height(20.dp))

      // ─── Daily Motivation (below time) ───
      DailyMotivation()

      Spacer(Modifier.height(28.dp))

      // ─── Permission Banners ───
      if (state.needsExactAlarmPermission) {
        TransparentWindow(title = "PERMISSION REQUIRED") {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("Exact alarm permission is needed to schedule wake-up alarms.",
                color = SystemTextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
              context.startActivity(android.content.Intent(
                  android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                  android.net.Uri.parse("package:${context.packageName}")))
            }, colors = ButtonDefaults.buttonColors(containerColor = SystemPrimary)) {
              Text("GRANT PERMISSION")
            }
          }
        }
        Spacer(Modifier.height(12.dp))
      }

      if (state.needsFullScreenPermission) {
        TransparentWindow(title = "FULL-SCREEN ALERT") {
          Column(modifier = Modifier.padding(8.dp)) {
            Text("Full-screen notification permission is needed so the alarm appears over your lock screen.",
                color = SystemTextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
              context.startActivity(android.content.Intent(
                  android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                  android.net.Uri.parse("package:${context.packageName}")))
            }, colors = ButtonDefaults.buttonColors(containerColor = SystemPrimary)) {
              Text("GRANT PERMISSION")
            }
          }
        }
        Spacer(Modifier.height(12.dp))
      }

      // ─── Wake-Up Alarm ───
      TransparentWindow(title = "WAKE-UP ALARM") {
        Column {
          TextButton(onClick = { viewModel.showTimePicker() }, modifier = Modifier.fillMaxWidth()) {
            Text(state.alarmTime, style = MaterialTheme.typography.displayMedium,
                color = if (state.alarmEnabled) SystemPrimary else SystemTextMuted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
          }
          Spacer(Modifier.height(12.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
            Text(if (state.alarmEnabled) "ALARM ACTIVE" else "ALARM DISABLED",
                style = MaterialTheme.typography.labelLarge,
                color = if (state.alarmEnabled) SystemAccent else SystemTextMuted)
            Switch(checked = state.alarmEnabled, onCheckedChange = { viewModel.toggleAlarm(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = SystemPrimary,
                    checkedTrackColor = SystemPrimary.copy(alpha = 0.3f),
                    uncheckedThumbColor = SystemTextMuted, uncheckedTrackColor = SystemBackground))
          }
          Spacer(Modifier.height(16.dp))
          AlarmDayChips(enabledDays = state.alarmDays, onToggleDay = { viewModel.toggleAlarmDay(it) })
        }
      }

      Spacer(Modifier.height(16.dp))

      // ─── Alarm Sound ───
      AlarmSoundSection(
          currentSoundUri = settings.alarmSoundUri,
          currentSoundTitle = settings.alarmSoundTitle,
          currentVolume = settings.alarmVolume,
          vibrate = settings.alarmVibrate,
          onSoundSelected = settingsViewModel::setAlarmSound,
          onVolumeChanged = settingsViewModel::setAlarmVolume,
          onVibrateChanged = settingsViewModel::setAlarmVibrate,
      )

      Spacer(Modifier.height(16.dp))

      // ─── Alarm Dismiss ───
      AlarmDismissSection(
          dismissBySteps = settings.dismissBySteps,
          dismissStepCount = settings.dismissStepCount,
          dismissByBed = settings.dismissByBed,
          onDismissByStepsChanged = settingsViewModel::setDismissBySteps,
          onDismissStepCountChanged = settingsViewModel::setDismissStepCount,
          onDismissByBedChanged = settingsViewModel::setDismissByBed,
      )

      Spacer(Modifier.height(32.dp))
    }
  }

  if (state.showTimePicker) {
    TimePickerDialog(
        initialTime = state.alarmTime,
        onConfirm = { hour, minute -> viewModel.setAlarmTime(hour, minute); viewModel.hideTimePicker() },
        onDismiss = { viewModel.hideTimePicker() }
    )
  }
}

// ─── Daily Motivation ────────────────────────────────────────────────────

@Composable
private fun DailyMotivation() {
  val quotes = listOf(
      "\"I alone level up.\"\n— Sung Jin-Woo",
      "\"The difference between the novice and the master\nis that the master has failed more times\nthan the novice has tried.\"",
      "\"Every morning you have two choices:\ncontinue to sleep with your dreams,\nor wake up and chase them.\"",
      "\"The System has chosen you.\nDo not waste this opportunity, Hunter.\"",
      "\"Arise. Your daily quest awaits.\nThe weak make excuses.\nThe strong make progress.\"",
      "\"You were not born to be average.\nYou were born to be a Hunter.\"",
      "\"Discipline is choosing between\nwhat you want now\nand what you want most.\"",
      "\"The only person you need to be\nbetter than is who you were yesterday.\"",
      "\"A Hunter does not wait for the perfect moment.\nA Hunter creates it.\"",
      "\"Pain is temporary. Quitting lasts forever.\nArise, Hunter.\"",
      "\"The dungeon does not care\nif you are tired.\nNeither should you.\"",
      "\"Small daily improvements\nare the key to staggering long-term results.\"",
      "\"You didn't come this far\nto only come this far.\"",
      "\"The System rewards consistency.\nShow up. Every. Single. Day.\"",
      "\"Level 1 or Level 100 —\nthe grind never changes.\nOnly you do.\"",
      "\"Your alarm is not your enemy.\nIt is your call to battle.\"",
      "\"The shadows obey the strong.\nBecome someone the shadows fear.\"",
      "\"Today's struggle is tomorrow's strength.\nKeep moving, Hunter.\"",
      "\"An S-Rank Hunter was once\nan E-Rank who never gave up.\"",
      "\"The quest is simple.\nWake up. Show up. Level up.\"",
      "\"Comfort is the enemy of growth.\nEmbrace the discomfort.\"",
      "\"You are one alarm away\nfrom changing your life.\"",
      "\"Do not pray for easy quests.\nPray to be a stronger Hunter.\"",
      "\"Champions do not hit snooze.\"",
      "\"The gate has opened.\nWill you enter, or will you run?\"",
      "\"Your potential is limitless.\nYour excuses are not.\"",
      "\"Rise before the sun\nand you will outrun the shadows.\"",
      "\"A Hunter's greatest weapon\nis not strength — it is routine.\"",
      "\"The System does not give quests\nyou cannot complete.\"",
      "\"Make your bed. Take your steps.\nConquer the day. Repeat.\"",
  )
  val quote = quotes[LocalDate.now().dayOfYear % quotes.size]

  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
    Text("[ SYSTEM MESSAGE ]",
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 4.sp),
        color = SystemPrimary.copy(alpha = 0.5f))
    Spacer(Modifier.height(12.dp))
    Text(quote, style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold, lineHeight = 32.sp),
        color = SystemPrimary, textAlign = TextAlign.Center)
  }
}

// ─── Alarm Sound Section ─────────────────────────────────────────────────

@Composable
private fun AlarmSoundSection(
    currentSoundUri: String, currentSoundTitle: String, currentVolume: Int, vibrate: Boolean,
    onSoundSelected: (String, String) -> Unit, onVolumeChanged: (Int) -> Unit, onVibrateChanged: (Boolean) -> Unit,
) {
  val context = LocalContext.current
  val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    if (uri != null) {
      val title = RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Custom sound"
      onSoundSelected(uri.toString(), title)
    } else { onSoundSelected("", "Silent") }
  }

  TransparentWindow(title = "ALARM SOUND") {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.MusicNote, null, tint = SystemBlue, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(12.dp))
          Column {
            Text("Alarm Sound", color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(currentSoundTitle, color = SystemBlue.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
          }
        }
        OutlinedButton(onClick = {
          val existingUri = if (currentSoundUri.isNotEmpty()) Uri.parse(currentSoundUri)
              else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          ringtonePickerLauncher.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
          })
        }, colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemBlue),
            shape = RoundedCornerShape(8.dp)) { Text("CHANGE", letterSpacing = 1.sp, fontSize = 12.sp) }
      }
      Spacer(Modifier.height(20.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = SystemBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text("Volume", color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Text("$currentVolume%", color = SystemBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
      }
      Slider(value = currentVolume.toFloat(), onValueChange = { onVolumeChanged(it.toInt()) },
          valueRange = 0f..100f, colors = SliderDefaults.colors(thumbColor = SystemBlue,
              activeTrackColor = SystemBlue, inactiveTrackColor = SystemBlue.copy(alpha = 0.15f)))
      Spacer(Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Vibrate with alarm", color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Switch(checked = vibrate, onCheckedChange = onVibrateChanged,
            colors = SwitchDefaults.colors(checkedThumbColor = SystemBlue,
                checkedTrackColor = SystemBlue.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)))
      }
    }
  }
}

// ─── Alarm Dismiss Section ───────────────────────────────────────────────

@Composable
private fun AlarmDismissSection(
    dismissBySteps: Boolean, dismissStepCount: Int, dismissByBed: Boolean,
    onDismissByStepsChanged: (Boolean) -> Unit, onDismissStepCountChanged: (Int) -> Unit,
    onDismissByBedChanged: (Boolean) -> Unit,
) {
  val presets = listOf(1, 3, 9, 15, 30)

  TransparentWindow(title = "ALARM DISMISS") {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = SystemBlue, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(12.dp))
          Column {
            Text("Dismiss by Walking", color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("Require steps to silence alarm", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
          }
        }
        Switch(checked = dismissBySteps, onCheckedChange = onDismissByStepsChanged,
            colors = SwitchDefaults.colors(checkedThumbColor = SystemBlue,
                checkedTrackColor = SystemBlue.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)))
      }

      if (dismissBySteps) {
        Spacer(Modifier.height(16.dp))
        Text("Steps to dismiss: $dismissStepCount", color = SystemBlue,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
          presets.forEach { preset ->
            FilterChip(selected = dismissStepCount == preset, onClick = { onDismissStepCountChanged(preset) },
                label = { Text("$preset", fontSize = 12.sp, fontWeight = if (dismissStepCount == preset) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SystemBlue.copy(alpha = 0.3f), selectedLabelColor = SystemBlue),
                shape = RoundedCornerShape(8.dp))
          }
        }
        Spacer(Modifier.height(8.dp))
        Slider(value = dismissStepCount.toFloat(), onValueChange = { onDismissStepCountChanged(it.toInt()) },
            valueRange = 1f..30f, colors = SliderDefaults.colors(thumbColor = SystemBlue,
                activeTrackColor = SystemBlue, inactiveTrackColor = SystemBlue.copy(alpha = 0.15f)))
        Text("More steps = harder to snooze back to sleep", color = Color.White.copy(alpha = 0.4f),
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
      }

      Spacer(Modifier.height(20.dp))

      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.Bed, null, tint = SystemBlue, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(12.dp))
          Column {
            Text("Dismiss by Making Bed", color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("Take a photo — AI checks if bed is made", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
          }
        }
        Switch(checked = dismissByBed, onCheckedChange = onDismissByBedChanged,
            colors = SwitchDefaults.colors(checkedThumbColor = SystemBlue,
                checkedTrackColor = SystemBlue.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)))
      }
    }
  }
}

// ─── Time Picker Dialog ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialTime: String, onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
  val parts = initialTime.split(":")
  val timePickerState = rememberTimePickerState(
      initialHour = parts[0].toIntOrNull() ?: 5, initialMinute = parts[1].toIntOrNull() ?: 30, is24Hour = true)

  AlertDialog(onDismissRequest = onDismiss,
      confirmButton = { TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) { Text("SET", color = SystemPrimary) } },
      dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = SystemTextSecondary) } },
      title = { Text("SET ALARM TIME", style = MaterialTheme.typography.titleMedium, color = SystemPrimary) },
      text = { TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(
          clockDialColor = SystemBackground, selectorColor = SystemPrimary, containerColor = SystemBackground,
          clockDialSelectedContentColor = SystemTextPrimary, clockDialUnselectedContentColor = SystemTextSecondary,
          timeSelectorSelectedContainerColor = SystemPrimary.copy(alpha = 0.2f),
          timeSelectorUnselectedContainerColor = SystemBackground,
          timeSelectorSelectedContentColor = SystemPrimary, timeSelectorUnselectedContentColor = SystemTextSecondary,
          periodSelectorSelectedContainerColor = SystemPrimary.copy(alpha = 0.2f))) },
      containerColor = SystemBackground.copy(alpha = 0.95f))
}

// ─── Transparent Window (wallpaper-friendly) ────────────────────────────

@Composable
private fun TransparentWindow(title: String, content: @Composable () -> Unit) {
  Column(
      modifier = Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(Color.Black.copy(alpha = 0.45f))
          .padding(1.dp)
  ) {
    if (title.isNotEmpty()) {
      Row(
          modifier = Modifier.fillMaxWidth()
              .background(Color.Black.copy(alpha = 0.3f))
              .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SystemPrimary))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = SystemPrimary, letterSpacing = 3.sp)
      }
    }
    Box(modifier = Modifier.padding(16.dp)) { content() }
  }
}

// ─── Day Chips ───────────────────────────────────────────────────────────

@Composable
private fun AlarmDayChips(enabledDays: Set<Int>, onToggleDay: (Int) -> Unit) {
  val dayLabels = listOf("S" to 1, "M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7)
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
    dayLabels.forEach { (label, dayOfWeek) ->
      val selected = dayOfWeek in enabledDays
      Box(modifier = Modifier.size(36.dp).clip(CircleShape)
              .background(if (selected) SystemPrimary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
              .clickable { onToggleDay(dayOfWeek) },
          contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) SystemPrimary else SystemTextMuted)
      }
    }
  }
}
