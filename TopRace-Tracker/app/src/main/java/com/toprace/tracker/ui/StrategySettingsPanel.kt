package com.toprace.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toprace.tracker.db.StrategySettings

private val Gold = Color(0xFFFFC53D)

/** Opened by long-pressing the title. */
@Composable
fun StrategySettingsPanel(state: SetupState, onSave: (StrategySettings) -> Unit) {
    val s0 = state.settings
    var fast by remember { mutableStateOf(s0.fastRoadBoost.toString()) }
    var struggle by remember { mutableStateOf(s0.struggleRoadPenalty.toString()) }
    var b1 by remember { mutableStateOf(s0.slot1Bias.toString()) }
    var b2 by remember { mutableStateOf(s0.slot2Bias.toString()) }
    var b3 by remember { mutableStateOf(s0.slot3Bias.toString()) }
    var blend by remember { mutableStateOf(s0.dataBlend.toFloat()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("Strategy Settings", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        FieldRow("Fast Road Multiplier", fast) { fast = it }
        FieldRow("Struggle Road Multiplier", struggle) { struggle = it }
        FieldRow("Slot 1 Bias", b1) { b1 = it }
        FieldRow("Slot 2 Bias", b2) { b2 = it }
        FieldRow("Slot 3 Bias", b3) { b3 = it }
        Text("Data vs Math: ${blend.toInt()}% data", color = Color.White, fontSize = 12.sp)
        Slider(value = blend, onValueChange = { blend = it }, valueRange = 0f..100f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF22C55E))
                    .clickable {
                        fun f(v: String) = v.toFloatOrNull()?.takeIf { it > 0f } ?: 1f
                        onSave(
                            StrategySettings(
                                id = 1,
                                fastRoadBoost = f(fast),
                                struggleRoadPenalty = f(struggle),
                                slot1Bias = f(b1),
                                slot2Bias = f(b2),
                                slot3Bias = f(b3),
                                dataBlend = blend.toInt().coerceIn(0, 100),
                            ),
                        )
                        state.showSettings = false
                    }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(Color(0x44FFFFFF))
                    .clickable { state.showSettings = false }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text("CANCEL", color = Color.White, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier.width(90.dp).border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = { v -> if (v.all { it.isDigit() || it == '.' }) onChange(v) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
    }
}
