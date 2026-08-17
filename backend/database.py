"""
HealthTrack AI - Database Management Module (Step 2: Database Initialization)

This module handles:
1. SQLite Database connections with Row factory and Foreign Key constraints enabled.
2. Schema creation and initialization from schema.sql.
3. Clean reset and sample data seeding utilities.
4. CLI helper commands for database operations.
"""

import os
import sqlite3
import sys
from pathlib import Path
from contextlib import contextmanager

# Base directory paths
BASE_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = BASE_DIR.parent
DEFAULT_DB_PATH = PROJECT_ROOT / "data" / "healthtrack.db"
SCHEMA_PATH = PROJECT_ROOT / "data" / "schema.sql"


def get_db_path():
    """
    Returns the database file path from environment variable or default.
    """
    return os.environ.get("HEALTHTRACK_DB_PATH", str(DEFAULT_DB_PATH))


def get_connection(db_path=None):
    """
    Establishes and returns an SQLite database connection.
    - Enables foreign key constraint verification.
    - Sets row_factory to sqlite3.Row for dictionary-like column access.
    - Configures a 30-second timeout to handle concurrent access gracefully.
    """
    path = db_path or get_db_path()
    conn = sqlite3.connect(path, timeout=30.0)
    conn.row_factory = sqlite3.Row
    # SQLite requires foreign keys to be explicitly enabled per connection
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn



@contextmanager
def get_db(db_path=None):
    """
    Context manager for database connections with automatic commit/rollback.
    Usage:
        with get_db() as db:
            db.execute("INSERT INTO users ...")
    """
    conn = get_connection(db_path)
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def init_db(db_path=None, reset=False):
    """
    Initializes the database by executing schema.sql.
    
    Args:
        db_path (str | Path, optional): Custom database file path.
        reset (bool): If True, drops all existing tables before re-creating schema.
    """
    target_path = Path(db_path or get_db_path())

    if not SCHEMA_PATH.exists():
        raise FileNotFoundError(f"Schema file not found at: {SCHEMA_PATH}")

    with open(SCHEMA_PATH, "r", encoding="utf-8") as f:
        schema_sql = f.read()

    with get_connection(target_path) as conn:
        if reset:
            conn.execute("PRAGMA foreign_keys = OFF;")
            cursor = conn.cursor()
            cursor.execute("SELECT name, type FROM sqlite_master WHERE type IN ('table', 'view') AND name NOT LIKE 'sqlite_%';")
            items = cursor.fetchall()
            for item in items:
                cursor.execute(f"DROP {item['type'].upper()} IF EXISTS \"{item['name']}\";")
            conn.commit()
            conn.execute("PRAGMA foreign_keys = ON;")
            print(f"[DB] Existing tables cleared in: {target_path}")

        conn.executescript(schema_sql)
        conn.commit()

    print(f"[DB] Database initialized successfully at: {target_path}")
    return True



def get_table_names(db_path=None):
    """
    Returns a list of all user table names in the database.
    """
    with get_db(db_path) as db:
        cursor = db.cursor()
        cursor.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name;"
        )
        return [row["name"] for row in cursor.fetchall()]


def get_table_info(table_name, db_path=None):
    """
    Returns column details for a given table (PRAGMA table_info).
    """
    with get_db(db_path) as db:
        cursor = db.cursor()
        cursor.execute(f"PRAGMA table_info({table_name});")
        return cursor.fetchall()


def seed_sample_data(db_path=None):
    """
    Populates sample records across all 8 tables to test data integrity and constraints.
    """
    with get_db(db_path) as db:
        cursor = db.cursor()
        
        # 1. Insert a test user
        cursor.execute("""
            INSERT OR IGNORE INTO users (
                id, username, email, password_hash, full_name, age, gender,
                height_cm, target_weight_kg, daily_step_goal, daily_water_goal_ml
            ) VALUES (
                1, 'alex_wellness', 'alex@healthtrack.ai', 'pbkdf2:sha256:dummyhash',
                'Alex Rivera', 28, 'male', 178.5, 72.0, 10000, 3000
            );
        """)

        # 2. Insert daily activity
        cursor.execute("""
            INSERT OR IGNORE INTO daily_activity (
                user_id, activity_date, steps, distance_km, calories_burned, active_minutes, floors_climbed, notes
            ) VALUES (
                1, '2026-08-14', 10450, 7.82, 540.5, 65, 12, 'Morning brisk run + evening walk'
            );
        """)

        # 3. Insert water intake records
        cursor.execute("""
            INSERT INTO water_intake (user_id, intake_date, amount_ml, beverage_type)
            VALUES 
                (1, '2026-08-14', 500, 'Water'),
                (1, '2026-08-14', 750, 'Electrolyte Water'),
                (1, '2026-08-14', 500, 'Herbal Tea');
        """)

        # 4. Insert sleep record
        cursor.execute("""
            INSERT INTO sleep_records (
                user_id, sleep_date, bedtime, wake_time, duration_minutes,
                sleep_quality_score, deep_sleep_minutes, rem_sleep_minutes, light_sleep_minutes, notes
            ) VALUES (
                1, '2026-08-14', '2026-08-13 23:15:00', '2026-08-14 07:15:00', 480,
                88, 110, 95, 275, 'Felt well rested'
            );
        """)

        # 5. Insert weight record
        cursor.execute("""
            INSERT INTO weight_records (
                user_id, record_date, weight_kg, body_fat_percentage, muscle_mass_kg, bmi, notes
            ) VALUES (
                1, '2026-08-14', 74.2, 16.5, 34.8, 23.3, 'Weekly morning weigh-in'
            );
        """)

        # 6. Insert heart rate entries
        cursor.execute("""
            INSERT INTO heart_rate (user_id, measured_at, bpm, resting_heart_rate, activity_context)
            VALUES 
                (1, '2026-08-14 07:30:00', 62, 58, 'resting'),
                (1, '2026-08-14 09:15:00', 142, 58, 'workout'),
                (1, '2026-08-14 14:00:00', 74, 58, 'normal');
        """)

        # 7. Insert goals
        cursor.execute("""
            INSERT INTO goals (
                user_id, goal_type, title, target_value, current_value, unit, start_date, target_date, status
            ) VALUES 
                (1, 'steps', 'Reach 10k Steps Daily', 10000, 10450, 'steps', '2026-08-01', '2026-08-31', 'in_progress'),
                (1, 'weight', 'Target Weight 72kg', 72.0, 74.2, 'kg', '2026-08-01', '2026-10-01', 'in_progress'),
                (1, 'water', 'Hydration Mastery 3L', 3000, 1750, 'ml', '2026-08-14', '2026-08-14', 'in_progress');
        """)

        # 8. Insert achievements
        cursor.execute("""
            INSERT OR IGNORE INTO achievements (
                user_id, badge_key, badge_name, badge_description, badge_icon, category
            ) VALUES 
                (1, 'first_step', 'First Step Forward', 'Logged your first activity on HealthTrack AI', 'flag', 'milestone'),
                (1, '10k_steps_club', '10K Steps Club', 'Walked 10,000+ steps in a single day', 'footprints', 'fitness'),
                (1, 'sleep_champion', 'Sleep Champion', 'Logged 8+ hours of restful sleep', 'moon', 'sleep');
        """)

    print("[DB] Sample seed data inserted successfully for test user 'alex_wellness'.")


if __name__ == "__main__":
    args = sys.argv[1:]
    
    if "--reset" in args:
        print("[DB CLI] Resetting database...")
        init_db(reset=True)
    elif "--init" in args or len(args) == 0:
        print("[DB CLI] Initializing database...")
        init_db(reset=False)

    if "--seed" in args:
        print("[DB CLI] Seeding sample data...")
        seed_sample_data()

    print("\nExisting tables in database:")
    for table in get_table_names():
        print(f" - {table}")
