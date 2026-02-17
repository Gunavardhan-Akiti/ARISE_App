package com.hunter.system.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for Quest operations.
 */
@Dao
interface QuestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity): Long

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Query("SELECT * FROM quests WHERE date = :date LIMIT 1")
    suspend fun getQuestByDate(date: LocalDate): QuestEntity?

    @Query("SELECT * FROM quests WHERE date = :date LIMIT 1")
    fun observeQuestByDate(date: LocalDate): Flow<QuestEntity?>

    @Query("SELECT * FROM quests WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveQuest(): QuestEntity?

    @Query("SELECT * FROM quests WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActiveQuest(): Flow<QuestEntity?>

    @Query("SELECT * FROM quests ORDER BY date DESC")
    fun observeAllQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests ORDER BY date DESC LIMIT :limit")
    fun observeRecentQuests(limit: Int): Flow<List<QuestEntity>>

    @Query("SELECT COUNT(*) FROM quests WHERE status = 'COMPLETE'")
    fun observeCompletedCount(): Flow<Int>

    /**
     * Calculate current streak: count consecutive completed quests
     * going backward from the most recent date.
     */
    @Query("""
        SELECT COUNT(*) FROM quests 
        WHERE status = 'COMPLETE' 
        AND date >= :sinceDate 
        ORDER BY date DESC
    """)
    suspend fun getCompletedQuestsSince(sinceDate: LocalDate): Int
}
