package com.toprace.tracker

import com.toprace.tracker.db.StrategySettings
import com.toprace.tracker.logic.AnalysisEngine
import com.toprace.tracker.logic.LiveInput
import com.toprace.tracker.logic.OddKeypad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTest {
    private val five = listOf(Car.THUNDER, Car.FLASH, Car.EAGLE, Car.JEEP, Car.APE)

    private fun analyze(live: LiveInput?) =
        AnalysisEngine.analyze(Road.HIGHWAY, 1, five, StrategySettings(), emptyList(), emptyList(), live)

    @Test fun noLiveVersionWithoutRoads() {
        val a = analyze(null)
        assertEquals(5, a.versions.size)
        assertNull(a.liveRank)
    }

    @Test fun liveRanksByTotalTime() {
        // all-Mountain track: Ape is the fastest of these five, Thunder the slowest
        val a = analyze(LiveInput(listOf(Road.MOUNTAIN, Road.MOUNTAIN, Road.MOUNTAIN), listOf(33, 33, 34)))
        assertEquals(6, a.versions.size)
        assertEquals(listOf(Car.APE, Car.JEEP, Car.EAGLE, Car.FLASH, Car.THUNDER), a.liveRank)
        assertEquals("fastest", a.version(AnalysisEngine.LIVE).note.getValue(Car.APE))
    }

    @Test fun lengthsChangeTheWinner() {
        // mostly Highway: Thunder; mostly Potholes: Jeep
        val fast = analyze(LiveInput(listOf(Road.HIGHWAY, Road.EXPRESSWAY, Road.POTHOLES), listOf(45, 45, 10)))
        val slow = analyze(LiveInput(listOf(Road.HIGHWAY, Road.EXPRESSWAY, Road.POTHOLES), listOf(10, 10, 80)))
        assertEquals(Car.THUNDER, fast.liveRank!![0])
        assertEquals(Car.JEEP, slow.liveRank!![0])
    }

    @Test fun liveSharesSumToOne() {
        val a = analyze(LiveInput(listOf(Road.DESERT, Road.BUMPY, Road.HIGHWAY), listOf(30, 30, 40)))
        assertEquals(1.0, a.version(AnalysisEngine.LIVE).share.values.sum(), 1e-9)
        assertTrue(a.winnerRanks(Car.APE).containsKey(AnalysisEngine.LIVE))
    }

    @Test fun oddKeypadCounts() {
        assertEquals(2.8, OddKeypad.value(listOf("1", "1", ".", "5", "1", "1", "1"))!!, 1e-9)
        assertEquals(11.7, OddKeypad.value(listOf("5", "5", "1", ".", "5", "1", "1"))!!, 1e-9)
        assertEquals(15.0, OddKeypad.value(listOf("5", "5", "5"))!!, 1e-9)
        assertEquals("2.8", OddKeypad.text(listOf("1", "1", ".", "5", "1", "1", "1")))
        assertNull(OddKeypad.value(emptyList()))
        assertNull(OddKeypad.value(listOf(".")))
        assertEquals(listOf("1"), OddKeypad.back(listOf("1", "5")))
    }
}
