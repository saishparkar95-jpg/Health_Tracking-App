-- =============================================================================
-- HealthTrack AI - Database Schema Definition
-- Database Engine: SQLite3
-- Step 2: Database Initialization & Tables Architecture
-- =============================================================================

-- Enable Foreign Key constraints in SQLite
PRAGMA foreign_keys = ON;

-- -----------------------------------------------------------------------------
-- 1. USERS TABLE
-- Stores user accounts, profile information, and personal health targets
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    username                TEXT NOT NULL UNIQUE,
    email                   TEXT NOT NULL UNIQUE,
    password_hash           TEXT NOT NULL,
    full_name               TEXT,
    age                     INTEGER,
    gender                  TEXT CHECK(gender IN ('male', 'female', 'other', 'prefer_not_to_say')),
    height_cm               REAL,
    target_weight_kg        REAL,
    daily_step_goal         INTEGER DEFAULT 10000,
    daily_water_goal_ml     INTEGER DEFAULT 2500,
    daily_calorie_goal      INTEGER DEFAULT 2000,
    daily_sleep_goal_hours  REAL DEFAULT 8.0,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- 2. DAILY ACTIVITY TABLE
-- Stores step counts, active minutes, estimated calories burned, and distance
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS daily_activity (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                 INTEGER NOT NULL,
    activity_date           DATE NOT NULL,
    steps                   INTEGER DEFAULT 0 CHECK(steps >= 0),
    distance_km             REAL DEFAULT 0.0 CHECK(distance_km >= 0.0),
    calories_burned         REAL DEFAULT 0.0 CHECK(calories_burned >= 0.0),
    active_minutes          INTEGER DEFAULT 0 CHECK(active_minutes >= 0),
    floors_climbed          INTEGER DEFAULT 0 CHECK(floors_climbed >= 0),
    notes                   TEXT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, activity_date)
);

-- -----------------------------------------------------------------------------
-- 3. WATER INTAKE TABLE
-- Tracks individual water/hydration logging events throughout the day
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS water_intake (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                 INTEGER NOT NULL,
    intake_date             DATE NOT NULL,
    amount_ml               INTEGER NOT NULL CHECK(amount_ml > 0),
    beverage_type           TEXT DEFAULT 'Water',
    logged_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 4. SLEEP RECORDS TABLE
-- Tracks nocturnal and nap sleep metrics, sleep quality, and sleep phases
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sleep_records (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                 INTEGER NOT NULL,
    sleep_date              DATE NOT NULL,
    bedtime                 TIMESTAMP NOT NULL,
    wake_time               TIMESTAMP NOT NULL,
    duration_minutes        INTEGER NOT NULL CHECK(duration_minutes >= 0),
    sleep_quality_score     INTEGER CHECK(sleep_quality_score BETWEEN 1 AND 100),
    deep_sleep_minutes      INTEGER DEFAULT 0 CHECK(deep_sleep_minutes >= 0),
    rem_sleep_minutes       INTEGER DEFAULT 0 CHECK(rem_sleep_minutes >= 0),
    light_sleep_minutes     INTEGER DEFAULT 0 CHECK(light_sleep_minutes >= 0),
    notes                   TEXT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 5. WEIGHT RECORDS TABLE
-- Tracks body weight over time, body fat %, muscle mass, and BMI
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS weight_records (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                 INTEGER NOT NULL,
    record_date             DATE NOT NULL,
    weight_kg               REAL NOT NULL CHECK(weight_kg > 0.0),
    body_fat_percentage     REAL CHECK(body_fat_percentage >= 0.0 AND body_fat_percentage <= 100.0),
    muscle_mass_kg          REAL CHECK(muscle_mass_kg >= 0.0),
    bmi                     REAL CHECK(bmi > 0.0),
    notes                   TEXT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 6. HEART RATE TABLE
-- Stores time-series heart rate measurements (BPM) and context (resting, workout)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS heart_rate (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                 INTEGER NOT NULL,
    measured_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    bpm                     INTEGER NOT NULL CHECK(bpm BETWEEN 30 AND 250),
    resting_heart_rate      INTEGER CHECK(resting_heart_rate BETWEEN 30 AND 150),
    activity_context        TEXT DEFAULT 'resting' CHECK(activity_context IN ('resting', 'workout', 'walking', 'normal', 'sleeping')),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 7. GOALS TABLE
-- Tracks short-term and long-term user health & fitness targets
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS goals (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                 INTEGER NOT NULL,
    goal_type               TEXT NOT NULL CHECK(goal_type IN ('weight', 'steps', 'water', 'sleep', 'calories', 'workout', 'custom')),
    title                   TEXT NOT NULL,
    target_value            REAL NOT NULL,
    current_value           REAL DEFAULT 0.0,
    unit                    TEXT NOT NULL,
    start_date              DATE NOT NULL,
    target_date             DATE,
    status                  TEXT DEFAULT 'in_progress' CHECK(status IN ('in_progress', 'completed', 'abandoned')),
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 8. ACHIEVEMENTS TABLE
-- Gamification badges earned by users for reaching milestones
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS achievements (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                 INTEGER NOT NULL,
    badge_key               TEXT NOT NULL,
    badge_name              TEXT NOT NULL,
    badge_description       TEXT NOT NULL,
    badge_icon              TEXT DEFAULT 'award',
    category                TEXT DEFAULT 'general' CHECK(category IN ('fitness', 'hydration', 'sleep', 'consistency', 'milestone', 'general')),
    unlocked_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, badge_key)
);

-- -----------------------------------------------------------------------------
-- PERFORMANCE INDEXES
-- Accelerates fast queries for user dashboards and historical analytics
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_daily_activity_user_date ON daily_activity(user_id, activity_date);
CREATE INDEX IF NOT EXISTS idx_water_intake_user_date ON water_intake(user_id, intake_date);
CREATE INDEX IF NOT EXISTS idx_sleep_records_user_date ON sleep_records(user_id, sleep_date);
CREATE INDEX IF NOT EXISTS idx_weight_records_user_date ON weight_records(user_id, record_date);
CREATE INDEX IF NOT EXISTS idx_heart_rate_user_time ON heart_rate(user_id, measured_at);
CREATE INDEX IF NOT EXISTS idx_goals_user_status ON goals(user_id, status);
CREATE INDEX IF NOT EXISTS idx_achievements_user ON achievements(user_id);
