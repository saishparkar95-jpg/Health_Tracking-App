"""
HealthTrack AI - Weight Tracking Service
Step 8: Weight Tracking Module & BMI Analytics

Handles:
- Adding weight records and computing BMI with user's height
- Updating profile height and target weight goals
- Providing informational BMI categories and healthy weight range estimations
- Aggregating weight progression time-series for Chart.js
- Managing weight history logs with strict user_id isolation
"""

from datetime import date, timedelta, datetime
from database import get_db


def calculate_bmi(weight_kg, height_cm):
    """
    Computes Body Mass Index (BMI) and informational category.
    
    Formula: BMI = weight_kg / (height_m ^ 2)
    
    Returns:
        dict: {
            'bmi': float (rounded to 1 decimal),
            'category': str,
            'category_class': str,
            'description': str,
            'healthy_min_kg': float,
            'healthy_max_kg': float
        }
    """
    if not height_cm or height_cm <= 0 or not weight_kg or weight_kg <= 0:
        return {
            "bmi": 0.0,
            "category": "Unknown",
            "category_class": "muted",
            "description": "Height and weight required to calculate BMI.",
            "healthy_min_kg": 0.0,
            "healthy_max_kg": 0.0
        }

    height_m = height_cm / 100.0
    bmi_raw = weight_kg / (height_m ** 2)
    bmi_val = round(bmi_raw, 1)

    # Calculate standard healthy reference range (BMI 18.5 - 24.9)
    healthy_min_kg = round(18.5 * (height_m ** 2), 1)
    healthy_max_kg = round(24.9 * (height_m ** 2), 1)

    if bmi_val < 18.5:
        cat = "Underweight"
        cat_class = "info"
        desc = "Below standard reference range (< 18.5)."
    elif 18.5 <= bmi_val <= 24.9:
        cat = "Normal Weight"
        cat_class = "success"
        desc = "Within standard reference range (18.5 – 24.9)."
    elif 25.0 <= bmi_val <= 29.9:
        cat = "Overweight"
        cat_class = "warning"
        desc = "Above standard reference range (25.0 – 29.9)."
    else:
        cat = "Obese Range"
        cat_class = "danger"
        desc = "Significantly above reference range (≥ 30.0)."

    return {
        "bmi": bmi_val,
        "category": cat,
        "category_class": cat_class,
        "description": desc,
        "healthy_min_kg": healthy_min_kg,
        "healthy_max_kg": healthy_max_kg
    }


def get_weight_module_data(user_id):
    """
    Retrieves all weight tracking data and BMI metrics for the user.
    """
    today_str = date.today().isoformat()

    with get_db() as db:
        cursor = db.cursor()

        # 1. Fetch user height and goals
        cursor.execute("SELECT height_cm, target_weight_kg FROM users WHERE id = ?;", (user_id,))
        user_row = cursor.fetchone()
        height_cm = float(user_row["height_cm"]) if (user_row and user_row["height_cm"]) else 170.0
        target_weight_kg = float(user_row["target_weight_kg"]) if (user_row and user_row["target_weight_kg"]) else None

        # 2. Fetch all weight records ordered chronologically for charts
        cursor.execute("""
            SELECT id, record_date, weight_kg, bmi, body_fat_percentage, notes, created_at 
            FROM weight_records 
            WHERE user_id = ? 
            ORDER BY record_date ASC, id ASC;
        """, (user_id,))
        chrono_records = [dict(r) for r in cursor.fetchall()]

        # Latest weight record
        if chrono_records:
            latest_rec = chrono_records[-1]
            current_weight_kg = float(latest_rec["weight_kg"])
            latest_date = latest_rec["record_date"]
            first_rec = chrono_records[0]
            first_weight_kg = float(first_rec["weight_kg"])
            overall_diff = round(current_weight_kg - first_weight_kg, 1)
        else:
            current_weight_kg = 70.0
            latest_date = today_str
            overall_diff = 0.0

        # Calculate BMI
        bmi_info = calculate_bmi(current_weight_kg, height_cm)

        # Target weight progress
        target_diff = round(current_weight_kg - target_weight_kg, 1) if target_weight_kg else None

        # 3. Chart Series Data (Recent 30 entries)
        recent_30 = chrono_records[-30:] if len(chrono_records) > 30 else chrono_records
        chart_labels = [r["record_date"] for r in recent_30]
        chart_weights = [r["weight_kg"] for r in recent_30]
        chart_bmis = [r["bmi"] if r["bmi"] else calculate_bmi(r["weight_kg"], height_cm)["bmi"] for r in recent_30]

        # 4. History log (Reverse chronological order for table)
        cursor.execute("""
            SELECT id, record_date, weight_kg, bmi, body_fat_percentage, notes, created_at 
            FROM weight_records 
            WHERE user_id = ? 
            ORDER BY record_date DESC, id DESC 
            LIMIT 40;
        """, (user_id,))
        history = []
        for r in cursor.fetchall():
            item = dict(r)
            if not item.get("bmi"):
                item["bmi"] = calculate_bmi(item["weight_kg"], height_cm)["bmi"]
            history.append(item)

    return {
        "today_str": today_str,
        "user_profile": {
            "height_cm": height_cm,
            "current_weight_kg": current_weight_kg,
            "target_weight_kg": target_weight_kg,
            "target_diff": target_diff,
            "overall_diff": overall_diff,
            "latest_date": latest_date
        },
        "bmi_info": bmi_info,
        "chart": {
            "labels": chart_labels,
            "weights": chart_weights,
            "bmis": chart_bmis,
            "target": target_weight_kg or current_weight_kg
        },
        "history": history
    }


def add_weight_record(user_id, weight_kg, record_date=None, notes=None):
    """
    Inserts a weight log and computes BMI.
    
    Raises:
        ValueError: If weight is outside realistic range (20.0 to 350.0 kg).
    """
    try:
        w_val = round(float(weight_kg), 1)
    except (ValueError, TypeError):
        raise ValueError("Please enter a valid numeric weight in kilograms (kg).")

    if w_val < 20.0 or w_val > 350.0:
        raise ValueError("Weight entry must be between 20.0 kg and 350.0 kg.")

    date_str = record_date or date.today().isoformat()
    clean_notes = (notes or "").strip()[:200] if notes else None

    with get_db() as db:
        cursor = db.cursor()

        # Fetch current height to calculate and store BMI
        cursor.execute("SELECT height_cm, target_weight_kg FROM users WHERE id = ?;", (user_id,))
        user_row = cursor.fetchone()
        height_cm = float(user_row["height_cm"]) if (user_row and user_row["height_cm"]) else 170.0
        target_weight = float(user_row["target_weight_kg"]) if (user_row and user_row["target_weight_kg"]) else None

        bmi_info = calculate_bmi(w_val, height_cm)

        # Insert record
        cursor.execute("""
            INSERT INTO weight_records (user_id, record_date, weight_kg, bmi, notes)
            VALUES (?, ?, ?, ?, ?);
        """, (user_id, date_str, w_val, bmi_info["bmi"], clean_notes))

        # Check for milestone achievements
        cursor.execute("SELECT COUNT(*) as count FROM weight_records WHERE user_id = ?;", (user_id,))
        count = cursor.fetchone()["count"]
        if count >= 1:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'weight_tracker', 'Weight Tracker', 'Logged your first body weight measurement', 'scale', 'weight');
            """, (user_id,))
        if target_weight and abs(w_val - target_weight) <= 0.5:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'goal_weight_met', 'Weight Goal Achieved', 'Reached within 0.5 kg of your target body weight!', 'trophy', 'weight');
            """, (user_id,))

    return w_val, bmi_info["bmi"]


def update_user_height(user_id, height_cm):
    """
    Updates the user's height in profile settings and refreshes stored BMI metrics.
    
    Raises:
        ValueError: If height is outside realistic limits (50.0 to 260.0 cm).
    """
    try:
        h_val = round(float(height_cm), 1)
    except (ValueError, TypeError):
        raise ValueError("Please enter a valid numeric height in centimeters (cm).")

    if h_val < 50.0 or h_val > 260.0:
        raise ValueError("Height must be between 50.0 cm and 260.0 cm.")

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("UPDATE users SET height_cm = ? WHERE id = ?;", (h_val, user_id))

        # Recalculate BMI for all weight records belonging to user
        cursor.execute("SELECT id, weight_kg FROM weight_records WHERE user_id = ?;", (user_id,))
        records = cursor.fetchall()
        for r in records:
            new_bmi = calculate_bmi(r["weight_kg"], h_val)["bmi"]
            cursor.execute("UPDATE weight_records SET bmi = ? WHERE id = ?;", (new_bmi, r["id"]))

    return h_val


def set_user_target_weight(user_id, target_weight_kg):
    """
    Sets or updates the user's target body weight goal.
    """
    try:
        target_val = round(float(target_weight_kg), 1)
    except (ValueError, TypeError):
        raise ValueError("Target weight must be a valid number in kilograms.")

    if target_val < 20.0 or target_val > 350.0:
        raise ValueError("Target weight must be between 20.0 kg and 350.0 kg.")

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("UPDATE users SET target_weight_kg = ? WHERE id = ?;", (target_val, user_id))

        today_str = date.today().isoformat()
        cursor.execute("""
            INSERT INTO goals (user_id, goal_type, title, target_value, unit, start_date, status)
            VALUES (?, 'weight', 'Target Body Weight', ?, 'kg', ?, 'in_progress');
        """, (user_id, target_val, today_str))

    return target_val


def delete_user_weight_record(user_id, record_id):
    """
    Deletes a weight entry.
    """
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("DELETE FROM weight_records WHERE id = ? AND user_id = ?;", (record_id, user_id))
        return cursor.rowcount > 0

