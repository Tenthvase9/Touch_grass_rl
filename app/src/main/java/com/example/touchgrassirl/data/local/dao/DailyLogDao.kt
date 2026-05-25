package com.example.touchgrassirl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.touchgrassirl.data.local.entity.DailyLogEntity

@Dao
interface DailyLogDao {

    @Query("SELECT * FROM daily_logs WHERE dateEpochDay = :epochDay")
    suspend fun getForDay(epochDay: Long): DailyLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyLogEntity): Long
}
