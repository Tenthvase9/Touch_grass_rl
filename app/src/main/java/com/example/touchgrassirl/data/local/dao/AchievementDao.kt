package com.example.touchgrassirl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.touchgrassirl.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY unlockedAtMillis DESC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT id FROM achievements")
    suspend fun getUnlockedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AchievementEntity): Long

    @Query("SELECT COUNT(*) > 0 FROM achievements WHERE id = :id")
    suspend fun isUnlocked(id: String): Boolean
}
