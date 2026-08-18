# HealthTrack AI 🚀

> **AI-Powered Holistic Wellness & Health Tracking Platform**  
> Complete with Python Flask Backend, Web PWA Frontend, and Native Android Jetpack Compose Mobile App.

---

## 📁 Repository Structure

```text
Health_Tracking-App/
├── .vscode/             # VS Code workspace settings & tasks
├── android/             # Native Android App (Kotlin, Jetpack Compose, Material3)
├── backend/             # Python Flask Backend, APIs & Service Modules
│   ├── app.py           # Main application entry point & routing
│   ├── auth.py          # User authentication & session management
│   ├── database.py      # SQLite connection & schema management
│   ├── *_service.py     # Modular health services (Steps, Water, Sleep, Heart, etc.)
│   └── test_*.py        # Automated test suites
├── data/                # Database storage & SQL schema
│   ├── healthtrack.db   # SQLite database
│   └── schema.sql       # Database schema definitions
├── docs/                # Project documentation & architecture guides
├── frontend/            # Web Frontend (PWA, Templates & Static Assets)
│   ├── static/          # CSS stylesheets, JavaScript & PWA assets
│   └── templates/       # Jinja2 HTML templates
└── scripts/             # Database verification & icon generation scripts
```

---


## ✨ Features

- **Pedometer & Step Tracking**: Real-time step counter, daily distance, and calorie expenditure.
- **Hydration Logging**: Daily water intake monitoring with customizable hydration goals.
- **Sleep & Rest Analytics**: Sleep duration tracking, sleep quality score, and bedtime reminders.
- **Heart Rate Monitor**: Pulse recording, resting heart rate analysis, and historical graphs.
- **Weight & BMI Calculator**: Body metric tracking with automatic BMI category evaluation.
- **AI Wellness Insights**: Heuristic wellness coaching, streak achievements, and actionable recommendations.
- **Daily Reset**: Automated midnight refresh resetting daily tracking metrics to 0 while archiving weekly logs.

---

## 🚀 Getting Started

### 1. Web Application (Flask Backend + Frontend)

```powershell
# Navigate to backend
cd backend

# Install dependencies
pip install -r requirements.txt

# Run the Flask web app
python app.py
```
Open [http://127.0.0.1:5000](http://127.0.0.1:5000) in your browser.

### 2. Android Mobile App

Open the `android/` directory in **Android Studio**:
- Connect your Android phone or launch an emulator.
- Click **Run (▶)** to install and run the app.
- Or build the APK via Gradle:
  ```powershell
  cd android
  .\gradlew.bat assembleDebug
  ```

---

## 🧪 Running Tests

To run the backend test suite:
```powershell
cd backend
python test_auth.py
python test_dashboard.py
python test_step_tracking.py
python test_heart_and_goals.py
python test_insights.py
```
