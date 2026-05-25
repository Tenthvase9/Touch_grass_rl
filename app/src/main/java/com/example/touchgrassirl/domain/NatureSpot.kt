package com.example.touchgrassirl.domain

enum class NatureSpotType {
    PARK,
    TRAIL,
    BEACH,
    GARDEN,
}

data class NatureSpot(
    val id: String,
    val name: String,
    val type: NatureSpotType,
    val latitude: Double,
    val longitude: Double,
    val visited: Boolean = false,
)

object NatureSpotGenerator {

    private val spotTemplates = listOf(
        Triple("park", "Meadow Park", NatureSpotType.PARK),
        Triple("trail", "Pine Loop Trail", NatureSpotType.TRAIL),
        Triple("garden", "Community Garden", NatureSpotType.GARDEN),
        Triple("beach", "Lakeside Shore", NatureSpotType.BEACH),
        Triple("park2", "Oak Grove Park", NatureSpotType.PARK),
        Triple("trail2", "Ridge Path", NatureSpotType.TRAIL),
    )

    /** Places nature spots in a ring ~0.6–1.2 km around the user's location. */
    fun generateNear(latitude: Double, longitude: Double): List<NatureSpot> {
        val metersPerDegreeLat = 111_320.0
        val metersPerDegreeLng = metersPerDegreeLat * kotlin.math.cos(Math.toRadians(latitude))

        return spotTemplates.mapIndexed { index, (id, name, type) ->
            val bearingRad = Math.toRadians((index * 60.0))
            val distanceMeters = 650.0 + (index * 90.0)
            val dLat = (distanceMeters * kotlin.math.cos(bearingRad)) / metersPerDegreeLat
            val dLng = (distanceMeters * kotlin.math.sin(bearingRad)) / metersPerDegreeLng
            NatureSpot(
                id = id,
                name = name,
                type = type,
                latitude = latitude + dLat,
                longitude = longitude + dLng,
            )
        }
    }

    fun distanceMeters(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
    ): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(toLat - fromLat)
        val dLng = Math.toRadians(toLng - fromLng)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(fromLat)) * kotlin.math.cos(Math.toRadians(toLat)) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
}
