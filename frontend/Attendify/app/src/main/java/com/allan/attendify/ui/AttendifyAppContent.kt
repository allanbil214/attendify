package com.allan.attendify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.allan.attendify.ui.navigation.Screen
import com.allan.attendify.ui.screens.auth.LoginScreen
import com.allan.attendify.ui.screens.auth.RegisterScreen
import com.allan.attendify.ui.screens.attendance.CheckInScreen
import com.allan.attendify.ui.screens.attendance.CheckOutScreen
import com.allan.attendify.ui.screens.history.HistoryScreen
import com.allan.attendify.ui.screens.home.HomeScreen
import com.allan.attendify.ui.screens.profile.ProfileScreen
import com.allan.attendify.ui.screens.splash.SplashScreen

@Composable
fun AttendifyAppContent() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCheckIn = { navController.navigate(Screen.CheckIn.route) },
                onNavigateToCheckOut = { navController.navigate(Screen.CheckOut.route) },
                onNavigateToHistory = { navController.navigateSingleTopTo(Screen.History.route) },
                onNavigateToProfile = { navController.navigateSingleTopTo(Screen.Profile.route) },
                onNavigateToLocations = { navController.navigate(Screen.Locations.route) },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) // Clear stack
                    }
                }
            )
        }
        composable(Screen.CheckIn.route) {
            CheckInScreen(
                onNavigateBack = { navController.popBackStack() },
                onCheckInSuccess = { navController.popBackStack() }
            )
        }
        composable(Screen.CheckOut.route) {
            CheckOutScreen(
                onNavigateBack = { navController.popBackStack() },
                onCheckOutSuccess = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigateSingleTopTo(Screen.Home.route) },
                onNavigateToProfile = { navController.navigateSingleTopTo(Screen.Profile.route) }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigateSingleTopTo(Screen.Home.route) },
                onNavigateToHistory = { navController.navigateSingleTopTo(Screen.History.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(Screen.Locations.route) {
            // Placeholder for Locations if we don't implement it yet
            // Or navigate back for now
            androidx.compose.material3.Text("Locations screen not implemented")
        }
    }
}

fun NavController.navigateSingleTopTo(route: String) =
    this.navigate(route) {
        popUpTo(this@navigateSingleTopTo.graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
