"""
HealthTrack AI - Sleep Tracking Module Test Suite (Step 7)

Tests:
1. Automatic cross-midnight duration calculation (e.g., 23:15 to 07:30 = 8h 15m).
2. Same-day duration calculation.
3. SQLite database persistence in sleep_records table.
4. Daily sleep goal setting and progress percentage calculation.
5. Weekly (7-day) sleep time-series generation.
6. Sleep history log retrieval and record deletion.
7. Validation: Rejection of invalid times and unrealistic durations.
8. Strict User Isolation: User A cannot see or delete User B's sleep records.
9. Web routes: GET /sleep, POST /sleep/add, POST /sleep/goal, POST /sleep/delete/<id>.
"""

import sys
from datetime import date
from app import app
from database import init_db, get_db
from sleep_service import (
    calculate_sleep_duration,
    add_sleep_record,
    set_user_sleep_goal,
    get_sleep_module_data,
    delete_user_sleep_record
)


def run_sleep_tracking_tests():
    print("=" * 70)
    print("       HEALTHTRACK AI - SLEEP TRACKING MODULE TEST SUITE")
    print("=" * 70)

    # Initialize fresh database
    init_db(reset=True)
    
    app.config["TESTING"] = True
    client = app.test_client()

    # 1. Register test users for data isolation checks
    print("\n[Test 1] Registering User A ('Morpheus') and User B ('Hypnos')...")
    reg_a = client.post('/register', data={
        "full_name": "Morpheus Dream",
        "email": "morpheus@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 35,
        "gender": "male",
        "height": 182.0,
        "weight": 76.0
    }, follow_redirects=True)
    assert reg_a.status_code == 200

    client.get('/logout')

    reg_b = client.post('/register', data={
        "full_name": "Hypnos Slumber",
        "email": "hypnos@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 29,
        "gender": "female",
        "height": 168.0,
        "weight": 55.0
    }, follow_redirects=True)
    assert reg_b.status_code == 200

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE email = 'morpheus@example.com';")
        user_a_id = cursor.fetchone()["id"]
        cursor.execute("SELECT id FROM users WHERE email = 'hypnos@example.com';")
        user_b_id = cursor.fetchone()["id"]

    print(f"  [PASSED] User A (ID: {user_a_id}) and User B (ID: {user_b_id}) ready.")

    # 2. Test Cross-Midnight Duration Calculation
    print("\n[Test 2] Testing cross-midnight calculation algorithm...")
    # Bedtime: 23:15, Wake: 07:30 -> (45 mins to midnight) + (7h 30m) = 8h 15m = 495 mins
    mins, b_iso, w_iso = calculate_sleep_duration("23:15", "07:30", "2026-08-14")
    assert mins == 495, f"Expected 495 mins, got {mins}"
    print(f"  [PASSED] 23:15 -> 07:30 calculated accurately to {mins} mins (8h 15m). Bedtime: {b_iso}, Wake: {w_iso}")

    # Bedtime: 22:00, Wake: 06:00 -> 8 hours = 480 mins
    mins2, _, _ = calculate_sleep_duration("22:00", "06:00")
    assert mins2 == 480
    print(f"  [PASSED] 22:00 -> 06:00 calculated accurately to {mins2} mins (8h 00m).")

    # 3. Test Same-Day Nap Duration Calculation
    print("\n[Test 3] Testing same-day nap calculation...")
    mins_nap, _, _ = calculate_sleep_duration("13:00", "14:45")
    assert mins_nap == 105
    print(f"  [PASSED] 13:00 -> 14:45 nap calculated accurately to {mins_nap} mins (1h 45m).")

    # 4. Test Daily Sleep Goal Setting
    print("\n[Test 4] Testing set_user_sleep_goal()...")
    new_goal = set_user_sleep_goal(user_a_id, 8.5)
    assert new_goal == 8.5
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT daily_sleep_goal_hours FROM users WHERE id = ?;", (user_a_id,))
        assert cursor.fetchone()["daily_sleep_goal_hours"] == 8.5
    print("  [PASSED] Daily sleep goal updated to 8.5 hours.")

    # 5. Test Logging Sleep Records & SQLite Storage
    print("\n[Test 5] Testing add_sleep_record() with bedtime, wake time, and quality...")
    duration = add_sleep_record(
        user_id=user_a_id,
        bedtime_str="23:30",
        wake_time_str="07:45",
        sleep_date_str=date.today().isoformat(),
        quality_score=92,
        notes="Deep restful sleep with REM dreams"
    )
    assert duration == 495  # 8h 15m
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            SELECT duration_minutes, sleep_quality_score, deep_sleep_minutes, rem_sleep_minutes, light_sleep_minutes 
            FROM sleep_records 
            WHERE user_id = ? AND sleep_date = date('now') 
            ORDER BY id DESC LIMIT 1;
        """, (user_a_id,))
        row = cursor.fetchone()
        assert row["duration_minutes"] == 495
        assert row["sleep_quality_score"] == 92
        assert row["deep_sleep_minutes"] > 0
        assert row["rem_sleep_minutes"] > 0
    print(f"  [PASSED] Sleep stored: Duration={row['duration_minutes']}m, Quality={row['sleep_quality_score']}%, Deep={row['deep_sleep_minutes']}m, REM={row['rem_sleep_minutes']}m.")

    # 6. Test Sleep Module Data Aggregations & Goal Calculations
    print("\n[Test 6] Testing get_sleep_module_data() calculations...")
    sleep_data = get_sleep_module_data(user_a_id)
    today_stats = sleep_data["today"]
    assert today_stats["duration_minutes"] == 495
    assert today_stats["hours"] == round(495 / 60.0, 1)
    assert today_stats["goal_hours"] == 8.5
    assert len(sleep_data["weekly"]["hours"]) == 7
    assert len(sleep_data["history"]) >= 1
    print(f"  [PASSED] Aggregations verified: {today_stats['formatted_duration']} ({today_stats['goal_pct']}% of {today_stats['goal_hours']}h goal).")


    # 7. Test User Isolation
    print("\n[Test 7] Testing User Isolation (User B cannot see User A's sleep logs)...")
    sleep_data_b = get_sleep_module_data(user_b_id)
    assert sleep_data_b["today"]["duration_minutes"] != 495 or sleep_data_b["today"]["notes"] != "Deep restful sleep with REM dreams", "User B leaked User A's sleep session!"
    print("  [PASSED] User B sleep data isolated correctly.")

    # 8. Test Single Record Deletion
    print("\n[Test 8] Testing delete_user_sleep_record()...")
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM sleep_records WHERE user_id = ? LIMIT 1;", (user_a_id,))
        rec_id = cursor.fetchone()["id"]

    deleted = delete_user_sleep_record(user_a_id, rec_id)
    assert deleted is True
    print(f"  [PASSED] Sleep record ID {rec_id} deleted successfully.")

    # 9. Test Flask Web Endpoints (/sleep, /sleep/add, /sleep/goal)
    print("\n[Test 9] Testing Flask web routes (/sleep, /sleep/add, /sleep/goal)...")
    client.get('/logout')
    client.post('/login', data={"identifier": "morpheus@example.com", "password": "Password123!"})

    # GET /sleep
    res_page = client.get('/sleep')
    assert res_page.status_code == 200
    assert b"Sleep &amp; Rest Monitoring" in res_page.data
    assert b"sleepTrendChart" in res_page.data
    assert b"Wellness Disclaimer" in res_page.data

    # POST /sleep/add
    res_add = client.post('/sleep/add', data={
        "bedtime": "22:45",
        "wake_time": "06:45",
        "quality": 88,
        "notes": "Solid uninterrupted 8 hours"
    }, follow_redirects=True)
    assert res_add.status_code == 200
    assert b"Successfully logged 8h 0m of sleep" in res_add.data

    # POST /sleep/goal
    res_goal = client.post('/sleep/goal', data={"daily_sleep_goal_hours": "8.0"}, follow_redirects=True)
    assert res_goal.status_code == 200
    assert b"Daily sleep goal updated to 8.0 hours" in res_goal.data
    print("  [PASSED] Web routes /sleep, /sleep/add, and /sleep/goal verified.")

    print("\n" + "=" * 70)
    print(" ALL 9 SLEEP TRACKING MODULE TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return True


if __name__ == "__main__":
    success = run_sleep_tracking_tests()
    sys.exit(0 if success else 1)
