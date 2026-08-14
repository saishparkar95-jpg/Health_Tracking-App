"""
HealthTrack AI - Step Tracking Service
Step 5: Step Tracking Module

Handles:
- Adding and updating today's and historical steps
- Setting and retrieving daily step goals
- Calculating goal percentages and remaining metrics
- Weekly (7-day) and Monthly (30-day) time-series data
- User activity history with edit and delete operations
- Strict user_id isolation
"""

from datetime import date, timedelta
from database import get_db


def calculate_activity_derivatives(steps):
    """
    Computes distance, calories, and active minutes derived from step count.
    - Distance: ~0.00075 km per step (75cm average stride)
    - Calories: ~0.045 kcal per step
    - Active minutes: ~100 steps per active minute of walking
    """
    steps_val = max(0, int(steps))
    distance_km = round(steps_val * 0.00075, 2)
    calories_burned = round(steps_val * 0.045, 1)
    active_minutes = round(steps_val / 100)
    return distance_km, calories_burned, active_minutes


def get_step_module_data(user_id):
    """
    Retrieves all step tracking metrics for the logged-in user:
    - Today's stats & goal progress
    - Weekly (past 7 days) and Monthly (past 30 days) chart series
    - Activity history table
    """
    today = date.today()
    today_str = today.isoformat()

    # 7-day range
    days_7 = [today - timedelta(days=i) for i in range(6, -1, -1)]
    days_7_strs = [d.isoformat() for d in days_7]
    days_7_labels = [d.strftime("%a (%b %d)") for d in days_7]
    days_7_short = [d.strftime("%a") for d in days_7]

    # 30-day range
    days_30 = [today - timedelta(days=i) for i in range(29, -1, -1)]
    days_30_strs = [d.isoformat() for d in days_30]
    days_30_labels = [d.strftime("%b %d") for d in days_30]

    with get_db() as db:
        cursor = db.cursor()

        # 1. Fetch User Goal
        cursor.execute("SELECT daily_step_goal FROM users WHERE id = ?;", (user_id,))
        user_row = cursor.fetchone()
        step_goal = user_row["daily_step_goal"] if (user_row and user_row["daily_step_goal"]) else 10000

        # 2. Today's Activity Record
        cursor.execute("""
            SELECT * FROM daily_activity 
            WHERE user_id = ? AND activity_date = ?;
        """, (user_id, today_str))
        today_row = cursor.fetchone()

        steps_today = today_row["steps"] if today_row else 0
        dist_today = today_row["distance_km"] if today_row else 0.0
        cal_today = today_row["calories_burned"] if today_row else 0.0
        active_mins_today = today_row["active_minutes"] if today_row else 0

        # Calculate progress
        goal_pct = min(100, round((steps_today / step_goal) * 100)) if step_goal > 0 else 0
        raw_goal_pct = round((steps_today / step_goal) * 100, 1) if step_goal > 0 else 0
        steps_remaining = max(0, step_goal - steps_today)
        is_goal_met = steps_today >= step_goal

        # 3. Weekly Data (Past 7 Days)
        cursor.execute("""
            SELECT activity_date, steps, distance_km, calories_burned, active_minutes
            FROM daily_activity
            WHERE user_id = ? AND activity_date >= ? AND activity_date <= ?;
        """, (user_id, days_7_strs[0], days_7_strs[-1]))
        w_records = {r["activity_date"]: dict(r) for r in cursor.fetchall()}

        weekly_steps = [w_records[d]["steps"] if d in w_records else 0 for d in days_7_strs]
        weekly_total = sum(weekly_steps)
        weekly_avg = round(weekly_total / 7)

        # 4. Monthly Data (Past 30 Days)
        cursor.execute("""
            SELECT activity_date, steps, distance_km, calories_burned, active_minutes
            FROM daily_activity
            WHERE user_id = ? AND activity_date >= ? AND activity_date <= ?;
        """, (user_id, days_30_strs[0], days_30_strs[-1]))
        m_records = {r["activity_date"]: dict(r) for r in cursor.fetchall()}

        monthly_steps = [m_records[d]["steps"] if d in m_records else 0 for d in days_30_strs]
        monthly_total = sum(monthly_steps)
        monthly_avg = round(monthly_total / 30)
        best_day_steps = max(monthly_steps) if monthly_steps else 0

        # 5. Full Activity History (Recent 30 entries)
        cursor.execute("""
            SELECT id, activity_date, steps, distance_km, calories_burned, active_minutes, notes, created_at
            FROM daily_activity
            WHERE user_id = ?
            ORDER BY activity_date DESC, id DESC
            LIMIT 30;
        """, (user_id,))
        history = [dict(r) for r in cursor.fetchall()]

    return {
        "today_str": today_str,
        "today": {
            "steps": steps_today,
            "distance_km": dist_today,
            "calories_burned": cal_today,
            "active_minutes": active_mins_today,
            "goal": step_goal,
            "goal_pct": goal_pct,
            "raw_goal_pct": raw_goal_pct,
            "remaining": steps_remaining,
            "is_goal_met": is_goal_met
        },
        "weekly": {
            "labels": days_7_short,
            "full_labels": days_7_labels,
            "data": weekly_steps,
            "total": weekly_total,
            "avg": weekly_avg
        },
        "monthly": {
            "labels": days_30_labels,
            "data": monthly_steps,
            "total": monthly_total,
            "avg": monthly_avg,
            "best_day": best_day_steps
        },
        "history": history
    }


def add_user_steps(user_id, steps_to_add, target_date=None, notes=None):
    """
    Increments today's (or target date's) steps in the database.
    
    Raises:
        ValueError: If steps count is invalid (negative or exceeds 100,000).
    """
    try:
        steps_val = int(steps_to_add)
    except (ValueError, TypeError):
        raise ValueError("Step count must be a valid whole number.")

    if steps_val <= 0:
        raise ValueError("Please enter a positive number of steps to add.")
    if steps_val > 100000:
        raise ValueError("Single step entry cannot exceed 100,000 steps.")

    date_str = target_date or date.today().isoformat()
    dist_km, cal_burned, active_mins = calculate_activity_derivatives(steps_val)

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            INSERT INTO daily_activity (user_id, activity_date, steps, distance_km, calories_burned, active_minutes, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(user_id, activity_date) DO UPDATE SET
                steps = steps + excluded.steps,
                distance_km = ROUND(distance_km + excluded.distance_km, 2),
                calories_burned = ROUND(calories_burned + excluded.calories_burned, 1),
                active_minutes = active_minutes + excluded.active_minutes,
                notes = COALESCE(excluded.notes, daily_activity.notes);
        """, (user_id, date_str, steps_val, dist_km, cal_burned, active_mins, notes))

        # Check total steps for milestone badges
        cursor.execute("SELECT steps FROM daily_activity WHERE user_id = ? AND activity_date = ?;", (user_id, date_str))
        total_day_steps = cursor.fetchone()["steps"]

        if total_day_steps >= 10000:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, '10k_steps_club', '10K Steps Club', 'Walked 10,000+ steps in a single day', 'footprints', 'fitness');
            """, (user_id,))
        if total_day_steps >= 20000:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, '20k_marathoner', '20K Step Master', 'Reached 20,000 steps in one day!', 'trophy', 'fitness');
            """, (user_id,))

    return total_day_steps


def update_user_steps(user_id, exact_steps, target_date=None, notes=None):
    """
    Sets the exact step count for today (or target date) in the database.
    
    Raises:
        ValueError: If steps count is negative.
    """
    try:
        steps_val = int(exact_steps)
    except (ValueError, TypeError):
        raise ValueError("Step count must be a valid whole number.")

    if steps_val < 0:
        raise ValueError("Step count cannot be negative.")
    if steps_val > 150000:
        raise ValueError("Step count cannot exceed 150,000 steps for a single day.")

    date_str = target_date or date.today().isoformat()
    dist_km, cal_burned, active_mins = calculate_activity_derivatives(steps_val)

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            INSERT INTO daily_activity (user_id, activity_date, steps, distance_km, calories_burned, active_minutes, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(user_id, activity_date) DO UPDATE SET
                steps = excluded.steps,
                distance_km = excluded.distance_km,
                calories_burned = excluded.calories_burned,
                active_minutes = excluded.active_minutes,
                notes = COALESCE(excluded.notes, daily_activity.notes);
        """, (user_id, date_str, steps_val, dist_km, cal_burned, active_mins, notes))

    return steps_val


def set_user_step_goal(user_id, new_goal):
    """
    Updates the user's daily step goal in users table and goals table.
    
    Raises:
        ValueError: If goal is outside realistic range (1,000 to 100,000).
    """
    try:
        goal_val = int(new_goal)
    except (ValueError, TypeError):
        raise ValueError("Goal must be a valid whole number.")

    if goal_val < 1000 or goal_val > 100000:
        raise ValueError("Daily step goal must be between 1,000 and 100,000 steps.")

    with get_db() as db:
        cursor = db.cursor()
        # Update users profile
        cursor.execute("UPDATE users SET daily_step_goal = ? WHERE id = ?;", (goal_val, user_id))

        # Update or Insert into goals table
        today_str = date.today().isoformat()
        cursor.execute("""
            INSERT INTO goals (user_id, goal_type, title, target_value, unit, start_date, status)
            VALUES (?, 'steps', 'Daily Steps Goal', ?, 'steps', ?, 'in_progress');
        """, (user_id, goal_val, today_str))

    return goal_val


def delete_user_step_record(user_id, record_id):
    """
    Deletes a step record belonging to the user.
    """
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("DELETE FROM daily_activity WHERE id = ? AND user_id = ?;", (record_id, user_id))
        return cursor.rowcount > 0
