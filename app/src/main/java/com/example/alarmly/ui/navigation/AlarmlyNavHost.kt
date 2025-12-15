package com.example.alarmly.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.alarmly.ui.screens.AlarmDetailScreen
import com.example.alarmly.ui.screens.AlarmListScreen
import com.example.alarmly.ui.screens.AlarmRingingScreen

@Composable
fun AlarmlyNavHost(
    navController: NavHostController,
    startDestination: String = Screen.AlarmList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.AlarmList.route) {
            AlarmListScreen(
                onNavigateToDetail = { alarmId ->
                    navController.navigate(Screen.AlarmDetail.createRoute(alarmId))
                }
            )
        }

        composable(
            route = Screen.AlarmDetail.route,
            arguments = listOf(
                navArgument("alarmId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val alarmIdString = backStackEntry.arguments?.getString("alarmId")
            val alarmId = if (alarmIdString == "new") null else alarmIdString?.toIntOrNull()

            AlarmDetailScreen(
                alarmId = alarmId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AlarmRinging.route,
            arguments = listOf(
                navArgument("alarmId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getInt("alarmId") ?: -1
            AlarmRingingScreen(
                alarmId = alarmId,
                onDismiss = {
                    // Charger connected - dismiss alarm and go back to alarm list
                    navController.navigate(Screen.AlarmList.route) {
                        popUpTo(Screen.AlarmList.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

