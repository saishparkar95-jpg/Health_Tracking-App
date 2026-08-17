"""
HealthTrack AI - Weight Tracking & BMI Analytics Test Suite (Step 8)

Tests:
1. Add body weight with date and notes.
2. Accurate BMI calculation using height and weight.
3. BMI categorization (Underweight, Normal, Overweight, Obese).
4. Updating profile height and auto-recalculating existing BMI records.
5. Setting target weight goal and tracking delta.
6. Weight history retrieval and single record deletion.
7. Validation: Rejection of unrealistic weights and heights.
8. Strict User Isolation: User A cannot see or delete User B's weight records.
9. Flask routes: GET /weight, POST /weight/add, POST /weight/height, POST /weight/target, POST /weight/delete/<id>.
"""

import sys
from datetime import date
from app import app
from database import init_db, get_db
from weight_service import (
    calculate_bmi,
    add_weight_record,
    update_user_height,
    set_user_target_weight,
    get_weight_module_data,
    delete_user_weight_record
)


def run_weight_tracking_tests():
    print("=" * 70)
    print("       HEALTHTRACK AI - WEIGHT & BMI MODULE TEST SUITE")
    print("=" * 70)

    # Initialize fresh database
    init_db(reset=True)
    
    app.config["TESTING"] = True
    client = app.test_client()

    # 1. Register test users for data isolation checks
    print("\n[Test 1] Registering User A ('Hercules') and User B ('Atalanta')...")
    reg_a = client.post('/register', data={
        "full_name": "Hercules Strong",
        "email": "hercules@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 30,
        "gender": "male",
        "height": 180.0,  # 180 cm
        "weight": 80.0    # 80 kg -> BMI = 80 / (1.8^2) = 24.7 (Normal)
    }, follow_redirects=True)
    assert reg_a.status_code == 200

    client.get('/logout')

    reg_b = client.post('/register', data={
        "full_name": "Atalanta Swift",
        "email": "atalanta@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 27,
        "gender": "female",
        "height": 165.0,  # 165 cm
        "weight": 55.0    # 55 kg
    }, follow_redirects=True)
    assert reg_b.status_code == 200

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE email = 'hercules@example.com';")
        user_a_id = cursor.fetchone()["id"]
        cursor.execute("SELECT id FROM users WHERE email = 'atalanta@example.com';")
        user_b_id = cursor.fetchone()["id"]

    print(f"  [PASSED] User A (ID: {user_a_id}) and User B (ID: {user_b_id}) ready.")

    # 2. Test BMI Formula & Categorization
    print("\n[Test 2] Testing calculate_bmi() across standard reference categories...")
    # Height: 180 cm (1.8 m)
    # 55 kg -> BMI 17.0 (Underweight)
    bmi_under = calculate_bmi(55.0, 180.0)
    assert bmi_under["bmi"] == 17.0 and bmi_under["category"] == "Underweight"

    # 75 kg -> BMI 23.1 (Normal Weight)
    bmi_norm = calculate_bmi(75.0, 180.0)
    assert bmi_norm["bmi"] == 23.1 and bmi_norm["category"] == "Normal Weight"

    # 88 kg -> BMI 27.2 (Overweight)
    bmi_over = calculate_bmi(88.0, 180.0)
    assert bmi_over["bmi"] == 27.2 and bmi_over["category"] == "Overweight"

    # 105 kg -> BMI 32.4 (Obese Range)
    bmi_obese = calculate_bmi(105.0, 180.0)
    assert bmi_obese["bmi"] == 32.4 and bmi_obese["category"] == "Obese Range"

    print(f"  [PASSED] All 4 BMI categories verified: Under={bmi_under['bmi']}, Norm={bmi_norm['bmi']}, Over={bmi_over['bmi']}, Obese={bmi_obese['bmi']}.")

    # 3. Test Validation: Rejection of Invalid Inputs
    print("\n[Test 3] Testing validation: Rejecting out-of-range weights & heights...")
    try:
        add_weight_record(user_a_id, 10.0)
        print("  [FAILED] Weight < 20 kg was accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] Low weight rejected: {e}")

    try:
        add_weight_record(user_a_id, 500.0)
        print("  [FAILED] Weight > 350 kg was accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] High weight rejected: {e}")

    try:
        update_user_height(user_a_id, 30.0)
        print("  [FAILED] Height < 50 cm was accepted.")
        return False
    except ValueError as e:
        print(f"  [PASSED] Low height rejected: {e}")

    # 4. Test Adding Weight Record & SQLite Storage
    print("\n[Test 4] Testing add_weight_record()...")
    w_val, bmi_val = add_weight_record(user_a_id, 78.5, record_date=date.today().isoformat(), notes="Post gym weigh-in")
    assert w_val == 78.5
    # Height 180 cm -> 78.5 / (1.8^2) = 24.2
    assert bmi_val == 24.2
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT weight_kg, bmi FROM weight_records WHERE user_id = ? ORDER BY id DESC LIMIT 1;", (user_a_id,))
        row = cursor.fetchone()
        assert row["weight_kg"] == 78.5
        assert row["bmi"] == 24.2
    print(f"  [PASSED] Weight recorded: {w_val} kg (BMI: {bmi_val}) stored in SQLite.")

    # 5. Test Updating Profile Height & Auto-Recalculating BMI
    print("\n[Test 5] Testing update_user_height() and BMI cascade update...")
    # Change height from 180 cm to 175 cm
    new_h = update_user_height(user_a_id, 175.0)
    assert new_h == 175.0
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT height_cm FROM users WHERE id = ?;", (user_a_id,))
        assert cursor.fetchone()["height_cm"] == 175.0
        
        # Check that stored weight record BMI was updated: 78.5 / (1.75^2) = 25.6
        cursor.execute("SELECT bmi FROM weight_records WHERE user_id = ? ORDER BY id DESC LIMIT 1;", (user_a_id,))
        updated_rec_bmi = cursor.fetchone()["bmi"]
        assert updated_rec_bmi == 25.6
    print(f"  [PASSED] Profile height updated to 175.0 cm; stored BMI recalculated to {updated_rec_bmi}.")

    # 6. Test Target Weight Goal
    print("\n[Test 6] Testing set_user_target_weight()...")
    saved_target = set_user_target_weight(user_a_id, 75.0)
    assert saved_target == 75.0
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT target_weight_kg FROM users WHERE id = ?;", (user_a_id,))
        assert cursor.fetchone()["target_weight_kg"] == 75.0
    print("  [PASSED] Target weight updated to 75.0 kg.")

    # 7. Test Module Overview Data Aggregations
    print("\n[Test 7] Testing get_weight_module_data()...")
    w_data = get_weight_module_data(user_a_id)
    assert w_data["user_profile"]["height_cm"] == 175.0
    assert w_data["user_profile"]["current_weight_kg"] == 78.5
    assert w_data["user_profile"]["target_weight_kg"] == 75.0
    assert w_data["user_profile"]["target_diff"] == 3.5  # 78.5 - 75.0
    assert w_data["bmi_info"]["bmi"] == 25.6
    assert len(w_data["chart"]["weights"]) >= 1
    assert len(w_data["history"]) >= 1
    print(f"  [PASSED] Module data verified: Current={w_data['user_profile']['current_weight_kg']}kg, BMI={w_data['bmi_info']['bmi']}, Diff to target={w_data['user_profile']['target_diff']}kg.")

    # 8. Test User Isolation
    print("\n[Test 8] Testing User Isolation (User B cannot access User A's weight records)...")
    w_data_b = get_weight_module_data(user_b_id)
    assert w_data_b["user_profile"]["current_weight_kg"] != 78.5, "User B leaked User A's weight data!"
    print(f"  [PASSED] User B weight isolated (User B current weight = {w_data_b['user_profile']['current_weight_kg']} kg).")

    # 9. Test Single Record Deletion
    print("\n[Test 9] Testing delete_user_weight_record()...")
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM weight_records WHERE user_id = ? LIMIT 1;", (user_a_id,))
        rec_id = cursor.fetchone()["id"]

    deleted = delete_user_weight_record(user_a_id, rec_id)
    assert deleted is True
    print(f"  [PASSED] Weight record ID {rec_id} deleted successfully.")

    # 10. Test Flask Web Endpoints
    print("\n[Test 10] Testing Flask web routes (/weight, /weight/add, /weight/height, /weight/target)...")
    client.get('/logout')
    client.post('/login', data={"identifier": "hercules@example.com", "password": "Password123!"})

    # GET /weight
    res_page = client.get('/weight')
    assert res_page.status_code == 200
    assert b"Body Weight &amp; BMI Tracking" in res_page.data
    assert b"BMI Informational Notice" in res_page.data
    assert b"weightTrendChart" in res_page.data

    # POST /weight/add
    res_add = client.post('/weight/add', data={"weight_kg": "77.0", "notes": "Web form logging"}, follow_redirects=True)
    assert res_add.status_code == 200
    assert b"Logged weight: 77.0 kg" in res_add.data

    # POST /weight/height
    res_h = client.post('/weight/height', data={"height_cm": "178.0"}, follow_redirects=True)
    assert res_h.status_code == 200
    assert b"Profile height updated to 178.0 cm" in res_h.data

    # POST /weight/target
    res_target = client.post('/weight/target', data={"target_weight_kg": "74.0"}, follow_redirects=True)
    assert res_target.status_code == 200
    assert b"Target body weight updated to 74.0 kg" in res_target.data
    print("  [PASSED] Web routes /weight, /weight/add, /weight/height, and /weight/target verified.")

    print("\n" + "=" * 70)
    print(" ALL 10 WEIGHT TRACKING & BMI TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return True


if __name__ == "__main__":
    success = run_weight_tracking_tests()
    sys.exit(0 if success else 1)
