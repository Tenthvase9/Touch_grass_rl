package com.example.touchgrassirl.data.repository

import android.content.Context
import android.util.Log
import com.example.touchgrassirl.data.local.entity.ActivityEntity
import com.example.touchgrassirl.data.local.entity.ChallengeEntity
import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.local.entity.GiftEntity
import com.example.touchgrassirl.data.local.entity.LocationEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Self-hosted PocketBase backend over plain HTTP (OkHttp + org.json only).
 *
 * Server schema fields coded against:
 *  - users (auth collection): name(text), profile_id(text unique), bio(text), avatar(text),
 *    outdoor_minutes(number), streak(number), longest_streak(number), level(number),
 *    weather_badges(json)
 *  - friends: requester(users rel), addressee(users rel), status(text), created(autodate)
 *  - gifts: from_user(users rel), to_user(users rel), gift_type(text), message(text), claimed(bool)
 *  - activities: owner(users rel), type(text), message(text), created(autodate)
 *  - locations: owner(users rel), latitude(number), longitude(number), timestamp(number)
 *  - challenges: owner(users rel), title(text), description(text), goal_minutes(number),
 *    end_date(number ms), participants(json array of user ids), progress(json map user_id->minutes)
 */
class PocketBaseSocialRepository(context: Context) : SocialRepository {

    private val prefs = context.getSharedPreferences("touch_grass_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var authToken: String? = null

    @Volatile
    private var myUserId: String? = null

    private val authMutex = Mutex()

    // ---------------------------------------------------------------- auth

    private suspend fun token(): String {
        authToken?.let { return it }
        return authMutex.withLock {
            authToken?.let { return it }
            withContext(Dispatchers.IO) { authenticate() }
        }
    }

    private fun authenticate(): String {
        val savedEmail = prefs.getString("pb_email", null)
        val savedPassword = prefs.getString("pb_password", null)
        if (savedEmail != null && savedPassword != null) {
            try {
                val response = post(
                    "/api/collections/users/auth-with-password",
                    JSONObject()
                        .put("identity", savedEmail)
                        .put("password", savedPassword)
                )
                if (response.code in 200..299) {
                    val json = response.use { resp -> JSONObject(resp.body!!.string()) }
                    val record = json.getJSONObject("record")
                    myUserId = record.getString("id")
                    authToken = json.getString("token")
                    return authToken!!
                }
                Log.w(TAG, "auth-with-password failed (${response.code}); recreating device user")
            } catch (e: IOException) {
                Log.w(TAG, "auth-with-password network error", e)
                throw e
            }
        }
        return createDeviceUser()
    }

    private fun createDeviceUser(): String {
        val email = "device_${randomHex(12)}@touchgrass.local"
        val password = randomSecureChars(10)
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("passwordConfirm", password)
            .put("name", "Nature Explorer")
        var attempt = 0
        while (true) {
            val profileId = generateProfileId()
            body.put("profile_id", profileId)
            val response = post("/api/collections/users/records", body)
            if (response.code in 200..299) {
                val json = response.use { resp -> JSONObject(resp.body!!.string()) }
                val record = json.getJSONObject("record")
                val id = record.getString("id")
                prefs.edit()
                    .putString("pb_email", email)
                    .putString("pb_password", password)
                    .putString("pb_user_id", id)
                    .putString("pb_profile_id", record.optString("profile_id", profileId))
                    .apply()
                myUserId = id
                val createdToken = json.optString("token").ifBlank { null }
                if (createdToken != null) {
                    authToken = createdToken
                    return authToken!!
                }
                return authenticate()
            }
            // PocketBase uniqueness error on profile_id: {"data":{"profile_id":{"code":"validation_not_unique"}}}
            val profileIdNotUnique = response.code == 400 && attempt++ < 1 && response.use { resp ->
                JSONObject(resp.body!!.string())
                    .optJSONObject("data")
                    ?.optJSONObject("profile_id") != null
            }
            if (profileIdNotUnique) continue
            throw IOException("PocketBase user creation failed: HTTP ${response.code}")
        }
    }

    // ------------------------------------------------------------- helpers

    private fun post(path: String, json: JSONObject): okhttp3.Response =
        client.newCall(request(path, "POST", json)).execute()

    private fun request(path: String, method: String, json: JSONObject?): Request =
        Request.Builder()
            .url("$BASE_URL$path".toHttpUrl())
            .method(
                method,
                json?.toString()?.toRequestBody("application/json".toMediaType())
            )
            .build()

    /** Runs an HTTP call; on 401 re-authenticates once and retries the same request. */
    private suspend fun authorized(request: Request): okhttp3.Response {
        val first = send(token(), request)
        if (first.code == 401) {
            authToken = null
            return send(token(), request)
        }
        return first
    }

    private suspend fun send(token: String, request: Request): okhttp3.Response =
        withContext(Dispatchers.IO) {
            client.newCall(request.newBuilder().header("Authorization", token).build()).execute()
        }

    private fun listUrl(
        collection: String,
        filter: String? = null,
        sort: String? = null,
        perPage: Int? = null,
        expand: String? = null,
    ) = "$BASE_URL/api/collections/$collection/records".toHttpUrl().newBuilder().apply {
        filter?.let { addQueryParameter("filter", it) }
        sort?.let { addQueryParameter("sort", it) }
        perPage?.let { addQueryParameter("perPage", it.toString()) }
        expand?.let { addQueryParameter("expand", it) }
    }.build()

    private fun parseItems(json: String): List<JSONObject> =
        JSONObject(json).optJSONArray("items")?.let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } ?: emptyList()

    // ------------------------------------------------------------- records

    private suspend fun listRecords(
        collection: String,
        filter: String? = null,
        sort: String? = null,
        perPage: Int? = null,
        expand: String? = null,
    ): List<JSONObject> {
        val response = authorized(Request.Builder().url(listUrl(collection, filter, sort, perPage, expand)).get().build())
        response.body?.let { body ->
            if (response.code in 200..299) {
                body.use { return@listRecords parseItems(it.string()) }
            }
        }
        response.close()
        throw IOException("PocketBase list $collection failed: HTTP ${response.code}")
    }

    private suspend fun createRecord(collection: String, body: JSONObject): JSONObject =
        withContext(Dispatchers.IO) {
            val response = authorized(request("/api/collections/$collection/records", "POST", body))
            val text = response.body?.string().orEmpty()
            if (response.code !in 200..299) {
                throw IOException("PocketBase create $collection failed (${response.code}): $text")
            }
            JSONObject(text)
        }

    private suspend fun patchRecord(collection: String, id: String, body: JSONObject): JSONObject =
        withContext(Dispatchers.IO) {
            val response = authorized(request("/api/collections/$collection/records/$id", "PATCH", body))
            val text = response.body?.string().orEmpty()
            if (response.code !in 200..299) {
                throw IOException("PocketBase update $collection/$id failed (${response.code}): $text")
            }
            JSONObject(text)
        }

    private suspend fun deleteRecord(collection: String, id: String) {
        withContext(Dispatchers.IO) {
            val response = authorized(request("/api/collections/$collection/records/$id", "DELETE", null))
            response.close()
            if (response.code !in 200..299) throw IOException("PocketBase delete $collection/$id: HTTP ${response.code}")
        }
    }

    private suspend fun getMyUser(): JSONObject {
        token()
        val id = myUserId
            ?: throw IOException("no user id after auth")
        val response = authorized(request("/api/collections/users/records/$id", "GET", null))
        val text = response.body?.string().orEmpty()
        if (response.code !in 200..299) {
            throw IOException("PocketBase fetch my user failed: HTTP ${response.code}")
        }
        return JSONObject(text)
    }

    private suspend fun setMyProfileId(): String {
        val me = getMyUser()
        val existing = me.optString("profile_id")
        if (existing.isNotBlank()) return existing
        var attempt = 0
        while (true) {
            val profileId = generateProfileId()
            try {
                val updated = patchRecord("users", me.getString("id"), JSONObject().put("profile_id", profileId))
                prefs.edit().putString("pb_profile_id", updated.optString("profile_id", profileId)).apply()
                return updated.optString("profile_id", profileId)
            } catch (e: IOException) {
                // PocketBase uniqueness body: {"data":{"profile_id":{"code":"validation_not_unique"}}}
                val notUnique = e.message?.contains("validation_not_unique") == true
                if (notUnique && attempt++ < 1) continue
                throw e
            }
        }
    }

    private fun generateProfileId(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val sb = StringBuilder("GRASS-")
        val rnd = SecureRandom()
        repeat(6) { sb.append(chars[rnd.nextInt(chars.length)]) }
        return sb.toString()
    }

    private fun randomHex(length: Int): String {
        val hex = "0123456789abcdef"
        val sb = StringBuilder(length)
        val rnd = SecureRandom()
        repeat(length) { sb.append(hex[rnd.nextInt(hex.length)]) }
        return sb.toString()
    }

    private fun randomSecureChars(length: Int): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
        val sb = StringBuilder(length)
        val rnd = SecureRandom()
        repeat(length) { sb.append(chars[rnd.nextInt(chars.length)]) }
        return sb.toString()
    }

    // PocketBase autodate arrives as "YYYY-MM-DD[ T]HH:MM:SS(.SSS)Z".
    // shortcut: manual SimpleDateFormat parse instead of java.time/OffsetDateTime, which needs
    // core library desugaring on this project's minSdk 24; upgrade to java.time if desugaring added.
    private fun pbDateMillis(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return try {
            val normalized = raw.trim().replace(' ', 'T').removeSuffix("Z")
                .let { if (it.contains('.')) it else "$it.000" }
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(normalized)?.time ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "failed to parse PocketBase date '$raw'", e)
            0L
        }
    }

    /** shortcut: 30s polling instead of PocketBase SSE realtime, upgrade when live updates needed */
    private fun <T> poll(block: suspend () -> List<T>): Flow<List<T>> = flow {
        while (true) {
            try {
                emit(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "poll failed", e)
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun mapToJSONObject(map: Map<String, Int>): JSONObject {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json
    }

    private fun badgesOf(me: JSONObject): Map<String, Int> =
        me.optJSONObject("weather_badges")?.let { obj ->
            buildMap {
                obj.keys().forEach { key -> put(key, obj.optInt(key, 0)) }
            }
        } ?: emptyMap()

    private fun friendFromRecord(myId: String, rec: JSONObject): FriendEntity? {
        val expand = rec.optJSONObject("expand") ?: return null
        val requester = expand.optJSONObject("requester")
        val addressee = expand.optJSONObject("addressee")
        // "other" party: names always come live from the users record (fixes the old
        // denormalized-name bug where the target's own name was stored on their friend doc)
        val other = if (requester?.optString("id") == myId) addressee else requester ?: addressee
        other ?: return null
        return FriendEntity(
            profileId = other.optString("profile_id"),
            uid = other.optString("id"),
            displayName = other.optString("name"),
            totalOutdoorMinutes = other.optInt("outdoor_minutes", 0),
            currentStreak = other.optInt("streak", 0),
            level = other.optInt("level", 1),
            status = "accepted",
        )
    }

    private fun giftFromRecord(rec: JSONObject): GiftEntity {
        val expand = rec.optJSONObject("expand")
        val from = expand?.optJSONObject("from_user")
        val to = expand?.optJSONObject("to_user")
        return GiftEntity(
            id = rec.optString("id"),
            fromProfileId = from?.optString("profile_id").orEmpty(),
            fromDisplayName = from?.optString("name").orEmpty(),
            toProfileId = to?.optString("profile_id").orEmpty(),
            giftType = rec.optString("gift_type"),
            message = rec.optString("message"),
            sentAtMillis = pbDateMillis(rec.optString("created")),
            claimed = rec.optBoolean("claimed", false),
        )
    }

    private suspend fun acceptFilter(friendUid: String): List<JSONObject> {
        token()
        val me = myUserId!!
        return listRecords(
            "friends",
            filter = "((requester='$me' && addressee='$friendUid') || (addressee='$me' && requester='$friendUid'))",
            perPage = 1,
        )
    }

    // ---------------------------------------------------------- interface

    override suspend fun ensureProfileCreated(): String = try {
        token()
        setMyProfileId()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "ensureProfileCreated failed", e)
        prefs.getString("pb_profile_id", "GRASS-LOCAL") ?: "GRASS-LOCAL"
    }

    override suspend fun getMyProfileId(): String = try {
        val me = getMyUser()
        me.optString("profile_id").takeIf { it.isNotBlank() } ?: setMyProfileId()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "getMyProfileId failed; falling back to ensureProfileCreated", e)
        ensureProfileCreated()
    }

    override fun observeFriends(): Flow<List<FriendEntity>> = poll {
        token()
        val me = myUserId!!
        listRecords(
            "friends",
            filter = "(status='accepted' && (requester='$me' || addressee='$me'))",
            sort = "-created",
            expand = "requester,addressee",
        ).mapNotNull { friendFromRecord(me, it) }
    }

    override fun observePendingRequests(): Flow<List<PendingRequestInfo>> = poll {
        token()
        val me = myUserId!!
        listRecords(
            "friends",
            filter = "(status='pending' && addressee='$me')",
            sort = "-created",
            expand = "requester",
        ).mapNotNull { rec ->
            val requester = rec.optJSONObject("expand")?.optJSONObject("requester") ?: return@mapNotNull null
            PendingRequestInfo(
                uid = requester.optString("id"),
                profileId = requester.optString("profile_id"),
                displayName = requester.optString("name"),
            )
        }
    }

    override suspend fun sendFriendRequest(targetProfileId: String): Unit { try {
        token()
        val me = myUserId ?: return
        if (targetProfileId.isBlank()) return
        val target = listRecords(
            "users",
            filter = "(profile_id='$targetProfileId')",
            perPage = 1,
        ).firstOrNull() ?: return
        val targetId = target.optString("id")
        if (targetId == me || targetId.isBlank()) return
        // one record per relationship; leave any existing pending/accepted record alone
        val existing = listRecords(
            "friends",
            filter = "((requester='$me' && addressee='$targetId') || (addressee='$me' && requester='$targetId'))",
            perPage = 1,
        )
        if (existing.isEmpty()) {
            createRecord(
                "friends",
                JSONObject()
                    .put("requester", me)
                    .put("addressee", targetId)
                    .put("status", "pending"),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "sendFriendRequest failed", e)
    }
    }

    override suspend fun acceptFriendRequest(friendUid: String): Unit { try {
        acceptFilter(friendUid).forEach { rec ->
            patchRecord("friends", rec.optString("id"), JSONObject().put("status", "accepted"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "acceptFriendRequest failed", e)
    }
    }

    override suspend fun declineFriendRequest(friendUid: String): Unit { try {
        acceptFilter(friendUid).forEach { rec -> deleteRecord("friends", rec.optString("id")) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "declineFriendRequest failed", e)
    }
    }

    override suspend fun removeFriend(friendUid: String): Unit { try {
        acceptFilter(friendUid).forEach { rec -> deleteRecord("friends", rec.optString("id")) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "removeFriend failed", e)
    }
    }

    override fun observeGifts(): Flow<List<GiftEntity>> = poll {
        token()
        val me = myUserId!!
        listRecords(
            "gifts",
            filter = "(to_user='$me')",
            sort = "-created",
            expand = "from_user,to_user",
        ).map { giftFromRecord(it) }
    }

    override suspend fun sendGift(toProfileId: String, giftType: String, message: String): Unit { try {
        token()
        val me = myUserId ?: return
        if (toProfileId.isBlank()) return
        val target = listRecords(
            "users",
            filter = "(profile_id='$toProfileId')",
            perPage = 1,
        ).firstOrNull() ?: return
        val targetId = target.optString("id")
        if (targetId.isBlank() || targetId == me) return
        createRecord(
            "gifts",
            JSONObject()
                .put("from_user", me)
                .put("to_user", targetId)
                .put("gift_type", giftType)
                .put("message", message)
                .put("claimed", false),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "sendGift failed", e)
    }
    }

    override suspend fun claimGift(giftId: String): Unit { try {
        patchRecord("gifts", giftId, JSONObject().put("claimed", true))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "claimGift failed", e)
    }
    }

    override suspend fun updateDisplayName(name: String): Unit { try {
        patchMyUser(JSONObject().put("name", name))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "updateDisplayName failed", e)
    }
    }

    private suspend fun patchMyUser(body: JSONObject): JSONObject {
        token()
        val me = myUserId!!
        return patchRecord("users", me, body)
    }

    override suspend fun getMyDisplayName(): String = try {
        getMyUser().optString("name").takeUnless { it.isBlank() } ?: "Nature Explorer"
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "getMyDisplayName failed", e)
        "Nature Explorer"
    }

    override suspend fun syncMyStats(outdoorMinutes: Int, streak: Int, level: Int): Unit { try {
        patchMyUser(
            JSONObject()
                .put("outdoor_minutes", outdoorMinutes)
                .put("streak", streak)
                .put("level", level)
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "syncMyStats failed", e)
    }
    }

    override suspend fun addActivity(type: String, message: String): Unit { try {
        token()
        val me = myUserId!!
        createRecord(
            "activities",
            JSONObject()
                .put("owner", me)
                .put("type", type)
                .put("message", message),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "addActivity failed", e)
    }
    }

    override fun observeActivities(): Flow<List<ActivityEntity>> = poll {
        token()
        val me = myUserId!!
        val meRecord = getMyUser()
        val profileId = meRecord.optString("profile_id")
        val name = meRecord.optString("name")
        listRecords(
            "activities",
            filter = "(owner='$me')",
            sort = "-created",
            perPage = 50,
        ).map { rec ->
            ActivityEntity(
                id = rec.optString("id"),
                profileId = profileId,
                displayName = name,
                type = rec.optString("type"),
                message = rec.optString("message"),
                timestampMillis = pbDateMillis(rec.optString("created")),
            )
        }
    }

    override suspend fun updateProfile(bio: String, avatar: String): Unit { try {
        patchMyUser(
            JSONObject()
                .put("bio", bio)
                .put("avatar", avatar)
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "updateProfile failed", e)
    }
    }

    override suspend fun getMyProfile(): Map<String, Any?> = try {
        val me = getMyUser()
        mapOf(
            "displayName" to (me.optString("name").takeUnless { it.isBlank() }),
            "bio" to (me.optString("bio").takeUnless { it.isBlank() }),
            "avatar" to (me.optString("avatar").takeUnless { it.isBlank() }),
            "outdoorMinutes" to me.optInt("outdoor_minutes", 0),
            "streak" to me.optInt("streak", 0),
            "level" to me.optInt("level", 1),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "getMyProfile failed", e)
        emptyMap()
    }

    override suspend fun getStreak(): Pair<Int, Int> = try {
        val me = getMyUser()
        me.optInt("streak", 0) to me.optInt("longest_streak", 0)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "getStreak failed", e)
        0 to 0
    }

    override suspend fun saveLocation(latitude: Double, longitude: Double, timestamp: Long): Unit { try {
        token()
        val me = myUserId!!
        createRecord(
            "locations",
            JSONObject()
                .put("owner", me)
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("timestamp", timestamp),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "saveLocation failed", e)
    }
    }

    override fun observeLocations(): Flow<List<LocationEntity>> = poll {
        token()
        val me = myUserId!!
        listRecords(
            "locations",
            filter = "(owner='$me')",
            sort = "-timestamp",
            perPage = 100,
        ).map { rec ->
            LocationEntity(
                id = rec.optString("id"),
                latitude = rec.optDouble("latitude", 0.0),
                longitude = rec.optDouble("longitude", 0.0),
                timestampMillis = rec.optLong("timestamp", 0L),
            )
        }
    }

    override suspend fun updateWeatherBadges(badges: Map<String, Int>): Unit { try {
        patchMyUser(JSONObject().put("weather_badges", mapToJSONObject(badges)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "updateWeatherBadges failed", e)
    }
    }

    override suspend fun getWeatherBadges(): Map<String, Int> = try {
        badgesOf(getMyUser())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "getWeatherBadges failed", e)
        emptyMap()
    }

    override suspend fun getAllFriendsStats(): List<FriendEntity> = try {
        token()
        val me = myUserId!!
        listRecords(
            "friends",
            filter = "(status='accepted' && (requester='$me' || addressee='$me'))",
            sort = "-created",
            expand = "requester,addressee",
        ).mapNotNull { friendFromRecord(me, it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "getAllFriendsStats failed", e)
        emptyList()
    }

    override suspend fun createChallenge(title: String, description: String, goalMinutes: Int, endDate: Long): Unit { try {
        token()
        val me = myUserId!!
        createRecord(
            "challenges",
            JSONObject()
                .put("owner", me)
                .put("title", title)
                .put("description", description)
                .put("goal_minutes", goalMinutes)
                .put("end_date", endDate)
                .put("participants", JSONArray(listOf(me)))
                .put("progress", JSONObject().put(me, 0)),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "createChallenge failed", e)
    }
    }

    private fun challengeFromRecord(rec: JSONObject): ChallengeEntity {
        val participants = rec.optJSONArray("participants")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                (arr.opt(i) as? String)?.takeIf { it.isNotBlank() }
            }
        } ?: emptyList()
        val progress = rec.optJSONObject("progress")?.let { obj ->
            buildMap {
                obj.keys().forEach { key -> put(key, obj.optInt(key, 0)) }
            }
        } ?: emptyMap()
        return ChallengeEntity(
            id = rec.optString("id"),
            title = rec.optString("title"),
            description = rec.optString("description"),
            goalMinutes = rec.optInt("goal_minutes", 0),
            endDate = rec.optLong("end_date", 0L),
            createdBy = rec.optString("owner"),
            participants = participants,
            progress = progress,
        )
    }

    override fun observeChallenges(): Flow<List<ChallengeEntity>> = poll {
        token()
        val me = myUserId!!
        listRecords(
            "challenges",
            filter = "(owner='$me')",
            sort = "-created",
        ).map { challengeFromRecord(it) }
    }

    override suspend fun joinChallenge(challengeId: String): Unit { try {
        token()
        val me = myUserId!!
        val response = authorized(request("/api/collections/challenges/records/$challengeId", "GET", null))
        val text = response.body?.string().orEmpty()
        response.close()
        if (response.code !in 200..299) {
            throw IOException("joinChallenge fetch failed: HTTP ${response.code}")
        }
        val rec = JSONObject(text)
        val participants = rec.optJSONArray("participants")?.let { arr ->
            (0 until arr.length()).mapNotNull { i -> (arr.opt(i) as? String) }.toMutableList()
        } ?: mutableListOf()
        val progress = rec.optJSONObject("progress") ?: JSONObject()
        if (me !in participants) {
            participants.add(me)
            progress.put(me, 0)
            patchRecord(
                "challenges", challengeId,
                JSONObject()
                    .put("participants", JSONArray(participants))
                    .put("progress", progress),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "joinChallenge failed", e)
    }
    }

    override suspend fun updateChallengeProgress(challengeId: String, minutes: Int): Unit { try {
        token()
        val me = myUserId!!
        val response = authorized(request("/api/collections/challenges/records/$challengeId", "GET", null))
        val text = response.body?.string().orEmpty()
        response.close()
        if (response.code !in 200..299) {
            throw IOException("updateChallengeProgress fetch failed: HTTP ${response.code}")
        }
        val progress = JSONObject(text).optJSONObject("progress") ?: JSONObject()
        progress.put(me, minutes)
        patchRecord("challenges", challengeId, JSONObject().put("progress", progress))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "updateChallengeProgress failed", e)
    }
    }

    override suspend fun addWeatherBadge(weatherType: String): Unit { try {
        val badges = getWeatherBadges().toMutableMap()
        badges[weatherType] = (badges[weatherType] ?: 0) + 1
        updateWeatherBadges(badges)
        addActivity("weather_badge", "Earned ${weatherType.replace("_", " ")} weather badge!")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "addWeatherBadge failed", e)
    }
    }

    companion object {
        private const val TAG = "PocketBase"
        const val BASE_URL = "http://192.168.0.170:8090"
        private const val POLL_INTERVAL_MS = 30_000L
    }
}
