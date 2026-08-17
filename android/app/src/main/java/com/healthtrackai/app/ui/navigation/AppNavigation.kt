package com.healthtrackai.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.healthtrackai.app.data.models.HealthStateHolder
import com.healthtrackai.app.ui.screens.AchievementsScreen
import com.healthtrackai.app.ui.screens.ActivityScreen
import com.healthtrackai.app.ui.screens.AiCoachScreen
import com.healthtrackai.app.ui.screens.AuthScreen
import com.healthtrackai.app.ui.screens.ChallengesScreen
import com.healthtrackai.app.ui.screens.ExerciseScreen
import com.healthtrackai.app.ui.screens.FoodScannerScreen
import com.healthtrackai.app.ui.screens.GoalsScreen
import com.healthtrackai.app.ui.screens.HomeScreen
import com.healthtrackai.app.ui.screens.MoodScreen
import com.healthtrackai.app.ui.screens.OnboardingScreen
import com.healthtrackai.app.ui.screens.ProfileScreen
import com.healthtrackai.app.ui.screens.ProfileSetupScreen
import com.healthtrackai.app.ui.screens.ProgressScreen
import com.healthtrackai.app.ui.screens.SettingsScreen
import com.healthtrackai.app.ui.screens.SplashScreen
import com.healthtrackai.app.ui.screens.TrackScreen
import com.healthtrackai.app.ui.screens.WalkSessionScreen

import com.healthtrackai.app.data.sensors.StepSensorTracker

@Composable
fun AppNavigation(
    navController: NavHostController,
    paddingValues: PaddingValues,
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    stepTracker: StepSensorTracker? = null,
    onRequestPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier.padding(paddingValues),
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        // 1. Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateNext = {
                    val nextRoute = when {
                        !healthState.isOnboardingCompleted -> Screen.Onboarding.route
                        !healthState.isProfileSetupCompleted -> Screen.ProfileSetup.route
                        !healthState.user.isSignedIn -> Screen.Auth.route
                        else -> Screen.Home.route
                    }
                    navController.navigate(nextRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 2. Onboarding Flow
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                healthState = healthState,
                onCompleteOnboarding = {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 3. Profile Setup Screen
        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                healthState = healthState,
                onCompleteSetup = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 4. Auth Screen
        composable(Screen.Auth.route) {
            AuthScreen(
                healthState = healthState,
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 5. Main Dashboard (Home)
        composable(Screen.Home.route) {
            HomeScreen(
                healthState = healthState,
                stepTracker = stepTracker,
                onRequestPermissions = onRequestPermissions,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToTrack = { _ ->
                    navController.navigate(Screen.Track.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToWalkSession = {
                    navController.navigate(Screen.WalkSession.route)
                },
                onNavigateToHeartRateScanner = {
                    navController.navigate(Screen.HeartRateScanner.route)
                },
                onNavigateToChallenges = {
                    navController.navigate(Screen.Challenges.route)
                }
            )
        }

        // 6. Progress & Trends Screen
        composable(Screen.Progress.route) {
            ProgressScreen(healthState = healthState)
        }

        // 7. Track Hub Screen
        composable(Screen.Track.route) {
            TrackScreen(
                healthState = healthState,
                onNavigateToFoodScanner = { navController.navigate(Screen.FoodScanner.route) },
                onNavigateToWalkSession = { navController.navigate(Screen.WalkSession.route) },
                onNavigateToHeartRateScanner = { navController.navigate(Screen.HeartRateScanner.route) },
                onNavigateToExercise = { navController.navigate(Screen.Exercise.route) },
                onNavigateToMood = { navController.navigate(Screen.Mood.route) }
            )
        }

        // 8. AI Coach Screen
        composable(Screen.AiCoach.route) {
            AiCoachScreen(healthState = healthState)
        }

        // 9. Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                healthState = healthState,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route)
                }
            )
        }

        // 10. Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                healthState = healthState,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route)
                }
            )
        }

        // 11. Exercise Tracker Screen
        composable(Screen.Exercise.route) {
            ExerciseScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 12. Mood Tracker Screen
        composable(Screen.Mood.route) {
            MoodScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 13. Food Scanner Screen
        composable(Screen.FoodScanner.route) {
            FoodScannerScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 14. Walk Session Screen
        composable(Screen.WalkSession.route) {
            WalkSessionScreen(
                healthState = healthState,
                stepTracker = stepTracker,
                onRequestPermissions = onRequestPermissions,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 15. Challenges Screen
        composable(Screen.Challenges.route) {
            ChallengesScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 16. Achievements Screen
        composable(Screen.Achievements.route) {
            AchievementsScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 17. Camera PPG Heart Rate Scanner Screen
        composable(Screen.HeartRateScanner.route) {
            com.healthtrackai.app.ui.screens.HeartRateScannerScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Legacy compatibility routes
        composable(Screen.Activity.route) {
            ActivityScreen(healthState = healthState)
        }
        composable(Screen.Goals.route) {
            GoalsScreen(healthState = healthState)
        }
    }
}
