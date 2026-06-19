package com.luca.trainbot.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.flow.map
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.luca.trainbot.core.data.AuthRepository
import com.luca.trainbot.core.data.AuthState
import com.luca.trainbot.core.data.OnboardingStore
import com.luca.trainbot.core.ml.MlProjectRepository
import com.luca.trainbot.core.network.LlmRepository
import com.luca.trainbot.core.network.LlmStreamingRepository
import com.luca.trainbot.feature.achievements.AchievementsScreen
import com.luca.trainbot.feature.achievements.AchievementsStore
import com.luca.trainbot.feature.auth.StudentLoginScreen
import com.luca.trainbot.feature.dailychallenge.DailyChallengeScreen
import com.luca.trainbot.feature.dailychallenge.DailyChallengeStore
import com.luca.trainbot.feature.home.HomeScreen
import com.luca.trainbot.feature.knowledge.KnowledgeScreen
import com.luca.trainbot.feature.llm.LlmScreen
import com.luca.trainbot.feature.onboarding.OnboardingScreen
import com.luca.trainbot.feature.settings.SettingsScreen
import com.luca.trainbot.feature.testing.TestingScreen
import com.luca.trainbot.feature.training.TrainingScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val HOME = "home"
    const val TRAINING = "training"
    const val TESTING = "testing"
    const val LLM = "llm"
    const val KNOWLEDGE = "knowledge"
    const val SETTINGS = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val DAILY_CHALLENGE = "daily_challenge"
}

@Composable
fun TrainBotNavGraph(
    authRepository: AuthRepository,
    llmRepository: LlmRepository,
    llmStreamingRepository: LlmStreamingRepository,
    mlProjectRepository: MlProjectRepository,
    onboardingStore: OnboardingStore,
    achievementsStore: AchievementsStore,
    dailyChallengeStore: DailyChallengeStore,
) {
    val navController = rememberNavController()
    // Gate routing on the REAL DataStore values (null = still loading) so we never
    // route from stale initial values (which skipped onboarding / bounced auth).
    val authState by remember(authRepository) {
        authRepository.authState.map { it as AuthState? }
    }.collectAsState(initial = null)
    val hasSeenOnboarding by remember(onboardingStore) {
        onboardingStore.hasSeenOnboarding.map { it as Boolean? }
    }.collectAsState(initial = null)

    val auth = authState
    val seen = hasSeenOnboarding
    if (auth == null || seen == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when {
        auth is AuthState.Authenticated -> Routes.HOME
        !seen -> Routes.ONBOARDING
        else -> Routes.LOGIN
    }

    // If the session is lost later (e.g. refresh token expired), return to login.
    LaunchedEffect(auth) {
        if (auth is AuthState.Unauthenticated &&
            navController.currentDestination?.route !in listOf(Routes.LOGIN, Routes.ONBOARDING)
        ) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onboardingStore = onboardingStore,
                onFinish = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.LOGIN) {
            StudentLoginScreen(
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
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
        composable(Routes.KNOWLEDGE) {
            KnowledgeScreen(mlProjectRepository = mlProjectRepository)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                authRepository = authRepository,
                onboardingStore = onboardingStore,
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onShowOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(achievementsStore = achievementsStore)
        }
        composable(Routes.DAILY_CHALLENGE) {
            DailyChallengeScreen(dailyChallengeStore = dailyChallengeStore)
        }
    }
}
