package com.example.touchgrassirl.data.repository

import com.example.touchgrassirl.data.local.entity.ActivityEntity
import com.example.touchgrassirl.data.local.entity.ChallengeEntity
import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.local.entity.GiftEntity
import com.example.touchgrassirl.data.local.entity.LocationEntity
import com.example.touchgrassirl.data.repository.PendingRequestInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseSocialRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : SocialRepository {
    private val uid: String
        get() = auth.currentUser!!.uid

    private fun myProfileDoc() = db.collection("users").document(uid)
    private fun friendsCollection() = myProfileDoc().collection("friends")
    private fun giftsCollection() = myProfileDoc().collection("gifts")

    override suspend fun ensureProfileCreated(): String {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
        val doc = myProfileDoc().get().await()
        if (!doc.exists()) {
            val profileId = generateProfileId()
            myProfileDoc().set(
                mapOf(
                    "profileId" to profileId,
                    "displayName" to "Nature Explorer",
                    "outdoorMinutes" to 0,
                    "streak" to 0,
                    "level" to 1,
                    "createdAt" to System.currentTimeMillis(),
                )
            ).await()
            return profileId
        }
        return doc.getString("profileId") ?: generateProfileId()
    }

    override suspend fun getMyProfileId(): String {
        val doc = myProfileDoc().get().await()
        return doc.getString("profileId") ?: ensureProfileCreated()
    }

    override fun observeFriends(): Flow<List<FriendEntity>> = callbackFlow {
        val listener = friendsCollection()
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snap, _ ->
                val friends = snap?.documents?.map { doc ->
                    FriendEntity(
                        profileId = doc.getString("profileId") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                        totalOutdoorMinutes = (doc.getLong("outdoorMinutes") ?: 0).toInt(),
                        currentStreak = (doc.getLong("streak") ?: 0).toInt(),
                        level = (doc.getLong("level") ?: 1).toInt(),
                        status = "accepted",
                    )
                } ?: emptyList()
                trySend(friends)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendFriendRequest(targetProfileId: String) {
        if (targetProfileId.isBlank()) return
        val targetDocs = db.collection("users")
            .whereEqualTo("profileId", targetProfileId)
            .get().await()
            .documents
        val targetDoc = targetDocs.firstOrNull() ?: return
        val targetUid = targetDoc.id
        if (targetUid == uid) return

        friendsCollection().document(targetUid).set(
            mapOf(
                "profileId" to targetProfileId,
                "displayName" to (targetDoc.getString("displayName") ?: "Friend"),
                "status" to "pending",
                "direction" to "outgoing",
                "sentAt" to System.currentTimeMillis(),
            )
        ).await()

        db.collection("users").document(targetUid)
            .collection("friends").document(uid)
            .set(
                mapOf(
                    "profileId" to getMyProfileId(),
                    "displayName" to "Someone",
                    "status" to "pending",
                    "direction" to "incoming",
                    "sentAt" to System.currentTimeMillis(),
                )
            ).await()
    }

    override suspend fun acceptFriendRequest(friendUid: String) {
        friendsCollection().document(friendUid)
            .update("status", "accepted").await()

        db.collection("users").document(friendUid)
            .collection("friends").document(uid)
            .update("status", "accepted").await()
    }

    override suspend fun declineFriendRequest(friendUid: String) {
        friendsCollection().document(friendUid).delete().await()
        db.collection("users").document(friendUid)
            .collection("friends").document(uid).delete().await()
    }

    override fun observePendingRequests(): Flow<List<PendingRequestInfo>> = callbackFlow {
        val listener = friendsCollection()
            .whereEqualTo("status", "pending")
            .whereEqualTo("direction", "incoming")
            .addSnapshotListener { snap, _ ->
                val requests = snap?.documents?.map { doc ->
                    PendingRequestInfo(
                        uid = doc.id,
                        profileId = doc.getString("profileId") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                    )
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    override fun observeGifts(): Flow<List<GiftEntity>> = callbackFlow {
        val listener = giftsCollection()
            .orderBy("sentAt")
            .addSnapshotListener { snap, _ ->
                val gifts = snap?.documents?.map { doc ->
                    GiftEntity(
                        id = doc.id,
                        fromProfileId = doc.getString("fromProfileId") ?: "",
                        fromDisplayName = doc.getString("fromDisplayName") ?: "",
                        toProfileId = doc.getString("toProfileId") ?: "",
                        giftType = doc.getString("giftType") ?: "",
                        message = doc.getString("message") ?: "",
                        sentAtMillis = doc.getLong("sentAt") ?: 0,
                        claimed = doc.getBoolean("claimed") ?: false,
                    )
                } ?: emptyList()
                trySend(gifts)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendGift(toProfileId: String, giftType: String, message: String) {
        val targetDocs = db.collection("users")
            .whereEqualTo("profileId", toProfileId)
            .get().await()
            .documents
        val targetDoc = targetDocs.firstOrNull() ?: return
        val targetUid = targetDoc.id
        val myProfileId = getMyProfileId()
        val myName = getMyDisplayName()

        db.collection("users").document(targetUid)
            .collection("gifts")
            .add(
                mapOf(
                    "fromProfileId" to myProfileId,
                    "fromDisplayName" to myName,
                    "toProfileId" to toProfileId,
                    "giftType" to giftType,
                    "message" to message,
                    "sentAt" to System.currentTimeMillis(),
                    "claimed" to false,
                )
            ).await()
    }

    override     suspend fun claimGift(giftId: String) {
        giftsCollection().document(giftId)
            .update("claimed", true).await()
    }

    override suspend fun updateDisplayName(name: String) {
        myProfileDoc().update("displayName", name).await()
    }

    override suspend fun getMyDisplayName(): String {
        val doc = myProfileDoc().get().await()
        return doc.getString("displayName") ?: "Nature Explorer"
    }

    override suspend fun syncMyStats(outdoorMinutes: Int, streak: Int, level: Int) {
        myProfileDoc().update(
            mapOf(
                "outdoorMinutes" to outdoorMinutes,
                "streak" to streak,
                "level" to level,
                "lastActive" to System.currentTimeMillis(),
            )
        ).await()
    }

    override suspend fun addActivity(type: String, message: String) {
        myProfileDoc().collection("activities").add(
            mapOf(
                "type" to type,
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
            )
        ).await()
    }

    override fun observeActivities(): Flow<List<ActivityEntity>> = callbackFlow {
        val listener = myProfileDoc().collection("activities")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                val activities = snap?.documents?.map { doc ->
                    ActivityEntity(
                        id = doc.id,
                        profileId = doc.getString("profileId") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                        type = doc.getString("type") ?: "",
                        message = doc.getString("message") ?: "",
                        timestampMillis = doc.getLong("timestamp") ?: 0,
                    )
                } ?: emptyList()
                trySend(activities)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateProfile(bio: String, avatar: String) {
        myProfileDoc().update(
            mapOf(
                "bio" to bio,
                "avatar" to avatar,
            )
        ).await()
    }

    override suspend fun getMyProfile(): Map<String, Any?> {
        val doc = myProfileDoc().get().await()
        return mapOf(
            "displayName" to doc.getString("displayName"),
            "bio" to doc.getString("bio"),
            "avatar" to doc.getString("avatar"),
            "outdoorMinutes" to (doc.getLong("outdoorMinutes") ?: 0),
            "streak" to (doc.getLong("streak") ?: 0),
            "level" to (doc.getLong("level") ?: 1),
        )
    }

    override suspend fun saveLocation(latitude: Double, longitude: Double, timestamp: Long) {
        myProfileDoc().collection("locations").add(
            mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "timestamp" to timestamp,
            )
        ).await()
    }

    override fun observeLocations(): Flow<List<LocationEntity>> = callbackFlow {
        val listener = myProfileDoc().collection("locations")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, _ ->
                val locations = snap?.documents?.map { doc ->
                    LocationEntity(
                        id = doc.id,
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        timestampMillis = doc.getLong("timestamp") ?: 0,
                    )
                } ?: emptyList()
                trySend(locations)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateWeatherBadges(badges: Map<String, Int>) {
        myProfileDoc().update("weatherBadges", badges).await()
    }

    override suspend fun getWeatherBadges(): Map<String, Int> {
        val doc = myProfileDoc().get().await()
        @Suppress("UNCHECKED_CAST")
        return (doc.get("weatherBadges") as? Map<String, Int>) ?: emptyMap()
    }

    suspend fun updateStreak(currentStreak: Int, longestStreak: Int) {
        myProfileDoc().update(
            mapOf(
                "currentStreak" to currentStreak,
                "longestStreak" to longestStreak,
            )
        ).await()
    }

    suspend fun getStreak(): Pair<Int, Int> {
        val doc = myProfileDoc().get().await()
        val current = (doc.getLong("currentStreak") ?: 0).toInt()
        val longest = (doc.getLong("longestStreak") ?: 0).toInt()
        return current to longest
    }

    override suspend fun getAllFriendsStats(): List<FriendEntity> {
        val friends = friendsCollection().whereEqualTo("status", "accepted").get().await()
        return friends.documents.map { doc ->
            FriendEntity(
                profileId = doc.getString("profileId") ?: "",
                displayName = doc.getString("displayName") ?: "",
                totalOutdoorMinutes = (doc.getLong("outdoorMinutes") ?: 0).toInt(),
                currentStreak = (doc.getLong("currentStreak") ?: 0).toInt(),
                level = (doc.getLong("level") ?: 1).toInt(),
                status = "accepted",
            )
        }
    }

    override suspend fun createChallenge(title: String, description: String, goalMinutes: Int, endDate: Long) {
        myProfileDoc().collection("challenges").add(
            mapOf(
                "title" to title,
                "description" to description,
                "goalMinutes" to goalMinutes,
                "endDate" to endDate,
                "createdBy" to uid,
                "createdAt" to System.currentTimeMillis(),
                "participants" to listOf(uid),
                "progress" to mapOf(uid to 0),
            )
        ).await()
    }

    override fun observeChallenges(): Flow<List<ChallengeEntity>> = callbackFlow {
        val listener = myProfileDoc().collection("challenges")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val challenges = snap?.documents?.map { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val participants = (doc.get("participants") as? List<String>) ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val progress = (doc.get("progress") as? Map<String, Long>) ?: emptyMap()
                    ChallengeEntity(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        goalMinutes = (doc.getLong("goalMinutes") ?: 0).toInt(),
                        endDate = doc.getLong("endDate") ?: 0,
                        createdBy = doc.getString("createdBy") ?: "",
                        participants = participants,
                        progress = progress.mapValues { it.value.toInt() },
                    )
                } ?: emptyList()
                trySend(challenges)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun joinChallenge(challengeId: String) {
        val doc = myProfileDoc().collection("challenges").document(challengeId).get().await()
        @Suppress("UNCHECKED_CAST")
        val participants = (doc.get("participants") as? List<String>)?.toMutableList() ?: mutableListOf()
        @Suppress("UNCHECKED_CAST")
        val progress = (doc.get("progress") as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()
        if (uid !in participants) {
            participants.add(uid)
            progress[uid] = 0
            myProfileDoc().collection("challenges").document(challengeId).update(
                mapOf("participants" to participants, "progress" to progress)
            ).await()
        }
    }

    override suspend fun updateChallengeProgress(challengeId: String, minutes: Int) {
        myProfileDoc().collection("challenges").document(challengeId)
            .update("progress.$uid", minutes.toLong()).await()
    }

    override suspend fun addWeatherBadge(weatherType: String) {
        val badges = getWeatherBadges().toMutableMap()
        badges[weatherType] = (badges[weatherType] ?: 0) + 1
        updateWeatherBadges(badges)
        addActivity("weather_badge", "Earned ${weatherType.replace("_", " ")} weather badge!")
    }

    private fun generateProfileId(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return "GRASS-" + (1..6).map { chars.random() }.joinToString("")
    }

    companion object {
        val GIFT_TYPES = listOf(
            "sunflower" to "\uD83C\uDF3B Sunflower",
            "sapling" to "\uD83C\uDF3F Sapling",
            "mushroom" to "\uD83C\uDF44 Mushroom",
            "clover" to "\u2740 Lucky Clover",
            "pine" to "\uD83C\uDF32 Pine Tree",
        )
    }
}


