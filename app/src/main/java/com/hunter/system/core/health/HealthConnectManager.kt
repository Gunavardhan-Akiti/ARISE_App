package com.hunter.system.core.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Connect client wrapper for step tracking (Section 5.3 of tech spec).
 *
 * Reads aggregated step data from Health Connect, which automatically
 * de-duplicates data from multiple sources (phone sensor, Wear OS,
 * Samsung Health, Fitbit, etc.).
 *
 * On Android 16, Health Connect is a framework module — no separate
 * APK install required.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Check if Health Connect is available on this device.
     * On Android 16, it should always be available as a framework module.
     */
    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    /**
     * Get aggregated step count within a time range.
     * Health Connect automatically de-duplicates from multiple sources.
     *
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Total step count, or 0 if unavailable
     */
    suspend fun getStepCount(startTime: Instant, endTime: Instant): Long {
        return try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            // Log error — will be handled properly in Phase 1
            0L
        }
    }

    /**
     * Get the baseline step count for quest activation.
     * Records the aggregate from midnight to activation time.
     *
     * Progress = current_aggregate - baseline_aggregate
     */
    suspend fun getBaselineSteps(): Long {
        val now = Instant.now()
        val startOfDay = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
        return getStepCount(startOfDay, now)
    }
}
