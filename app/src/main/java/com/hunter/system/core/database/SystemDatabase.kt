package com.hunter.system.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [QuestEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(Converters::class)
abstract class SystemDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "solo_leveling_system.db"
    }
}
