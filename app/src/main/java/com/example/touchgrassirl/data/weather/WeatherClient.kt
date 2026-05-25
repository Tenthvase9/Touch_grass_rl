package com.example.touchgrassirl.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WeatherClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
) {

    suspend fun isRaining(latitude: Double, longitude: Double): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val url =
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=precipitation,rain,showers&timezone=auto"
            val request = Request.Builder().url(url).build()
            val body = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                response.body?.string().orEmpty()
            }
            val current = JSONObject(body).optJSONObject("current") ?: return@withContext false
            val precipitation = current.optDouble("precipitation", 0.0)
            val rain = current.optDouble("rain", 0.0)
            val showers = current.optDouble("showers", 0.0)
            precipitation > 0.1 || rain > 0.0 || showers > 0.0
        }.getOrDefault(false)
    }
}
