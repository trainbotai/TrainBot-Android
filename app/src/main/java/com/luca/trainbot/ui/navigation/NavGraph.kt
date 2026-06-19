package com.luca.trainbot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.luca.trainbot.core.data.AuthRepository
import com.luca.trainbot.core.data.AuthState
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.LlmStreamingRepository
import com.luca.trainbot.feature.auth.StudentLoginScreen
import com.luca.trainbot.feature.home.HomeScreen
import com.luca.trainbot.feature.knowledge.KnowledgeScreen
import com.luca.trainbot.feature.llm.LlmScreen
import com.luca.trainbot.feature.settings.SettingsScreen
import com.luca.trainbot.feature.testing.TestingScreen
import com.luca.trainbot.feature.training.TrainingScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val TRAINING = "training"
    const val TESTING = "testing"
    const val LLM = "llm"
    const val KNOWLEDGE = "knowledge"
    const val SETTINGS = "settings"
}

@Composable
fun TrainBotNavGraph(
    authRepository: AuthRepository,
    llmRepository: LlmRepository,
    llmStreamingRepository: LlmStreamingRepository,
) {
    val navController = rememberNavController()
    val authState by authRepository.authState.collectAsState(initial = AuthState.Unauthenticated)

    val startDestination = when (authState) {
        is AuthState.Authenticated -> Routes.HOME
        AuthState.Unauthenticated -> Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            StudentLoginScreen(
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.TRAINING) { TrainingScreen() }
        composable(Routes.TESTING) { TestingScreen() }
        composable(Routes.LLM) {
            LlmScreen(
                llmRepository = llmRepository,
                llmStreamingRepository = llmStreamingRepository,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.KNOWLEDGE) { KnowledgeScreen() }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                authRepository = authRepository,
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
