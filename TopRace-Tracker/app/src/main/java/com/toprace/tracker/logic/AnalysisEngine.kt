package com.toprace.tracker.logic

import com.toprace.tracker.Car
import com.toprace.tracker.Road
import com.toprace.tracker.SpeedDatabase
import kotlin.math.roundToInt
import com.toprace.tracker.db.LearnedStats
import com.toprace.tracker.db.RoundEntity
import com.toprace.tracker.db.StrategySettings

/** [confidence] is the car's share (0..1) of the BLEND score; [sample] = rounds logged for it on this road. */
data class Pick(val car: Car, val confidence: Double, val sample: Int)

/** One version's ordering of the 5 offered cars; [share] sums to 1 over the 5 cars. */
data class VersionRank(
    val name: String,
    val ranked: List<Car>,
    val share: Map<Car, Double>,
    /** Short text shown instead of the % when a version is not a probability (LIVE: time gap). */
    val note: Map<Car, String> = emptyMap(),
)

/** The 3 real roads (slot 1..3, top to bottom) and their lengths in percent. */
data class LiveInput(val roads: List<Road>, val lens: List<Int>)

data class Analysis(
    val versions: List<VersionRank>,
    val picks: List<Pick>,
    val pattern: String?,
) {
    fun version(name: String): VersionRank = versions.first { it.name == name }
    val mathRank: List<Car> get() = version(AnalysisEngine.MATH).ranked
    val baseRank: List<Car> get() = version(AnalysisEngine.BASE).ranked
    val dataRank: List<Car> get() = version(AnalysisEngine.DATA).ranked
    val blendRank: List<Car> get() = version(AnalysisEngine.BLEND).ranked
    val recentRank: List<Car> get() = version(AnalysisEngine.RECENT).ranked
    val liveRank: List<Car>? get() = versions.firstOrNull { it.name == AnalysisEngine.LIVE }?.ranked

    /** A version's full ordering as saved in the DB / CSV: "Eagle|Jeep|Shark|Ape|Whale", best first. */
    fun orderText(name: String): String = version(name).ranked.joinToString("|") { it.displayName }

    /** Rank (1..5) of [winner] in every version. */
    fun winnerRanks(winner: Car): Map<String, Int> =
        versions.associate { it.name to it.ranked.indexOf(winner) + 1 }
}

object AnalysisEngine {
    const val MATH = "MATH"
    /** Baseline: shortest base odd first. */
    const val BASE = "BASE"
    const val DATA = "DATA"
    const val BLEND = "BLEND"
    const val RECENT = "RECENT"
    /** Total time over the 3 real roads and lengths (needs LiveInput). */
    const val LIVE = "LIVE"

    /** [rounds] newest first. */
    fun analyze(
        road: Road,
        slot: Int,
        cars: List<Car>,
        settings: StrategySettings,
        stats: List<LearnedStats>,
        rounds: List<RoundEntity>,
        live: LiveInput? = null,
    ): Analysis {
        val math = MathEngine.scores(road, slot, cars, settings)
        val dataRaw = DataEngine.probabilities(road, cars, stats)
        val data = MathEngine.normalize(dataRaw)

        // no learned rounds for these cars on this road -> pure math
        val hasData = cars.any { c ->
            stats.any { it.carName == c.displayName && it.roadName == road.displayName && it.totalRounds > 0 }
        }
        val wData = settings.dataBlend.coerceIn(0, 100) / 100.0
        val w = if (hasData) wData else 0.0
        val blend = cars.associateWith { w * data.getValue(it) + (1 - w) * math.getValue(it) }

        val base = cars.associateWith { 1.0 / it.baseOdd }

        val raced = rounds.count { it.winner != null }
        val recent = if (raced < DataEngine.RECENT_MIN_ROUNDS) {
            blend
        } else {
            val rec = MathEngine.normalize(DataEngine.recentProbabilities(cars, rounds))
            cars.associateWith { wData * rec.getValue(it) + (1 - wData) * math.getValue(it) }
        }

        fun v(name: String, score: Map<Car, Double>) = VersionRank(name, rank(cars, score), shares(cars, score))
        val versions = listOf(v(MATH, math), v(BASE, base), v(DATA, dataRaw), v(BLEND, blend), v(RECENT, recent))

        val liveV = live?.let { liveVersion(cars, it) }
        val all = versions + listOfNotNull(liveV)

        val blendV = versions[3]
        val picks = blendV.ranked.take(3).map { c ->
            val n = stats.firstOrNull { it.carName == c.displayName && it.roadName == road.displayName }?.totalRounds ?: 0
            Pick(c, blendV.share.getValue(c), n)
        }
        return Analysis(all, picks, PatternEngine.alert(rounds))
    }

    /** time(car) = sum over the 3 blocks of (length share) / speed on that road; smaller is faster. */
    private fun liveVersion(cars: List<Car>, li: LiveInput): VersionRank {
        val total = li.lens.sum().coerceAtLeast(1).toDouble()
        val time = cars.associateWith { c ->
            var t = 0.0
            for (i in li.roads.indices) t += (li.lens[i] / total) / SpeedDatabase.speed(li.roads[i], c)
            t
        }
        val best = time.values.min()
        val ranked = cars.sortedWith(compareBy<Car> { time.getValue(it) }.thenBy { it.baseOdd }.thenBy { it.ordinal })
        val note = cars.associateWith { c ->
            val gap = (time.getValue(c) / best - 1.0) * 100.0
            if (gap < 0.5) "fastest" else "+" + gap.roundToInt() + "%"
        }
        return VersionRank(LIVE, ranked, shares(cars, cars.associateWith { 1.0 / time.getValue(it) }), note)
    }

    private fun shares(cars: List<Car>, score: Map<Car, Double>): Map<Car, Double> {
        val sum = score.values.sum()
        return cars.associateWith { if (sum > 0.0) score.getValue(it) / sum else 1.0 / cars.size }
    }

    private fun rank(cars: List<Car>, score: Map<Car, Double>): List<Car> =
        cars.sortedWith(
            compareByDescending<Car> { score.getValue(it) }.thenBy { it.baseOdd }.thenBy { it.ordinal },
        )
}
