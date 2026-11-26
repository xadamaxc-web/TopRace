package com.toprace.tracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundDao {
    @Insert
    suspend fun insert(round: RoundEntity): Long

    @Query("SELECT * FROM rounds ORDER BY timestamp DESC LIMIT :n")
    fun last(n: Int): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds ORDER BY id ASC")
    suspend fun all(): List<RoundEntity>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM strategy_settings WHERE id = 1")
    suspend fun get(): StrategySettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: StrategySettings)
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM learned_stats WHERE carName = :car AND roadName = :road")
    suspend fun find(car: String, road: String): LearnedStats?

    @Query("SELECT * FROM learned_stats")
    fun all(): Flow<List<LearnedStats>>

    @Query("SELECT * FROM learned_stats ORDER BY carName, roadName")
    suspend fun allNow(): List<LearnedStats>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: LearnedStats)
}
