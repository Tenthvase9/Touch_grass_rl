package com.example.touchgrassirl.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {

    @Test
    fun outdoorMinutesXp_matchesVisionTable() {
        assertEquals(10, ProgressCalculator.outdoorMinutesXp(10))
    }

    @Test
    fun stepsXp_awardsAtThreshold() {
        assertEquals(0, ProgressCalculator.stepsXp(999))
        assertEquals(GameConstants.XP_PER_1000_STEPS, ProgressCalculator.stepsXp(1000))
    }

    @Test
    fun levelFromTotalXp_incrementsEveryHundred() {
        assertEquals(1, ProgressCalculator.levelFromTotalXp(0))
        assertEquals(2, ProgressCalculator.levelFromTotalXp(100))
    }
}
