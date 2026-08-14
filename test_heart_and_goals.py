"""
HealthTrack AI - Heart Rate Tracking & Goals/Achievements Test Suite (Step 9)

Tests:
1. Add heart rate reading with BPM, date, time, context, and notes.
2. Validate numeric BPM (reject < 30 or > 240 BPM).
3. Calculate cardio stats (average BPM, minimum, maximum, total readings).
4. User isolation for heart rate records.
5. Heart rate record deletion.
6. Setting custom daily health goals (steps, water, sleep, target weight).
7. Evaluating today's completion percentages & status indicators.
8. Dynamic achievement engine unlocking badges (5k steps, 10k steps, 7d streak, water goal, sleep goal, cardio tracker, weight tracker).
9. Flask endpoints: GET /heart, POST /heart/add, POST /heart/delete/<id>, GET /goals, POST /goals/update.
"""

import sys
from datetime import date, timedelta
from app import app
from database import init_db, get_db
from heart_service import (
    get_heart_module_data,
    add_heart_rate_record,
    delete_heart_rate_record
)
from goals_service import (
    get_goals_and_achievements_data,
    update_all_user_goals,
    evaluate_and_award_achievements
)
from step_service import add_user_steps


def run_heart_and_goals_tests():
    print("=" * 70)
    print("   HEALTHTRACK AI - HEART RATE & GOALS/ACHIEVEMENTS TEST SUITE")
    print("=" * 70)

    # Initialize fresh database
    init_db(reset=True)

    app.config["TESTING"] = True
    client = app.test_client()

    # 1. Register test users
    print("\n[Test 1] Registering User A ('Aria Vance') and User B ('Leo Thorne')...")
    reg_a = client.post('/register', data={
        "full_name": "Aria Vance",
        "email": "aria@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 28,
        "gender": "female",
        "height": 168.0,
        "weight": 62.0
    }, follow_redirects=True)
    assert reg_a.status_code == 200

    client.get('/logout')

    reg_b = client.post('/register', data={
        "full_name": "Leo Thorne",
        "email": "leo@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 32,
        "gender": "male",
        "height": 182.0,
        "weight": 84.0
    }, follow_redirects=True)
    assert reg_b.status_code == 200

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE email = 'aria@example.com';")
        user_a_id = cursor.fetchone()["id"]
        cursor.execute("SELECT id FROM users WHERE email = 'leo@example.com';")
        user_b_id = cursor.fetchone()["id"]

    print(f"  [PASSED] User A (ID: {user_a_id}) and User B (ID: {user_b_id}) ready.")

    # 2. Test Heart Rate Validation
    print("\n[Test 2] Testing BPM numeric input validation (Rejecting < 30 or > 240 BPM)...")
    try:
        add_heart_rate_record(user_a_id, 20)
        print("  [FAILED] BPM < 30 was accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] Low BPM rejected: {e}")

    try:
        add_heart_rate_record(user_a_id, 300)
        print("  [FAILED] BPM > 240 was accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] High BPM rejected: {e}")

    # 3. Test Adding Heart Rate Records & Stats Computation
    print("\n[Test 3] Testing add_heart_rate_record() and statistics aggregation...")
    today_str = date.today().isoformat()
    add_heart_rate_record(user_a_id, 65, measured_date=today_str, measured_time="08:00", context="Resting")
    add_heart_rate_record(user_a_id, 125, measured_date=today_str, measured_time="12:30", context="Post-Workout")
    add_heart_rate_record(user_a_id, 74, measured_date=today_str, measured_time="19:00", context="Evening Wind-Down")

    h_data = get_heart_module_data(user_a_id)
    assert h_data["latest"]["bpm"] == 74
    assert h_data["latest"]["context"] == "Resting"
    assert h_data["stats"]["min_bpm"] == 65
    assert h_data["stats"]["max_bpm"] == 125
    assert h_data["stats"]["avg_bpm"] == round((65 + 125 + 74) / 3)
    assert h_data["stats"]["total_readings"] >= 3
    print(f"  [PASSED] Stats verified: Latest={h_data['latest']['bpm']} BPM, Avg={h_data['stats']['avg_bpm']} BPM, Min={h_data['stats']['min_bpm']}, Max={h_data['stats']['max_bpm']}.")

    # 4. Test User Isolation & Heart Rate Record Deletion
    print("\n[Test 4] Testing User Isolation & delete_heart_rate_record()...")
    h_data_b = get_heart_module_data(user_b_id)
    assert h_data_b["stats"]["avg_bpm"] != h_data["stats"]["avg_bpm"] or h_data_b["stats"]["total_readings"] != h_data["stats"]["total_readings"]

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM heart_rate WHERE user_id = ? LIMIT 1;", (user_a_id,))
        rec_id = cursor.fetchone()["id"]

    deleted = delete_heart_rate_record(user_a_id, rec_id)
    assert deleted is True
    print(f"  [PASSED] Record ID {rec_id} deleted and user isolation confirmed.")

    # 5. Test Goals Updating
    print("\n[Test 5] Testing update_all_user_goals()...")
    update_all_user_goals(user_a_id, step_goal=12000, water_goal=3000, sleep_goal=8.5, target_weight=60.0)
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT daily_step_goal, daily_water_goal_ml, daily_sleep_goal_hours, target_weight_kg FROM users WHERE id = ?;", (user_a_id,))
        user_row = cursor.fetchone()
        assert user_row["daily_step_goal"] == 12000
        assert user_row["daily_water_goal_ml"] == 3000
        assert user_row["daily_sleep_goal_hours"] == 8.5
        assert user_row["target_weight_kg"] == 60.0
    print("  [PASSED] Daily goals updated: Steps=12,000, Water=3,000ml, Sleep=8.5h, Target Weight=60.0kg.")

    # 6. Test Goals Completion Evaluation
    print("\n[Test 6] Testing get_goals_and_achievements_data() completion percentages...")
    # Add steps for today
    add_user_steps(user_a_id, 6000, target_date=today_str)
    goals_view = get_goals_and_achievements_data(user_a_id)
    step_goal_item = next(g for g in goals_view["goals"] if g["key"] == "steps")
    assert step_goal_item["current_val"] >= 6000
    assert step_goal_item["pct"] >= 50
    print(f"  [PASSED] Goal progress verified: Step progress is {step_goal_item['pct']}% ({step_goal_item['current_display']}).")

    # 7. Test Achievement Unlocking Engine
    print("\n[Test 7] Testing dynamic achievement unlocking engine...")
    # 6000 steps unlocks 'first_5k_steps'
    evaluate_and_award_achievements(user_a_id)
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT badge_key FROM achievements WHERE user_id = ?;", (user_a_id,))
        unlocked_keys = [r["badge_key"] for r in cursor.fetchall()]
        assert "first_5k_steps" in unlocked_keys
        assert "heart_pioneer" in unlocked_keys
        print(f"  [PASSED] Achievements unlocked: {unlocked_keys}")

    # Now add 5000 more steps -> 11,000 steps total -> unlocks '10k_steps_club'
    add_user_steps(user_a_id, 5000, target_date=today_str)
    evaluate_and_award_achievements(user_a_id)
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT badge_key FROM achievements WHERE user_id = ?;", (user_a_id,))
        unlocked_keys_2 = [r["badge_key"] for r in cursor.fetchall()]
        assert "10k_steps_club" in unlocked_keys_2
        print(f"  [PASSED] 10k Steps Club unlocked: {unlocked_keys_2}")

    # 8. Test 7-Day Activity Streak Achievement
    print("\n[Test 8] Testing 7-day activity streak achievement...")
    with get_db() as db:
        cursor = db.cursor()
        for i in range(1, 8):
            past_date = (date.today() - timedelta(days=i)).isoformat()
            cursor.execute("""
                INSERT OR REPLACE INTO daily_activity (user_id, activity_date, steps)
                VALUES (?, ?, 3500);
            """, (user_a_id, past_date))

    evaluate_and_award_achievements(user_a_id)
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT badge_key FROM achievements WHERE user_id = ?;", (user_a_id,))
        unlocked_keys_streak = [r["badge_key"] for r in cursor.fetchall()]
        assert "activity_streak_7d" in unlocked_keys_streak
        print(f"  [PASSED] 7-Day Activity Streak unlocked: {unlocked_keys_streak}")

    # 9. Test Flask Web Endpoints (/heart, /heart/add, /goals, /goals/update)
    print("\n[Test 9] Testing Flask web routes for Heart Rate and Goals...")
    client.get('/logout')
    client.post('/login', data={"identifier": "aria@example.com", "password": "Password123!"})

    # GET /heart
    res_heart = client.get('/heart')
    assert res_heart.status_code == 200
    assert b"Heart Rate Monitoring" in res_heart.data
    assert b"Medical Disclaimer" in res_heart.data
    assert b"heartTrendChart" in res_heart.data

    # POST /heart/add
    res_add_h = client.post('/heart/add', data={
        "bpm": "78",
        "date": today_str,
        "time": "15:00",
        "context": "Walking / Light Activity",
        "notes": "Testing web route"
    }, follow_redirects=True)
    assert res_add_h.status_code == 200
    assert b"Recorded heart rate: 78 BPM" in res_add_h.data

    # GET /goals
    res_goals = client.get('/goals')
    assert res_goals.status_code == 200
    assert b"Goals &amp; Achievement Badges" in res_goals.data
    assert b"Active Health Goals" in res_goals.data
    assert b"Achievements &amp; Milestone Badges" in res_goals.data

    # POST /goals/update
    res_update_g = client.post('/goals/update', data={
        "daily_step_goal": "15000",
        "daily_water_goal_ml": "3200",
        "daily_sleep_goal_hours": "8.0",
        "target_weight_kg": "61.5"
    }, follow_redirects=True)
    assert res_update_g.status_code == 200
    assert b"All daily health goals have been updated successfully!" in res_update_g.data
    print("  [PASSED] Web routes /heart, /heart/add, /goals, and /goals/update tested and verified.")

    print("\n" + "=" * 70)
    print(" ALL 9 HEART RATE & GOALS/ACHIEVEMENT TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return True


if __name__ == "__main__":
    success = run_heart_and_goals_tests()
    sys.exit(0 if success else 1)
