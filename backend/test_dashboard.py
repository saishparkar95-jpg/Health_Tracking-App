"""
HealthTrack AI - Step 4 Dashboard Test Suite

Verifies:
1. Real DB data retrieval for dashboard.
2. Calculation of Today's Overview (Steps, Water, Sleep, Weight, Heart Rate).
3. Progress card calculations vs goals.
4. Past 7-day time series data for Chart.js.
5. Quick Log endpoints saving actual records to SQLite.
"""

import sys
from app import app
from database import init_db, get_db
from dashboard_service import get_user_dashboard_metrics, log_user_activity, log_user_water, log_user_sleep


def run_dashboard_tests():
    print("=" * 70)
    print("         HEALTHTRACK AI - STEP 4 DASHBOARD TEST SUITE")
    print("=" * 70)

    # Initialize fresh test database
    init_db(reset=True)
    
    app.config["TESTING"] = True
    client = app.test_client()

    # 1. Register a test user
    print("\n[Test 1] Registering test user 'Marcus Vance'...")
    reg_res = client.post('/register', data={
        "full_name": "Marcus Vance",
        "email": "marcus@healthtrack.ai",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 30,
        "gender": "male",
        "height": 180.0,
        "weight": 75.0
    }, follow_redirects=True)
    if reg_res.status_code == 200 and b"Marcus Vance" in reg_res.data:
        print("  [PASSED] Test user registered and authenticated.")
    else:
        print(f"  [FAILED] Failed to register test user. Status: {reg_res.status_code}")
        return False

    # Get user_id
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE email = 'marcus@healthtrack.ai';")
        user_id = cursor.fetchone()["id"]

    # 2. Test Dashboard Data Service
    print("\n[Test 2] Querying dashboard metrics from SQLite database...")
    metrics = get_user_dashboard_metrics(user_id)
    
    # Check Today's Overview keys
    today_stats = metrics["today"]
    assert "steps" in today_stats, "Missing steps in today's overview"
    assert "water_ml" in today_stats, "Missing water in today's overview"
    assert "sleep_hours" in today_stats, "Missing sleep in today's overview"
    assert "weight_kg" in today_stats, "Missing weight in today's overview"
    assert "heart_rate_bpm" in today_stats, "Missing heart rate in today's overview"
    print(f"  [PASSED] Today's Overview verified: Steps={today_stats['steps']}, Water={today_stats['water_ml']}ml, Sleep={today_stats['sleep_hours']}h, Weight={today_stats['weight_kg']}kg, HR={today_stats['heart_rate_bpm']}bpm")

    # Check Progress Cards
    progress = metrics["progress"]
    assert progress["step_goal"] > 0, "Invalid step goal"
    assert progress["water_goal_ml"] > 0, "Invalid water goal"
    assert progress["sleep_goal_hours"] > 0, "Invalid sleep goal"
    print(f"  [PASSED] Progress goals verified: Steps Goal={progress['step_goal']}, Water Goal={progress['water_goal_ml']}ml, Sleep Goal={progress['sleep_goal_hours']}h")

    # Check Charts data (7 elements each)
    charts = metrics["charts"]
    assert len(charts["labels"]) == 7, f"Expected 7 day labels, got {len(charts['labels'])}"
    assert len(charts["steps"]) == 7, f"Expected 7 steps data points, got {len(charts['steps'])}"
    assert len(charts["water"]) == 7, f"Expected 7 water data points, got {len(charts['water'])}"
    assert len(charts["sleep"]) == 7, f"Expected 7 sleep data points, got {len(charts['sleep'])}"
    print(f"  [PASSED] Weekly Chart.js data arrays verified (7 days history).")

    # 3. Test Dashboard HTML rendering and Chart.js integration
    print("\n[Test 3] Testing GET /dashboard template rendering...")
    dash_res = client.get('/dashboard')
    if dash_res.status_code == 200:
        assert b"Personal Health Dashboard" in dash_res.data
        assert b"weeklyStepsChart" in dash_res.data
        assert b"weeklyWaterChart" in dash_res.data
        assert b"weeklySleepChart" in dash_res.data
        assert b"desktop-sidebar" in dash_res.data
        assert b"mobile-bottom-nav" in dash_res.data
        print("  [PASSED] Dashboard rendered with Sidebar, Mobile Nav, and all 3 Chart.js canvases.")
    else:
        print(f"  [FAILED] Dashboard GET request failed with status {dash_res.status_code}")
        return False

    # 4. Test Quick Log Steps
    print("\n[Test 4] Testing Quick Log Steps endpoint...")
    log_res = client.post('/log/steps', data={"steps": 3500}, follow_redirects=True)
    if log_res.status_code == 200:
        updated_metrics = get_user_dashboard_metrics(user_id)
        assert updated_metrics["today"]["steps"] >= 3500
        print(f"  [PASSED] Quick logged steps updated today's total to {updated_metrics['today']['steps']}.")
    else:
        print("  [FAILED] Quick log steps failed.")
        return False

    # 5. Test Quick Log Water
    print("\n[Test 5] Testing Quick Log Water endpoint...")
    water_log_res = client.post('/log/water', data={"amount_ml": 500, "beverage_type": "Electrolyte Water"}, follow_redirects=True)
    if water_log_res.status_code == 200:
        updated_metrics = get_user_dashboard_metrics(user_id)
        assert updated_metrics["today"]["water_ml"] >= 500
        print(f"  [PASSED] Quick logged water updated today's total to {updated_metrics['today']['water_ml']} ml.")
    else:
        print("  [FAILED] Quick log water failed.")
        return False

    # 6. Test Quick Log Sleep
    print("\n[Test 6] Testing Quick Log Sleep endpoint...")
    sleep_log_res = client.post('/log/sleep', data={"hours": 8.5, "quality": 90}, follow_redirects=True)
    if sleep_log_res.status_code == 200:
        updated_metrics = get_user_dashboard_metrics(user_id)
        assert updated_metrics["today"]["sleep_hours"] == 8.5
        print(f"  [PASSED] Quick logged sleep updated today's total to {updated_metrics['today']['sleep_hours']} hours.")
    else:
        print("  [FAILED] Quick log sleep failed.")
        return False

    print("\n" + "=" * 70)
    print(" ALL STEP 4 DASHBOARD & CHART TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return True


if __name__ == "__main__":
    success = run_dashboard_tests()
    sys.exit(0 if success else 1)
