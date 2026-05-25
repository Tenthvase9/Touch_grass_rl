package com.example.touchgrassirl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.touchgrassirl.data.local.entity.CollectedCollectibleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedCollectibleDao {

    @Query("SELECT * FROM collected_collectibles ORDER BY collectedAtMillis DESC")
    fun observeAll(): Flow<List<CollectedCollectibleEntity>>

    @Query("SELECT id FROM collected_collectibles")
    suspend fun getCollectedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CollectedCollectibleEntity): Long
}
