package com.example.touchgrassirl.domain

import com.example.touchgrassirl.R

object LevelTitles {

    fun titleResForLevel(level: Int): Int = when {
        level >= 20 -> R.string.title_nature_guardian
        level >= 10 -> R.string.title_forest_scout
        level >= 5 -> R.string.title_grass_walker
        else -> R.string.title_seed
    }
}
