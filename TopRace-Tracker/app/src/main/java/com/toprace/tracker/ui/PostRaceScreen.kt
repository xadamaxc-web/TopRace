package com.toprace.tracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toprace.tracker.Car
import com.toprace.tracker.Road
import com.toprace.tracker.SpriteSlicer
import com.toprace.tracker.db.RoundEntity
import com.toprace.tracker.logic.AnalysisEngine
import com.toprace.tracker.logic.OddKeypad
import com.toprace.tracker.logic.Outcome
import kotlin.math.roundToInt

private val Gold = Color(0xFFFFC53D)
private val CellBg = Color(0x33FFFFFF)

@Composable
fun PostRaceScreen(
    state: SetupState,
    sprites: SpriteSlicer,
    onSave: (RoundEntity, Road, List<Car>, Car?) -> Unit,
) {
    val road = state.road ?: return
    val a = state.currentAnalysis() ?: return
    val winner = state.winner
    val haptic = LocalHapticFeedback.current

    val ranks: Map<String, Int> = if (winner != null) a.winnerRanks(winner) else emptyMap()
    val rankMine: Int? = winner?.let { w -> state.mine.indexOf(w).takeIf { it >= 0 }?.plus(1) }
    val odd = OddKeypad.value(state.oddTaps.toList())
    val canSave = winner != null && odd != null && odd >= 1.0

    Column(Modifier.fillMaxSize().padding(top = 2.dp)) {
        Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable { state.postRace = false }.padding(end = 10.dp, top = 2.dp, bottom = 2.dp)) {
                Text("◀ Back", color = Color.White, fontSize = 12.sp)
            }
            Text("RESULT", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(road.displayName + " · Slot " + state.slot, color = Color(0xCCFFFFFF), fontSize = 11.sp)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // left: winner's final odd (tap counter), SURE, stake
            Column(Modifier.weight(0.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Winner's odd", color = Color(0xCCFFFFFF), fontSize = 10.sp)
                    Text(OddKeypad.text(state.oddTaps.toList()), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (odd != null && odd > 500.0) Text("check", color = Color(0xFFF97316), fontSize = 10.sp)
                }
                Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (key in listOf("1", "5", ".", "⌫")) {
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(Color(0x55FFFFFF))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (key == "⌫") {
                                        if (state.oddTaps.isNotEmpty()) state.oddTaps.removeAt(state.oddTaps.size - 1)
                                    } else if (key != "." || !state.oddTaps.contains(".")) {
                                        state.oddTaps.add(key)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) { Text(key, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.sure) "★ SURE" else "SURE", color = if (state.sure) Color.Black else Color.White,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (state.sure) Gold else CellBg)
                            .clickable { state.sure = !state.sure }.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    Text("Stake", color = Color(0xCCFFFFFF), fontSize = 10.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for ((v, label) in listOf(0 to "none", 20 to "20", 33 to "33", 50 to "50", 75 to "75", 100 to "ALL")) {
                        val on = state.stake == v
                        Text(
                            label, color = if (on) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) Gold else CellBg)
                                .clickable { state.stake = v }.padding(vertical = 5.dp),
                        )
                    }
                }
            }
            // right: winner, W1/W2/W3/LOSE per version, SAVE
            Column(Modifier.weight(0.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "Winner: " + (winner?.displayName ?: "tap below"),
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                )
                Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.cars.toList().forEach { c ->
                        val sel = winner == c
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                                .background(if (sel) Color(0x55FFC53D) else CellBg)
                                .border(if (sel) 2.dp else 0.dp, if (sel) Gold else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { state.winner = c },
                        ) {
                            Image(
                                bitmap = sprites.vehicle(c), contentDescription = c.displayName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(start = 2.dp, end = 2.dp, bottom = 11.dp),
                            )
                            Text(
                                c.displayName, color = Color.White, fontSize = 9.sp,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
                // results worked out from the one winner tap
                val chips = listOf(
                    "MATH" to ranks[AnalysisEngine.MATH], "BASE" to ranks[AnalysisEngine.BASE],
                    "DATA" to ranks[AnalysisEngine.DATA], "BLND" to ranks[AnalysisEngine.BLEND],
                    "RCNT" to ranks[AnalysisEngine.RECENT], "LIVE" to ranks[AnalysisEngine.LIVE],
                    "MINE" to rankMine,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    chips.forEach { (name, rk) ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(CellBg).padding(vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(name, color = Color(0xCCFFFFFF), fontSize = 7.sp, textAlign = TextAlign.Center)
                            Text(
                                Outcome.code(rk) ?: "—",
                                color = if (rk == null) Color(0x99FFFFFF) else resultColor(rk),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f))
                Box(
                    Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(6.dp))
                        .background(if (canSave) Color(0xFF22C55E) else Color(0x44FFFFFF))
                        .clickable(enabled = canSave) {
                            val w = winner ?: return@clickable
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val c = state.cars
                            val rk = a.winnerRanks(w)
                            val hidden = (1..3).filter { it != state.slot }.mapNotNull { state.slotRoads[it - 1]?.displayName }
                            val roadsSet = state.slotRoads.all { it != null }
                            val entity = RoundEntity(
                                timestamp = System.currentTimeMillis(),
                                revealedRoad = road.displayName,
                                revealedSlot = state.slot,
                                hiddenRoad2 = hidden.getOrNull(0),
                                hiddenRoad3 = hidden.getOrNull(1),
                                car1 = c[0].displayName, car2 = c[1].displayName, car3 = c[2].displayName,
                                car4 = c[3].displayName, car5 = c[4].displayName,
                                winner = w.displayName,
                                winnerOdd = odd,
                                picksShown = a.picks.joinToString("|") { it.car.displayName },
                                confShown = a.picks.joinToString("|") { (it.confidence * 100).roundToInt().toString() },
                                rankMath = rk[AnalysisEngine.MATH],
                                rankBase = rk[AnalysisEngine.BASE],
                                rankData = rk[AnalysisEngine.DATA],
                                rankBlend = rk[AnalysisEngine.BLEND],
                                rankRecent = rk[AnalysisEngine.RECENT],
                                orderMath = a.orderText(AnalysisEngine.MATH),
                                orderData = a.orderText(AnalysisEngine.DATA),
                                orderBlend = a.orderText(AnalysisEngine.BLEND),
                                orderRecent = a.orderText(AnalysisEngine.RECENT),
                                roadS1 = state.slotRoads[0]?.displayName,
                                roadS2 = state.slotRoads[1]?.displayName,
                                roadS3 = state.slotRoads[2]?.displayName,
                                lenS1 = if (roadsSet) state.lens[0] else null,
                                lenS2 = if (roadsSet) state.lens[1] else null,
                                lenS3 = if (roadsSet) state.lens[2] else null,
                                mineOrder = state.mine.takeIf { it.isNotEmpty() }?.joinToString("|") { it.displayName },
                                mineLockedAt = state.mineLockedAt.takeIf { it > 0L },
                                rankMine = rankMine,
                                sure = if (state.sure) 1 else 0,
                                stakePct = state.stake.takeIf { it > 0 },
                                rankLive = rk[AnalysisEngine.LIVE],
                                orderLive = if (a.liveRank != null) a.orderText(AnalysisEngine.LIVE) else null,
                            )
                            onSave(entity, road, c.toList(), w)
                            state.reset()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (canSave) "SAVE" else if (winner == null) "tap the winner" else "enter the winner's odd",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
