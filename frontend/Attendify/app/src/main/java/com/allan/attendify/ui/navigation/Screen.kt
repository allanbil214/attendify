package com.allan.attendify.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object CheckIn : Screen("check_in")
    object CheckOut : Screen("check_out")
    object History : Screen("history")
    object Profile : Screen("profile")
    object Locations : Screen("locations")
}
