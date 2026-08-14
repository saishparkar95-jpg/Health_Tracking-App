"""
HealthTrack AI - Water Tracking Service
Step 6: Water Tracking Module

Handles:
- Adding water intake logs (quick presets & custom amounts)
- Setting and retrieving daily hydration goals
- Computing daily totals, progress percentages, and remaining ml
- Aggregating weekly (7-day) hydration time-series for Chart.js
- Retrieving and deleting user water intake logs with strict user_id isolation
"""

from datetime import date, timedelta, datetime
from database import get_db


def get_water_module_data(user_id):
    """
    Retrieves all water tracking metrics for the logged-in user:
    - Today's total ml, goal, progress %, glasses equivalent
    - Weekly (past 7 days) chart data
    - Water intake history log
    """
    today = date.today()
    today_str = today.isoformat()

    # Past 7 days
    days_7 = [today - timedelta(days=i) for i in range(6, -1, -1)]
    days_7_strs = [d.isoformat() for d in days_7]
    days_7_short = [d.strftime("%a") for d in days_7]
    days_7_labels = [d.strftime("%a (%b %d)") for d in days_7]

    with get_db() as db:
        cursor = db.cursor()

        # 1. Fetch User Water Goal
        cursor.execute("SELECT daily_water_goal_ml FROM users WHERE id = ?;", (user_id,))
        user_row = cursor.fetchone()
        water_goal = user_row["daily_water_goal_ml"] if (user_row and user_row["daily_water_goal_ml"]) else 2500

        # 2. Today's Water Logs & Sum
        cursor.execute("""
            SELECT id, intake_date, amount_ml, beverage_type, logged_at 
            FROM water_intake 
            WHERE user_id = ? AND intake_date = ?
            ORDER BY logged_at DESC, id DESC;
        """, (user_id, today_str))
        today_logs = [dict(r) for r in cursor.fetchall()]

        today_total_ml = sum(r["amount_ml"] for r in today_logs)
        today_log_count = len(today_logs)
        today_glasses = round(today_total_ml / 250.0, 1)

        # Calculate progress percentages
        goal_pct = min(100, round((today_total_ml / water_goal) * 100)) if water_goal > 0 else 0
        raw_goal_pct = round((today_total_ml / water_goal) * 100, 1) if water_goal > 0 else 0
        remaining_ml = max(0, water_goal - today_total_ml)
        is_goal_met = today_total_ml >= water_goal

        # 3. Weekly Data (Past 7 Days Sums)
        cursor.execute("""
            SELECT intake_date, SUM(amount_ml) as total_ml 
            FROM water_intake 
            WHERE user_id = ? AND intake_date >= ? AND intake_date <= ?
            GROUP BY intake_date;
        """, (user_id, days_7_strs[0], days_7_strs[-1]))
        w_sums = {r["intake_date"]: r["total_ml"] for r in cursor.fetchall()}

        weekly_water_data = [w_sums[d] if d in w_sums else 0 for d in days_7_strs]
        weekly_total = sum(weekly_water_data)
        weekly_avg = round(weekly_total / 7)

        # 4. Full Water History (Recent 40 records)
        cursor.execute("""
            SELECT id, intake_date, amount_ml, beverage_type, logged_at 
            FROM water_intake 
            WHERE user_id = ? 
            ORDER BY intake_date DESC, logged_at DESC, id DESC 
            LIMIT 40;
        """, (user_id,))
        history = [dict(r) for r in cursor.fetchall()]

    return {
        "today_str": today_str,
        "today": {
            "total_ml": today_total_ml,
            "goal_ml": water_goal,
            "goal_pct": goal_pct,
            "raw_goal_pct": raw_goal_pct,
            "remaining_ml": remaining_ml,
            "glasses": today_glasses,
            "log_count": today_log_count,
            "is_goal_met": is_goal_met,
            "logs": today_logs
        },
        "weekly": {
            "labels": days_7_short,
            "full_labels": days_7_labels,
            "data": weekly_water_data,
            "total_ml": weekly_total,
            "avg_ml": weekly_avg,
            "goal_ml": water_goal
        },
        "history": history
    }


def add_water_intake(user_id, amount_ml, beverage_type="Water", intake_date=None):
    """
    Records a hydration intake event in SQLite.
    
    Raises:
        ValueError: If amount is non-positive or exceeds realistic limits (5,000ml).
    """
    try:
        amt = int(amount_ml)
    except (ValueError, TypeError):
        raise ValueError("Water volume must be a valid whole number in milliliters (ml).")

    if amt <= 0:
        raise ValueError("Please enter a positive water amount greater than 0 ml.")
    if amt > 5000:
        raise ValueError("Single water intake entry cannot exceed 5,000 ml (5 Liters).")

    bev_clean = (beverage_type or "Water").strip()
    if len(bev_clean) > 30:
        bev_clean = bev_clean[:30]

    date_str = intake_date or date.today().isoformat()

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            INSERT INTO water_intake (user_id, intake_date, amount_ml, beverage_type, logged_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP);
        """, (user_id, date_str, amt, bev_clean))

        # Check total daily intake for achievement milestones
        cursor.execute("""
            SELECT SUM(amount_ml) as total_ml 
            FROM water_intake 
            WHERE user_id = ? AND intake_date = ?;
        """, (user_id, date_str))
        res = cursor.fetchone()
        new_day_total = res["total_ml"] if res else amt

        cursor.execute("SELECT daily_water_goal_ml FROM users WHERE id = ?;", (user_id,))
        user_goal = cursor.fetchone()["daily_water_goal_ml"] or 2500

        if new_day_total >= user_goal:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'water_champion', 'Hydration Champion', 'Reached your daily water intake goal!', 'droplet', 'hydration');
            """, (user_id,))
        if new_day_total >= 3500:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'aqua_master', 'Aqua Master', 'Drank 3,500+ ml in a single day!', 'shield', 'hydration');
            """, (user_id,))

    return new_day_total


def set_user_water_goal(user_id, new_goal_ml):
    """
    Updates the user's daily hydration target.
    
    Raises:
        ValueError: If goal is outside realistic range (500ml to 10,000ml).
    """
    try:
        goal_val = int(new_goal_ml)
    except (ValueError, TypeError):
        raise ValueError("Water goal must be a valid whole number in milliliters.")

    if goal_val < 500 or goal_val > 10000:
        raise ValueError("Daily water goal must be between 500 ml and 10,000 ml.")

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("UPDATE users SET daily_water_goal_ml = ? WHERE id = ?;", (goal_val, user_id))

        # Update goals table
        today_str = date.today().isoformat()
        cursor.execute("""
            INSERT INTO goals (user_id, goal_type, title, target_value, unit, start_date, status)
            VALUES (?, 'water', 'Daily Water Goal', ?, 'ml', ?, 'in_progress');
        """, (user_id, goal_val, today_str))

    return goal_val


def delete_user_water_log(user_id, log_id):
    """
    Deletes a water intake record belonging to the user.
    """
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("DELETE FROM water_intake WHERE id = ? AND user_id = ?;", (log_id, user_id))
        return cursor.rowcount > 0
