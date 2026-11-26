package com.toprace.tracker

import com.toprace.tracker.db.LearnedStats
import com.toprace.tracker.db.RoundEntity
import com.toprace.tracker.db.StrategySettings
import com.toprace.tracker.logic.AnalysisEngine
import com.toprace.tracker.logic.MathEngine
import com.toprace.tracker.logic.Outcome
import com.toprace.tracker.logic.PatternEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogicTest {

    private fun round(winner: String?) = RoundEntity(
        timestamp = 0, revealedRoad = "Highway", revealedSlot = 1,
        car1 = "Jeep", car2 = "Ape", car3 = "Eagle", car4 = "Flash", car5 = "Thunder",
        winner = winner,
    )

    private val five = listOf(Car.THUNDER, Car.FLASH, Car.EAGLE, Car.JEEP, Car.APE)

    private fun analyze(
        cars: List<Car> = five,
        settings: StrategySettings = StrategySettings(),
        stats: List<LearnedStats> = emptyList(),
        rounds: List<RoundEntity> = emptyList(),
    ) = AnalysisEngine.analyze(Road.HIGHWAY, 0, cars, settings, stats, rounds)

    @Test fun hiddenRoadsSameTier() {
        assertEquals(setOf(Road.DESERT, Road.EXPRESSWAY), MathEngine.hiddenRoads(Road.HIGHWAY).toSet())
        assertEquals(setOf(Road.BUMPY, Road.POTHOLES), MathEngine.hiddenRoads(Road.MOUNTAIN).toSet())
    }

    @Test fun rawMathFormula() {
        assertEquals(2880.0, MathEngine.rawMath(Road.HIGHWAY, Car.THUNDER), 1e-9)
        assertEquals(1008.0, MathEngine.rawMath(Road.MOUNTAIN, Car.APE), 1e-9)
    }

    @Test fun mathRankOnHighway() {
        val a = analyze(cars = five.shuffled())
        assertEquals(listOf(Car.THUNDER, Car.FLASH, Car.EAGLE, Car.APE, Car.JEEP), a.mathRank)
    }

    @Test fun baseRankByBaseOdd() {
        val cars = listOf(Car.WHALE, Car.APE, Car.SHARK, Car.DRAGON, Car.EAGLE)
        val a = analyze(cars = cars)
        assertEquals(listOf(Car.SHARK, Car.EAGLE, Car.APE, Car.DRAGON, Car.WHALE), a.baseRank)
    }

    @Test fun dataBlendHundredFollowsLearnedWins() {
        val stats = listOf(LearnedStats(carName = "Jeep", roadName = "Highway", wins = 10, totalRounds = 10))
        val a = analyze(settings = StrategySettings(dataBlend = 100), stats = stats)
        assertEquals(Car.JEEP, a.picks[0].car)
        assertEquals(10, a.picks[0].sample)
    }

    @Test fun noDataFallsBackToPureMath() {
        val a = analyze(settings = StrategySettings(dataBlend = 100))
        assertEquals(listOf(Car.THUNDER, Car.FLASH, Car.EAGLE), a.picks.map { it.car })
    }

    @Test fun everyVersionRanksAllFiveAndSharesSumToOne() {
        val a = analyze()
        assertEquals(5, a.versions.size)
        for (v in a.versions) {
            assertEquals(five.toSet(), v.ranked.toSet())
            assertEquals(5, v.ranked.size)
            assertEquals(1.0, v.share.values.sum(), 1e-9)
        }
    }

    @Test fun picksAreTopThreeOfBlendWithConfidence() {
        val a = analyze()
        assertEquals(a.blendRank.take(3), a.picks.map { it.car })
        assertEquals(a.picks.sortedByDescending { it.confidence }.map { it.car }, a.picks.map { it.car })
    }

    @Test fun winnerRankCoversFourthAndFifth() {
        val a = analyze()
        val ranks = a.winnerRanks(a.mathRank[4])
        assertEquals(5, ranks[AnalysisEngine.MATH])
        assertEquals(setOf("MATH", "BASE", "DATA", "BLEND", "RECENT"), ranks.keys)
    }

    @Test fun orderTextHoldsAllFiveCarsBestFirst() {
        val a = analyze()
        for (name in listOf(AnalysisEngine.MATH, AnalysisEngine.DATA, AnalysisEngine.BLEND, AnalysisEngine.RECENT)) {
            val cars = a.orderText(name).split("|")
            assertEquals(5, cars.size)
            assertEquals(five.map { it.displayName }.toSet(), cars.toSet())
            assertEquals(a.version(name).ranked.map { it.displayName }, cars)
        }
        assertEquals("Thunder|Flash|Eagle|Ape|Jeep", a.orderText(AnalysisEngine.MATH))
    }

    @Test fun savedOrderMatchesWinnerRank() {
        val a = analyze()
        for (winner in five) {
            val ranks = a.winnerRanks(winner)
            for (name in listOf(AnalysisEngine.MATH, AnalysisEngine.DATA, AnalysisEngine.BLEND, AnalysisEngine.RECENT)) {
                assertEquals(ranks.getValue(name), a.orderText(name).split("|").indexOf(winner.displayName) + 1)
            }
        }
    }

    @Test fun recentCopiesBlendBelowThirtyRounds() {
        val a = analyze(rounds = List(29) { round("Jeep") })
        assertEquals(a.blendRank, a.recentRank)
    }

    @Test fun recentFollowsFormAfterThirtyRounds() {
        val a = analyze(settings = StrategySettings(dataBlend = 100), rounds = List(40) { round("Jeep") })
        assertEquals(Car.JEEP, a.recentRank[0])
    }

    @Test fun outcomeCodes() {
        assertEquals("W1", Outcome.code(1))
        assertEquals("W3", Outcome.code(3))
        assertEquals("LOSE", Outcome.code(4))
        assertEquals("LOSE", Outcome.code(5))
        assertNull(Outcome.code(null))
    }

    @Test fun streakAlert() {
        val r = listOf(round("Jeep"), round("Jeep"), round("Jeep"), round("Ape"))
        assertEquals("🔥 STREAK: Jeep won last 3", PatternEngine.alert(r))
    }

    @Test fun hotAlert() {
        val r = listOf("Ape", "Jeep", "Ape", "Eagle", "Ape").map { round(it) }
        assertEquals("📈 HOT: Ape won 3 of last 5", PatternEngine.alert(r))
    }

    @Test fun noPattern() {
        assertNull(PatternEngine.alert(emptyList()))
        assertNull(PatternEngine.alert(listOf(round("Ape"), round("Jeep"))))
    }
}
