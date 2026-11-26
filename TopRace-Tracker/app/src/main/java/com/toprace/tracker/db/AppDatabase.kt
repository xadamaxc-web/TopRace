package com.toprace.tracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RoundEntity::class, StrategySettings::class, LearnedStats::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roundDao(): RoundDao
    abstract fun settingsDao(): SettingsDao
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** S7: per-version winner ranks + the picks shown. Existing rounds are kept (new columns stay null). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rounds ADD COLUMN picks_shown TEXT")
                db.execSQL("ALTER TABLE rounds ADD COLUMN conf_shown TEXT")
                for (c in listOf("rank_math", "rank_base", "rank_data", "rank_blend", "rank_recent")) {
                    db.execSQL("ALTER TABLE rounds ADD COLUMN $c INTEGER")
                }
            }
        }

        /** S8: full ordering of the 5 cars for MATH / DATA / BLEND / RECENT. Existing rounds are kept (new columns stay null). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (c in listOf("order_math", "order_data", "order_blend", "order_recent")) {
                    db.execSQL("ALTER TABLE rounds ADD COLUMN $c TEXT")
                }
            }
        }

        /** S9: slots, lengths, my own order, SURE, stake and the LIVE version. Existing rounds are kept (new columns stay null). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (c in listOf("road_s1", "road_s2", "road_s3", "mine_order", "order_live")) {
                    db.execSQL("ALTER TABLE rounds ADD COLUMN $c TEXT")
                }
                for (c in listOf("len_s1", "len_s2", "len_s3", "mine_locked_at", "rank_mine", "sure", "stake_pct", "rank_live")) {
                    db.execSQL("ALTER TABLE rounds ADD COLUMN $c INTEGER")
                }
            }
        }

        fun get(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext, AppDatabase::class.java, "toprace.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }
    }
}
