package com.hunter.system.features.alarm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hunter.system.core.classification.BedMadeChecker
import com.hunter.system.core.datastore.SystemPreferences
import com.hunter.system.core.lock.KioskManager
import com.hunter.system.core.services.QuestForegroundService
import com.hunter.system.core.theme.SoloLevelingSystemTheme
import com.hunter.system.features.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

enum class BedCheckState { IDLE, ANALYZING, BED_MADE, BED_NOT_MADE }

@AndroidEntryPoint
class AlarmRingingActivity : ComponentActivity(), SensorEventListener {

  companion object {
    private const val TAG = "AlarmRingingActivity"
  }

  @Inject lateinit var preferences: SystemPreferences
  @Inject lateinit var bedMadeChecker: BedMadeChecker

  private lateinit var sensorManager: SensorManager
  private var stepSensor: Sensor? = null

  private var baselineSteps: Long = -1L
  private var stepsRequired: Int = SystemPreferences.DEFAULT_DISMISS_STEP_COUNT
  private var dismissBySteps: Boolean = SystemPreferences.DEFAULT_DISMISS_BY_STEPS
  private var dismissByBed: Boolean = SystemPreferences.DEFAULT_DISMISS_BY_BED

  private val _stepsTaken = MutableStateFlow(0)
  private val stepsTaken = _stepsTaken.asStateFlow()

  private val _stepsSatisfied = MutableStateFlow(false)
  private val _bedSatisfied = MutableStateFlow(false)
  private val _bedCheckState = MutableStateFlow(BedCheckState.IDLE)

  private val _canDismiss = MutableStateFlow(false)
  private val canDismiss = _canDismiss.asStateFlow()

  private var photoUri: Uri? = null

  private val cameraPermissionLauncher =
          registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera()
          }

  private val takePictureLauncher =
          registerForActivityResult(TakePictureWithExplicitGrant()) { success ->
            if (success && photoUri != null) {
              _bedCheckState.value = BedCheckState.ANALYZING
              CoroutineScope(Dispatchers.Main).launch {
                val result = bedMadeChecker.isBedMade(photoUri!!)
                if (result) {
                  _bedCheckState.value = BedCheckState.BED_MADE
                  _bedSatisfied.value = true
                  updateCanDismiss()
                } else {
                  _bedCheckState.value = BedCheckState.BED_NOT_MADE
                }
              }
            } else {
              _bedCheckState.value = BedCheckState.IDLE
            }
          }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setShowWhenLocked(true)
    setTurnScreenOn(true)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() { /* Must dismiss via slide-up */ }
    })

    runBlocking {
      dismissBySteps = preferences.dismissBySteps.first()
      stepsRequired = preferences.dismissStepCount.first()
      dismissByBed = preferences.dismissByBed.first()
    }

    sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    if (dismissBySteps && stepSensor != null) {
      sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
      Log.i(TAG, "Step counter registered — need $stepsRequired steps")
    } else if (dismissBySteps) {
      Log.w(TAG, "No step counter sensor — marking steps satisfied")
      _stepsSatisfied.value = true
    }

    if (!dismissBySteps) _stepsSatisfied.value = true
    if (!dismissByBed) _bedSatisfied.value = true

    updateCanDismiss()

    setContent {
      SoloLevelingSystemTheme {
        val steps by stepsTaken.collectAsState()
        val dismissEnabled by canDismiss.collectAsState()
        val bedState by _bedCheckState.collectAsState()
        val bedDone by _bedSatisfied.collectAsState()

        AlarmRingingScreen(
                stepsTaken = steps,
                stepsRequired = stepsRequired,
                showStepTracker = dismissBySteps,
                showBedChecker = dismissByBed,
                bedCheckState = bedState,
                bedSatisfied = bedDone,
                canDismiss = dismissEnabled,
                onTakePhoto = { onTakePhotoRequested() },
                onRetryBed = { _bedCheckState.value = BedCheckState.IDLE },
                onDismiss = { if (dismissEnabled) dismissAlarm() }
        )
      }
    }
  }

  private fun updateCanDismiss() {
    val noConditions = !dismissBySteps && !dismissByBed
    val stepsDone = !dismissBySteps || _stepsSatisfied.value
    val bedDone = !dismissByBed || _bedSatisfied.value

    _canDismiss.value = when {
      noConditions -> true
      dismissBySteps && dismissByBed -> _stepsSatisfied.value || _bedSatisfied.value
      else -> stepsDone && bedDone
    }
  }

  private fun onTakePhotoRequested() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      launchCamera()
    } else {
      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  private fun launchCamera() {
    val photoFile = File(cacheDir, "bed_check_${System.currentTimeMillis()}.jpg")
    photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
    takePictureLauncher.launch(photoUri!!)
  }

  override fun onDestroy() {
    super.onDestroy()
    sensorManager.unregisterListener(this)
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

    val totalSteps = event.values[0].toLong()
    if (baselineSteps < 0L) {
      baselineSteps = totalSteps
      Log.i(TAG, "Step baseline set: $baselineSteps")
    }

    val taken = (totalSteps - baselineSteps).toInt().coerceAtLeast(0)
    _stepsTaken.value = taken

    if (taken >= stepsRequired && !_stepsSatisfied.value) {
      _stepsSatisfied.value = true
      updateCanDismiss()
      Log.i(TAG, "Step threshold reached ($taken/$stepsRequired)")
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

  private fun dismissAlarm() {
    KioskManager.onAlarmDismissed()
    AlarmRingingService.stop(this)
    QuestForegroundService.start(this)

    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
              putExtra(MainActivity.EXTRA_FROM_ALARM, true)
            }
    startActivity(intent)
    finish()
  }
}

// ─── Compose UI ───────────────────────────────────────────────────────────

@Composable
private fun AlarmRingingScreen(
        stepsTaken: Int,
        stepsRequired: Int,
        showStepTracker: Boolean,
        showBedChecker: Boolean,
        bedCheckState: BedCheckState,
        bedSatisfied: Boolean,
        canDismiss: Boolean,
        onTakePhoto: () -> Unit,
        onRetryBed: () -> Unit,
        onDismiss: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")

  val pulseAlpha by infiniteTransition.animateFloat(
          initialValue = 0.3f, targetValue = 1f,
          animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                  label = "pulse"
          )

  val arrowOffset by infiniteTransition.animateFloat(
          initialValue = 0f, targetValue = -20f,
          animationSpec = infiniteRepeatable(animation = tween(700, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                  label = "arrow"
          )

  val stepProgress = if (stepsRequired > 0) (stepsTaken.toFloat() / stepsRequired).coerceIn(0f, 1f) else 1f

  val slideOffset = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val slideThreshold = -250f

  Box(
          modifier = Modifier.fillMaxSize().background(
                  Brush.verticalGradient(listOf(Color(0xFF0A0A14), Color(0xFF1A0008), Color(0xFF2D0010), Color(0xFF0A0A14)))
                          ),
          contentAlignment = Alignment.Center
  ) {
    Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
    ) {
      Spacer(Modifier.height(60.dp))

      // ─── System Alert Header ───
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                text = "[ SYSTEM ALERT ]",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 6.sp),
                color = Color(0xFFFF4444).copy(alpha = pulseAlpha),
                textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
                text = "ARISE",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 12.sp, fontSize = 64.sp),
                color = Color(0xFFFF2222).copy(alpha = pulseAlpha),
                textAlign = TextAlign.Center
        )
        Text(
                text = "HUNTER",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 16.sp),
                color = Color(0xFFFF6666).copy(alpha = 0.8f),
                textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Text(
                text = "\"You have been chosen as a Player.\nYour daily quest has arrived.\"",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
        )
      }

      // ─── Challenges Section ───
      Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(bottom = 48.dp)
      ) {
        // ── Step counter progress ──
        if (showStepTracker) {
          StepTrackerBlock(stepsTaken, stepsRequired, stepProgress, canDismiss)
          Spacer(Modifier.height(16.dp))
        }

        // ── Bed check block ──
        if (showBedChecker) {
          BedCheckBlock(bedCheckState, bedSatisfied, onTakePhoto, onRetryBed)
          Spacer(Modifier.height(24.dp))
        }

        // ── Slide-up dismiss or locked ──
        if (canDismiss) {
          DismissSlider(slideOffset, slideThreshold, arrowOffset, scope, onDismiss)
        } else {
          LockedIndicator(pulseAlpha)
        }
      }
    }
  }
}

@Composable
private fun StepTrackerBlock(stepsTaken: Int, stepsRequired: Int, stepProgress: Float, canDismiss: Boolean) {
  val stepsDone = stepsTaken >= stepsRequired

  Box(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                  .clip(RoundedCornerShape(16.dp))
                                  .background(Color.White.copy(alpha = 0.05f))
                                  .padding(20.dp)
          ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                imageVector = Icons.Default.DirectionsWalk, contentDescription = null,
                tint = if (stepsDone) Color(0xFF00FF88) else Color(0xFFFF4444),
                        modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                text = if (stepsDone) "WALK COMPLETE" else "WALK TO DISMISS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp),
                color = if (stepsDone) Color(0xFF00FF88) else Color(0xFFFF4444)
        )
      }
              Spacer(Modifier.height(16.dp))
              Text(
                      text = "${stepsTaken.coerceAtMost(stepsRequired)}",
              style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
              color = if (stepsDone) Color(0xFF00FF88) else Color.White
      )
      Text(text = "/ $stepsRequired steps", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.5f))
              Spacer(Modifier.height(12.dp))
              LinearProgressIndicator(
                      progress = { stepProgress },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
              color = if (stepsDone) Color(0xFF00FF88) else Color(0xFFFF4444),
                      trackColor = Color.White.copy(alpha = 0.1f),
              )
            }
          }
}

@Composable
private fun BedCheckBlock(
        bedCheckState: BedCheckState,
        bedSatisfied: Boolean,
        onTakePhoto: () -> Unit,
        onRetry: () -> Unit
) {
  Box(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(Color.White.copy(alpha = 0.05f))
                  .padding(20.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
                imageVector = Icons.Default.Bed, contentDescription = null,
                tint = if (bedSatisfied) Color(0xFF00FF88) else Color(0xFF4488FF),
                modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
                text = if (bedSatisfied) "BED MADE" else "MAKE YOUR BED",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp),
                color = if (bedSatisfied) Color(0xFF00FF88) else Color(0xFF4488FF)
        )
      }
      Spacer(Modifier.height(16.dp))

      when (bedCheckState) {
        BedCheckState.IDLE -> {
          Text(
                  text = "Take a photo of your made bed to dismiss",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.5f),
                  textAlign = TextAlign.Center
          )
          Spacer(Modifier.height(12.dp))
          Button(
                  onClick = onTakePhoto,
                  colors = ButtonDefaults.buttonColors(
                          containerColor = Color(0xFF4488FF).copy(alpha = 0.3f),
                          contentColor = Color(0xFF4488FF)
                  )
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("TAKE PHOTO", letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
          }
        }
        BedCheckState.ANALYZING -> {
          CircularProgressIndicator(
                  modifier = Modifier.size(40.dp),
                  color = Color(0xFF4488FF),
                  strokeWidth = 3.dp
          )
          Spacer(Modifier.height(8.dp))
          Text(
                  text = "Analyzing bed…",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFF4488FF)
          )
        }
        BedCheckState.BED_MADE -> {
          Icon(
                  imageVector = Icons.Default.CheckCircle, contentDescription = null,
                  tint = Color(0xFF00FF88), modifier = Modifier.size(40.dp)
          )
          Spacer(Modifier.height(8.dp))
          Text(
                  text = "Bed is made! Slide to dismiss.",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF00FF88)
          )
        }
        BedCheckState.BED_NOT_MADE -> {
          Icon(
                  imageVector = Icons.Default.Close, contentDescription = null,
                  tint = Color(0xFFFF4444), modifier = Modifier.size(40.dp)
          )
          Spacer(Modifier.height(8.dp))
          Text(
                  text = "Bed not made. Make the bed and try again.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFFF4444),
                  textAlign = TextAlign.Center
          )
          Spacer(Modifier.height(12.dp))
          Button(
                  onClick = onRetry,
                  colors = ButtonDefaults.buttonColors(
                          containerColor = Color(0xFFFF4444).copy(alpha = 0.3f),
                          contentColor = Color(0xFFFF4444)
                  )
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("TRY AGAIN", letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun DismissSlider(
        slideOffset: Animatable<Float, *>,
        slideThreshold: Float,
        arrowOffset: Float,
        scope: kotlinx.coroutines.CoroutineScope,
        onDismiss: () -> Unit
) {
  val slideProgress = (-slideOffset.value / -slideThreshold).coerceIn(0f, 1f)
  val accentColor = Color(0xFF00FF88)

  Icon(
          imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null,
          modifier = Modifier.size(36.dp).offset { IntOffset(0, arrowOffset.roundToInt() - 10) }.alpha(0.4f),
          tint = accentColor
  )
  Icon(
          imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null,
          modifier = Modifier.size(36.dp).offset { IntOffset(0, arrowOffset.roundToInt() + 10) }.alpha(0.7f),
          tint = accentColor
  )
  Spacer(Modifier.height(4.dp))
          Text(
                  text = "SLIDE UP TO ACCEPT QUEST",
                  style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                  color = accentColor.copy(alpha = 0.8f)
          )
          Spacer(Modifier.height(12.dp))

          Box(
          modifier = Modifier.size(130.dp)
                                  .offset { IntOffset(0, slideOffset.value.roundToInt()) }
                                  .clip(CircleShape)
                  .background(accentColor.copy(alpha = 0.15f + (slideProgress * 0.35f)))
                                  .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                            onDragEnd = {
                                              if (slideOffset.value <= slideThreshold) {
                                                onDismiss()
                                              } else {
                                scope.launch { slideOffset.animateTo(0f, animationSpec = tween(200)) }
                                              }
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                              change.consume()
                                              scope.launch {
                                slideOffset.snapTo((slideOffset.value + dragAmount).coerceIn(slideThreshold * 1.2f, 0f))
                                              }
                                            }
                                    )
                                  },
                  contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
              imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null,
                      tint = accentColor,
                      modifier = Modifier.size(32.dp).scale(1f + slideProgress * 0.3f)
              )
              Text(
                      text = if (slideProgress > 0.8f) "RELEASE!" else "ACCEPT\nQUEST",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                      color = accentColor,
                      textAlign = TextAlign.Center,
                      lineHeight = 16.sp
              )
            }
          }

          if (slideProgress > 0f) {
            Spacer(Modifier.height(8.dp))
            Text(
                    text = "${(slideProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(alpha = slideProgress)
            )
          }
}

@Composable
private fun LockedIndicator(pulseAlpha: Float) {
          Icon(
          imageVector = Icons.Default.Lock, contentDescription = "Locked",
                  modifier = Modifier.size(32.dp).alpha(pulseAlpha * 0.6f),
                  tint = Color(0xFFFF4444)
          )
          Spacer(Modifier.height(8.dp))
          Text(
          text = "COMPLETE A CHALLENGE TO UNLOCK",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp),
                  color = Color(0xFFFF4444).copy(alpha = 0.8f)
          )
          Spacer(Modifier.height(4.dp))
          Text(
          text = "Walk or make your bed to silence the alarm",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.4f)
          )
          Spacer(Modifier.height(16.dp))
          Box(
          modifier = Modifier.size(130.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)),
                  contentAlignment = Alignment.Center
          ) {
            Icon(
            imageVector = Icons.Default.Lock, contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.Gray.copy(alpha = 0.4f)
            )
          }
        }

/**
 * Custom TakePicture contract that explicitly grants read/write URI permissions to the camera app.
 * Android 18 will remove implicit URI grants for ACTION_IMAGE_CAPTURE, so these flags are required.
 */
private class TakePictureWithExplicitGrant : ActivityResultContract<Uri, Boolean>() {
  override fun createIntent(context: Context, input: Uri): Intent {
    return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
  }

  override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
    return resultCode == android.app.Activity.RESULT_OK
  }
}
