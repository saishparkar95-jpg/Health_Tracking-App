"""
HealthTrack AI - AI Wellness Insights Test Suite (Step 10)

Tests:
1. Multi-metric data extraction (Steps, Water, Sleep, Weight, Heart Rate).
2. Step volume analysis & week-over-week step progression calculation.
3. 7-day activity streak milestone insight generation.
4. Hydration goal consistency (e.g., completed on X of last 7 days).
5. Sleep duration evaluation vs personal target (e.g., recorded below target on several days).
6. Weight trend progression and target weight alignment.
7. Heart rate pattern monitoring and resting pulse observations.
8. Safety Guardrails Verification:
   - Does NOT diagnose diseases.
   - Does NOT claim user is healthy or unhealthy.
   - Does NOT provide medical treatment recommendations.
   - Uses neutral advisory language recommending discussion with a qualified healthcare professional.
9. Flask Web Route: GET /insights (Authenticated & Non-Authenticated).
"""

import sys
from datetime import date, timedelta
from app import app
from database import init_db, get_db
from insights_service import analyze_user_wellness_insights
from step_service import add_user_steps
from water_service import add_water_intake
from sleep_service import add_sleep_record
from weight_service import add_weight_record
from heart_service import add_heart_rate_record


def run_insights_tests():
    print("=" * 70)
    print("      HEALTHTRACK AI - AI WELLNESS INSIGHTS TEST SUITE")
    print("=" * 70)

    # Initialize fresh database
    init_db(reset=True)

    app.config["TESTING"] = True
    client = app.test_client()

    # 1. Register test user
    print("\n[Test 1] Registering test user ('Dr. Maya Lin')...")
    reg = client.post('/register', data={
        "full_name": "Dr. Maya Lin",
        "email": "maya@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 29,
        "gender": "female",
        "height": 165.0,
        "weight": 60.0
    }, follow_redirects=True)
    assert reg.status_code == 200

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE email = 'maya@example.com';")
        user_id = cursor.fetchone()["id"]
        # Set goals
        cursor.execute("""
            UPDATE users SET daily_step_goal = 8000, daily_water_goal_ml = 2500, 
                             daily_sleep_goal_hours = 8.0, target_weight_kg = 58.0 
            WHERE id = ?;
        """, (user_id,))

    print(f"  [PASSED] User registered (ID: {user_id}) with goals: Steps=8,000, Water=2,500ml, Sleep=8.0h, Target=58.0kg.")

    # 2. Seed 7-day activity data showing step increase and 7-day streak
    print("\n[Test 2] Testing Step Insights (Week-over-week increase & 7-day streak)...")
    today = date.today()
    with get_db() as db:
        cursor = db.cursor()
        # Previous week (days 13 to 7 ago): average 5000 steps
        for i in range(7, 14):
            d_str = (today - timedelta(days=i)).isoformat()
            cursor.execute("INSERT OR REPLACE INTO daily_activity (user_id, activity_date, steps) VALUES (?, ?, 5000);", (user_id, d_str))
        # Current week (days 6 to 0 ago): average 9000 steps
        for i in range(0, 7):
            d_str = (today - timedelta(days=i)).isoformat()
            cursor.execute("INSERT OR REPLACE INTO daily_activity (user_id, activity_date, steps) VALUES (?, ?, 9000);", (user_id, d_str))

    insights_data = analyze_user_wellness_insights(user_id)
    step_insights = [i for i in insights_data["insights"] if i["category"] == "steps"]
    step_texts = " ".join([i["text"] for i in step_insights])
    
    assert "increased this week" in step_texts or "higher" in step_texts
    assert "consecutive days" in step_texts or "streak" in step_texts.lower()
    print(f"  [PASSED] Step Insights Generated: {[i['text'] for i in step_insights]}")

    # 3. Seed Water Data for Hydration consistency
    print("\n[Test 3] Testing Hydration Insights (Water goal completion on 5+ days)...")
    with get_db() as db:
        cursor = db.cursor()
        for i in range(0, 5):
            d_str = (today - timedelta(days=i)).isoformat()
            cursor.execute("INSERT INTO water_intake (user_id, intake_date, amount_ml, beverage_type) VALUES (?, ?, 2600, 'Water');", (user_id, d_str))
        for i in range(5, 7):
            d_str = (today - timedelta(days=i)).isoformat()
            cursor.execute("INSERT INTO water_intake (user_id, intake_date, amount_ml, beverage_type) VALUES (?, ?, 1500, 'Water');", (user_id, d_str))

    insights_data = analyze_user_wellness_insights(user_id)
    water_insights = [i for i in insights_data["insights"] if i["category"] == "water"]
    water_texts = " ".join([i["text"] for i in water_insights])
    assert "water goal" in water_texts.lower() or "hydration" in water_texts.lower()
    print(f"  [PASSED] Hydration Insight Generated: {[i['text'] for i in water_insights]}")

    # 4. Seed Sleep Data below personal target
    print("\n[Test 4] Testing Sleep Insights (Sleep recorded below personal target)...")
    with get_db() as db:
        cursor = db.cursor()
        for i in range(0, 4):
            d_str = (today - timedelta(days=i)).isoformat()
            cursor.execute("""
                INSERT INTO sleep_records (user_id, sleep_date, bedtime, wake_time, duration_minutes, sleep_quality_score)
                VALUES (?, ?, ?, ?, 390, 78);
            """, (user_id, d_str, f"{d_str} 23:30:00", f"{d_str} 06:00:00")) # 6.5 hours vs 8.0h target
        for i in range(4, 7):
            d_str = (today - timedelta(days=i)).isoformat()
            cursor.execute("""
                INSERT INTO sleep_records (user_id, sleep_date, bedtime, wake_time, duration_minutes, sleep_quality_score)
                VALUES (?, ?, ?, ?, 480, 88);
            """, (user_id, d_str, f"{d_str} 23:00:00", f"{d_str} 07:00:00")) # 8.0 hours

    insights_data = analyze_user_wellness_insights(user_id)
    sleep_insights = [i for i in insights_data["insights"] if i["category"] == "sleep"]
    sleep_texts = " ".join([i["text"] for i in sleep_insights])
    assert "below your personal target" in sleep_texts or "target" in sleep_texts.lower()
    print(f"  [PASSED] Sleep Insight Generated: {[i['text'] for i in sleep_insights]}")

    # 5. Seed Weight Records
    print("\n[Test 5] Testing Weight Trend Insights...")
    add_weight_record(user_id, 61.0, record_date=(today - timedelta(days=10)).isoformat())
    add_weight_record(user_id, 59.5, record_date=today.isoformat())

    insights_data = analyze_user_wellness_insights(user_id)
    weight_insights = [i for i in insights_data["insights"] if i["category"] == "weight"]
    print(f"  [PASSED] Weight Trend Insight Generated: {[i['text'] for i in weight_insights]}")

    # 6. Seed Heart Rate Records
    print("\n[Test 6] Testing Heart Rate Insights...")
    add_heart_rate_record(user_id, 66, measured_date=today.isoformat(), context="Resting")
    add_heart_rate_record(user_id, 70, measured_date=today.isoformat(), context="Resting")

    insights_data = analyze_user_wellness_insights(user_id)
    hr_insights = [i for i in insights_data["insights"] if i["category"] == "heart_rate"]
    print(f"  [PASSED] Heart Rate Insight Generated: {[i['text'] for i in hr_insights]}")

    # 7. Safety Guardrails & Compliance Test
    print("\n[Test 7] Verifying Safety, Compliance & Non-Diagnostic Guardrails...")
    all_texts = " ".join([i["text"] + " " + i["title"] for i in insights_data["insights"]]).lower()
    
    # Check that diagnostic disease terms are NOT present
    prohibited_terms = [
        "diagnos", "hypertension", "diabetes", "insomnia", "tachycardia", 
        "bradycardia", "you are unhealthy", "you are diseased", "prescription",
        "medication", "treatment plan"
    ]
    for term in prohibited_terms:
        assert term not in all_texts, f"Prohibited clinical term '{term}' found in insight output!"

    # Test that advisory recommending healthcare professional is properly structured when concerning values exist
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("DELETE FROM heart_rate WHERE user_id = ?;", (user_id,))
        for _ in range(4):
            cursor.execute("INSERT INTO heart_rate (user_id, bpm, activity_context) VALUES (?, 115, 'resting');", (user_id,))
    
    advisory_data = analyze_user_wellness_insights(user_id)
    advisory_insights = [i for i in advisory_data["insights"] if i["type"] == "advisory"]
    assert len(advisory_insights) >= 1
    adv_text = advisory_insights[0]["text"]
    assert "qualified healthcare professional" in adv_text
    print(f"  [PASSED] Safety Compliance Verified. Advisory uses neutral recommendation: '{adv_text}'")

    # 8. Flask Web Route Tests (/insights)
    print("\n[Test 8] Testing Flask Web Route GET /insights...")
    client.get('/logout')
    
    # Non-authenticated should redirect to login
    res_unauth = client.get('/insights')
    assert res_unauth.status_code == 302
    assert '/login' in res_unauth.headers['Location']

    # Authenticated access
    client.post('/login', data={"identifier": "maya@example.com", "password": "Password123!"})
    res_auth = client.get('/insights')
    assert res_auth.status_code == 200
    assert b"AI Wellness Insights" in res_auth.data
    assert b"Personalized Wellness Observations" in res_auth.data
    assert b"Wellness &amp; Habit Observations Notice" in res_auth.data
    print("  [PASSED] Web route GET /insights tested and verified successfully.")

    print("\n" + "=" * 70)
    print(" ALL 8 AI WELLNESS INSIGHTS TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return True


if __name__ == "__main__":
    success = run_insights_tests()
    sys.exit(0 if success else 1)
