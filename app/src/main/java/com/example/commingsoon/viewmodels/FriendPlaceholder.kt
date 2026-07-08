package com.example.commingsoon.viewmodels

object FriendPlaceholder {

    val friends = listOf(
        Friend(
            id = 1,
            name = "Emma",
            image = null,
            sharedWithMe = listOf(
                JourneyPlaceholder.journeys[0],
                JourneyPlaceholder.journeys[2]
            ),
            sharedByMe = listOf(
                JourneyPlaceholder.journeys[4]
            ),
            liveLocation = FriendLocation(
                48.2082,
                16.3738
            )
        ),
        Friend(
            id = 2,
            name = "Lucas",
            image = null,
            sharedWithMe = listOf(
                JourneyPlaceholder.journeys[1]
            ),
            sharedByMe = listOf(
                JourneyPlaceholder.journeys[3],
                JourneyPlaceholder.journeys[5]
            ),
            liveLocation = FriendLocation(
                51.5074,
                -0.1278
            )
        ),
        Friend(
            id = 3,
            name = "Sophia",
            image = null,
            sharedWithMe = emptyList(),
            sharedByMe = listOf(
                JourneyPlaceholder.journeys[6]
            ),
            liveLocation = FriendLocation(
                35.6762,
                139.6503
            )
        ),
        Friend(
            id = 4,
            name = "Noah",
            image = null,
            sharedWithMe = listOf(
                JourneyPlaceholder.journeys[7]
            ),
            sharedByMe = emptyList(),
            liveLocation = FriendLocation(
                -33.8688,
                151.2093
            )
        )
    )
}