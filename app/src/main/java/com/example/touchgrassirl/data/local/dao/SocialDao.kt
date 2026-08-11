package com.example.touchgrassirl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.touchgrassirl.data.local.entity.ActivityEntity
import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.local.entity.GiftEntity
import com.example.touchgrassirl.data.local.entity.MyProfileEntity
import com.example.touchgrassirl.data.local.entity.PendingRequestEntity
import com.example.touchgrassirl.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {

    @Query("SELECT * FROM my_profile WHERE id = 1")
    fun observeMyProfile(): Flow<MyProfileEntity?>

    @Query("SELECT * FROM my_profile WHERE id = 1")
    suspend fun getMyProfile(): MyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMyProfile(profile: MyProfileEntity)

    @Query("SELECT * FROM friends ORDER BY totalOutdoorMinutes DESC")
    fun observeFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE profileId = :profileId")
    suspend fun getFriend(profileId: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFriend(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE profileId = :profileId")
    suspend fun deleteFriend(profileId: String)

    @Query("SELECT * FROM pending_requests")
    fun observePendingRequests(): Flow<List<PendingRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingRequest(request: PendingRequestEntity)

    @Query("DELETE FROM pending_requests WHERE profileId = :profileId")
    suspend fun deletePendingRequest(profileId: String)

    @Query("SELECT * FROM gifts ORDER BY sentAtMillis DESC")
    fun observeGifts(): Flow<List<GiftEntity>>

    @Query("SELECT * FROM gifts WHERE toProfileId = :myProfileId AND claimed = 0")
    fun observeUnclaimedGifts(myProfileId: String): Flow<List<GiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGift(gift: GiftEntity)

    @Query("UPDATE gifts SET claimed = 1 WHERE id = :giftId")
    suspend fun claimGift(giftId: String)

    @Query("SELECT * FROM activities ORDER BY timestampMillis DESC LIMIT 50")
    fun observeActivities(): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE timestampMillis < :olderThanMillis")
    suspend fun pruneOldActivities(olderThanMillis: Long)

    @Insert
    suspend fun enqueueSync(operation: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY createdAtMillis ASC LIMIT 10")
    suspend fun getPendingSyncs(): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun removeSync(id: Long)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)
}
