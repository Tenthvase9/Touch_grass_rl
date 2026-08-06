package com.example.touchgrassirl.domain

import com.example.touchgrassirl.R
import com.example.touchgrassirl.data.local.entity.DailyLogEntity
import com.example.touchgrassirl.data.local.entity.UserProgressEntity

enum class AchievementCategory {
    WALKING,
    EXPLORATION,
    CONSISTENCY,
    RARE,
}

data class AchievementDefinition(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val category: AchievementCategory,
    val emoji: String,
)

object AchievementCatalog {

    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = "first_grass",
            titleRes = R.string.achievement_first_walk_title,
            descriptionRes = R.string.achievement_first_walk_desc,
            category = AchievementCategory.WALKING,
            emoji = "🌱",
        ),
        AchievementDefinition(
            id = "ten_k_steps",
            titleRes = R.string.achievement_ten_k_steps_title,
            descriptionRes = R.string.achievement_ten_k_steps_desc,
            category = AchievementCategory.WALKING,
            emoji = "👟",
        ),
        AchievementDefinition(
            id = "marathon_grass",
            titleRes = R.string.achievement_marathon_grass_title,
            descriptionRes = R.string.achievement_marathon_grass_desc,
            category = AchievementCategory.WALKING,
            emoji = "🏃",
        ),
        AchievementDefinition(
            id = "park_explorer",
            titleRes = R.string.achievement_park_explorer_title,
            descriptionRes = R.string.achievement_park_explorer_desc,
            category = AchievementCategory.EXPLORATION,
            emoji = "🌳",
        ),
        AchievementDefinition(
            id = "trail_wanderer",
            titleRes = R.string.achievement_trail_wanderer_title,
            descriptionRes = R.string.achievement_trail_wanderer_desc,
            category = AchievementCategory.EXPLORATION,
            emoji = "🥾",
        ),
        AchievementDefinition(
            id = "streak_7",
            titleRes = R.string.achievement_streak_7_title,
            descriptionRes = R.string.achievement_streak_7_desc,
            category = AchievementCategory.CONSISTENCY,
            emoji = "🔥",
        ),
        AchievementDefinition(
            id = "streak_30",
            titleRes = R.string.achievement_streak_30_title,
            descriptionRes = R.string.achievement_streak_30_desc,
            category = AchievementCategory.CONSISTENCY,
            emoji = "💫",
        ),
        AchievementDefinition(
            id = "rain_grass",
            titleRes = R.string.achievement_rain_grass_title,
            descriptionRes = R.string.achievement_rain_grass_desc,
            category = AchievementCategory.RARE,
            emoji = "🐸",
        ),
        AchievementDefinition(
            id = "sunrise_walker",
            titleRes = R.string.achievement_sunrise_walker_title,
            descriptionRes = R.string.achievement_sunrise_walker_desc,
            category = AchievementCategory.RARE,
            emoji = "🌅",
        ),
        AchievementDefinition(
            id = "midnight_explorer",
            titleRes = R.string.achievement_midnight_explorer_title,
            descriptionRes = R.string.achievement_midnight_explorer_desc,
            category = AchievementCategory.RARE,
            emoji = "🌙",
        ),
        AchievementDefinition(
            id = "century_grass",
            titleRes = R.string.achievement_century_grass_title,
            descriptionRes = R.string.achievement_century_grass_desc,
            category = AchievementCategory.WALKING,
            emoji = "💯",
        ),
        AchievementDefinition(
            id = "collector",
            titleRes = R.string.achievement_collector_title,
            descriptionRes = R.string.achievement_collector_desc,
            category = AchievementCategory.EXPLORATION,
            emoji = "🏆",
        ),
        AchievementDefinition(
            id = "all_weather",
            titleRes = R.string.achievement_all_weather_title,
            descriptionRes = R.string.achievement_all_weather_desc,
            category = AchievementCategory.RARE,
            emoji = "🌈",
        ),
        AchievementDefinition(
            id = "streak_3",
            titleRes = R.string.achievement_streak_3_title,
            descriptionRes = R.string.achievement_streak_3_desc,
            category = AchievementCategory.CONSISTENCY,
            emoji = "✨",
        ),
        AchievementDefinition(
            id = "distance_10k",
            titleRes = R.string.achievement_distance_10k_title,
            descriptionRes = R.string.achievement_distance_10k_desc,
            category = AchievementCategory.WALKING,
            emoji = "🎯",
        ),
        AchievementDefinition(
            id = "week_warrior",
            titleRes = R.string.achievement_week_warrior_title,
            descriptionRes = R.string.achievement_week_warrior_desc,
            category = AchievementCategory.CONSISTENCY,
            emoji = "📅",
        ),
    )

    fun byId(id: String): AchievementDefinition? = all.find { it.id == id }

    fun idsToUnlock(
        progress: UserProgressEntity,
        todayLog: DailyLogEntity?,
        sessionCounted: Boolean,
        sessionDurationMinutes: Int,
        sessionStartHour: Int?,
        isRaining: Boolean = false,
        visitedParkToday: Boolean = false,
    ): List<String> {
        val unlocked = mutableListOf<String>()
        val totalMinutes = progress.totalOutdoorMinutes +
            if (sessionCounted) sessionDurationMinutes else 0
        val todayMinutes = (todayLog?.outdoorMinutes ?: 0) +
            if (sessionCounted) sessionDurationMinutes else 0
        val todaySteps = todayLog?.steps ?: 0
        val streak = progress.currentStreak

        if (sessionCounted && progress.totalSessionsCompleted == 0) {
            unlocked.add("first_grass")
        }
        if (todaySteps >= GameConstants.STEPS_XP_THRESHOLD) {
            unlocked.add("ten_k_steps")
        }
        if (totalMinutes >= 180) {
            unlocked.add("marathon_grass")
        }
        if (progress.totalSessionsCompleted >= 5) {
            unlocked.add("trail_wanderer")
        }
        if (streak >= 7) {
            unlocked.add("streak_7")
        }
        if (streak >= 30) {
            unlocked.add("streak_30")
        }
        if (sessionStartHour != null && sessionStartHour < 7 && sessionCounted) {
            unlocked.add("sunrise_walker")
        }
        if (sessionStartHour != null && sessionStartHour >= 22 && sessionCounted) {
            unlocked.add("midnight_explorer")
        }
        if (visitedParkToday) {
            unlocked.add("park_explorer")
        }
        if (isRaining && sessionCounted) {
            unlocked.add("rain_grass")
        }
        if (totalMinutes >= 100) {
            unlocked.add("century_grass")
        }
        if (progress.totalSessionsCompleted >= 3) {
            unlocked.add("collector")
        }
        if (streak >= 3) {
            unlocked.add("streak_3")
        }
        if (progress.totalOutdoorMinutes >= 600) {
            unlocked.add("distance_10k")
        }
        if (progress.totalSessionsCompleted >= 7) {
            unlocked.add("week_warrior")
        }
        if (progress.dailyGoalMinutes >= 30 && streak >= 5 && isRaining && sessionCounted) {
            unlocked.add("all_weather")
        }
        return unlocked
    }
}
