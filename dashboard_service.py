"""
HealthTrack AI - Dashboard Data Service (Step 4)

Fetches and computes real database metrics for the logged-in user:
- Today's overview (Steps, Water, Sleep, Weight, Heart Rate)
- Progress metrics vs daily goals
- Past 7-day time series for weekly charts
- Quick logging handlers
"""

from datetime import date, timedelta, datetime
from database import get_db


def get_past_7_days():
    """
    Returns a list of date objects for the past 7 days up to today.
    """
    today = date.today()
    return [today - timedelta(days=i) for i in range(6, -1, -1)]


def get_user_dashboard_metrics(user_id):
    """
    Aggregates all real-time dashboard data for a given user from SQLite.
    
    Returns:
        dict: Complete dashboard state containing today's stats, progress cards,
              weekly time-series arrays for Chart.js, and user profile data.
    """
    today_str = date.today().isoformat()
    days_7 = get_past_7_days()
    day_date_strings = [d.isoformat() for d in days_7]
    day_labels = [d.strftime("%a (%b %d)") for d in days_7]
    day_short_labels = [d.strftime("%a") for d in days_7]

    with get_db() as db:
        cursor = db.cursor()

        # 1. Fetch User Record
        cursor.execute("SELECT * FROM users WHERE id = ?;", (user_id,))
        user = cursor.fetchone()
        if not user:
            return None

        # User goals & thresholds
        step_goal = user["daily_step_goal"] or 10000
        water_goal_ml = user["daily_water_goal_ml"] or 2500
        sleep_goal_hours = float(user["daily_sleep_goal_hours"] or 8.0)
        calorie_goal = user["daily_calorie_goal"] or 2000
        height_cm = user["height_cm"] or 170.0

        # 2. Today's Activity Record
        cursor.execute("""
            SELECT * FROM daily_activity 
            WHERE user_id = ? AND activity_date = ?;
        """, (user_id, today_str))
        today_activity = cursor.fetchone()

        steps_today = today_activity["steps"] if today_activity else 0
        distance_today = today_activity["distance_km"] if today_activity else 0.0
        calories_today = today_activity["calories_burned"] if today_activity else 0.0
        active_mins_today = today_activity["active_minutes"] if today_activity else 0

        # 3. Today's Water Intake
        cursor.execute("""
            SELECT SUM(amount_ml) as total_ml, COUNT(*) as log_count 
            FROM water_intake 
            WHERE user_id = ? AND intake_date = ?;
        """, (user_id, today_str))
        water_res = cursor.fetchone()
        water_today_ml = water_res["total_ml"] if (water_res and water_res["total_ml"]) else 0
        water_logs_count = water_res["log_count"] if (water_res and water_res["log_count"]) else 0

        # 4. Latest Sleep Record (Today or Most Recent)
        cursor.execute("""
            SELECT * FROM sleep_records 
            WHERE user_id = ? 
            ORDER BY sleep_date DESC, id DESC LIMIT 1;
        """, (user_id,))
        latest_sleep = cursor.fetchone()
        
        if latest_sleep:
            sleep_duration_mins = latest_sleep["duration_minutes"]
            sleep_hours = round(sleep_duration_mins / 60.0, 1)
            sleep_quality = latest_sleep["sleep_quality_score"] or 80
            sleep_deep_mins = latest_sleep["deep_sleep_minutes"] or 0
            sleep_rem_mins = latest_sleep["rem_sleep_minutes"] or 0
        else:
            sleep_duration_mins = 0
            sleep_hours = 0.0
            sleep_quality = 0
            sleep_deep_mins = 0
            sleep_rem_mins = 0

        # 5. Latest Weight Record & Calculated BMI
        cursor.execute("""
            SELECT * FROM weight_records 
            WHERE user_id = ? 
            ORDER BY record_date DESC, id DESC LIMIT 1;
        """, (user_id,))
        latest_weight = cursor.fetchone()
        
        if latest_weight:
            current_weight_kg = latest_weight["weight_kg"]
            body_fat = latest_weight["body_fat_percentage"]
        else:
            current_weight_kg = user["target_weight_kg"] or 70.0
            body_fat = None

        height_m = height_cm / 100.0
        bmi = round(current_weight_kg / (height_m * height_m), 1) if height_m > 0 else 22.0

        # 6. Latest Heart Rate Record
        cursor.execute("""
            SELECT * FROM heart_rate 
            WHERE user_id = ? 
            ORDER BY measured_at DESC, id DESC LIMIT 1;
        """, (user_id,))
        latest_hr = cursor.fetchone()
        
        current_bpm = latest_hr["bpm"] if latest_hr else 72
        resting_hr = latest_hr["resting_heart_rate"] if (latest_hr and latest_hr["resting_heart_rate"]) else 64
        hr_context = latest_hr["activity_context"] if latest_hr else "resting"

        # 7. Calculate Progress Card Percentages
        step_progress_pct = min(100, round((steps_today / step_goal) * 100)) if step_goal > 0 else 0
        water_progress_pct = min(100, round((water_today_ml / water_goal_ml) * 100)) if water_goal_ml > 0 else 0
        sleep_progress_pct = min(100, round((sleep_hours / sleep_goal_hours) * 100)) if sleep_goal_hours > 0 else 0

        # 8. Weekly Chart Series (Past 7 Days real queries)
        # Daily steps query for past 7 days
        cursor.execute("""
            SELECT activity_date, steps, distance_km, calories_burned 
            FROM daily_activity 
            WHERE user_id = ? AND activity_date >= ? AND activity_date <= ?;
        """, (user_id, day_date_strings[0], day_date_strings[-1]))
        act_rows = {row["activity_date"]: dict(row) for row in cursor.fetchall()}

        # Water sum per day for past 7 days
        cursor.execute("""
            SELECT intake_date, SUM(amount_ml) as total_ml 
            FROM water_intake 
            WHERE user_id = ? AND intake_date >= ? AND intake_date <= ?
            GROUP BY intake_date;
        """, (user_id, day_date_strings[0], day_date_strings[-1]))
        water_rows = {row["intake_date"]: row["total_ml"] for row in cursor.fetchall()}

        # Sleep duration per day for past 7 days
        cursor.execute("""
            SELECT sleep_date, duration_minutes, sleep_quality_score 
            FROM sleep_records 
            WHERE user_id = ? AND sleep_date >= ? AND sleep_date <= ?;
        """, (user_id, day_date_strings[0], day_date_strings[-1]))
        sleep_rows = {row["sleep_date"]: dict(row) for row in cursor.fetchall()}

        weekly_steps_data = []
        weekly_water_data = []
        weekly_sleep_data = []

        for d_str in day_date_strings:
            # Steps
            step_val = act_rows[d_str]["steps"] if d_str in act_rows else 0
            weekly_steps_data.append(step_val)

            # Water
            water_val = water_rows[d_str] if d_str in water_rows else 0
            weekly_water_data.append(water_val)

            # Sleep (in hours)
            if d_str in sleep_rows:
                s_hours = round(sleep_rows[d_str]["duration_minutes"] / 60.0, 1)
            else:
                s_hours = 0.0
            weekly_sleep_data.append(s_hours)

        # 9. Recent User Achievements
        cursor.execute("""
            SELECT * FROM achievements 
            WHERE user_id = ? 
            ORDER BY unlocked_at DESC LIMIT 4;
        """, (user_id,))
        achievements = [dict(r) for r in cursor.fetchall()]

    return {
        "user": dict(user),
        "today_str": today_str,
        # Today's Overview
        "today": {
            "steps": steps_today,
            "distance_km": round(distance_today, 2),
            "calories_burned": round(calories_today, 1),
            "active_minutes": active_mins_today,
            "water_ml": water_today_ml,
            "water_logs_count": water_logs_count,
            "sleep_hours": sleep_hours,
            "sleep_quality": sleep_quality,
            "sleep_deep_mins": sleep_deep_mins,
            "sleep_rem_mins": sleep_rem_mins,
            "weight_kg": current_weight_kg,
            "bmi": bmi,
            "heart_rate_bpm": current_bpm,
            "resting_hr": resting_hr,
            "hr_context": hr_context
        },
        # Progress Cards
        "progress": {
            "step_goal": step_goal,
            "step_current": steps_today,
            "step_pct": step_progress_pct,
            "step_remaining": max(0, step_goal - steps_today),
            
            "water_goal_ml": water_goal_ml,
            "water_current_ml": water_today_ml,
            "water_pct": water_progress_pct,
            "water_remaining_ml": max(0, water_goal_ml - water_today_ml),

            "sleep_goal_hours": sleep_goal_hours,
            "sleep_current_hours": sleep_hours,
            "sleep_pct": sleep_progress_pct,
            "sleep_quality": sleep_quality
        },
        # Chart Data
        "charts": {
            "labels": day_labels,
            "short_labels": day_short_labels,
            "steps": weekly_steps_data,
            "water": weekly_water_data,
            "sleep": weekly_sleep_data,
            "step_goal": step_goal,
            "water_goal": water_goal_ml,
            "sleep_goal": sleep_goal_hours
        },
        "achievements": achievements
    }


def ensure_user_has_initial_data(user_id):
    """
    Populates sample activity history for the user if their database tables are completely empty,
    ensuring newly registered users see meaningful charts and progress right away.
    """
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT COUNT(*) as count FROM daily_activity WHERE user_id = ?;", (user_id,))
        if cursor.fetchone()["count"] > 0:
            return  # Already has records

        today = date.today()
        # Seed 7 days of realistic baseline records
        sample_activities = [
            (6, 6800, 4.8, 380, 45),
            (5, 8200, 5.9, 430, 52),
            (4, 9400, 6.7, 490, 58),
            (3, 7100, 5.0, 395, 42),
            (2, 10500, 7.6, 550, 70),
            (1, 8900, 6.3, 460, 55),
            (0, 7850, 5.5, 410, 50),
        ]
        
        sample_waters = [
            (6, [500, 500, 750]),
            (5, [750, 500, 750, 250]),
            (4, [500, 500, 500, 500]),
            (3, [750, 750, 500]),
            (2, [1000, 750, 750]),
            (1, [500, 500, 750, 500]),
            (0, [500, 750, 500]),
        ]

        sample_sleeps = [
            (6, 450, 82, 90, 80, 280),
            (5, 420, 76, 75, 70, 275),
            (4, 480, 89, 105, 95, 280),
            (3, 410, 74, 70, 65, 275),
            (2, 510, 92, 115, 105, 290),
            (1, 460, 85, 95, 85, 280),
            (0, 475, 88, 100, 90, 285),
        ]

        for days_ago, steps, dist, cal, active_m in sample_activities:
            d_str = (today - timedelta(days=days_ago)).isoformat()
            cursor.execute("""
                INSERT OR IGNORE INTO daily_activity (user_id, activity_date, steps, distance_km, calories_burned, active_minutes)
                VALUES (?, ?, ?, ?, ?, ?);
            """, (user_id, d_str, steps, dist, cal, active_m))

        for days_ago, amounts in sample_waters:
            d_str = (today - timedelta(days=days_ago)).isoformat()
            for amt in amounts:
                cursor.execute("""
                    INSERT INTO water_intake (user_id, intake_date, amount_ml, beverage_type)
                    VALUES (?, ?, ?, 'Water');
                """, (user_id, d_str, amt))

        for days_ago, mins, quality, deep, rem, light in sample_sleeps:
            d_date = today - timedelta(days=days_ago)
            d_str = d_date.isoformat()
            bedtime = f"{d_str} 23:00:00"
            wake = f"{d_str} 07:00:00"
            cursor.execute("""
                INSERT INTO sleep_records (user_id, sleep_date, bedtime, wake_time, duration_minutes, sleep_quality_score, deep_sleep_minutes, rem_sleep_minutes, light_sleep_minutes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            """, (user_id, d_str, bedtime, wake, mins, quality, deep, rem, light))

        # Insert Heart Rate samples
        cursor.execute("""
            INSERT INTO heart_rate (user_id, measured_at, bpm, resting_heart_rate, activity_context)
            VALUES 
                (?, datetime('now', '-3 hours'), 68, 62, 'resting'),
                (?, datetime('now', '-1 hours'), 125, 62, 'workout'),
                (?, datetime('now'), 72, 62, 'normal');
        """, (user_id, user_id, user_id))


def log_user_activity(user_id, steps, distance_km=None, calories=None, active_mins=None):
    """
    Logs or increments today's activity for the user.
    """
    today_str = date.today().isoformat()
    steps_val = int(steps)
    dist_val = float(distance_km) if distance_km is not None else round(steps_val * 0.00075, 2)
    cal_val = float(calories) if calories is not None else round(steps_val * 0.045, 1)
    act_val = int(active_mins) if active_mins is not None else round(steps_val / 150)

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            INSERT INTO daily_activity (user_id, activity_date, steps, distance_km, calories_burned, active_minutes)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(user_id, activity_date) DO UPDATE SET
                steps = steps + excluded.steps,
                distance_km = distance_km + excluded.distance_km,
                calories_burned = calories_burned + excluded.calories_burned,
                active_minutes = active_minutes + excluded.active_minutes;
        """, (user_id, today_str, steps_val, dist_val, cal_val, act_val))


def log_user_water(user_id, amount_ml, beverage_type="Water"):
    """
    Logs a hydration record for today.
    """
    today_str = date.today().isoformat()
    amt_val = int(amount_ml)
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            INSERT INTO water_intake (user_id, intake_date, amount_ml, beverage_type)
            VALUES (?, ?, ?, ?);
        """, (user_id, today_str, amt_val, beverage_type))


def log_user_sleep(user_id, hours, quality_score=85):
    """
    Logs a sleep record for today.
    """
    today_str = date.today().isoformat()
    total_mins = int(float(hours) * 60)
    deep_mins = int(total_mins * 0.22)
    rem_mins = int(total_mins * 0.20)
    light_mins = total_mins - deep_mins - rem_mins
    
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            INSERT INTO sleep_records (
                user_id, sleep_date, bedtime, wake_time, duration_minutes,
                sleep_quality_score, deep_sleep_minutes, rem_sleep_minutes, light_sleep_minutes
            ) VALUES (?, ?, datetime('now', '-8 hours'), datetime('now'), ?, ?, ?, ?, ?);
        """, (user_id, today_str, total_mins, int(quality_score), deep_mins, rem_mins, light_mins))


def log_user_weight(user_id, weight_kg):
    """
    Logs a weight record for today and updates target weight in user table.
    """
    today_str = date.today().isoformat()
    w_val = float(weight_kg)
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT height_cm FROM users WHERE id = ?;", (user_id,))
        user = cursor.fetchone()
        height_m = (user["height_cm"] if user else 175.0) / 100.0
        bmi_val = round(w_val / (height_m * height_m), 1) if height_m > 0 else 22.0

        cursor.execute("""
            INSERT INTO weight_records (user_id, record_date, weight_kg, bmi)
            VALUES (?, ?, ?, ?);
        """, (user_id, today_str, w_val, bmi_val))

        cursor.execute("UPDATE users SET target_weight_kg = ? WHERE id = ?;", (w_val, user_id))


def log_user_heart_rate(user_id, bpm, context="normal"):
    """
    Logs a heart rate measurement.
    """
    bpm_val = int(bpm)
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            INSERT INTO heart_rate (user_id, bpm, activity_context)
            VALUES (?, ?, ?);
        """, (user_id, bpm_val, context))
