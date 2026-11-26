package com.toprace.tracker.ui

import androidx.compose.ui.graphics.Color

/** Colour of a result from the winner's rank in a version: W1 green, W2 yellow, W3 orange, LOSE gray, none = dim. */
fun resultColor(rank: Int?): Color = when {
    rank == null || rank <= 0 -> Color(0x33FFFFFF)
    rank == 1 -> Color(0xFF22C55E)
    rank == 2 -> Color(0xFFEAB308)
    rank == 3 -> Color(0xFFF97316)
    else -> Color(0xFF9CA3AF)
}
