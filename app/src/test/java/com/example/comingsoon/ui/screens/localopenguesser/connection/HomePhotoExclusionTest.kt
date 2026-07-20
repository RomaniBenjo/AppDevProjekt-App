package com.example.comingsoon.ui.screens.localopenguesser.connection

import com.example.comingsoon.ui.screens.localopenguesser.IndexedPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePhotoExclusionTest {
    @Test
    fun noneDoesNotExcludePhotos() {
        val photos = listOf(photo(1, "Austria", 48.2, 16.3))

        assertTrue(homePhotosToExclude(photos, HomePhotoExclusionMode.NONE).isEmpty())
    }

    @Test
    fun countryModeExcludesEveryPhotoFromMostPhotographedCountry() {
        val photos = listOf(
            photo(1, "Austria", 48.2, 16.3),
            photo(2, "Austria", 47.8, 13.0),
            photo(3, "Austria", 47.0, 15.4),
            photo(4, "Germany", 52.5, 13.4),
            photo(5, "Germany", 48.1, 11.6)
        )

        assertEquals(
            setOf(1L, 2L, 3L),
            homePhotosToExclude(photos, HomePhotoExclusionMode.MOST_PHOTOGRAPHED_COUNTRY)
        )
    }

    @Test
    fun countryTieUsesStableAlphabeticalChoice() {
        val photos = listOf(
            photo(1, "Germany", 52.5, 13.4),
            photo(2, "Austria", 48.2, 16.3)
        )

        assertEquals(
            setOf(2L),
            homePhotosToExclude(photos, HomePhotoExclusionMode.MOST_PHOTOGRAPHED_COUNTRY)
        )
    }

    @Test
    fun clusterModeExcludesLargestConnectedClusterInHomeCountry() {
        val photos = listOf(
            // These three form one chain: 1 -> 2 -> 3, even though 1 and 3 are > 50 km apart.
            photo(1, "Austria", 0.0, 0.0),
            photo(2, "Austria", 0.0, 0.4),
            photo(3, "Austria", 0.0, 0.8),
            photo(4, "Austria", 5.0, 5.0),
            photo(5, "Austria", 5.0, 5.1),
            photo(6, "Germany", 52.5, 13.4)
        )

        assertEquals(
            setOf(1L, 2L, 3L),
            homePhotosToExclude(photos, HomePhotoExclusionMode.LARGEST_HOME_CLUSTER)
        )
    }

    private fun photo(
        mediaId: Long,
        country: String,
        latitude: Double,
        longitude: Double
    ) = IndexedPhoto(
        mediaId = mediaId,
        dateModified = 0,
        size = 1,
        latitude = latitude,
        longitude = longitude,
        country = country,
        unreadable = false
    )
}
