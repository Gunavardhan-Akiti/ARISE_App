package com.hunter.system.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for Solo Leveling SYSTEM.
 * Stores quest history, streak data, and completion stats.
 */
@Database(
    entities = [QuestEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SystemDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao

    companion object {
        const val DATABASE_NAME = "solo_leveling_system.db"
    }
}
