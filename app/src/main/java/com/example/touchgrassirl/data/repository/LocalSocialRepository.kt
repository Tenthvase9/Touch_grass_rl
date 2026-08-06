package com.example.touchgrassirl.data.repository

import com.example.touchgrassirl.data.local.dao.SocialDao
import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.local.entity.GiftEntity
import com.example.touchgrassirl.data.local.entity.MyProfileEntity
import com.example.touchgrassirl.data.local.entity.PendingRequestEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalSocialRepository(
    private val socialDao: SocialDao,
) : SocialRepository {

    override fun observeFriends(): Flow<List<FriendEntity>> = socialDao.observeFriends()

    override fun observePendingRequests(): Flow<List<PendingRequestInfo>> =
        socialDao.observePendingRequests().map { list ->
            list.filter { it.direction == "incoming" }.map {
                PendingRequestInfo(
                    uid = it.profileId,
                    profileId = it.profileId,
                    displayName = it.displayName,
                )
            }
        }

    override fun observeGifts(): Flow<List<GiftEntity>> = socialDao.observeGifts()

    override suspend fun ensureProfileCreated(): String {
        val existing = socialDao.getMyProfile()
        if (existing != null) return existing.profileId
        val profile = MyProfileEntity(
            profileId = generateProfileId(),
            displayName = "Nature Explorer",
        )
        socialDao.upsertMyProfile(profile)
        return profile.profileId
    }

    override suspend fun getMyProfileId(): String {
        return socialDao.getMyProfile()?.profileId ?: ensureProfileCreated()
    }

    override suspend fun sendFriendRequest(targetProfileId: String) {
        if (targetProfileId.isBlank()) return
        val myProfile = socialDao.getMyProfile() ?: return
        if (targetProfileId == myProfile.profileId) return

        val existing = socialDao.getFriend(targetProfileId)
        if (existing != null) return

        socialDao.upsertPendingRequest(
            PendingRequestEntity(
                profileId = targetProfileId,
                displayName = "User-${targetProfileId.takeLast(5)}",
                direction = "outgoing",
            )
        )

        socialDao.upsertFriend(
            FriendEntity(
                profileId = targetProfileId,
                displayName = "User-${targetProfileId.takeLast(5)}",
                status = "pending",
            )
        )
    }

    override suspend fun acceptFriendRequest(friendUid: String) {
        val friend = socialDao.getFriend(friendUid) ?: return
        socialDao.upsertFriend(friend.copy(status = "accepted"))
        socialDao.deletePendingRequest(friendUid)
    }

    override suspend fun declineFriendRequest(friendUid: String) {
        socialDao.deleteFriend(friendUid)
        socialDao.deletePendingRequest(friendUid)
    }

    override suspend fun sendGift(toProfileId: String, giftType: String, message: String) {
        val myProfile = socialDao.getMyProfile() ?: return
        val gift = GiftEntity(
            id = UUID.randomUUID().toString(),
            fromProfileId = myProfile.profileId,
            fromDisplayName = myProfile.displayName,
            toProfileId = toProfileId,
            giftType = giftType,
            message = message,
        )
        socialDao.upsertGift(gift)
    }

    override suspend fun claimGift(giftId: String) {
        socialDao.claimGift(giftId)
    }

    private fun generateProfileId(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val prefix = "GRASS"
        val code = (1..6).map { chars.random() }.joinToString("")
        return "$prefix-$code"
    }
}
