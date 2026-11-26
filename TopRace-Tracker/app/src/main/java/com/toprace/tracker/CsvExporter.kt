package com.toprace.tracker

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.toprace.tracker.db.AppDatabase
import com.toprace.tracker.logic.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes rounds + learned stats as CSV into the public Download folder. */
object CsvExporter {
    data class Result(val roundsFile: String, val statsFile: String, val rounds: Int)

    suspend fun export(ctx: Context): Result = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(ctx)
        val rounds = db.roundDao().all()
        val stats = db.statsDao().allNow()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        val rc = StringBuilder(
            "id,timestamp,datetime,revealed_road,revealed_slot,hidden_road_2,hidden_road_3," +
                "car1,car2,car3,car4,car5,winner,picks_shown,conf_shown," +
                "rank_math,rank_base,rank_data,rank_blend,rank_recent," +
                "res_math,res_base,res_data,res_blend,res_recent," +
                "order_math,order_data,order_blend,order_recent," +
                "road_s1,road_s2,road_s3,len_s1,len_s2,len_s3,winner_odd," +
                "mine_order,mine_locked_at,rank_mine,res_mine,sure,stake_pct,rank_live,res_live,order_live\n",
        )
        for (r in rounds) {
            rc.append(
                listOf(
                    r.id, r.timestamp, iso.format(Date(r.timestamp)), r.revealedRoad, r.revealedSlot,
                    r.hiddenRoad2, r.hiddenRoad3, r.car1, r.car2, r.car3, r.car4, r.car5,
                    r.winner, r.picksShown, r.confShown,
                    r.rankMath, r.rankBase, r.rankData, r.rankBlend, r.rankRecent,
                    Outcome.code(r.rankMath), Outcome.code(r.rankBase), Outcome.code(r.rankData),
                    Outcome.code(r.rankBlend), Outcome.code(r.rankRecent),
                    r.orderMath, r.orderData, r.orderBlend, r.orderRecent,
                    r.roadS1, r.roadS2, r.roadS3, r.lenS1, r.lenS2, r.lenS3, r.winnerOdd,
                    r.mineOrder, r.mineLockedAt, r.rankMine, Outcome.code(r.rankMine), r.sure, r.stakePct,
                    r.rankLive, Outcome.code(r.rankLive), r.orderLive,
                ).joinToString(",") { cell(it) },
            ).append('\n')
        }

        val sc = StringBuilder("car,road,wins,total_rounds\n")
        for (s in stats) {
            sc.append(listOf(s.carName, s.roadName, s.wins, s.totalRounds).joinToString(",") { cell(it) }).append('\n')
        }

        val roundsName = "toprace_rounds_$stamp.csv"
        val statsName = "toprace_stats_$stamp.csv"
        write(ctx, roundsName, rc.toString())
        write(ctx, statsName, sc.toString())
        Result(roundsName, statsName, rounds.size)
    }

    private fun cell(v: Any?): String {
        val t = v?.toString() ?: ""
        return if (t.any { it == ',' || it == '"' || it == '\n' }) "\"" + t.replace("\"", "\"\"") + "\"" else t
    }

    private fun write(ctx: Context, name: String, text: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            // Android 10+: scoped storage, MediaStore is the supported way into Download/
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create $name in Downloads")
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                ?: error("Could not open $name for writing")
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            File(dir, name).writeText(text, Charsets.UTF_8)
        }
    }
}
