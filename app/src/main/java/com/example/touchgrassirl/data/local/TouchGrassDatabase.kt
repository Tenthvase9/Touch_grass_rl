package com.example.touchgrassirl.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.touchgrassirl.data.local.dao.AchievementDao
import com.example.touchgrassirl.data.local.dao.CollectedCollectibleDao
import com.example.touchgrassirl.data.local.dao.DailyLogDao
import com.example.touchgrassirl.data.local.dao.OutdoorSessionDao
import com.example.touchgrassirl.data.local.dao.UserProgressDao
import com.example.touchgrassirl.data.local.dao.VisitedSpotDao
import com.example.touchgrassirl.data.local.entity.AchievementEntity
import com.example.touchgrassirl.data.local.entity.CollectedCollectibleEntity
import com.example.touchgrassirl.data.local.entity.DailyLogEntity
import com.example.touchgrassirl.data.local.entity.OutdoorSessionEntity
import com.example.touchgrassirl.data.local.entity.UserProgressEntity
import com.example.touchgrassirl.data.local.entity.VisitedSpotEntity

@Database(
    entities = [
        UserProgressEntity::class,
        DailyLogEntity::class,
        OutdoorSessionEntity::class,
        AchievementEntity::class,
        VisitedSpotEntity::class,
        CollectedCollectibleEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class TouchGrassDatabase : RoomDatabase() {

    abstract fun userProgressDao(): UserProgressDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun outdoorSessionDao(): OutdoorSessionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun visitedSpotDao(): VisitedSpotDao
    abstract fun collectedCollectibleDao(): CollectedCollectibleDao

    companion object {
        @Volatile
        private var instance: TouchGrassDatabase? = null

        fun getInstance(context: Context): TouchGrassDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TouchGrassDatabase::class.java,
                    "touch_grass.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
