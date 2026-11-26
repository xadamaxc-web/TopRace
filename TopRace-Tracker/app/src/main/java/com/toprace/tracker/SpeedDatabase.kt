package com.toprace.tracker

import com.toprace.tracker.Car.*
import com.toprace.tracker.Road.*

object SpeedDatabase {
    val table: Map<Road, Map<Car, Int>> = mapOf(
        MOUNTAIN to mapOf(APE to 102, JEEP to 98, WHALE to 90, DRAGON to 88, SHARK to 85, POWER to 84, EAGLE to 80, FLASH to 62, THUNDER to 57),
        BUMPY to mapOf(SHARK to 160, DRAGON to 158, JEEP to 139, EAGLE to 138, APE to 132, WHALE to 123, POWER to 122, FLASH to 101, THUNDER to 98),
        POTHOLES to mapOf(JEEP to 128, SHARK to 119, DRAGON to 105, APE to 102, POWER to 96, WHALE to 92, EAGLE to 90, THUNDER to 85, FLASH to 84),
        HIGHWAY to mapOf(THUNDER to 328, FLASH to 308, EAGLE to 270, WHALE to 225, POWER to 220, DRAGON to 195, SHARK to 190, APE to 180, JEEP to 155),
        EXPRESSWAY to mapOf(THUNDER to 350, FLASH to 340, EAGLE to 310, POWER to 250, WHALE to 240, SHARK to 210, DRAGON to 200, APE to 190, JEEP to 180),
        DESERT to mapOf(THUNDER to 282, FLASH to 260, POWER to 225, EAGLE to 220, SHARK to 180, WHALE to 179, DRAGON to 178, APE to 162, JEEP to 148),
    )

    fun speed(road: Road, car: Car): Int = table.getValue(road).getValue(car)

    fun maxSpeed(road: Road): Int = table.getValue(road).values.max()

    /** Cars ordered fastest first. */
    fun ranked(road: Road): List<Pair<Car, Int>> =
        table.getValue(road).entries.sortedByDescending { it.value }.map { it.key to it.value }
}
