package com.toprace.tracker.logic

import com.toprace.tracker.Car
import com.toprace.tracker.Road
import com.toprace.tracker.db.LearnedStats
import com.toprace.tracker.db.RoundEntity
import kotlin.math.pow

object DataEngine {
    const val PRIOR_K = 10.0
    /** RECENT copies BLEND until this many rounds with a winner exist. */
    const val RECENT_MIN_ROUNDS = 30
    /** A round this many rounds old counts half as much as the newest one. */
    const val RECENT_HALF_LIFE = 50.0

    /** Bayesian win rate per car on this road: prior = 1 / baseOdd, strength k. */
    fun probabilities(road: Road, cars: List<Car>, stats: List<LearnedStats>): Map<Car, Double> {
        val byKey = stats.associateBy { it.carName to it.roadName }
        return cars.associateWith { car ->
            val s = byKey[car.displayName to road.displayName]
            val wins = s?.wins ?: 0
            val total = s?.totalRounds ?: 0
            (wins + PRIOR_K * (1.0 / car.baseOdd)) / (total + PRIOR_K)
        }
    }

    /**
     * Recent form on all roads: each past round is weighted 0.5^(age / halfLife), then the same
     * prior as [probabilities]. [rounds] is newest first.
     */
    fun recentProbabilities(
        cars: List<Car>,
        rounds: List<RoundEntity>,
        halfLife: Double = RECENT_HALF_LIFE,
    ): Map<Car, Double> {
        val wins = HashMap<String, Double>()
        val offered = HashMap<String, Double>()
        rounds.forEachIndexed { age, r ->
            val winner = r.winner ?: return@forEachIndexed
            val weight = 0.5.pow(age / halfLife)
            for (name in listOf(r.car1, r.car2, r.car3, r.car4, r.car5)) {
                offered[name] = (offered[name] ?: 0.0) + weight
                if (name == winner) wins[name] = (wins[name] ?: 0.0) + weight
            }
        }
        return cars.associateWith { car ->
            ((wins[car.displayName] ?: 0.0) + PRIOR_K / car.baseOdd) /
                ((offered[car.displayName] ?: 0.0) + PRIOR_K)
        }
    }
}
