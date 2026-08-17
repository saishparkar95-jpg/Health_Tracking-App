"""
HealthTrack AI - Step Tracking Module Test Suite (Step 5)

Tests:
1. Add today's steps (increments properly).
2. Update today's steps (overrides properly).
3. Database storage & verification in daily_activity.
4. Daily step goal setting & goal percentage calculation.
5. Weekly (7-day) and Monthly (30-day) aggregation series.
6. Activity history retrieval and single record deletion.
7. Validation: Rejection of negative / invalid step inputs.
8. Strict User Isolation: User A cannot access or mutate User B's steps.
"""

import sys
from app import app
from database import init_db, get_db
from step_service import (
    add_user_steps,
    update_user_steps,
    set_user_step_goal,
    get_step_module_data,
    delete_user_step_record
)


def run_step_tracking_tests():
    print("=" * 70)
    print("       HEALTHTRACK AI - STEP TRACKING MODULE TEST SUITE")
    print("=" * 70)

    # Initialize fresh database
    init_db(reset=True)
    
    app.config["TESTING"] = True
    client = app.test_client()

    # 1. Create two test users to verify data isolation
    print("\n[Test 1] Setting up two separate test users (User A & User B)...")
    reg_a = client.post('/register', data={
        "full_name": "User Alpha",
        "email": "alpha@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 28,
        "gender": "male",
        "height": 175.0,
        "weight": 70.0
    }, follow_redirects=True)
    assert reg_a.status_code == 200

    client.get('/logout')

    reg_b = client.post('/register', data={
        "full_name": "User Beta",
        "email": "beta@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 25,
        "gender": "female",
        "height": 165.0,
        "weight": 58.0
    }, follow_redirects=True)
    assert reg_b.status_code == 200

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE email = 'alpha@example.com';")
        user_a_id = cursor.fetchone()["id"]
        cursor.execute("SELECT id FROM users WHERE email = 'beta@example.com';")
        user_b_id = cursor.fetchone()["id"]

    print(f"  [PASSED] User A (ID: {user_a_id}) and User B (ID: {user_b_id}) registered.")

    # 2. Test Goal Setting
    print("\n[Test 2] Testing set_user_step_goal()...")
    new_goal = set_user_step_goal(user_a_id, 12000)
    assert new_goal == 12000
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT daily_step_goal FROM users WHERE id = ?;", (user_a_id,))
        assert cursor.fetchone()["daily_step_goal"] == 12000
    print("  [PASSED] Daily step goal updated to 12,000 steps.")

    # 3. Test Negative / Invalid Step Rejections
    print("\n[Test 3] Testing validation: Rejecting negative step counts...")
    try:
        add_user_steps(user_a_id, -500)
        print("  [FAILED] Negative steps were accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] Negative steps properly rejected: {e}")

    try:
        update_user_steps(user_a_id, -100)
        print("  [FAILED] Negative exact steps were accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] Negative exact steps rejected: {e}")

    # 4. Test Adding Today's Steps
    print("\n[Test 4] Testing add_user_steps() increment...")
    total_1 = add_user_steps(user_a_id, 3000)
    total_2 = add_user_steps(user_a_id, 2500)
    assert total_2 == total_1 + 2500
    print(f"  [PASSED] Successfully incremented steps: {total_1} -> {total_2} steps.")

    # 5. Test Updating Exact Steps
    print("\n[Test 5] Testing update_user_steps() exact count override...")
    exact_set = update_user_steps(user_a_id, 10500)
    assert exact_set == 10500
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT steps, distance_km, calories_burned, active_minutes FROM daily_activity WHERE user_id = ? AND activity_date = date('now');", (user_a_id,))
        row = cursor.fetchone()
        assert row["steps"] == 10500
        assert row["distance_km"] > 0
        assert row["calories_burned"] > 0
        assert row["active_minutes"] > 0
    print(f"  [PASSED] Exact step update verified: Steps=10,500, Dist={row['distance_km']}km, Cal={row['calories_burned']}kcal, Active={row['active_minutes']}m.")

    # 6. Test Step Module Overview Data & Percentages
    print("\n[Test 6] Testing get_step_module_data() calculations...")
    step_data = get_step_module_data(user_a_id)
    today_stats = step_data["today"]
    assert today_stats["steps"] == 10500
    assert today_stats["goal"] == 12000
    assert today_stats["goal_pct"] == round((10500 / 12000) * 100)
    assert today_stats["remaining"] == 1500
    assert len(step_data["weekly"]["data"]) == 7
    assert len(step_data["monthly"]["data"]) == 30
    assert len(step_data["history"]) >= 1
    print(f"  [PASSED] Calculations verified: {today_stats['goal_pct']}% goal met, {today_stats['remaining']} remaining.")

    # 7. Test User Isolation
    print("\n[Test 7] Testing User Isolation (User B cannot see User A's steps)...")
    step_data_b = get_step_module_data(user_b_id)
    assert step_data_b["today"]["steps"] != 10500, "User B leaked User A's step data!"
    print(f"  [PASSED] User B steps isolated correctly (User B steps = {step_data_b['today']['steps']}).")

    # 8. Test Single Record Deletion
    print("\n[Test 8] Testing delete_user_step_record()...")
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM daily_activity WHERE user_id = ? LIMIT 1;", (user_a_id,))
        rec_id = cursor.fetchone()["id"]

    deleted = delete_user_step_record(user_a_id, rec_id)
    assert deleted is True
    print(f"  [PASSED] Record ID {rec_id} deleted successfully.")

    # 9. Test Flask Web Endpoints
    print("\n[Test 9] Testing Flask web routes for Step Tracking (/steps, /steps/add, /steps/goal)...")
    # Login as User A
    client.get('/logout')
    client.post('/login', data={"identifier": "alpha@example.com", "password": "Password123!"})
    
    # GET /steps
    res = client.get('/steps')
    assert res.status_code == 200
    assert b"Step Tracking Module" in res.data
    assert b"stepTrendChart" in res.data

    # POST /steps/add via Web Form
    res_add = client.post('/steps/add', data={"steps": 4000}, follow_redirects=True)
    assert res_add.status_code == 200
    assert b"Successfully added" in res_add.data

    # POST /steps/goal via Web Form
    res_goal = client.post('/steps/goal', data={"daily_step_goal": 15000}, follow_redirects=True)
    assert res_goal.status_code == 200
    assert b"Daily step goal updated" in res_goal.data
    print("  [PASSED] Web routes /steps, /steps/add, and /steps/goal tested and verified.")

    print("\n" + "=" * 70)
    print(" ALL 9 STEP TRACKING MODULE TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return True


if __name__ == "__main__":
    success = run_step_tracking_tests()
    sys.exit(0 if success else 1)
