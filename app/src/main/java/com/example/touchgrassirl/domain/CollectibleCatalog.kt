package com.example.touchgrassirl.domain

import com.example.touchgrassirl.R

enum class CollectibleCondition {
    SUNNY,
    RAIN,
    NIGHT,
    PARK_VISIT,
}

data class CollectibleDefinition(
    val id: String,
    val titleRes: Int,
    val emoji: String,
    val condition: CollectibleCondition,
)

object CollectibleCatalog {

    const val GOLDEN_LEAF = "golden_leaf"
    const val RAIN_FROG = "rain_frog"
    const val MOON_FLOWER = "moon_flower"
    const val PARK_SEED = "park_seed"
    const val SUNSTONE = "sunstone"
    const val TRAIL_FEATHER = "trail_feather"
    const val DEW_DROP = "dew_drop"

    val all: List<CollectibleDefinition> = listOf(
        CollectibleDefinition(
            id = GOLDEN_LEAF,
            titleRes = R.string.collectible_golden_leaf,
            emoji = "🍃",
            condition = CollectibleCondition.SUNNY,
        ),
        CollectibleDefinition(
            id = RAIN_FROG,
            titleRes = R.string.collectible_rain_frog,
            emoji = "🐸",
            condition = CollectibleCondition.RAIN,
        ),
        CollectibleDefinition(
            id = MOON_FLOWER,
            titleRes = R.string.collectible_moon_flower,
            emoji = "🌙",
            condition = CollectibleCondition.NIGHT,
        ),
        CollectibleDefinition(
            id = PARK_SEED,
            titleRes = R.string.collectible_park_seed,
            emoji = "🌰",
            condition = CollectibleCondition.PARK_VISIT,
        ),
        CollectibleDefinition(
            id = SUNSTONE,
            titleRes = R.string.collectible_sunstone,
            emoji = "\uD83D\uDD36",
            condition = CollectibleCondition.SUNNY,
        ),
        CollectibleDefinition(
            id = TRAIL_FEATHER,
            titleRes = R.string.collectible_trail_feather,
            emoji = "\uD83E\uDEB6",
            condition = CollectibleCondition.PARK_VISIT,
        ),
        CollectibleDefinition(
            id = DEW_DROP,
            titleRes = R.string.collectible_dew_drop,
            emoji = "\uD83D\uDCA7",
            condition = CollectibleCondition.RAIN,
        ),
    )

    fun eligibleIds(
        hourOfDay: Int,
        isRaining: Boolean,
        visitedParkThisCheck: Boolean,
        roll: Double = Math.random(),
    ): List<String> {
        val isNight = hourOfDay < 6 || hourOfDay >= 20
        val isSunny = !isRaining && hourOfDay in 8..18
        return buildList {
            if (isSunny) add(GOLDEN_LEAF)
            if (isRaining) add(RAIN_FROG)
            if (isNight) add(MOON_FLOWER)
            if (visitedParkThisCheck) add(PARK_SEED)
            if (isSunny && roll < 0.25) add(SUNSTONE)
            if (visitedParkThisCheck && roll < 0.30) add(TRAIL_FEATHER)
            if (isRaining && roll < 0.40) add(DEW_DROP)
        }
    }

    fun byId(id: String): CollectibleDefinition? = all.find { it.id == id }
}
