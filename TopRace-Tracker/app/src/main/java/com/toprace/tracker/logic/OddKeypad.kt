package com.toprace.tracker.logic

/**
 * Tap-counter keypad for the winner's final odd. Keys: "1", "5", "." and backspace.
 * Before the "." each tap adds to the whole part; after it, to the tenths (max one decimal).
 *   "1","1",".","5","1","1","1" -> 2.8     "5","5","1",".","5","1","1" -> 11.7
 */
object OddKeypad {
    fun value(taps: List<String>): Double? {
        if (taps.none { it == "1" || it == "5" }) return null
        var whole = 0
        var tenths = 0
        var dot = false
        for (t in taps) {
            when (t) {
                "." -> dot = true
                "1", "5" -> {
                    val n = t.toInt()
                    if (dot) tenths += n else whole += n
                }
            }
        }
        return whole + minOf(tenths, 9) / 10.0
    }

    fun text(taps: List<String>): String {
        if (taps.isEmpty()) return "—"
        var whole = 0
        var tenths = 0
        var dot = false
        for (t in taps) {
            when (t) {
                "." -> dot = true
                "1", "5" -> { val n = t.toInt(); if (dot) tenths += n else whole += n }
            }
        }
        return if (dot) "$whole." + minOf(tenths, 9) else "$whole"
    }

    /** Backspace = undo the last tap. */
    fun back(taps: List<String>): List<String> = taps.dropLast(1)
}
