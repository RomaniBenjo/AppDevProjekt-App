package com.example.commingsoon.ui.screens.localopenguesser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Resolves GPS coordinates without Android's potentially network-backed Geocoder. */
internal class OfflineCountryResolver private constructor(
    private val countries: List<CountryShape>
) {
    fun countryAt(latitude: Double, longitude: Double): String? = countries
        .asSequence()
        .filter { it.bounds.contains(latitude, longitude) }
        .firstOrNull { country -> country.polygons.any { it.contains(latitude, longitude) } }
        ?.name

    companion object {
        private const val COUNTRY_DATA_ASSET = "maps/countries_110m.geojson"

        fun load(context: Context): OfflineCountryResolver {
            val json = context.assets.open(COUNTRY_DATA_ASSET).bufferedReader().use { it.readText() }
            val features = JSONObject(json).getJSONArray("features")
            val countries = buildList {
                for (index in 0 until features.length()) {
                    val feature = features.getJSONObject(index)
                    val geometry = feature.getJSONObject("geometry")
                    val polygons = geometry.toPolygons()
                    if (polygons.isNotEmpty()) {
                        add(
                            CountryShape(
                                name = feature.getJSONObject("properties").optString("ADMIN", "Unknown"),
                                bounds = feature.optJSONArray("bbox")?.toBounds()
                                    ?: Bounds.from(polygons),
                                polygons = polygons
                            )
                        )
                    }
                }
            }
            return OfflineCountryResolver(countries)
        }
    }
}

private data class CountryShape(
    val name: String,
    val bounds: Bounds,
    val polygons: List<Polygon>
)

private data class Coordinate(val longitude: Double, val latitude: Double)

private data class Bounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double
) {
    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in south..north && longitude in west..east

    companion object {
        fun from(polygons: List<Polygon>): Bounds {
            val points = polygons.flatMap { polygon -> polygon.rings.flatten() }
            return Bounds(
                west = points.minOf { it.longitude },
                south = points.minOf { it.latitude },
                east = points.maxOf { it.longitude },
                north = points.maxOf { it.latitude }
            )
        }
    }
}

private data class Polygon(val rings: List<List<Coordinate>>) {
    fun contains(latitude: Double, longitude: Double): Boolean {
        val exterior = rings.firstOrNull() ?: return false
        if (!exterior.containsPoint(latitude, longitude)) return false
        return rings.drop(1).none { it.containsPoint(latitude, longitude) }
    }
}

private fun List<Coordinate>.containsPoint(latitude: Double, longitude: Double): Boolean {
    if (size < 3) return false
    var inside = false
    var previous = last()
    for (current in this) {
        val crossesLatitude = (current.latitude > latitude) != (previous.latitude > latitude)
        if (crossesLatitude) {
            val crossingLongitude = (previous.longitude - current.longitude) *
                (latitude - current.latitude) / (previous.latitude - current.latitude) +
                current.longitude
            if (longitude < crossingLongitude) inside = !inside
        }
        previous = current
    }
    return inside
}

private fun JSONObject.toPolygons(): List<Polygon> {
    val coordinates = getJSONArray("coordinates")
    return when (getString("type")) {
        "Polygon" -> listOf(coordinates.toPolygon())
        "MultiPolygon" -> buildList {
            for (index in 0 until coordinates.length()) {
                add(coordinates.getJSONArray(index).toPolygon())
            }
        }
        else -> emptyList()
    }
}

private fun JSONArray.toPolygon(): Polygon = Polygon(
    rings = buildList {
        for (ringIndex in 0 until length()) {
            val ring = getJSONArray(ringIndex)
            add(buildList {
                for (pointIndex in 0 until ring.length()) {
                    val point = ring.getJSONArray(pointIndex)
                    add(Coordinate(point.getDouble(0), point.getDouble(1)))
                }
            })
        }
    }
)

private fun JSONArray.toBounds() = Bounds(
    west = getDouble(0),
    south = getDouble(1),
    east = getDouble(2),
    north = getDouble(3)
)
