package com.example.touchgrassirl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.touchgrassirl.data.local.entity.OutdoorSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutdoorSessionDao {

    @Query("SELECT * FROM outdoor_sessions WHERE isActive = 1 LIMIT 1")
    fun observeActiveSession(): Flow<OutdoorSessionEntity?>

    @Query("SELECT * FROM outdoor_sessions WHERE id = :id")
    suspend fun getById(id: Long): OutdoorSessionEntity?

    @Query("SELECT * FROM outdoor_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): OutdoorSessionEntity?

    @Insert
    suspend fun insert(session: OutdoorSessionEntity): Long

    @Update
    suspend fun update(session: OutdoorSessionEntity): Int
}
