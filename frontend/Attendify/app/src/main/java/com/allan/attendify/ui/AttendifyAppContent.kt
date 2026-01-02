package com.allan.attendify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AttendifyAppContent(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        mainViewModel.logoutEvent.collectLatest {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

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
                navController = navController,
                onNavigateToCheckIn = { navController.navigate(Screen.CheckIn.route) },
                onNavigateToCheckOut = { navController.navigate(Screen.CheckOut.route) },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
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
            HistoryScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                onLogout = {
                    // This can now be handled by the MainViewModel's global logout
                    // but we can still keep manual logout functionality here if needed.
                    // For now, we will just trigger the main logout flow
                }
            )
        }
        composable(Screen.Locations.route) {
            // Placeholder for Locations if we don't implement it yet
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
