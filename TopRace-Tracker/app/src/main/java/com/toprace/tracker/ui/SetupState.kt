package com.toprace.tracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toprace.tracker.Car
import com.toprace.tracker.Defaults
import com.toprace.tracker.Road
import com.toprace.tracker.db.LearnedStats
import com.toprace.tracker.db.RoundEntity
import com.toprace.tracker.db.StrategySettings
import com.toprace.tracker.logic.Analysis
import com.toprace.tracker.logic.AnalysisEngine
import com.toprace.tracker.logic.LiveInput
import org.json.JSONArray
import org.json.JSONObject

class SetupState {
    /** Slot of the revealed road: 0 = none, 1..3 = top to bottom on the game's mini-map. */
    var slot by mutableStateOf(0)
    var road by mutableStateOf<Road?>(null)
    /** The 5 offered cars in the order tapped (Copy). */
    val cars = mutableStateListOf<Car>()
    /** My own 1st..5th order (Mine). */
    val mine = mutableStateListOf<Car>()
    var copyMode by mutableStateOf(true)
    var mineLockedAt by mutableStateOf(0L)
    var dotPage by mutableStateOf(0)

    var analyzing by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var minimized by mutableStateOf(false)

    /** Road in each slot (index 0..2) and block lengths in percent. */
    val slotRoads = mutableStateListOf<Road?>(null, null, null)
    val lens = mutableStateListOf(33, 33, 34)
    var editBlock by mutableStateOf(0)

    // result screen
    var postRace by mutableStateOf(false)
    var winner by mutableStateOf<Car?>(null)
    val oddTaps = mutableStateListOf<String>()
    var sure by mutableStateOf(false)
    /** 0 = no bet, else 20 / 33 / 50 / 75 / 100 (ALL IN). */
    var stake by mutableStateOf(0)

    // loaded from Room by OverlayService
    var settings by mutableStateOf(StrategySettings())
    var stats by mutableStateOf<List<LearnedStats>>(emptyList())
    var rounds by mutableStateOf<List<RoundEntity>>(emptyList()) // newest first

    val ready: Boolean get() = slot in 1..3 && road != null && cars.size == Defaults.PICKS_REQUIRED

    fun selectSlot(s: Int) {
        val r = road
        if (slot in 1..3 && r != null && slotRoads[slot - 1] == r) slotRoads[slot - 1] = null
        slot = if (slot == s) 0 else s
        if (slot in 1..3 && r != null) slotRoads[slot - 1] = r
        editBlock = if (slot in 1..3) slot - 1 else 0
    }

    fun selectRoad(r: Road) {
        if (slot !in 1..3) return
        road = r
        slotRoads[slot - 1] = r
    }

    fun toggleCar(car: Car) {
        if (car in cars) { cars.remove(car); mine.remove(car) }
        else if (cars.size < Defaults.PICKS_REQUIRED) cars.add(car)
    }

    fun toggleMine(car: Car) {
        if (car !in cars) return
        if (car in mine) mine.remove(car) else mine.add(car)
    }

    /** Move the separator [i] (0 = between block 1 and 2, 1 = between 2 and 3) to [pct] percent. */
    fun setBoundary(i: Int, pct: Int) {
        val min = 8
        if (i == 0) {
            val b2 = lens[0] + lens[1]
            val b1 = pct.coerceIn(min, b2 - min)
            lens[0] = b1; lens[1] = b2 - b1
        } else {
            val b1 = lens[0]
            val b2 = pct.coerceIn(b1 + min, 100 - min)
            lens[1] = b2 - b1; lens[2] = 100 - b2
        }
    }

    fun setBlockRoad(i: Int, r: Road) {
        slotRoads[i] = r
        if (i == slot - 1) road = r
    }

    fun liveInput(): LiveInput? {
        val rs = slotRoads.toList()
        if (rs.any { it == null }) return null
        return LiveInput(rs.map { it!! }, lens.toList())
    }

    fun currentAnalysis(): Analysis? {
        val r = road ?: return null
        if (cars.size != Defaults.PICKS_REQUIRED) return null
        return AnalysisEngine.analyze(r, slot, cars.toList(), settings, stats, rounds, liveInput())
    }

    /** The winner tap on the analysis screen: every version's W1/W2/W3/LOSE follows from it. */
    fun openPostRace(car: Car) {
        winner = car
        postRace = true
    }

    fun toJson(): String = JSONObject().apply {
        put("slot", slot)
        put("road", road?.name ?: "")
        put("cars", JSONArray(cars.map { it.name }))
        put("mine", JSONArray(mine.map { it.name }))
        put("copyMode", copyMode)
        put("mineLockedAt", mineLockedAt)
        put("screen", if (postRace) 2 else if (analyzing) 1 else 0)
        put("slotRoads", JSONArray(slotRoads.map { it?.name ?: "" }))
        put("lens", JSONArray(lens.toList()))
        put("winner", winner?.name ?: "")
        put("odd", JSONArray(oddTaps.toList()))
        put("sure", sure)
        put("stake", stake)
    }.toString()

    fun restore(json: String) {
        runCatching {
            val o = JSONObject(json)
            slot = o.optInt("slot")
            road = o.optString("road").takeIf { it.isNotEmpty() }?.let { Road.valueOf(it) }
            cars.clear()
            o.optJSONArray("cars")?.let { a -> for (i in 0 until a.length()) cars.add(Car.valueOf(a.getString(i))) }
            mine.clear()
            o.optJSONArray("mine")?.let { a -> for (i in 0 until a.length()) mine.add(Car.valueOf(a.getString(i))) }
            copyMode = o.optBoolean("copyMode", true)
            mineLockedAt = o.optLong("mineLockedAt")
            o.optJSONArray("slotRoads")?.let { a ->
                for (i in 0 until minOf(3, a.length())) {
                    slotRoads[i] = a.getString(i).takeIf { it.isNotEmpty() }?.let { Road.valueOf(it) }
                }
            }
            o.optJSONArray("lens")?.let { a ->
                if (a.length() == 3) {
                    for (i in 0..2) lens[i] = a.getInt(i)
                }
            }
            winner = o.optString("winner").takeIf { it.isNotEmpty() }?.let { Car.valueOf(it) }
            oddTaps.clear()
            o.optJSONArray("odd")?.let { a -> for (i in 0 until a.length()) oddTaps.add(a.getString(i)) }
            sure = o.optBoolean("sure")
            stake = o.optInt("stake")
            val screen = o.optInt("screen")
            if (ready) {
                analyzing = screen >= 1
                postRace = screen == 2 && winner != null
            }
        }
    }

    fun reset() {
        road = null; slot = 0; cars.clear(); mine.clear(); copyMode = true; mineLockedAt = 0L
        analyzing = false; showSettings = false; postRace = false; winner = null
        for (i in 0..2) slotRoads[i] = null
        lens[0] = 33; lens[1] = 33; lens[2] = 34; editBlock = 0
        oddTaps.clear(); sure = false; stake = 0
    }
}
