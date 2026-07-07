package com.example.commingsoon.navigation

sealed class NavScreens (
    val route: String,
    val title: String
) {
    object Home : NavScreens("home", "Home")
    object Journey : NavScreens("journey", "Journey")
    object OpenGuesser : NavScreens("openguesser","Play OpenGuesser")
    object Friends : NavScreens("friends","Friends")
    object AddFriend : NavScreens("addfriend", "Add Friend")
    object Settings : NavScreens("settings","App Settings")
    object Profile : NavScreens("profile","Profile")
}

/*
* val drawerItems = listOf(
    Screen.Home,
    Screen.Journey,
    Screen.OpenGuesser,
    Screen.Friends,
    Screen.Settings,
    Screen.Profile
)
*
* drawerItems.forEach { item ->

    NavigationDrawerItem(

        label = {
            Text(item.title)
        },

        selected = false,

        onClick = {
            navController.navigate(item.route)
        }
    )
}
* */