package com.toprace.tracker.logic

/** Winner's rank in a version's ordering -> W1 / W2 / W3 / LOSE. */
object Outcome {
    fun code(rank: Int?): String? = when {
        rank == null || rank <= 0 -> null
        rank <= 3 -> "W$rank"
        else -> "LOSE"
    }
}
