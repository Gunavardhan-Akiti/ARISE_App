package com.hunter.system.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Quest entity per the tech spec database schema (Section 8.1).
 *
 * Tracks each daily quest: when it was triggered, baseline steps,
 * final steps, completion status, and duration.
 */
@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Quest date */
    val date: LocalDate,

    /** Alarm trigger time */
    val alarmTime: LocalTime,

    /** Health Connect aggregate step count at quest start */
    val baselineSteps: Int,

    /** Health Connect aggregate step count at completion (null if not yet complete) */
    val finalSteps: Int? = null,

    /** Target step count (default 10,000) */
    val goalSteps: Int = 10_000,

    /** Quest status: PENDING, ACTIVE, COMPLETE, FAILED */
    val status: QuestStatus = QuestStatus.PENDING,

    /** Timestamp of quest completion (null if not yet complete) */
    val completedAt: Instant? = null,

    /** Minutes from start to completion (null if not yet complete) */
    val durationMin: Int? = null,

    /** JSON string of Health Connect DataOrigin breakdown (null if not tracked) */
    val stepSources: String? = null
)

enum class QuestStatus {
    PENDING,
    ACTIVE,
    COMPLETE,
    FAILED
}
