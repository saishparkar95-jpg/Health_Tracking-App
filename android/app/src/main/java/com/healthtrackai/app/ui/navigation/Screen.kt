package com.healthtrackai.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Splash : Screen(
        route = "splash",
        title = "Splash",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Onboarding : Screen(
        route = "onboarding",
        title = "Welcome",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object ProfileSetup : Screen(
        route = "profile_setup",
        title = "Profile Setup",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    data object Auth : Screen(
        route = "auth",
        title = "Sign In",
        selectedIcon = Icons.Filled.Lock,
        unselectedIcon = Icons.Outlined.Lock
    )

    // 5 Primary Bottom Navigation Screens
    data object Home : Screen(
        route = "home",
        title = "HOME",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Activity : Screen(
        route = "activity",
        title = "ACTIVITY",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    data object Sleep : Screen(
        route = "sleep",
        title = "SLEEP",
        selectedIcon = Icons.Filled.Bedtime,
        unselectedIcon = Icons.Outlined.Bedtime
    )

    data object Insights : Screen(
        route = "insights",
        title = "INSIGHTS",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology
    )

    data object Profile : Screen(
        route = "profile",
        title = "PROFILE",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    // Dedicated Sub-Screens
    data object Heart : Screen(
        route = "heart",
        title = "Heart Rate",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    )

    data object Hydration : Screen(
        route = "hydration",
        title = "Hydration",
        selectedIcon = Icons.Filled.WaterDrop,
        unselectedIcon = Icons.Outlined.WaterDrop
    )

    data object DataSources : Screen(
        route = "data_sources",
        title = "Data Sources",
        selectedIcon = Icons.Filled.Cable,
        unselectedIcon = Icons.Outlined.Cable
    )

    data object HealthConnectPermission : Screen(
        route = "health_connect_permission",
        title = "Connect Health Data",
        selectedIcon = Icons.Filled.Cable,
        unselectedIcon = Icons.Outlined.Cable
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object Exercise : Screen(
        route = "exercise",
        title = "Exercise Tracker",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    data object Mood : Screen(
        route = "mood",
        title = "Mood Tracker",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology
    )

    data object FoodScanner : Screen(
        route = "food_scanner",
        title = "Scan Meal",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline
    )

    data object WalkSession : Screen(
        route = "walk_session",
        title = "Live Walk",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    data object Challenges : Screen(
        route = "challenges",
        title = "Challenges",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents
    )

    data object Achievements : Screen(
        route = "achievements",
        title = "Achievements",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents
    )

    data object HeartRateScanner : Screen(
        route = "heart_rate_scanner",
        title = "Pulse Scanner",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    )

    // Legacy Route Aliases
    data object Progress : Screen(
        route = "progress",
        title = "Progress",
        selectedIcon = Icons.Filled.Timeline,
        unselectedIcon = Icons.Outlined.Timeline
    )

    data object Track : Screen(
        route = "track",
        title = "Track",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline
    )

    data object AiCoach : Screen(
        route = "ai_coach",
        title = "AI Coach",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology
    )

    data object Goals : Screen(
        route = "goals",
        title = "Goals",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents
    )

    companion object {
        // 5 Primary Bottom Navigation Tabs as specified in prompt: HOME, ACTIVITY, SLEEP, INSIGHTS, PROFILE
        val bottomNavItems = listOf(Home, Activity, Sleep, Insights, Profile)
    }
}
