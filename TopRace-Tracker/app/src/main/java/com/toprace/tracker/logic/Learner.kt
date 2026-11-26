package com.toprace.tracker.logic

import androidx.room.withTransaction
import com.toprace.tracker.Car
import com.toprace.tracker.Road
import com.toprace.tracker.db.AppDatabase
import com.toprace.tracker.db.LearnedStats
import com.toprace.tracker.db.RoundEntity

object Learner {
    /** Every raced car gets totalRounds+1 on this road; the winner also wins+1. */
    suspend fun update(db: AppDatabase, road: Road, cars: List<Car>, winner: Car?) {
        if (winner == null) return
        val dao = db.statsDao()
        for (c in cars) {
            val old = dao.find(c.displayName, road.displayName)
                ?: LearnedStats(carName = c.displayName, roadName = road.displayName)
            dao.upsert(old.copy(wins = old.wins + if (c == winner) 1 else 0, totalRounds = old.totalRounds + 1))
        }
    }

    /** Save round + learner update atomically. HUD updates through the rounds Flow. */
    suspend fun saveRound(db: AppDatabase, round: RoundEntity, road: Road, cars: List<Car>, winner: Car?) {
        db.withTransaction {
            db.roundDao().insert(round)
            update(db, road, cars, winner)
        }
    }
}
