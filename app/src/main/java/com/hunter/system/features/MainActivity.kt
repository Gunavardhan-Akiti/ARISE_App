package com.hunter.system.features

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.hunter.system.core.theme.SoloLevelingSystemTheme
import com.hunter.system.features.alarm.AlarmScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  companion object {
    const val EXTRA_FROM_ALARM = "extra_from_alarm"
    private const val TAG = "MainActivity"
  }

  private val permissionLauncher =
          registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

  private val healthConnectPermissionLauncher =
          registerForActivityResult(
              PermissionController.createRequestPermissionResultContract()
          ) { granted ->
            Log.i(TAG, "Health Connect permissions granted: $granted")
          }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    requestRequiredPermissions()
    requestHealthConnectPermission()

    Log.i(TAG, "onCreate")

    setContent {
      SoloLevelingSystemTheme {
        AlarmScreen()
      }
    }
  }

  private fun requestRequiredPermissions() {
    val needed = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) !=
                      PackageManager.PERMISSION_GRANTED
      ) {
        needed.add(Manifest.permission.ACTIVITY_RECOGNITION)
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                      PackageManager.PERMISSION_GRANTED
      ) {
        needed.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    if (needed.isNotEmpty()) {
      permissionLauncher.launch(needed.toTypedArray())
    }
  }

  private fun requestHealthConnectPermission() {
    try {
      val client = HealthConnectClient.getOrCreate(this)
      val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
      healthConnectPermissionLauncher.launch(permissions)
    } catch (e: Exception) {
      Log.w(TAG, "Health Connect not available on this device: ${e.message}")
    }
  }
}
