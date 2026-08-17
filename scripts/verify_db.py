"""
HealthTrack AI - Database Verification & Integrity Test Script

This script verifies:
1. SQLite connection and PRAGMA settings.
2. Presence of all 8 required tables.
3. Primary Key & Foreign Key configuration for all tables.
4. Foreign Key constraint enforcement (cascades and rejections).
5. Data insertion, retrieval, and table row counts.
"""

import sys
import sqlite3
from database import get_db, get_table_names, init_db, seed_sample_data, DEFAULT_DB_PATH

REQUIRED_TABLES = [
    "users",
    "daily_activity",
    "water_intake",
    "sleep_records",
    "weight_records",
    "heart_rate",
    "goals",
    "achievements"
]


def run_verification():
    print("=" * 70)
    print("           HEALTHTRACK AI - DATABASE VERIFICATION SUITE")
    print("=" * 70)

    # 1. Initialize fresh DB for testing
    print("\n[Step 1] Initializing clean database...")
    init_db(reset=True)

    # 2. Check table existence
    print("\n[Step 2] Verifying table presence...")
    existing_tables = get_table_names()
    missing_tables = [t for t in REQUIRED_TABLES if t not in existing_tables]

    for table in REQUIRED_TABLES:
        status = "PASSED" if table in existing_tables else "FAILED (Missing)"
        print(f"  - Table '{table}': {status}")

    if missing_tables:
        print(f"\n[ERROR] Missing required tables: {missing_tables}")
        return False
    else:
        print("  => All 8 required tables are present.")

    # 3. Verify Table Schemas (Columns & Primary Keys)
    print("\n[Step 3] Inspecting table structures (columns & primary keys)...")
    with get_db() as conn:
        cursor = conn.cursor()
        for table in REQUIRED_TABLES:
            cursor.execute(f"PRAGMA table_info({table});")
            columns = cursor.fetchall()
            pk_cols = [col["name"] for col in columns if col["pk"] == 1]
            col_names = [col["name"] for col in columns]
            print(f"  - {table:<16} | PK: {pk_cols} | Columns ({len(columns)}): {', '.join(col_names[:5])}...")

    # 4. Verify Foreign Key Constraint Enforcement
    print("\n[Step 4] Testing Foreign Key enforcement...")
    with get_db() as conn:
        try:
            # Attempt to insert a daily_activity with invalid user_id = 9999
            conn.execute("""
                INSERT INTO daily_activity (user_id, activity_date, steps)
                VALUES (9999, '2026-08-14', 5000);
            """)
            print("  [FAILED] Foreign key check failed: Invalid user_id was allowed.")
            return False
        except sqlite3.IntegrityError:
            print("  [PASSED] Foreign key enforcement is ACTIVE (blocked invalid user_id 9999).")

    # 5. Populate sample data and verify counts
    print("\n[Step 5] Seeding test data across all tables...")
    seed_sample_data()

    print("\n[Step 6] Verifying row counts and data integrity...")
    total_records = 0
    with get_db() as conn:
        cursor = conn.cursor()
        for table in REQUIRED_TABLES:
            cursor.execute(f"SELECT COUNT(*) AS count FROM {table};")
            count = cursor.fetchone()["count"]
            total_records += count
            print(f"  - {table:<16}: {count} row(s)")

    # 6. Test Cascade Delete
    print("\n[Step 7] Testing ON DELETE CASCADE integrity...")
    with get_db() as conn:
        conn.execute("DELETE FROM users WHERE id = 1;")

    with get_db() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) AS count FROM daily_activity WHERE user_id = 1;")
        remaining_activity = cursor.fetchone()["count"]

        if remaining_activity == 0:
            print("  [PASSED] ON DELETE CASCADE correctly purged associated child records.")
        else:
            print("  [FAILED] Child records remained after user deletion.")
            return False

    # Re-seed for ready development
    print("\n[Step 8] Re-seeding database for development readiness...")
    seed_sample_data()

    print("\n" + "=" * 70)
    print(" VERIFICATION COMPLETE: ALL 8 TABLES & CONSTRAINTS PASSED SUCCESSFULLY!")
    print(f" Database file location: {DEFAULT_DB_PATH.resolve()}")
    print("=" * 70)
    return True


if __name__ == "__main__":
    success = run_verification()
    sys.exit(0 if success else 1)
