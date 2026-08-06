package com.example.touchgrassirl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.touchgrassirl.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {

    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun observeProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getProgress(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: UserProgressEntity): Long

    @Update
    suspend fun update(progress: UserProgressEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserProgressEntity): Long
}
