package com.example.touchgrassirl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.touchgrassirl.data.local.entity.VisitedSpotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitedSpotDao {

    @Query("SELECT spotId FROM visited_spots")
    fun observeVisitedIds(): Flow<List<String>>

    @Query("SELECT spotId FROM visited_spots")
    suspend fun getVisitedIds(): List<String>

    @Query(
        "SELECT COUNT(*) > 0 FROM visited_spots WHERE spotType = 'PARK' AND visitedAtMillis >= :sinceMillis",
    )
    suspend fun hasVisitedParkSince(sinceMillis: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: VisitedSpotEntity): Long
}
