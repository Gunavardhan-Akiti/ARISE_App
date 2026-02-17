package com.hunter.system.core.sensors

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StepSource { HEALTH_CONNECT, SENSOR, NONE }

@Singleton
class StepProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepCounterManager: StepCounterManager,
) {
    companion object {
        private const val TAG = "StepProvider"
        val REQUIRED_PERMISSIONS = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    }

    private val _source = MutableStateFlow(StepSource.NONE)
    val source: StateFlow<StepSource> = _source.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasHealthConnectPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.w(TAG, "Health Connect not available: ${e.message}")
            null
        }
    }

    suspend fun checkPermission() {
        val client = healthConnectClient ?: run {
            _hasPermission.value = false
            return
        }
        try {
            val granted = client.permissionController.getGrantedPermissions()
            _hasPermission.value = granted.containsAll(REQUIRED_PERMISSIONS)
            Log.i(TAG, "Health Connect permission check: ${_hasPermission.value}")
        } catch (e: Exception) {
            _hasPermission.value = false
            Log.w(TAG, "Permission check failed: ${e.message}")
        }
    }

    suspend fun getTodaySteps(): Int {
        if (_hasPermission.value) {
            healthConnectClient?.let { client ->
                try {
                    val startOfDay = LocalDate.now()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                    val now = Instant.now()
                    val response = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                        )
                    )
                    response[StepsRecord.COUNT_TOTAL]?.let {
                        _source.value = StepSource.HEALTH_CONNECT
                        Log.d(TAG, "Health Connect steps: ${it.toInt()}")
                        return it.toInt()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Health Connect read failed: ${e.message}")
                }
            }
        }

        _source.value = StepSource.SENSOR
        return stepCounterManager.progress.value.current
    }

    fun isHealthConnectAvailable(): Boolean = healthConnectClient != null
}
