package com.toprace.tracker.logic

import com.toprace.tracker.Car
import com.toprace.tracker.Road
import com.toprace.tracker.SpeedDatabase
import com.toprace.tracker.Tier
import com.toprace.tracker.db.StrategySettings

object MathEngine {
    /** The 2 hidden roads are assumed to be the other roads of the revealed road's tier. */
    fun hiddenRoads(revealed: Road): List<Road> =
        Road.entries.filter { it.fast == revealed.fast && it != revealed }

    /** Revealed speed x3 + average hidden-road speed x6. */
    fun rawMath(road: Road, car: Car): Double {
        val hiddenAvg = hiddenRoads(road).map { SpeedDatabase.speed(it, car) }.average()
        return SpeedDatabase.speed(road, car) * 3 + hiddenAvg * 6
    }

    /**
     * Multipliers apply to Fast-tier cars only (a uniform multiplier would not change any rank):
     * fast/struggle road multiplier by revealed road type, plus the bias of the revealed slot.
     */
    fun multiplier(road: Road, slot: Int, car: Car, s: StrategySettings): Double {
        if (car.tier != Tier.FAST) return 1.0
        var m = if (road.fast) s.fastRoadBoost else s.struggleRoadPenalty
        m *= when (slot) {
            1 -> s.slot1Bias
            2 -> s.slot2Bias
            3 -> s.slot3Bias
            else -> 1f
        }
        return m.toDouble()
    }

    /** Scores normalized to 0..1 (divided by the best car). */
    fun scores(road: Road, slot: Int, cars: List<Car>, s: StrategySettings): Map<Car, Double> {
        val raw = cars.associateWith { rawMath(road, it) * multiplier(road, slot, it, s) }
        return normalize(raw)
    }

    fun normalize(m: Map<Car, Double>): Map<Car, Double> {
        val max = m.values.maxOrNull() ?: return m
        if (max <= 0.0) return m
        return m.mapValues { it.value / max }
    }
}
