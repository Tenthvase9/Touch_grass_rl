package com.example.touchgrassirl.data.repository

import com.example.touchgrassirl.data.local.TouchGrassDatabase
import com.example.touchgrassirl.data.local.entity.AchievementEntity
import com.example.touchgrassirl.data.local.entity.CollectedCollectibleEntity
import com.example.touchgrassirl.data.local.entity.DailyLogEntity
import com.example.touchgrassirl.data.local.entity.OutdoorSessionEntity
import com.example.touchgrassirl.data.local.entity.UserProgressEntity
import com.example.touchgrassirl.data.local.entity.VisitedSpotEntity
import com.example.touchgrassirl.data.location.OutdoorLocationTracker
import com.example.touchgrassirl.data.weather.WeatherClient
import com.example.touchgrassirl.domain.AchievementCatalog
import com.example.touchgrassirl.domain.CollectibleCatalog
import com.example.touchgrassirl.domain.DailyChallengeCatalog
import com.example.touchgrassirl.domain.DailyChallengeDefinition
import com.example.touchgrassirl.domain.GameConstants
import com.example.touchgrassirl.domain.LevelTitles
import com.example.touchgrassirl.domain.NatureSpot
import com.example.touchgrassirl.domain.NatureSpotGenerator
import com.example.touchgrassirl.domain.NatureSpotType
import com.example.touchgrassirl.domain.ProgressCalculator
import com.example.touchgrassirl.domain.SessionMotionSnapshot
import org.osmdroid.util.GeoPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TouchGrassRepository(
    private val database: TouchGrassDatabase,
    val locationTracker: OutdoorLocationTracker,
    private val weatherClient: WeatherClient,
) {

    private val progressDao = database.userProgressDao()
    private val dailyLogDao = database.dailyLogDao()
    private val sessionDao = database.outdoorSessionDao()
    private val achievementDao = database.achievementDao()
    private val visitedSpotDao = database.visitedSpotDao()
    private val collectibleDao = database.collectedCollectibleDao()

    private var spotsAnchor: GeoPoint? = null
    private var generatedSpots: List<NatureSpot> = emptyList()
    private var cachedIsRaining: Boolean? = null
    private var rainCachedAtMillis: Long = 0L

    companion object {
        private const val VISIT_RADIUS_METERS = 80.0
        private const val SPOT_REGENERATE_DISTANCE_METERS = 1_500.0
    }

    fun observeProgress(): Flow<UserProgressEntity> =
        progressDao.observeProgress().map { it ?: UserProgressEntity() }

    fun observeActiveSession(): Flow<OutdoorSessionEntity?> =
        sessionDao.observeActiveSession()

    fun observeUnlockedAchievements(): Flow<List<AchievementEntity>> =
        achievementDao.observeAll()

    fun observeCollectedCollectibles(): Flow<List<CollectedCollectibleEntity>> =
        collectibleDao.observeAll()

    fun observeVisitedSpotIds(): Flow<Set<String>> =
        visitedSpotDao.observeVisitedIds().map { it.toSet() }

    suspend fun ensureProgressInitialized() {
        if (progressDao.getProgress() == null) {
            progressDao.insert(UserProgressEntity())
        }
    }

    suspend fun getTodayLog(): DailyLogEntity? {
        val today = LocalDate.now().toEpochDay()
        return dailyLogDao.getForDay(today)
    }

    suspend fun ensureTodayLog(): DailyLogEntity {
        val today = LocalDate.now().toEpochDay()
        val existing = dailyLogDao.getForDay(today)
        if (existing != null) {
            if (existing.challengeId == null) {
                val challenge = DailyChallengeCatalog.forDate()
                val updated = existing.copy(challengeId = challenge.id)
                dailyLogDao.upsert(updated)
                return updated
            }
            return existing
        }
        val challenge = DailyChallengeCatalog.forDate()
        val log = DailyLogEntity(
            dateEpochDay = today,
            challengeId = challenge.id,
        )
        dailyLogDao.upsert(log)
        return log
    }

    fun todayChallenge(): DailyChallengeDefinition =
        DailyChallengeCatalog.forDate()

    suspend fun hasTouchedGrassToday(): Boolean =
        getTodayLog()?.touchedGrass == true

    suspend fun getTodayOutdoorMinutes(): Int =
        getTodayLog()?.outdoorMinutes ?: 0

    suspend fun getActiveSession(): OutdoorSessionEntity? =
        sessionDao.getActiveSession()

    suspend fun getNatureSpots(): List<NatureSpot> {
        val location = locationTracker.getCurrentLocation()
            ?: spotsAnchor
            ?: GeoPoint(37.7749, -122.4194) // fallback when GPS unavailable

        val shouldRegenerate = spotsAnchor?.let { anchor ->
            NatureSpotGenerator.distanceMeters(
                anchor.latitude,
                anchor.longitude,
                location.latitude,
                location.longitude,
            ) > SPOT_REGENERATE_DISTANCE_METERS
        } ?: true

        if (shouldRegenerate) {
            spotsAnchor = location
            generatedSpots = NatureSpotGenerator.generateNear(
                location.latitude,
                location.longitude,
            )
        }

        val visited = visitedSpotDao.getVisitedIds().toSet()
        return generatedSpots.map { it.copy(visited = visited.contains(it.id)) }
    }

    suspend fun processLocationUpdate(latitude: Double, longitude: Double): LocationExplorationResult {
        val spots = getNatureSpots()
        val visitedIds = visitedSpotDao.getVisitedIds().toSet()
        val newlyVisited = mutableListOf<NatureSpot>()
        var visitedParkNow = false

        for (spot in spots) {
            if (spot.id in visitedIds) continue
            val distance = NatureSpotGenerator.distanceMeters(
                latitude,
                longitude,
                spot.latitude,
                spot.longitude,
            )
            if (distance <= VISIT_RADIUS_METERS) {
                visitedSpotDao.insert(
                    VisitedSpotEntity(
                        spotId = spot.id,
                        visitedAtMillis = System.currentTimeMillis(),
                        spotName = spot.name,
                        spotType = spot.type.name,
                    ),
                )
                newlyVisited.add(spot)
                if (spot.type == NatureSpotType.PARK) {
                    visitedParkNow = true
                }
            }
        }

        var parkXpAwarded = 0
        if (visitedParkNow) {
            val log = ensureTodayLog()
            if (!log.parkVisitXpClaimed) {
                parkXpAwarded = GameConstants.XP_PARK_VISIT
                dailyLogDao.upsert(
                    log.copy(
                        xpEarned = log.xpEarned + parkXpAwarded,
                        parkVisitXpClaimed = true,
                    ),
                )
            }
        }

        val hour = java.time.LocalTime.now().hour
        val isRaining = cachedIsRaining(latitude, longitude)
        val newlyCollected = awardCollectibles(
            hour = hour,
            isRaining = isRaining,
            visitedParkNow = visitedParkNow,
        )

        return LocationExplorationResult(
            newlyVisitedSpots = newlyVisited,
            newlyCollectedIds = newlyCollected,
            parkXpAwarded = parkXpAwarded,
        )
    }

    private suspend fun cachedIsRaining(latitude: Double, longitude: Double): Boolean {
        val now = System.currentTimeMillis()
        if (cachedIsRaining != null && now - rainCachedAtMillis < 10 * 60 * 1000) {
            return cachedIsRaining!!
        }
        val raining = weatherClient.isRaining(latitude, longitude)
        cachedIsRaining = raining
        rainCachedAtMillis = now
        return raining
    }

    private suspend fun awardCollectibles(
        hour: Int,
        isRaining: Boolean,
        visitedParkNow: Boolean,
    ): List<String> {
        val already = collectibleDao.getCollectedIds().toSet()
        val eligible = CollectibleCatalog.eligibleIds(hour, isRaining, visitedParkNow)
        val newly = mutableListOf<String>()
        val now = System.currentTimeMillis()
        for (id in eligible) {
            if (id !in already) {
                collectibleDao.insert(CollectedCollectibleEntity(id = id, collectedAtMillis = now))
                newly.add(id)
            }
        }
        return newly
    }

    suspend fun startSession(): OutdoorSessionEntity {
        ensureTodayLog()
        sessionDao.getActiveSession()?.let { return it }
        val id = sessionDao.insert(
            OutdoorSessionEntity(
                startMillis = System.currentTimeMillis(),
                isActive = true,
            ),
        )
        return sessionDao.getById(id)!!
    }

    suspend fun endSession(
        sessionId: Long,
        motion: SessionMotionSnapshot = SessionMotionSnapshot(),
    ): SessionResult {
        ensureProgressInitialized()
        val session = sessionDao.getById(sessionId)
            ?: return emptySessionResult()

        val endMillis = System.currentTimeMillis()
        val durationMinutes = ((endMillis - session.startMillis) / 60_000L)
            .toInt()
            .coerceAtLeast(0)
        val sessionStartHour = Instant.ofEpochMilli(session.startMillis)
            .atZone(ZoneId.systemDefault())
            .hour

        val counted = durationMinutes >= GameConstants.MIN_OUTDOOR_MINUTES
        val progress = progressDao.getProgress() ?: UserProgressEntity()
        val today = LocalDate.now().toEpochDay()
        val baseLog = ensureTodayLog()
        val alreadyTouchedToday = baseLog.touchedGrass

        var outdoorXp = 0
        var streakXp = 0
        var challengeXp = 0
        var stepsXp = 0
        var newStreak = progress.currentStreak
        var touchedGrassToday = alreadyTouchedToday
        var challengeCompleted = baseLog.challengeCompleted
        val startOfDayMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val visitedParkToday = visitedSpotDao.hasVisitedParkSince(startOfDayMillis)

        if (counted) {
            outdoorXp = ProgressCalculator.outdoorMinutesXp(durationMinutes)
            touchedGrassToday = true

            if (!alreadyTouchedToday) {
                newStreak = when (progress.lastTouchGrassEpochDay) {
                    null -> 1
                    today - 1 -> progress.currentStreak + 1
                    today -> progress.currentStreak
                    else -> 1
                }
                if (newStreak >= 2) {
                    streakXp = GameConstants.XP_DAILY_STREAK
                }
            }

            val updatedMinutes = baseLog.outdoorMinutes + durationMinutes
            val updatedSteps = baseLog.steps + motion.steps
            val updatedDistance = baseLog.distanceMeters + motion.distanceMeters

            if (!baseLog.stepsXpClaimed && updatedSteps >= GameConstants.STEPS_XP_THRESHOLD) {
                stepsXp = ProgressCalculator.stepsXp(updatedSteps)
            }

            val challenge = baseLog.challengeId?.let { id ->
                DailyChallengeCatalog.all.find { it.id == id }
            } ?: DailyChallengeCatalog.forDate()

            val newlyComplete = !baseLog.challengeCompleted &&
                DailyChallengeCatalog.isComplete(
                    challenge = challenge,
                    outdoorMinutes = updatedMinutes,
                    touchedGrassToday = true,
                    sessionStartedHour = sessionStartHour,
                    visitedParkToday = visitedParkToday,
                )
            if (newlyComplete) {
                challengeCompleted = true
                challengeXp = GameConstants.XP_CHALLENGE_COMPLETE
            }

            val updatedXp = baseLog.xpEarned + outdoorXp + streakXp + challengeXp + stepsXp
            dailyLogDao.upsert(
                baseLog.copy(
                    touchedGrass = true,
                    outdoorMinutes = updatedMinutes,
                    steps = updatedSteps,
                    distanceMeters = updatedDistance,
                    xpEarned = updatedXp,
                    challengeCompleted = challengeCompleted,
                    stepsXpClaimed = baseLog.stepsXpClaimed || stepsXp > 0,
                ),
            )
        }

        val totalXpGain = outdoorXp + streakXp + challengeXp + stepsXp
        val previousLevel = ProgressCalculator.levelFromTotalXp(progress.totalXp)
        val newTotalXp = progress.totalXp + totalXpGain
        val newLevel = ProgressCalculator.levelFromTotalXp(newTotalXp)
        val leveledUp = newLevel > previousLevel

        val updatedLog = if (counted) dailyLogDao.getForDay(today) else baseLog

        val newlyUnlocked = if (counted) {
            unlockAchievements(
                progress = progress,
                todayLog = updatedLog,
                sessionCounted = true,
                sessionDurationMinutes = durationMinutes,
                sessionStartHour = sessionStartHour,
                isRaining = motion.isRaining,
                visitedParkToday = visitedParkToday,
            )
        } else {
            emptyList()
        }

        val updatedProgress = progress.copy(
            totalXp = newTotalXp,
            currentStreak = if (counted && !alreadyTouchedToday) newStreak else progress.currentStreak,
            longestStreak = maxOf(
                progress.longestStreak,
                if (counted && !alreadyTouchedToday) newStreak else progress.currentStreak,
            ),
            lastTouchGrassEpochDay = if (counted) today else progress.lastTouchGrassEpochDay,
            totalSessionsCompleted = progress.totalSessionsCompleted +
                if (counted) 1 else 0,
            totalOutdoorMinutes = progress.totalOutdoorMinutes +
                if (counted) durationMinutes else 0,
            gardenPlotCount = ProgressCalculator.gardenPlotsForLevel(newLevel),
        )
        progressDao.update(updatedProgress)

        sessionDao.update(
            session.copy(
                endMillis = endMillis,
                durationMinutes = durationMinutes,
                sessionSteps = motion.steps,
                sessionDistanceMeters = motion.distanceMeters,
                xpAwarded = totalXpGain,
                countedForDaily = counted,
                isActive = false,
            ),
        )

        return SessionResult(
            durationMinutes = durationMinutes,
            sessionSteps = motion.steps,
            sessionDistanceMeters = motion.distanceMeters,
            xpEarned = totalXpGain,
            outdoorXp = outdoorXp,
            streakXp = streakXp,
            challengeXp = challengeXp,
            stepsXp = stepsXp,
            touchedGrassToday = touchedGrassToday,
            countedThisSession = counted,
            newStreak = updatedProgress.currentStreak,
            leveledUp = leveledUp,
            newLevel = newLevel,
            levelTitleRes = LevelTitles.titleResForLevel(newLevel),
            gardenPlots = updatedProgress.gardenPlotCount,
            challengeCompleted = challengeCompleted && challengeXp > 0,
            newlyUnlockedAchievementIds = newlyUnlocked,
            newlyCollectedIds = motion.newlyCollectedIds,
            newlyVisitedSpotNames = motion.newlyVisitedSpotIds.mapNotNull { id ->
                generatedSpots.find { it.id == id }?.name
            },
        )
    }

    private suspend fun unlockAchievements(
        progress: UserProgressEntity,
        todayLog: DailyLogEntity?,
        sessionCounted: Boolean,
        sessionDurationMinutes: Int,
        sessionStartHour: Int,
        isRaining: Boolean,
        visitedParkToday: Boolean,
    ): List<String> {
        val candidates = AchievementCatalog.idsToUnlock(
            progress = progress,
            todayLog = todayLog,
            sessionCounted = sessionCounted,
            sessionDurationMinutes = sessionDurationMinutes,
            sessionStartHour = sessionStartHour,
            isRaining = isRaining,
            visitedParkToday = visitedParkToday,
        )
        val newlyUnlocked = mutableListOf<String>()
        val now = System.currentTimeMillis()
        for (id in candidates) {
            if (!achievementDao.isUnlocked(id)) {
                achievementDao.insert(AchievementEntity(id = id, unlockedAtMillis = now))
                newlyUnlocked.add(id)
            }
        }
        return newlyUnlocked
    }

    private fun emptySessionResult() = SessionResult(
        durationMinutes = 0,
        sessionSteps = 0,
        sessionDistanceMeters = 0,
        xpEarned = 0,
        outdoorXp = 0,
        streakXp = 0,
        challengeXp = 0,
        stepsXp = 0,
        touchedGrassToday = false,
        countedThisSession = false,
        newStreak = 0,
        leveledUp = false,
        newLevel = 1,
        levelTitleRes = LevelTitles.titleResForLevel(1),
        gardenPlots = 1,
        challengeCompleted = false,
        newlyUnlockedAchievementIds = emptyList(),
        newlyCollectedIds = emptyList(),
        newlyVisitedSpotNames = emptyList(),
    )
}
