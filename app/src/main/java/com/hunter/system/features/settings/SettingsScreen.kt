package com.hunter.system.features.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hunter.system.core.theme.*

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Box(modifier = Modifier.fillMaxSize().background(SystemBlack)) {
    ParticleBackground()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
      Spacer(Modifier.height(16.dp))

      GlitchText(
              text = "SYSTEM CONFIG",
              style =
                      MaterialTheme.typography.headlineMedium.copy(
                              color = SystemBlue,
                              fontWeight = FontWeight.Black,
                              letterSpacing = 4.sp
                      )
      )

      Spacer(Modifier.height(24.dp))

      // ── Step Goal ──────────────────────────────────────────
      StepGoalSection(currentGoal = uiState.stepGoal, onGoalChanged = viewModel::setStepGoal)

      Spacer(Modifier.height(20.dp))

      // ── Alarm Sound & Volume ───────────────────────────────
      AlarmSoundSection(
              currentSoundUri = uiState.alarmSoundUri,
              currentSoundTitle = uiState.alarmSoundTitle,
              currentVolume = uiState.alarmVolume,
              vibrate = uiState.alarmVibrate,
              onSoundSelected = viewModel::setAlarmSound,
              onVolumeChanged = viewModel::setAlarmVolume,
              onVibrateChanged = viewModel::setAlarmVibrate
      )

      Spacer(Modifier.height(20.dp))

      // ── Alarm Dismiss Config ───────────────────────────────
      AlarmDismissSection(
              dismissBySteps = uiState.dismissBySteps,
              dismissStepCount = uiState.dismissStepCount,
              dismissByBed = uiState.dismissByBed,
              onDismissByStepsChanged = viewModel::setDismissBySteps,
              onDismissStepCountChanged = viewModel::setDismissStepCount,
              onDismissByBedChanged = viewModel::setDismissByBed
      )

      Spacer(Modifier.height(32.dp))
    }
  }
}

// ── Step Goal Section ───────────────────────────────────────────────────

@Composable
private fun StepGoalSection(currentGoal: Int, onGoalChanged: (Int) -> Unit) {
  val presets = listOf(1_000, 2_000, 4_000, 6_000, 10_000)

  SystemWindow(title = "DAILY STEP GOAL") {
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
              text = "%,d".format(currentGoal),
              color = SystemBlue,
              style = MaterialTheme.typography.displaySmall,
              fontWeight = FontWeight.Black
      )
      Text(
              text = "STEPS",
              color = SystemBlue.copy(alpha = 0.6f),
              style = MaterialTheme.typography.labelMedium,
              letterSpacing = 3.sp
      )

      Spacer(Modifier.height(16.dp))

      // Preset chips
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        presets.forEach { preset ->
          val selected = currentGoal == preset
          FilterChip(
                  selected = selected,
                  onClick = { onGoalChanged(preset) },
                  label = {
                    Text(
                            text = if (preset >= 1000) "${preset / 1000}K" else "$preset",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  colors =
                          FilterChipDefaults.filterChipColors(
                                  selectedContainerColor = SystemBlue.copy(alpha = 0.3f),
                                  selectedLabelColor = SystemBlue
                          ),
                  shape = RoundedCornerShape(8.dp)
          )
        }
      }

      Spacer(Modifier.height(12.dp))

      // Slider for fine-tuning
      Slider(
              value = currentGoal.toFloat(),
              onValueChange = { onGoalChanged(it.toInt()) },
              valueRange = 10f..10_000f,
              steps = 0,
              colors =
                      SliderDefaults.colors(
                              thumbColor = SystemBlue,
                              activeTrackColor = SystemBlue,
                              inactiveTrackColor = SystemBlue.copy(alpha = 0.15f)
                      )
      )
    }
  }
}

// ── Alarm Dismiss Section ────────────────────────────────────────────────

@Composable
private fun AlarmDismissSection(
        dismissBySteps: Boolean,
        dismissStepCount: Int,
        dismissByBed: Boolean,
        onDismissByStepsChanged: (Boolean) -> Unit,
        onDismissStepCountChanged: (Int) -> Unit,
        onDismissByBedChanged: (Boolean) -> Unit
) {
  val presets = listOf(1, 3, 9, 15, 30)

  SystemWindow(title = "ALARM DISMISS") {
    Column(modifier = Modifier.padding(16.dp)) {
      // Toggle: dismiss by steps
      Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
                  Icons.Default.DirectionsWalk,
                  contentDescription = null,
                  tint = SystemBlue,
                  modifier = Modifier.size(20.dp)
          )
          Spacer(Modifier.width(12.dp))
          Column {
            Text(
                    text = "Dismiss by Walking",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
            )
            Text(
                    text = "Require steps to silence alarm",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
            )
          }
        }
        Switch(
                checked = dismissBySteps,
                onCheckedChange = onDismissByStepsChanged,
                colors =
                        SwitchDefaults.colors(
                                checkedThumbColor = SystemBlue,
                                checkedTrackColor = SystemBlue.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                        )
        )
      }

      // Step count config (only shown when enabled)
      if (dismissBySteps) {
        Spacer(Modifier.height(16.dp))

        Text(
                text = "Steps to dismiss: $dismissStepCount",
                color = SystemBlue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(8.dp))

        // Preset chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
          presets.forEach { preset ->
            val selected = dismissStepCount == preset
            FilterChip(
                    selected = selected,
                    onClick = { onDismissStepCountChanged(preset) },
                    label = {
                      Text(
                              text = "$preset",
                              fontSize = 12.sp,
                              fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                      )
                    },
                    colors =
                            FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemBlue.copy(alpha = 0.3f),
                                    selectedLabelColor = SystemBlue
                            ),
                    shape = RoundedCornerShape(8.dp)
            )
          }
        }

        Spacer(Modifier.height(8.dp))

        // Slider for fine-tuning
        Slider(
                value = dismissStepCount.toFloat(),
                onValueChange = { onDismissStepCountChanged(it.toInt()) },
                valueRange = 1f..30f,
                steps = 0,
                colors =
                        SliderDefaults.colors(
                                thumbColor = SystemBlue,
                                activeTrackColor = SystemBlue,
                                inactiveTrackColor = SystemBlue.copy(alpha = 0.15f)
                        )
        )

        Text(
                text = "More steps = harder to snooze back to sleep",
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
        )
      }

      Spacer(Modifier.height(20.dp))

      // Toggle: dismiss by bed photo
      Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
                  Icons.Default.Bed,
                  contentDescription = null,
                  tint = SystemBlue,
                  modifier = Modifier.size(20.dp)
          )
          Spacer(Modifier.width(12.dp))
          Column {
            Text(
                    text = "Dismiss by Making Bed",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
            )
            Text(
                    text = "Take a photo — AI checks if bed is made",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
            )
          }
        }
        Switch(
                checked = dismissByBed,
                onCheckedChange = onDismissByBedChanged,
                colors =
                        SwitchDefaults.colors(
                                checkedThumbColor = SystemBlue,
                                checkedTrackColor = SystemBlue.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                        )
        )
      }
    }
  }
}

// ── Alarm Sound Section ─────────────────────────────────────────────────

@Composable
private fun AlarmSoundSection(
        currentSoundUri: String,
        currentSoundTitle: String,
        currentVolume: Int,
        vibrate: Boolean,
        onSoundSelected: (String, String) -> Unit,
        onVolumeChanged: (Int) -> Unit,
        onVibrateChanged: (Boolean) -> Unit
) {
  val context = LocalContext.current

  // Ringtone picker launcher
  val ringtonePickerLauncher =
          rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                  result ->
            val uri: Uri? =
                    result.data?.getParcelableExtra(
                            RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                            Uri::class.java
                    )
            if (uri != null) {
              val ringtone = RingtoneManager.getRingtone(context, uri)
              val title = ringtone?.getTitle(context) ?: "Custom sound"
              onSoundSelected(uri.toString(), title)
            } else {
              // User picked "None" / silent
              onSoundSelected("", "Silent")
            }
          }

  SystemWindow(title = "ALARM SOUND") {
    Column(modifier = Modifier.padding(16.dp)) {
      // Sound picker
      Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
                  Icons.Default.MusicNote,
                  contentDescription = null,
                  tint = SystemBlue,
                  modifier = Modifier.size(20.dp)
          )
          Spacer(Modifier.width(12.dp))
          Column {
            Text(
                    text = "Alarm Sound",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
            )
            Text(
                    text = currentSoundTitle,
                    color = SystemBlue.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
            )
          }
        }
        OutlinedButton(
                onClick = {
                  val existingUri =
                          if (currentSoundUri.isNotEmpty()) {
                            Uri.parse(currentSoundUri)
                          } else {
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                          }
                  val intent =
                          Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                    RingtoneManager.TYPE_ALARM
                            )
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                          }
                  ringtonePickerLauncher.launch(intent)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemBlue),
                shape = RoundedCornerShape(8.dp)
        ) { Text("CHANGE", letterSpacing = 1.sp, fontSize = 12.sp) }
      }

      Spacer(Modifier.height(20.dp))

      // Volume slider
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
                Icons.Default.VolumeUp,
                contentDescription = null,
                tint = SystemBlue,
                modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
                text = "Volume",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(8.dp))
        Text(
                text = "$currentVolume%",
                color = SystemBlue,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
        )
      }
      Slider(
              value = currentVolume.toFloat(),
              onValueChange = { onVolumeChanged(it.toInt()) },
              valueRange = 0f..100f,
              steps = 0,
              colors =
                      SliderDefaults.colors(
                              thumbColor = SystemBlue,
                              activeTrackColor = SystemBlue,
                              inactiveTrackColor = SystemBlue.copy(alpha = 0.15f)
                      )
      )

      Spacer(Modifier.height(8.dp))

      // Vibrate toggle
      Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
                text = "Vibrate with alarm",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
        )
        Switch(
                checked = vibrate,
                onCheckedChange = onVibrateChanged,
                colors =
                        SwitchDefaults.colors(
                                checkedThumbColor = SystemBlue,
                                checkedTrackColor = SystemBlue.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                        )
        )
      }
    }
  }
}

// ── Criteria Row ────────────────────────────────────────────────────────

@Composable
private fun CriteriaRow(name: String, active: Boolean, comingSoon: Boolean = false) {
  Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
            modifier =
                    Modifier.size(8.dp)
                            .background(
                                    color =
                                            if (active) SystemBlue
                                            else Color.Gray.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                            )
    )
    Spacer(Modifier.width(12.dp))
    Text(
            text = name,
            color = if (active) Color.White else Color.White.copy(alpha = 0.4f),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
    )
    if (comingSoon) {
      Text(
              text = "COMING SOON",
              color = SystemBlue.copy(alpha = 0.4f),
              style = MaterialTheme.typography.labelSmall,
              letterSpacing = 1.sp
      )
    } else if (active) {
      Text(
              text = "ACTIVE",
              color = SystemBlue,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
      )
    }
  }
}
