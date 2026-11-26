package com.toprace.tracker.ui

import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toprace.tracker.Car
import com.toprace.tracker.Defaults
import com.toprace.tracker.Road
import com.toprace.tracker.SpriteSlicer
import com.toprace.tracker.db.RoundEntity
import com.toprace.tracker.db.StrategySettings

private val Panel = Color(0xF010161E)
private val Gold = Color(0xFFFFC53D)
private val Cell = Color(0x33FFFFFF)
private val Teal = Color(0xFF2B8FB3)

@Composable
fun SetupScreen(
    state: SetupState,
    sprites: SpriteSlicer,
    onDrag: (Float) -> Unit,
    onClose: () -> Unit,
    onSaveSettings: (StrategySettings) -> Unit,
    onSaveRound: (RoundEntity, Road, List<Car>, Car?) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Panel).padding(6.dp)) {
        if (state.minimized) {
            TitleBar(state, onDrag, onClose)
        } else if (state.postRace) {
            TitleBar(state, onDrag, onClose)
            PostRaceScreen(state, sprites, onSaveRound)
        } else if (state.showSettings) {
            TitleBar(state, onDrag, onClose)
            StrategySettingsPanel(state, onSaveSettings)
        } else if (state.analyzing) {
            TitleBar(state, onDrag, onClose)
            AnalysisScreen(state, sprites)
        } else {
            TopBand(state, onDrag, onClose, Modifier.weight(0.26f).fillMaxWidth())
            Row(
                Modifier.weight(0.74f).fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SlotsColumn(state, sprites, Modifier.weight(0.13f).fillMaxHeight())
                RoadsColumn(state, sprites, Modifier.weight(0.22f).fillMaxHeight())
                RightColumn(state, sprites, Modifier.weight(0.65f).fillMaxHeight())
            }
        }
    }
}

/** Title that drags the window; a long press (no movement) opens Strategy Settings. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DragTitle(state: SetupState, onDrag: (Float) -> Unit, modifier: Modifier) {
    val lastY = remember { floatArrayOf(0f, 0f) } // [0] last rawY, [1] total movement
    Box(
        modifier.pointerInteropFilter { ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lastY[0] = ev.rawY; lastY[1] = 0f; true }
                MotionEvent.ACTION_MOVE -> {
                    val d = ev.rawY - lastY[0]
                    lastY[1] += kotlin.math.abs(d)
                    onDrag(d)
                    lastY[0] = ev.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (lastY[1] < 12f && ev.eventTime - ev.downTime >= 600) state.showSettings = true
                    true
                }
                else -> false
            }
        },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text("TOP RACE  ⠿", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun TitleBar(state: SetupState, onDrag: (Float) -> Unit, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        DragTitle(state, onDrag, Modifier.weight(1f).fillMaxHeight())
        Box(Modifier.clickable { state.minimized = !state.minimized }.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(if (state.minimized) "▢" else "—", color = Color.White, fontSize = 16.sp)
        }
        Box(Modifier.clickable { onClose() }.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text("✕", color = Color.White, fontSize = 16.sp)
        }
    }
}

/** Title + legend + page arrows on the left, 3 rows x 20 dots in the middle, clear/shrink on the right. */
@Composable
private fun TopBand(state: SetupState, onDrag: (Float) -> Unit, onClose: () -> Unit, modifier: Modifier) {
    val page = state.dotPage
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(0.27f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            DragTitle(state, onDrag, Modifier.fillMaxWidth().height(22.dp))
            Text(Dots.title(page), color = Color(0xFF9FB0C3), fontSize = 9.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for ((c, label) in Dots.legend(page)) {
                    Text(
                        label, color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c).padding(horizontal = 3.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("◀", color = Gold, fontSize = 16.sp, modifier = Modifier.clickable { state.dotPage = (page + Dots.PAGES - 1) % Dots.PAGES })
                Text("${page + 1}/${Dots.PAGES}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("▶", color = Gold, fontSize = 16.sp, modifier = Modifier.clickable { state.dotPage = (page + 1) % Dots.PAGES })
            }
        }
        DotsGrid(state.rounds, page, Modifier.weight(1f).fillMaxHeight())
        Column(Modifier.weight(0.09f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFFB5306B))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Teal)
                    .clickable { state.minimized = true },
                contentAlignment = Alignment.Center,
            ) { Text("−", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun DotsGrid(rounds: List<RoundEntity>, page: Int, modifier: Modifier) {
    val perRow = Defaults.HUD_SIZE / 3
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
        for (r in 0..2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until perRow) {
                    val rd = rounds.getOrNull(r * perRow + c)
                    var m = Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(Dots.color(page, rd))
                    if (rd?.sure == 1) m = m.border(1.5.dp, Color.White, CircleShape)
                    Box(m)
                }
            }
        }
    }
}

@Composable
private fun SlotsColumn(state: SetupState, sprites: SpriteSlicer, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF3A1A1D)).padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (s in 1..3) {
            val on = state.slot == s
            val r = state.slotRoads[s - 1]
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(Color(0xFF27121A))
                    .border(if (on) 2.dp else 1.dp, if (on) Gold else Color(0xFF7A3A40), RoundedCornerShape(5.dp))
                    .clickable { state.selectSlot(s) },
                contentAlignment = Alignment.Center,
            ) {
                if (r != null) {
                    Image(
                        bitmap = sprites.road(r), contentDescription = r.displayName,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text("?", color = Color(0xFFD9868E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "$s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun RoadsColumn(state: SetupState, sprites: SpriteSlicer, modifier: Modifier) {
    val enabled = state.slot in 1..3
    Column(
        modifier.alpha(if (enabled) 1f else 0.3f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF3A2F12)).padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (road in Road.entries) {
            val sel = enabled && state.road == road
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(5.dp))
                    .border(if (sel) 2.dp else 1.dp, if (sel) Color.White else Color(0xFF6D5A20), RoundedCornerShape(5.dp))
                    .clickable(enabled = enabled) { state.selectRoad(road) },
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

@Composable
private fun RightColumn(state: SetupState, sprites: SpriteSlicer, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().height(30.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, Teal, RoundedCornerShape(6.dp))) {
                for ((copy, label) in listOf(true to "✍️ Copy", false to "💭 Mine")) {
                    val on = state.copyMode == copy
                    Box(
                        Modifier.fillMaxHeight().background(if (on) Teal else Color.Transparent)
                            .clickable { state.copyMode = copy }.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, color = if (on) Color.White else Color(0xFF9FB0C3), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
            val ready = state.ready
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                    .background(if (ready) Color(0xFF22C55E) else Color(0xFF2E3744))
                    .clickable(enabled = ready) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // my own order is locked here, before the race result is known
                        state.mineLockedAt = System.currentTimeMillis()
                        state.analyzing = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("ANALYSE", color = if (ready) Color(0xFF04210F) else Color(0xFF7C8896), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        VehicleGrid(state, sprites, Modifier.weight(1f).fillMaxWidth())
        PickStrip(state, sprites, Modifier.fillMaxWidth().height(46.dp))
    }
}

@Composable
private fun VehicleGrid(state: SetupState, sprites: SpriteSlicer, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    Column(modifier.border(1.dp, Teal, RoundedCornerShape(6.dp)).padding(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (r in 0..2) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (c in 0..2) {
                    val car = Car.entries.first { it.row == r && it.col == c }
                    val inCopy = car in state.cars
                    val idx = if (state.copyMode) state.cars.indexOf(car) else state.mine.indexOf(car)
                    val sel = idx >= 0
                    val dim = !state.copyMode && !inCopy
                    Box(
                        Modifier.weight(1f).fillMaxHeight().alpha(if (dim) 0.28f else 1f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (sel) Color(0x55FFC53D) else Cell)
                            .border(if (sel) 2.dp else 0.dp, if (sel) Gold else Color.Transparent, RoundedCornerShape(5.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (state.copyMode) state.toggleCar(car) else state.toggleMine(car)
                            },
                    ) {
                        Image(
                            bitmap = sprites.vehicle(car), contentDescription = car.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(bottom = 11.dp, start = 2.dp, end = 2.dp),
                        )
                        Text(
                            car.displayName, color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                        if (sel) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(2.dp).size(15.dp).clip(CircleShape).background(Gold),
                                contentAlignment = Alignment.Center,
                            ) { Text("${idx + 1}", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

/** The 5 chosen cars in order: the game's order in Copy, my own order (1st = trophy) in Mine. */
@Composable
private fun PickStrip(state: SetupState, sprites: SpriteSlicer, modifier: Modifier) {
    val list: List<Car> = if (state.copyMode) state.cars.toList() else state.mine.toList()
    Row(
        modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF0F3A40)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (i in 0 until Defaults.PICKS_REQUIRED) {
            val car = list.getOrNull(i)
            val mineMode = !state.copyMode
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(5.dp))
                    .background(if (mineMode && car != null) Color(0xFF134C40) else Color(0xFF0B2A2F))
                    .border(1.dp, if (mineMode && car != null) Color(0xFF7DFFB0) else Color(0xFF1B5A62), RoundedCornerShape(5.dp)),
            ) {
                if (car != null) {
                    Image(
                        bitmap = sprites.vehicle(car), contentDescription = car.displayName,
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(2.dp),
                    )
                }
                Text("${i + 1}", color = Color(0xFF7FD0D8), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
                if (mineMode && i == 0 && car != null) {
                    Text("🏆", fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}
