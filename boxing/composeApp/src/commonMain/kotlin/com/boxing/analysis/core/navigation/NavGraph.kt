package com.boxing.analysis.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.boxing.analysis.presentation.home.HomeScreen
import com.boxing.analysis.presentation.progress.ProgressScreen
import com.boxing.analysis.presentation.recording.RecordingScreen
import com.boxing.analysis.presentation.session.SessionDetailScreen

sealed class Screen(val route: String) {
    data object Home      : Screen("home")
    data object Recording : Screen("recording")
    data object Progress  : Screen("progress")
    data class  Session(val sessionId: String = "{sessionId}") : Screen("session/$sessionId")
}

@Composable
fun BoxingNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onStartRecording = { navController.navigate(Screen.Recording.route) },
                onOpenSession    = { id -> navController.navigate("session/$id") },
                onOpenProgress   = { navController.navigate(Screen.Progress.route) },
            )
        }

        composable(Screen.Recording.route) {
            RecordingScreen(
                onSessionSubmitted = { id ->
                    navController.popBackStack()
                    navController.navigate("session/$id")
                },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Session().route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { back ->
            SessionDetailScreen(
                sessionId = back.arguments!!.getString("sessionId")!!,
                onBack    = { navController.popBackStack() },
            )
        }

        composable(Screen.Progress.route) {
            ProgressScreen(onBack = { navController.popBackStack() })
        }
    }
}
