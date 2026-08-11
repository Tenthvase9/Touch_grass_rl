package com.example.touchgrassirl.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.touchgrassirl.data.local.dao.AchievementDao
import com.example.touchgrassirl.data.local.dao.CollectedCollectibleDao
import com.example.touchgrassirl.data.local.dao.DailyLogDao
import com.example.touchgrassirl.data.local.dao.OutdoorSessionDao
import com.example.touchgrassirl.data.local.dao.SocialDao
import com.example.touchgrassirl.data.local.dao.UserProgressDao
import com.example.touchgrassirl.data.local.dao.VisitedSpotDao
import com.example.touchgrassirl.data.local.entity.AchievementEntity
import com.example.touchgrassirl.data.local.entity.ActivityEntity
import com.example.touchgrassirl.data.local.entity.CollectedCollectibleEntity
import com.example.touchgrassirl.data.local.entity.DailyLogEntity
import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.local.entity.GiftEntity
import com.example.touchgrassirl.data.local.entity.MyProfileEntity
import com.example.touchgrassirl.data.local.entity.OutdoorSessionEntity
import com.example.touchgrassirl.data.local.entity.PendingRequestEntity
import com.example.touchgrassirl.data.local.entity.SyncQueueEntity
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
        FriendEntity::class,
        PendingRequestEntity::class,
        GiftEntity::class,
        ActivityEntity::class,
        MyProfileEntity::class,
        SyncQueueEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class TouchGrassDatabase : RoomDatabase() {

    abstract fun userProgressDao(): UserProgressDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun outdoorSessionDao(): OutdoorSessionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun visitedSpotDao(): VisitedSpotDao
    abstract fun collectedCollectibleDao(): CollectedCollectibleDao
    abstract fun socialDao(): SocialDao

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
                    .addMigrations(MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE friends ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                database.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, operation TEXT NOT NULL, data TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, retryCount INTEGER NOT NULL DEFAULT 0)")
            }
        }
    }
}
