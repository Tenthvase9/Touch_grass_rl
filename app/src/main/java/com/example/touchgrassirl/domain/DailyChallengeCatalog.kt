package com.example.touchgrassirl.domain

import com.example.touchgrassirl.R
import java.time.LocalDate

enum class ChallengeType {
    OUTDOOR_MINUTES,
    BEFORE_HOUR,
}

data class DailyChallengeDefinition(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val type: ChallengeType,
    val targetMinutes: Int = 0,
    val beforeHour: Int = 0,
)

object DailyChallengeCatalog {

    val all: List<DailyChallengeDefinition> = listOf(
        DailyChallengeDefinition(
            id = "outdoor_45",
            titleRes = R.string.challenge_outdoor_45_title,
            descriptionRes = R.string.challenge_outdoor_45_desc,
            type = ChallengeType.OUTDOOR_MINUTES,
            targetMinutes = 45,
        ),
        DailyChallengeDefinition(
            id = "outdoor_30",
            titleRes = R.string.challenge_outdoor_30_title,
            descriptionRes = R.string.challenge_outdoor_30_desc,
            type = ChallengeType.OUTDOOR_MINUTES,
            targetMinutes = 30,
        ),
        DailyChallengeDefinition(
            id = "before_9am",
            titleRes = R.string.challenge_before_9_title,
            descriptionRes = R.string.challenge_before_9_desc,
            type = ChallengeType.BEFORE_HOUR,
            beforeHour = 9,
            targetMinutes = GameConstants.MIN_OUTDOOR_MINUTES,
        ),
        DailyChallengeDefinition(
            id = "outdoor_20",
            titleRes = R.string.challenge_outdoor_20_title,
            descriptionRes = R.string.challenge_outdoor_20_desc,
            type = ChallengeType.OUTDOOR_MINUTES,
            targetMinutes = 20,
        ),
        DailyChallengeDefinition(
            id = "visit_park",
            titleRes = R.string.challenge_visit_park_title,
            descriptionRes = R.string.challenge_visit_park_desc,
            type = ChallengeType.OUTDOOR_MINUTES,
            targetMinutes = GameConstants.MIN_OUTDOOR_MINUTES,
        ),
    )

    fun forDate(date: LocalDate = LocalDate.now()): DailyChallengeDefinition {
        val index = (date.toEpochDay() % all.size).toInt().let { if (it < 0) -it else it }
        return all[index]
    }

    fun isComplete(
        challenge: DailyChallengeDefinition,
        outdoorMinutes: Int,
        touchedGrassToday: Boolean,
        sessionStartedHour: Int? = null,
        visitedParkToday: Boolean = false,
    ): Boolean {
        if (challenge.id == "visit_park") {
            return touchedGrassToday &&
                visitedParkToday &&
                outdoorMinutes >= GameConstants.MIN_OUTDOOR_MINUTES
        }
        return when (challenge.type) {
            ChallengeType.OUTDOOR_MINUTES ->
                touchedGrassToday && outdoorMinutes >= challenge.targetMinutes
            ChallengeType.BEFORE_HOUR ->
                touchedGrassToday &&
                    outdoorMinutes >= challenge.targetMinutes &&
                    sessionStartedHour != null &&
                    sessionStartedHour < challenge.beforeHour
        }
    }
}
