package com.toprace.tracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toprace.tracker.Car
import com.toprace.tracker.Road
import com.toprace.tracker.SpriteSlicer
import com.toprace.tracker.logic.Analysis
import com.toprace.tracker.logic.AnalysisEngine
import com.toprace.tracker.logic.Outcome
import com.toprace.tracker.logic.Pick
import com.toprace.tracker.logic.VersionRank
import kotlin.math.abs
import kotlin.math.roundToInt

private val Gold = Color(0xFFFFC53D)
private val CellBg = Color(0x33FFFFFF)
private const val PAGES = 4

private fun pct(x: Double): String = "${(x * 100).roundToInt()}%"

@Composable
fun AnalysisScreen(state: SetupState, sprites: SpriteSlicer) {
    val road = state.road ?: return
    val cars = state.cars.toList()
    val live = state.liveInput()
    val result = remember(road, state.slot, cars, state.settings, state.stats, state.rounds, live) {
        AnalysisEngine.analyze(road, state.slot, cars, state.settings, state.stats, state.rounds, live)
    }
    val pager = rememberPagerState(pageCount = { PAGES })
    val pageName = listOf("Ranks", "Picks", "Roads", "History")

    Column(Modifier.fillMaxSize().padding(top = 2.dp)) {
        Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable { state.analyzing = false }.padding(end = 10.dp, top = 2.dp, bottom = 2.dp)) {
                Text("◀ Setup", color = Color.White, fontSize = 12.sp)
            }
            Text(
                road.displayName + "  ·  Slot " + (if (state.slot == 0) "-" else state.slot.toString()),
                color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
            )
            Text(
                pageName[pager.currentPage] + "  " + (pager.currentPage + 1) + "/" + PAGES + "   Tap the winner ▾",
                color = Color(0xCCFFFFFF), fontSize = 11.sp,
            )
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            when (page) {
                0 -> RankPage(state, result, sprites)
                1 -> FinalPage(result, road.displayName, sprites)
                2 -> RoadsPage(state, result, sprites)
                else -> HistoryPage(state, sprites)
            }
        }
        WinnerBar(state, sprites)
    }
}

@Composable
private fun RankPage(state: SetupState, a: Analysis, sprites: SpriteSlicer) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (state.mine.isNotEmpty()) {
            val rest = state.cars.filter { it !in state.mine }
            val m = state.mine.toList() + rest
            RankRow(VersionRank("MINE", m, m.associateWith { 0.0 }, m.associateWith { "" }), sprites, Modifier.weight(1f))
        }
        a.versions.forEach { RankRow(it, sprites, Modifier.weight(1f)) }
    }
}

@Composable
private fun RankRow(v: VersionRank, sprites: SpriteSlicer, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(v.name, color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
        v.ranked.forEachIndexed { i, car ->
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(CellBg)) {
                Image(
                    bitmap = sprites.vehicle(car), contentDescription = car.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(start = 2.dp, end = 2.dp, bottom = 10.dp),
                )
                Text(
                    car.displayName, color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp),
                )
                Box(
                    Modifier.align(Alignment.TopStart).padding(2.dp).size(14.dp).clip(CircleShape)
                        .background(if (i == 0) Gold else Color(0xFFCBD5E1)),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                val label = if (v.note.isNotEmpty()) v.note[car] ?: "" else pct(v.share.getValue(car))
                Text(
                    label, color = Color(0xCCFFFFFF), fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun FinalPage(a: Analysis, roadName: String, sprites: SpriteSlicer) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        a.picks.forEachIndexed { i, p -> PickRow(i + 1, p, roadName, sprites, Modifier.weight(1f)) }
        val live = a.version(AnalysisEngine.LIVE.takeIf { a.liveRank != null } ?: AnalysisEngine.BLEND)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x33FFC53D)).padding(8.dp),
        ) {
            val line = if (a.liveRank != null) {
                "LIVE: " + live.ranked.take(3).joinToString(" › ") { it.displayName + " " + (live.note[it] ?: "") }
            } else {
                a.pattern ?: "No pattern yet"
            }
            Text(line, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PickRow(rank: Int, p: Pick, roadName: String, sprites: SpriteSlicer, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CellBg).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(if (rank == 1) Gold else Color(0xFFCBD5E1)), contentAlignment = Alignment.Center) {
            Text("$rank", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Image(
            bitmap = sprites.vehicle(p.car), contentDescription = p.car.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(start = 6.dp).width(84.dp).fillMaxHeight().padding(vertical = 2.dp),
        )
        Column(Modifier.weight(1f).padding(start = 6.dp)) {
            Text(p.car.displayName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                if (p.sample < 10) "weak: ${p.sample} rounds on $roadName" else "${p.sample} rounds on $roadName",
                color = Color(0xCCFFFFFF), fontSize = 10.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("CONFIDENCE", color = Color(0x99FFFFFF), fontSize = 8.sp)
            Text(pct(p.confidence), color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Page 2: the 3 road blocks (top = slot 1). Drag the two separators to match the game's mini-map,
 * tap a block and choose its road. The LIVE ranking appears once all 3 roads are set.
 */
@Composable
private fun RoadsPage(state: SetupState, a: Analysis, sprites: SpriteSlicer) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LengthBar(state, sprites, Modifier.width(92.dp).fillMaxHeight())
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Block ${state.editBlock + 1}: tap its road. Drag the gold lines to match the map.",
                color = Color(0xCCFFFFFF), fontSize = 10.sp,
            )
            for (rr in 0..1) {
                Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (cc in 0..2) {
                        val road = Road.entries[rr * 3 + cc]
                        val sel = state.slotRoads[state.editBlock] == road
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp))
                                .border(if (sel) 2.dp else 1.dp, if (sel) Color.White else Color(0x44FFFFFF), RoundedCornerShape(5.dp))
                                .clickable { state.setBlockRoad(state.editBlock, road) },
                        ) {
                            Image(
                                bitmap = sprites.road(road), contentDescription = road.displayName,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                            )
                            Text(
                                road.displayName, color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xAA000000)),
                            )
                        }
                    }
                }
            }
            val live = a.versions.firstOrNull { it.name == AnalysisEngine.LIVE }
            if (live != null) {
                RankRow(live, sprites, Modifier.weight(1f))
            } else {
                Text("Set all 3 roads to see the LIVE ranking.", color = Gold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LengthBar(state: SetupState, sprites: SpriteSlicer, modifier: Modifier) {
    var hPx by remember { mutableIntStateOf(1) }
    var dragging by remember { mutableIntStateOf(-1) }
    Box(
        modifier.onSizeChanged { hPx = it.height.coerceAtLeast(1) }.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { o ->
                    val p = o.y / hPx * 100f
                    val b1 = state.lens[0].toFloat()
                    val b2 = (state.lens[0] + state.lens[1]).toFloat()
                    dragging = if (abs(p - b1) <= abs(p - b2)) 0 else 1
                },
                onDragEnd = { dragging = -1 },
                onDragCancel = { dragging = -1 },
            ) { change, _ ->
                change.consume()
                if (dragging >= 0) state.setBoundary(dragging, (change.position.y / hPx * 100f).roundToInt())
            }
        },
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 0..2) {
                val r = state.slotRoads[i]
                val sel = state.editBlock == i
                Box(
                    Modifier.weight(state.lens[i].toFloat().coerceAtLeast(1f)).fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp)).background(Color(0xFF3A3F4A))
                        .border(if (sel) 2.dp else 1.dp, if (sel) Gold else Color(0xFFB8860B), RoundedCornerShape(4.dp))
                        .clickable { state.editBlock = i },
                    contentAlignment = Alignment.Center,
                ) {
                    if (r != null) {
                        Image(
                            bitmap = sprites.road(r), contentDescription = r.displayName,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        (if (r == null) "???" else r.displayName) + "\n" + state.lens[i] + "%",
                        color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.background(Color(0x99000000)),
                    )
                }
            }
        }
    }
}

/** Page 3: winners, newest first; filter to this road + slot. */
@Composable
private fun HistoryPage(state: SetupState, sprites: SpriteSlicer) {
    var onlyHere by remember { mutableStateOf(true) }
    val road = state.road
    val rows = state.rounds.filter { it.winner != null && (!onlyHere || (it.revealedRoad == road?.displayName && it.revealedSlot == state.slot)) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for ((flag, label) in listOf(true to "This road + slot", false to "All")) {
                Text(
                    label, color = if (onlyHere == flag) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (onlyHere == flag) Gold else CellBg)
                        .clickable { onlyHere = flag }.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Text("${rows.size} rounds", color = Color(0xCCFFFFFF), fontSize = 10.sp)
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(rows) { r ->
                val car = Car.entries.firstOrNull { it.displayName == r.winner }
                Row(
                    Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(5.dp)).background(CellBg).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val rd = Road.entries.firstOrNull { it.displayName == r.revealedRoad }
                    if (rd != null) {
                        Image(
                            bitmap = sprites.road(rd), contentDescription = rd.displayName,
                            contentScale = ContentScale.Crop, modifier = Modifier.width(34.dp).fillMaxHeight().clip(RoundedCornerShape(3.dp)),
                        )
                    }
                    Text("S${r.revealedSlot}", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (car != null) {
                        Image(
                            bitmap = sprites.vehicle(car), contentDescription = car.displayName,
                            contentScale = ContentScale.Fit, modifier = Modifier.width(44.dp).fillMaxHeight(),
                        )
                    }
                    Text(r.winner ?: "", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    val odd = r.winnerOdd
                    if (odd != null) {
                        Text(
                            (if (odd >= 10.0) "★ " else "") + odd, color = if (odd >= 10.0) Gold else Color.White,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    for ((tag, rk) in listOf("B" to r.rankBlend, "M" to r.rankMine)) {
                        val code = Outcome.code(rk)
                        if (code != null) {
                            Text(
                                tag + code.removePrefix("LOSE").ifEmpty { "L" }, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(resultColor(rk)).padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The 5 offered cars as buttons: tap the winner, which opens the result screen. */
@Composable
private fun WinnerBar(state: SetupState, sprites: SpriteSlicer) {
    val haptic = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth().height(46.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        state.cars.toList().forEach { car ->
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                    .background(Color(0x9922C55E))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.openPostRace(car)
                    },
            ) {
                Image(
                    bitmap = sprites.vehicle(car), contentDescription = car.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(start = 2.dp, end = 2.dp, bottom = 10.dp),
                )
                Text(
                    car.displayName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
