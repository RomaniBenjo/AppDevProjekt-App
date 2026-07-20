package com.example.comingsoon.ui.screens.localopenguesser.connection

import com.example.comingsoon.ui.screens.localopenguesser.IndexedPhoto
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

internal const val HOME_CLUSTER_RADIUS_KM = 50.0

/**
 * Finds photos that should not be considered for a game. The inferred home country is the
 * country containing the most eligible photos. A home cluster is a connected component where
 * every connection is at most [clusterRadiusKm] apart.
 */
internal fun homePhotosToExclude(
    photos: Collection<IndexedPhoto>,
    mode: HomePhotoExclusionMode,
    clusterRadiusKm: Double = HOME_CLUSTER_RADIUS_KM
): Set<Long> {
    if (mode == HomePhotoExclusionMode.NONE) return emptySet()

    val photosByCountry = photos.asSequence()
        .filter { it.latitude != null && it.longitude != null && it.country != null }
        .groupBy { checkNotNull(it.country) }
    val homeCountry = photosByCountry.entries
        .sortedWith(compareByDescending<Map.Entry<String, List<IndexedPhoto>>> { it.value.size }
            .thenBy { it.key })
        .firstOrNull()
        ?.value
        .orEmpty()

    if (mode == HomePhotoExclusionMode.MOST_PHOTOGRAPHED_COUNTRY) {
        return homeCountry.mapTo(mutableSetOf(), IndexedPhoto::mediaId)
    }
    return largestGeographicCluster(homeCountry, clusterRadiusKm)
        .mapTo(mutableSetOf(), IndexedPhoto::mediaId)
}

private data class CartesianPhoto(
    val photo: IndexedPhoto,
    val x: Double,
    val y: Double,
    val z: Double
)

private data class SpatialCell(val x: Int, val y: Int, val z: Int)

private fun largestGeographicCluster(
    photos: List<IndexedPhoto>,
    radiusKm: Double
): List<IndexedPhoto> {
    if (photos.isEmpty() || radiusKm <= 0.0) return emptyList()
    if (photos.size == 1) return photos

    val earthRadiusKm = 6_371.0
    val chordRadius = 2.0 * earthRadiusKm * sin(radiusKm / (2.0 * earthRadiusKm))
    val cellSize = chordRadius / sqrt(3.0)
    val points = photos.sortedBy(IndexedPhoto::mediaId).map { photo ->
        val latitude = Math.toRadians(checkNotNull(photo.latitude))
        val longitude = Math.toRadians(checkNotNull(photo.longitude))
        val latitudeRadius = earthRadiusKm * cos(latitude)
        CartesianPhoto(
            photo = photo,
            x = latitudeRadius * cos(longitude),
            y = latitudeRadius * sin(longitude),
            z = earthRadiusKm * sin(latitude)
        )
    }
    val parents = IntArray(points.size) { it }
    val sizes = IntArray(points.size) { 1 }
    val cells = mutableMapOf<SpatialCell, MutableList<Int>>()
    val cellSearchRadius = 2
    val maximumDistanceSquared = chordRadius * chordRadius

    fun find(index: Int): Int {
        var root = index
        while (parents[root] != root) root = parents[root]
        var current = index
        while (parents[current] != current) {
            val next = parents[current]
            parents[current] = root
            current = next
        }
        return root
    }

    fun union(first: Int, second: Int) {
        var firstRoot = find(first)
        var secondRoot = find(second)
        if (firstRoot == secondRoot) return
        if (sizes[firstRoot] < sizes[secondRoot]) {
            val swap = firstRoot
            firstRoot = secondRoot
            secondRoot = swap
        }
        parents[secondRoot] = firstRoot
        sizes[firstRoot] += sizes[secondRoot]
    }

    points.forEachIndexed { index, point ->
        val cell = SpatialCell(
            floor(point.x / cellSize).toInt(),
            floor(point.y / cellSize).toInt(),
            floor(point.z / cellSize).toInt()
        )
        for (xOffset in -cellSearchRadius..cellSearchRadius) {
            for (yOffset in -cellSearchRadius..cellSearchRadius) {
                for (zOffset in -cellSearchRadius..cellSearchRadius) {
                    cells[SpatialCell(cell.x + xOffset, cell.y + yOffset, cell.z + zOffset)]
                        ?.forEach { otherIndex ->
                            val other = points[otherIndex]
                            val xDistance = point.x - other.x
                            val yDistance = point.y - other.y
                            val zDistance = point.z - other.z
                            val distanceSquared = xDistance * xDistance +
                                yDistance * yDistance + zDistance * zDistance
                            if (distanceSquared <= maximumDistanceSquared) {
                                union(index, otherIndex)
                            }
                        }
                }
            }
        }
        cells.getOrPut(cell) { mutableListOf() }.add(index)
    }

    return points.indices
        .groupBy(::find)
        .values
        .sortedWith(compareByDescending<List<Int>> { it.size }
            .thenBy { component -> component.minOf { points[it].photo.mediaId } })
        .first()
        .map { points[it].photo }
}
