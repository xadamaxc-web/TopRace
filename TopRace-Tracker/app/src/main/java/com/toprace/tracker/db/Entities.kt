package com.toprace.tracker.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "rounds")
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "revealed_road") val revealedRoad: String,
    @ColumnInfo(name = "revealed_slot") val revealedSlot: Int,
    @ColumnInfo(name = "hidden_road_2") val hiddenRoad2: String? = null,
    @ColumnInfo(name = "hidden_road_3") val hiddenRoad3: String? = null,
    val car1: String,
    val car2: String,
    val car3: String,
    val car4: String,
    val car5: String,
    val winner: String? = null,
    @ColumnInfo(name = "winner_odd") val winnerOdd: Double? = null,
    /** "G", "Y" or "R" */
    @ColumnInfo(name = "hud_color") val hudColor: String? = null,
    /** Top-3 picks the BLEND version showed, e.g. "Eagle|Jeep|Shark", and their confidence %, e.g. "31|24|19". */
    @ColumnInfo(name = "picks_shown") val picksShown: String? = null,
    @ColumnInfo(name = "conf_shown") val confShown: String? = null,
    /** Rank (1..5) the winner had in each version's ordering of the 5 cars. */
    @ColumnInfo(name = "rank_math") val rankMath: Int? = null,
    @ColumnInfo(name = "rank_base") val rankBase: Int? = null,
    @ColumnInfo(name = "rank_data") val rankData: Int? = null,
    @ColumnInfo(name = "rank_blend") val rankBlend: Int? = null,
    @ColumnInfo(name = "rank_recent") val rankRecent: Int? = null,
    /** S8: each version's full ordering of the 5 cars, best first, e.g. "Eagle|Jeep|Shark|Ape|Whale". */
    @ColumnInfo(name = "order_math") val orderMath: String? = null,
    @ColumnInfo(name = "order_data") val orderData: String? = null,
    @ColumnInfo(name = "order_blend") val orderBlend: String? = null,
    @ColumnInfo(name = "order_recent") val orderRecent: String? = null,
    /** S9: the road in each slot (top to bottom) and each block's length in percent (sum 100). */
    @ColumnInfo(name = "road_s1") val roadS1: String? = null,
    @ColumnInfo(name = "road_s2") val roadS2: String? = null,
    @ColumnInfo(name = "road_s3") val roadS3: String? = null,
    @ColumnInfo(name = "len_s1") val lenS1: Int? = null,
    @ColumnInfo(name = "len_s2") val lenS2: Int? = null,
    @ColumnInfo(name = "len_s3") val lenS3: Int? = null,
    /** S9: my own 1st..5th order, locked when ANALYSE was tapped (before the winner is known). */
    @ColumnInfo(name = "mine_order") val mineOrder: String? = null,
    @ColumnInfo(name = "mine_locked_at") val mineLockedAt: Long? = null,
    @ColumnInfo(name = "rank_mine") val rankMine: Int? = null,
    @ColumnInfo(name = "sure") val sure: Int? = null,
    @ColumnInfo(name = "stake_pct") val stakePct: Int? = null,
    /** S9: LIVE version = ranking by total time over the 3 real roads and lengths. */
    @ColumnInfo(name = "rank_live") val rankLive: Int? = null,
    @ColumnInfo(name = "order_live") val orderLive: String? = null,
)

@Entity(tableName = "strategy_settings")
data class StrategySettings(
    @PrimaryKey val id: Int = 1,
    val fastRoadBoost: Float = 1.0f,
    val struggleRoadPenalty: Float = 1.0f,
    val slot1Bias: Float = 1.0f,
    val slot2Bias: Float = 1.0f,
    val slot3Bias: Float = 1.0f,
    /** 0 = pure math, 100 = pure data */
    val dataBlend: Int = 50,
)

@Entity(
    tableName = "learned_stats",
    indices = [Index(value = ["carName", "roadName"], unique = true)],
)
data class LearnedStats(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carName: String,
    val roadName: String,
    val wins: Int = 0,
    val totalRounds: Int = 0,
)
