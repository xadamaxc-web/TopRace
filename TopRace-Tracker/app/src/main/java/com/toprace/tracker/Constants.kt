package com.toprace.tracker

enum class Tier { FAST, MEDIUM }

/** row/col = position in sprite_vehicles.webp (3x3). */
enum class Car(
    val displayName: String,
    val row: Int,
    val col: Int,
    val baseOdd: Double,
    val tier: Tier,
) {
    SHARK("Shark", 0, 0, 2.8, Tier.MEDIUM),
    EAGLE("Eagle", 0, 1, 3.2, Tier.FAST),
    JEEP("Jeep", 0, 2, 3.8, Tier.MEDIUM),
    THUNDER("Thunder", 1, 0, 3.8, Tier.FAST),
    APE("Ape", 1, 1, 5.5, Tier.MEDIUM),
    DRAGON("Dragon", 1, 2, 6.8, Tier.MEDIUM),
    FLASH("Flash", 2, 0, 6.9, Tier.FAST),
    POWER("Power", 2, 1, 11.7, Tier.FAST),
    WHALE("Whale", 2, 2, 14.9, Tier.FAST);

    companion object {
        fun fromName(name: String): Car = entries.first { it.displayName == name }
    }
}

/** row/col = position in sprite_roads.webp (3 cols x 2 rows). */
enum class Road(
    val displayName: String,
    val row: Int,
    val col: Int,
    val fast: Boolean, // assumption: Highway/Expressway/Desert = fast
) {
    MOUNTAIN("Mountain", 0, 0, false),
    POTHOLES("Potholes", 0, 1, false),
    BUMPY("Bumpy", 0, 2, false),
    DESERT("Desert", 1, 0, true),
    EXPRESSWAY("Expressway", 1, 1, true),
    HIGHWAY("Highway", 1, 2, true);

    companion object {
        fun fromName(name: String): Road = entries.first { it.displayName == name }
    }
}

object Defaults {
    const val PICKS_REQUIRED = 5
    /** HUD circles shown (3 rows of HUD_SIZE / 3). */
    const val HUD_SIZE = 60
}
