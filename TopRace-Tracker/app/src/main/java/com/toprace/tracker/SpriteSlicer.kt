package com.toprace.tracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Slices the equal-cell sprite sheets. Vehicles 3x3, roads 3 cols x 2 rows. */
class SpriteSlicer(ctx: Context) {
    private val vehicleSheet = load(ctx, R.drawable.sprite_vehicles)
    private val roadSheet = load(ctx, R.drawable.sprite_roads)
    private val vehicleCache = HashMap<Car, ImageBitmap>()
    private val roadCache = HashMap<Road, ImageBitmap>()

    fun vehicle(car: Car): ImageBitmap =
        vehicleCache.getOrPut(car) { cell(vehicleSheet, 3, 3, car.row, car.col) }

    fun road(road: Road): ImageBitmap =
        roadCache.getOrPut(road) { cell(roadSheet, 3, 2, road.row, road.col) }

    private fun load(ctx: Context, id: Int): Bitmap {
        val o = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(ctx.resources, id, o)
    }

    private fun cell(b: Bitmap, cols: Int, rows: Int, row: Int, col: Int): ImageBitmap {
        val w = b.width / cols
        val h = b.height / rows
        return Bitmap.createBitmap(b, col * w, row * h, w, h).asImageBitmap()
    }
}
