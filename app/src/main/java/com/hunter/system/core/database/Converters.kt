package com.hunter.system.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room TypeConverters for Java Time types used in QuestEntity.
 */
class Converters {

    // ─── LocalDate ───
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }

    // ─── LocalTime ───
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? = timeString?.let { LocalTime.parse(it) }

    // ─── Instant ───
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? = epochMilli?.let { Instant.ofEpochMilli(it) }

    // ─── QuestStatus ───
    @TypeConverter
    fun fromQuestStatus(status: QuestStatus?): String? = status?.name

    @TypeConverter
    fun toQuestStatus(statusString: String?): QuestStatus? =
        statusString?.let { QuestStatus.valueOf(it) }
}
