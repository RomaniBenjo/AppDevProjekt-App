package com.example.commingsoon.viewmodels

import java.time.LocalDate

object JourneyPlaceholder {
    val journeys = listOf(
        Journey(
            id = 1,
            title = "Japan",
            startDate = LocalDate.of(2026, 8, 13),
            endDate = LocalDate.of(2026, 11, 10),
            locations = listOf(
                JourneyLocation(1, "Tokyo", 35.6764, 139.6500),
                JourneyLocation(2, "Kyoto", 35.0116, 135.7681),
                JourneyLocation(3, "Osaka", 34.6937, 135.5023)
            ),
            visitedCountries = listOf("Japan")
        ),
        Journey(
            id = 2,
            title = "Iceland",
            startDate = LocalDate.of(2027, 3, 13),
            endDate = LocalDate.of(2027, 3, 25),
            locations = listOf(
                JourneyLocation(4, "Reykjavík", 64.1466, -21.9426),
                JourneyLocation(5, "Þingvellir", 64.2559, -21.1294),
                JourneyLocation(6, "Gullfoss", 64.3275, -20.1218),
                JourneyLocation(7, "Skógafoss", 63.5321, -19.5114),
                JourneyLocation(8, "Vík", 63.4186, -19.0060)
            ),
            visitedCountries = listOf("Iceland")
        ),
        Journey(
            id = 3,
            title = "Sweden",
            startDate = LocalDate.of(2027, 4, 2),
            endDate = LocalDate.of(2027, 4, 8),
            locations = listOf(
                JourneyLocation(9, "Stockholm", 59.3293, 18.0686),
                JourneyLocation(10, "Uppsala", 59.8586, 17.6389),
                JourneyLocation(11, "Örebro", 59.2741, 15.2066),
                JourneyLocation(12, "Göteborg", 57.7089, 11.9746),
                JourneyLocation(13, "Malmö", 55.6050, 13.0038)
            ),
            visitedCountries = listOf("Sweden")
        ),
        Journey(
            id = 4,
            title = "Canada",
            startDate = LocalDate.of(2027, 5, 2),
            endDate = LocalDate.of(2027, 7, 30),
            locations = listOf(
                JourneyLocation(14, "Vancouver", 49.2827, -123.1207),
                JourneyLocation(15, "Whistler", 50.1163, -122.9574),
                JourneyLocation(16, "Banff", 51.1784, -115.5708),
                JourneyLocation(17, "Calgary", 51.0447, -114.0719),
                JourneyLocation(18, "Edmonton", 53.5461, -113.4938),
                JourneyLocation(19, "Toronto", 43.6532, -79.3832),
                JourneyLocation(20, "Ottawa", 45.4215, -75.6972),
                JourneyLocation(21, "Montréal", 45.5019, -73.5674),
                JourneyLocation(22, "Québec", 46.8139, -71.2080),
                JourneyLocation(23, "Niagara Falls", 43.0896, -79.0849)
            ),
            visitedCountries = listOf("Canada")
        ),
        Journey(
            id = 5,
            title = "Netherlands",
            startDate = LocalDate.of(2027, 10, 16),
            endDate = LocalDate.of(2027, 10, 20),
            locations = listOf(
                JourneyLocation(24, "Amsterdam", 52.3676, 4.9041),
                JourneyLocation(25, "Rotterdam", 51.9244, 4.4777),
                JourneyLocation(26, "Den Haag", 52.0705, 4.3007),
                JourneyLocation(27, "Utrecht", 52.0907, 5.1214),
                JourneyLocation(28, "Eindhoven", 51.4416, 5.4697),
                JourneyLocation(29, "Maastricht", 50.8514, 5.6910),
                JourneyLocation(30, "Groningen", 53.2194, 6.5665),
                JourneyLocation(31, "Leiden", 52.1601, 4.4970),
                JourneyLocation(32, "Delft", 52.0116, 4.3571),
                JourneyLocation(33, "Haarlem", 52.3874, 4.6462),
                JourneyLocation(34, "Kinderdijk", 51.8820, 4.6333),
                JourneyLocation(35, "Zaanse Schans", 52.4731, 4.8177)
            ),
            visitedCountries = listOf("Netherlands")
        ),
        Journey(
            id = 6,
            title = "China",
            startDate = LocalDate.of(2027, 11, 16),
            endDate = LocalDate.of(2027, 12, 20),
            locations = listOf(
                JourneyLocation(36, "Beijing", 39.9042, 116.4074),
                JourneyLocation(37, "Shanghai", 31.2304, 121.4737),
                JourneyLocation(38, "Xi'an", 34.3416, 108.9398),
                JourneyLocation(39, "Chengdu", 30.5728, 104.0668)
            ),
            visitedCountries = listOf("China")
        ),
        Journey(
            id = 7,
            title = "Australia",
            startDate = LocalDate.of(2028, 2, 16),
            endDate = LocalDate.of(2028, 2, 20),
            locations = listOf(
                JourneyLocation(40, "Sydney", -33.8688, 151.2093),
                JourneyLocation(41, "Melbourne", -37.8136, 144.9631),
                JourneyLocation(42, "Brisbane", -27.4698, 153.0251),
                JourneyLocation(43, "Gold Coast", -28.0167, 153.4000),
                JourneyLocation(44, "Cairns", -16.9186, 145.7781),
                JourneyLocation(45, "Uluru", -25.3444, 131.0369),
                JourneyLocation(46, "Alice Springs", -23.6980, 133.8807),
                JourneyLocation(47, "Darwin", -12.4634, 130.8456),
                JourneyLocation(48, "Perth", -31.9505, 115.8605),
                JourneyLocation(49, "Adelaide", -34.9285, 138.6007),
                JourneyLocation(50, "Hobart", -42.8821, 147.3272),
                JourneyLocation(51, "Canberra", -35.2809, 149.1300)
            ),
            visitedCountries = listOf("Australia")
        ),
        Journey(
            id = 8,
            title = "Netherlands & Germany",
            startDate = LocalDate.of(2028, 5, 16),
            endDate = LocalDate.of(2028, 5, 20),
            locations = listOf(
                JourneyLocation(52, "Amsterdam", 52.3676, 4.9041),
                JourneyLocation(53, "Rotterdam", 51.9244, 4.4777),
                JourneyLocation(54, "Den Haag", 52.0705, 4.3007),
                JourneyLocation(55, "Utrecht", 52.0907, 5.1214),
                JourneyLocation(56, "Eindhoven", 51.4416, 5.4697),
                JourneyLocation(57, "Maastricht", 50.8514, 5.6910),
                JourneyLocation(58, "Groningen", 53.2194, 6.5665),
                JourneyLocation(59, "Leiden", 52.1601, 4.4970),
                JourneyLocation(60, "Delft", 52.0116, 4.3571),
                JourneyLocation(61, "Haarlem", 52.3874, 4.6462),
                JourneyLocation(62, "Kinderdijk", 51.8820, 4.6333),
                JourneyLocation(63, "Zaanse Schans", 52.4731, 4.8177)
            ),
            visitedCountries = listOf("Netherlands", "Germany")
        )
    )
}