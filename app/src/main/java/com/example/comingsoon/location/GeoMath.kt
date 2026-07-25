package com.example.comingsoon.location

import org.maplibre.android.geometry.LatLng
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Points on a circle of [radiusMeters] around [center], using the spherical destination-point
 * formula so the circle stays a true real-world size at any zoom level (a MapLibre style
 * `circle-radius` paint property is defined in screen pixels, which would mis-scale on zoom).
 */
fun circlePolygonPoints(center: LatLng, radiusMeters: Double, points: Int = 48): List<LatLng> {
    if (radiusMeters <= 0) return emptyList()
    val angularDistance = radiusMeters / EARTH_RADIUS_METERS
    val lat1 = Math.toRadians(center.latitude)
    val lon1 = Math.toRadians(center.longitude)

    return (0 until points).map { i ->
        val bearing = 2 * PI * i / points
        val lat2 = asin(
            sin(lat1) * cos(angularDistance) + cos(lat1) * sin(angularDistance) * cos(bearing)
        )
        val lon2 = lon1 + atan2(
            sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2)
        )
        LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }
}
