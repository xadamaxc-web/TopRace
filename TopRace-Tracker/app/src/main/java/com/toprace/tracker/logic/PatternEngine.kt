package com.toprace.tracker.logic

import com.toprace.tracker.db.RoundEntity

object PatternEngine {
    /** [rounds] newest first. Streak of 2+ wins, else a car winning 3+ of the last 5. */
    fun alert(rounds: List<RoundEntity>): String? {
        val winners = rounds.mapNotNull { it.winner }
        if (winners.isEmpty()) return null

        val first = winners.first()
        val streak = winners.takeWhile { it == first }.size
        if (streak >= 2) return "🔥 STREAK: $first won last $streak"

        val window = winners.take(5)
        if (window.size >= 5) {
            val top = window.groupingBy { it }.eachCount().maxByOrNull { it.value }
            if (top != null && top.value >= 3) return "📈 HOT: ${top.key} won ${top.value} of last ${window.size}"
        }
        return null
    }
}
