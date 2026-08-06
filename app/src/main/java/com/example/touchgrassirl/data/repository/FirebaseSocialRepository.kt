package com.example.touchgrassirl.data.repository

import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.local.entity.GiftEntity
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


