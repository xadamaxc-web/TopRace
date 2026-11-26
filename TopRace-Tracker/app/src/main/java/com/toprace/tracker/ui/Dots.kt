package com.toprace.tracker.ui

import androidx.compose.ui.graphics.Color
import com.toprace.tracker.db.RoundEntity

/** The 5 dot pages. Each page colours one question about every round, newest first. */
object Dots {
    const val PAGES = 5
    private val dim = Color(0x22FFFFFF)
    private val green = Color(0xFF22C55E)
    private val yellow = Color(0xFFEAB308)
    private val orange = Color(0xFFF97316)
    private val red = Color(0xFFEF4444)
    private val gray = Color(0xFF9CA3AF)
    private val rank5 = listOf(green, yellow, orange, red, gray)
    private val conf = listOf(Color(0xFF64748B), Color(0xFF38BDF8), Color(0xFFA78BFA), Color(0xFFF472B6))
    private val odd = listOf(Color(0xFF64748B), Color(0xFF38BDF8), Color(0xFFF97316), Color(0xFFF472B6))

    fun legend(page: Int): List<Pair<Color, String>> = when (page) {
        0 -> listOf(green to "W1", yellow to "W2", orange to "W3", gray to "L")
        1 -> listOf(green to "M1", yellow to "M2", orange to "M3", red to "M4", gray to "NM")
        2 -> listOf(conf[0] to "<25", conf[1] to "25", conf[2] to "30", conf[3] to "35+")
        3 -> listOf(green to "1st", yellow to "2nd", orange to "3rd", red to "4th", gray to "5th")
        else -> listOf(odd[0] to "<2", odd[1] to "2-5", odd[2] to "5-10", odd[3] to "10+")
    }

    fun title(page: Int): String = when (page) {
        0 -> "BLEND W/L"
        1 -> "MATH rank"
        2 -> "Confidence"
        3 -> "MINE rank"
        else -> "Winner odd"
    }

    fun color(page: Int, r: RoundEntity?): Color {
        if (r == null) return dim
        return when (page) {
            0 -> resultColor(r.rankBlend)
            1 -> r.rankMath?.let { rank5[(it - 1).coerceIn(0, 4)] } ?: dim
            2 -> {
                val c = r.confShown?.split("|")?.firstOrNull()?.toIntOrNull() ?: return dim
                when {
                    c < 25 -> conf[0]
                    c < 30 -> conf[1]
                    c < 35 -> conf[2]
                    else -> conf[3]
                }
            }
            3 -> r.rankMine?.let { rank5[(it - 1).coerceIn(0, 4)] } ?: dim
            else -> {
                val o = r.winnerOdd ?: return dim
                when {
                    o < 2.0 -> odd[0]
                    o < 5.0 -> odd[1]
                    o < 10.0 -> odd[2]
                    else -> odd[3]
                }
            }
        }
    }
}
