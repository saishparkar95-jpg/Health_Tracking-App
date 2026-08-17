# HealthTrack AI - Technical Architecture & Overview

## Architecture Overview

HealthTrack AI is organized into three distinct tiers:

1. **Mobile Client (`android/`)**:
   - Built with **Kotlin** and **Jetpack Compose**.
   - Material 3 design with light/dark theme toggle.
   - Real-time hardware step sensor integration with accelerometer fallback.
   - Local persistence via `SharedPreferences` with automatic midnight date reset to 0.

2. **Web Frontend (`frontend/`)**:
   - Responsive Glassmorphic UI with CSS tokens and modern typography.
   - PWA support with service worker (`sw.js`) and web app manifest.
   - Interactive charts powered by Chart.js.

3. **Backend & Services (`backend/` & `data/`)**:
   - Python **Flask** web application.
   - SQLite database layer with foreign key constraints and transactional integrity.
   - Isolated service modules (`step_service.py`, `water_service.py`, `sleep_service.py`, `insights_service.py`, etc.).
   - Full test coverage across all features.
