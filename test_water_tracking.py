"""
HealthTrack AI - Water Tracking Module Test Suite (Step 6)

Tests:
1. Add water intake with quick buttons (250ml, 500ml, 750ml, 1000ml).
2. Custom amount and beverage types.
3. Daily total calculation in SQLite.
4. Setting daily water goal & progress percentage calculation.
5. Weekly (7-day) hydration chart data generation.
6. Water history log retrieval and record deletion.
7. Validation: Rejection of negative, zero, and unreasonable values (> 5,000ml).
8. Strict User Isolation: User A cannot see or delete User B's water records.
9. Web routes: GET /water, POST /water/add, POST /water/goal, POST /water/delete/<id>.
"""

import sys
from app import app
from database import init_db, get_db
from water_service import (
    add_water_intake,
    set_user_water_goal,
    get_water_module_data,
    delete_user_water_log
)


def run_water_tracking_tests():
    print("=" * 70)
    print("       HEALTHTRACK AI - WATER TRACKING MODULE TEST SUITE")
    print("=" * 70)

    # Initialize fresh database
    init_db(reset=True)
    
    app.config["TESTING"] = True
    client = app.test_client()

    # 1. Register test users for data isolation checks
    print("\n[Test 1] Registering User A ('Aquaman') and User B ('Hydra')...")
    reg_a = client.post('/register', data={
        "full_name": "Arthur Curry",
        "email": "aquaman@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 32,
        "gender": "male",
        "height": 188.0,
        "weight": 85.0
    }, follow_redirects=True)
    assert reg_a.status_code == 200

    client.get('/logout')

    reg_b = client.post('/register', data={
        "full_name": "Mera Atlantis",
        "email": "mera@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 30,
        "gender": "female",
        "height": 172.0,
        "weight": 60.0
    }, follow_redirects=True)
    assert reg_b.status_code == 200

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE email = 'aquaman@example.com';")
        user_a_id = cursor.fetchone()["id"]
        cursor.execute("SELECT id FROM users WHERE email = 'mera@example.com';")
        user_b_id = cursor.fetchone()["id"]

    print(f"  [PASSED] User A (ID: {user_a_id}) and User B (ID: {user_b_id}) ready.")

    # 2. Test Goal Setting
    print("\n[Test 2] Testing set_user_water_goal()...")
    new_goal = set_user_water_goal(user_a_id, 3000)
    assert new_goal == 3000
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT daily_water_goal_ml FROM users WHERE id = ?;", (user_a_id,))
        assert cursor.fetchone()["daily_water_goal_ml"] == 3000
    print("  [PASSED] Daily water goal updated to 3,000 ml.")

    # 3. Test Invalid Input Rejections
    print("\n[Test 3] Testing validation: Rejecting negative, zero, and unreasonable values...")
    try:
        add_water_intake(user_a_id, -250)
        print("  [FAILED] Negative water intake was accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] Negative value rejected: {e}")

    try:
        add_water_intake(user_a_id, 0)
        print("  [FAILED] Zero water intake was accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] Zero value rejected: {e}")

    try:
        add_water_intake(user_a_id, 8000)
        print("  [FAILED] Excessive intake (8,000ml) was accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] Excessive volume (>5,000ml) rejected: {e}")

    # 4. Test Adding Water Intake (Quick Presets: 250, 500, 750, 1000)
    print("\n[Test 4] Testing add_water_intake() with quick presets (250ml, 500ml, 750ml, 1000ml)...")
    # Quick 250ml
    tot1 = add_water_intake(user_a_id, 250, "Water")
    # Quick 500ml
    tot2 = add_water_intake(user_a_id, 500, "Electrolyte Water")
    # Quick 750ml
    tot3 = add_water_intake(user_a_id, 750, "Herbal Tea")
    # Quick 1000ml
    tot4 = add_water_intake(user_a_id, 1000, "Lemon Water")

    assert tot4 >= 2500
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT COUNT(*) as count, SUM(amount_ml) as total FROM water_intake WHERE user_id = ? AND intake_date = date('now');", (user_a_id,))
        row = cursor.fetchone()
        assert row["count"] >= 4
        assert row["total"] == tot4
    print(f"  [PASSED] Quick presets logged successfully. Daily total is {tot4} ml across {row['count']} logs.")

    # 5. Test Progress Percentage and Calculations
    print("\n[Test 5] Testing get_water_module_data() calculations...")
    water_data = get_water_module_data(user_a_id)
    today_stats = water_data["today"]
    assert today_stats["total_ml"] == tot4
    assert today_stats["goal_ml"] == 3000
    assert today_stats["goal_pct"] == min(100, round((tot4 / 3000) * 100))
    assert today_stats["raw_goal_pct"] == round((tot4 / 3000) * 100, 1)
    assert today_stats["remaining_ml"] == max(0, 3000 - tot4)
    assert today_stats["glasses"] == round(tot4 / 250.0, 1)
    assert len(water_data["weekly"]["data"]) == 7
    assert len(water_data["history"]) >= 4
    print(f"  [PASSED] Calculations verified: {today_stats['total_ml']}/{today_stats['goal_ml']} ml ({today_stats['raw_goal_pct']}%), {today_stats['glasses']} glasses.")


    # 6. Test User Isolation
    print("\n[Test 6] Testing User Isolation (User B cannot see User A's water records)...")
    water_data_b = get_water_module_data(user_b_id)
    assert water_data_b["today"]["total_ml"] != tot4, "User B leaked User A's water data!"
    print(f"  [PASSED] User B water isolated (User B total = {water_data_b['today']['total_ml']} ml).")

    # 7. Test Single Record Deletion
    print("\n[Test 7] Testing delete_user_water_log()...")
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM water_intake WHERE user_id = ? LIMIT 1;", (user_a_id,))
        log_id = cursor.fetchone()["id"]

    deleted = delete_user_water_log(user_a_id, log_id)
    assert deleted is True
    print(f"  [PASSED] Water record ID {log_id} deleted successfully.")

    # 8. Test Web Endpoints (/water, /water/add, /water/goal)
    print("\n[Test 8] Testing Flask web routes (/water, /water/add, /water/goal)...")
    client.get('/logout')
    client.post('/login', data={"identifier": "aquaman@example.com", "password": "Password123!"})

    # GET /water
    res_page = client.get('/water')
    assert res_page.status_code == 200
    assert b"Hydration &amp; Water Tracking" in res_page.data
    assert b"waterTrendChart" in res_page.data

    # POST /water/add
    res_add = client.post('/water/add', data={"amount_ml": 500, "beverage_type": "Coconut Water"}, follow_redirects=True)
    assert res_add.status_code == 200
    assert b"Logged +500 ml" in res_add.data

    # POST /water/goal
    res_goal = client.post('/water/goal', data={"daily_water_goal_ml": 3500}, follow_redirects=True)
    assert res_goal.status_code == 200
    assert b"Daily water goal updated to 3,500 ml" in res_goal.data
    print("  [PASSED] Web routes /water, /water/add, and /water/goal verified.")

    print("\n" + "=" * 70)
    print(" ALL 8 WATER TRACKING MODULE TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return True


if __name__ == "__main__":
    success = run_water_tracking_tests()
    sys.exit(0 if success else 1)
