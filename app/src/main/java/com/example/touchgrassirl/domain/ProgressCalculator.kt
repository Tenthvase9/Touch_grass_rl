package com.example.touchgrassirl.domain

object ProgressCalculator {

    fun levelFromTotalXp(totalXp: Int): Int =
        (totalXp / GameConstants.XP_PER_LEVEL) + 1

    fun xpProgressInLevel(totalXp: Int): Pair<Int, Int> {
        val level = levelFromTotalXp(totalXp)
        val xpForCurrentLevel = (level - 1) * GameConstants.XP_PER_LEVEL
        val xpInLevel = totalXp - xpForCurrentLevel
        return xpInLevel to GameConstants.XP_PER_LEVEL
    }

    fun outdoorMinutesXp(minutes: Int): Int =
        minutes.coerceAtLeast(0) * GameConstants.XP_PER_OUTDOOR_MINUTE

    fun stepsXp(steps: Int): Int =
        if (steps >= GameConstants.STEPS_XP_THRESHOLD) GameConstants.XP_PER_1000_STEPS else 0

    fun gardenPlotsForLevel(level: Int): Int =
        1 + (level / GameConstants.GARDEN_PLOT_UNLOCK_EVERY_LEVELS)
}
