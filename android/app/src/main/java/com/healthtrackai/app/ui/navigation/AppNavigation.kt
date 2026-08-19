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
import com.healthtrackai.app.data.sensors.StepSensorTracker
import com.healthtrackai.app.ui.screens.AchievementsScreen
import com.healthtrackai.app.ui.screens.ActivityScreen
import com.healthtrackai.app.ui.screens.AiCoachScreen
import com.healthtrackai.app.ui.screens.AuthScreen
import com.healthtrackai.app.ui.screens.ChallengesScreen
import com.healthtrackai.app.ui.screens.DataSourceScreen
import com.healthtrackai.app.ui.screens.ExerciseScreen
import com.healthtrackai.app.ui.screens.FoodScannerScreen
import com.healthtrackai.app.ui.screens.GoalsScreen
import com.healthtrackai.app.ui.screens.HealthConnectPermissionScreen
import com.healthtrackai.app.ui.screens.HeartRateScannerScreen
import com.healthtrackai.app.ui.screens.HeartScreen
import com.healthtrackai.app.ui.screens.HomeScreen
import com.healthtrackai.app.ui.screens.HydrationScreen
import com.healthtrackai.app.ui.screens.InsightsScreen
import com.healthtrackai.app.ui.screens.MoodScreen
import com.healthtrackai.app.ui.screens.OnboardingScreen
import com.healthtrackai.app.ui.screens.ProfileScreen
import com.healthtrackai.app.ui.screens.ProfileSetupScreen
import com.healthtrackai.app.ui.screens.ProgressScreen
import com.healthtrackai.app.ui.screens.SettingsScreen
import com.healthtrackai.app.ui.screens.SleepScreen
import com.healthtrackai.app.ui.screens.SplashScreen
import com.healthtrackai.app.ui.screens.TrackScreen
import com.healthtrackai.app.ui.screens.WalkSessionScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    paddingValues: PaddingValues,
    healthState: HealthStateHolder = remember { HealthStateHolder() },
    stepTracker: StepSensorTracker? = null,
    onRequestPermissions: () -> Unit = {},
    onRequestHealthConnectPermissions: () -> Unit = {},
    onRefreshHealthConnect: () -> Unit = {},
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

        // 5. Main Dashboard (HOME)
        composable(Screen.Home.route) {
            HomeScreen(
                healthState = healthState,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToActivity = { navController.navigate(Screen.Activity.route) },
                onNavigateToSleep = { navController.navigate(Screen.Sleep.route) },
                onNavigateToHeart = { navController.navigate(Screen.Heart.route) },
                onNavigateToHydration = { navController.navigate(Screen.Hydration.route) },
                onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                onNavigateToPermissions = { navController.navigate(Screen.HealthConnectPermission.route) },
                onRefreshHealthConnect = onRefreshHealthConnect
            )
        }

        // 6. ACTIVITY Screen
        composable(Screen.Activity.route) {
            ActivityScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() },
                onRefresh = onRefreshHealthConnect
            )
        }

        // 7. SLEEP Screen
        composable(Screen.Sleep.route) {
            SleepScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() },
                onRefresh = onRefreshHealthConnect
            )
        }

        // 8. INSIGHTS Screen (AI Coach + Daily Summary + Weekly Report)
        composable(Screen.Insights.route) {
            InsightsScreen(
                healthState = healthState
            )
        }

        // 9. PROFILE Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                healthState = healthState,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToDataSources = { navController.navigate(Screen.DataSources.route) },
                onNavigateToPermissions = { navController.navigate(Screen.HealthConnectPermission.route) },
                onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                onRefreshHealthConnect = onRefreshHealthConnect,
                onNavigateToAuth = { navController.navigate(Screen.Auth.route) }
            )
        }

        // 10. HEART Screen
        composable(Screen.Heart.route) {
            HeartScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() },
                onRefresh = onRefreshHealthConnect
            )
        }

        // 11. HYDRATION Screen
        composable(Screen.Hydration.route) {
            HydrationScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() },
                onRefresh = onRefreshHealthConnect
            )
        }

        // 12. DATA SOURCES Screen
        composable(Screen.DataSources.route) {
            DataSourceScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() },
                onRefresh = onRefreshHealthConnect,
                onNavigateToPermissions = { navController.navigate(Screen.HealthConnectPermission.route) }
            )
        }

        // 13. HEALTH CONNECT PERMISSION ONBOARDING Screen
        composable(Screen.HealthConnectPermission.route) {
            HealthConnectPermissionScreen(
                healthState = healthState,
                onRequestHealthConnectPermissions = onRequestHealthConnectPermissions,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 14. Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAuth = { navController.navigate(Screen.Auth.route) }
            )
        }

        // 15. Goals Screen
        composable(Screen.Goals.route) {
            GoalsScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 16. Exercise Tracker Screen
        composable(Screen.Exercise.route) {
            ExerciseScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 17. Mood Tracker Screen
        composable(Screen.Mood.route) {
            MoodScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 18. Food Scanner Screen
        composable(Screen.FoodScanner.route) {
            FoodScannerScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 19. Walk Session Screen
        composable(Screen.WalkSession.route) {
            WalkSessionScreen(
                healthState = healthState,
                stepTracker = stepTracker,
                onRequestPermissions = onRequestPermissions,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 20. Challenges Screen
        composable(Screen.Challenges.route) {
            ChallengesScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 21. Achievements Screen
        composable(Screen.Achievements.route) {
            AchievementsScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 22. Camera Pulse Scanner Screen
        composable(Screen.HeartRateScanner.route) {
            HeartRateScannerScreen(
                healthState = healthState,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Legacy compatibility routes
        composable(Screen.Progress.route) {
            ProgressScreen(healthState = healthState)
        }
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
        composable(Screen.AiCoach.route) {
            AiCoachScreen(healthState = healthState)
        }
    }
}
