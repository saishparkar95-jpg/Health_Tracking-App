# HealthTrack AI - Android Mobile App 📱

> Native Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3**.

---

## 📁 Package Architecture (`com.healthtrackai.app`)

```text
android/app/src/main/java/com/healthtrackai/app/
│
├── MainActivity.kt                      # Main Activity with edge-to-edge & sensor permissions
│
├── 📁 data/                             # Data Layer (Sensors, Preferences, Services)
│   ├── 📁 location/
│   │   └── GpsRouteTracker.kt          # GPS tracking for outdoor walk/run sessions
│   ├── 📁 models/
│   │   ├── DashboardSampleData.kt      # Mock data & sample analytics
│   │   └── HealthAppState.kt           # Central HealthStateHolder & daily reset engine
│   ├── 📁 notifications/
│   │   ├── NotificationHelper.kt       # System notification channels & sticky live widgets
│   │   └── SmartHealthReminderManager.kt # Hydration, inactivity, and bedtime alerts
│   ├── 📁 preferences/
│   │   └── AppPreferences.kt           # SharedPreferences storage & midnight date persistence
│   ├── 📁 reports/
│   │   └── HealthReportExporter.kt     # Weekly PDF / summary exporter
│   ├── 📁 sensors/
│   │   └── StepSensorTracker.kt        # Real-time pedometer & hardware step detector
│   ├── 📁 services/
│   │   ├── AiCoachService.kt           # Personalized AI wellness suggestions
│   │   ├── HealthScoreCalculator.kt    # Dynamic 0-100 composite vitality score
│   │   ├── MoodCorrelationEngine.kt    # Correlation insights between mood & activity
│   │   └── NutritionAnalysisService.kt # Food scanner nutritional estimation
│   └── 📁 voice/
│       └── VoiceWellnessLogger.kt      # Speech recognition for voice logging
│
└── 📁 ui/                               # Presentation Layer (Compose UI & Theme)
    ├── 📁 components/                   # Reusable UI Cards & Modals
    │   ├── BottomNavigationBar.kt       # 5-tab main bottom bar
    │   ├── DailyActivityCard.kt         # Step, hydration & sleep progress rings
    │   ├── DailyChallengeCard.kt        # Gamified daily wellness quests
    │   ├── HealthScoreCard.kt           # Vitality score hero widget
    │   ├── MetricCard.kt                # Individual metric cards
    │   ├── ReportExportDialog.kt        # Export summary dialog
    │   ├── SmartwatchMetricDialogs.kt   # Manual logger dialogs (Steps, Water, Sleep, Heart)
    │   ├── TodaysPlanCard.kt            # AI-generated daily action plan
    │   ├── VoiceLoggerDialog.kt         # Voice health prompt modal
    │   ├── WeeklyActivityCard.kt        # 7-day bar chart visualization
    │   └── WellnessInsightCard.kt       # AI coach tip card
    ├── 📁 navigation/
    │   ├── AppNavigation.kt             # NavHost with animated screen transitions
    │   └── Screen.kt                    # Sealed route classes & bottom nav tabs
    ├── 📁 screens/                      # Feature Screens
    │   ├── AchievementsScreen.kt        # Milestone badges & unlockable trophies
    │   ├── ActivityScreen.kt            # Deep-dive movement & workout history
    │   ├── AiCoachScreen.kt             # Interactive AI wellness companion chat
    │   ├── AuthScreen.kt                # Sign In / Account Registration tabs
    │   ├── ChallengesScreen.kt          # Daily quests & community challenges
    │   ├── ExerciseScreen.kt            # Live workout timer & calorie tracker
    │   ├── FoodScannerScreen.kt         # Camera food scanner & macro analyzer
    │   ├── GoalsScreen.kt               # Custom daily target setting
    │   ├── HeartRateScannerScreen.kt    # Camera pulse/PPG sensor measurement
    │   ├── HomeScreen.kt                # Main wellness dashboard
    │   ├── MoodScreen.kt                # Daily emotional check-in & notes
    │   ├── OnboardingScreen.kt          # 5-step welcome walkthrough
    │   ├── ProfileScreen.kt             # User profile, body metrics & unit toggle
    │   ├── ProfileSetupScreen.kt        # Initial biometric configuration
    │   ├── ProgressScreen.kt            # Weekly & monthly trend analytics
    │   ├── SettingsScreen.kt            # Dark/light theme & notification toggles
    │   ├── SplashScreen.kt              # Animated launch splash
    │   ├── TrackScreen.kt               # Quick logging hub for all metrics
    │   └── WalkSessionScreen.kt         # Live GPS walk tracking with real-time cadence
    └── 📁 theme/
        ├── Color.kt                     # Emerald, Cyan, Purple & Dark palette
        ├── Theme.kt                     # Dynamic Material3 Theme & Insets
        └── Type.kt                      # Typography & text styles
```

---

## 🛠️ Building & Running

### Using Android Studio
1. Open the `android/` folder in Android Studio.
2. Allow Gradle sync to complete.
3. Select your device and click **Run (▶)**.

### Using Command Line
```powershell
cd android
.\gradlew.bat assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`
