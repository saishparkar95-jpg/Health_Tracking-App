"""
HealthTrack AI - Goals & Achievements Service
Step 9: Goals Dashboard & Dynamic Achievement Unlocking System

Handles:
- Goal management (Steps, Water, Sleep, Target Weight)
- Today's progress evaluation and percentage calculation
- Dynamic achievement engine that inspects user activities and unlocks milestone badges
"""

from datetime import date, timedelta, datetime
from database import get_db


# Catalog of available achievements in HealthTrack AI
ACHIEVEMENT_CATALOG = [
    {
        "key": "first_5k_steps",
        "name": "First 5,000 Steps",
        "description": "Walked at least 5,000 steps in a single day",
        "icon": "👟",
        "category": "fitness"
    },
    {
        "key": "10k_steps_club",
        "name": "10,000 Steps Club",
        "description": "Achieved the gold standard of 10,000 steps in a single day",
        "icon": "🏆",
        "category": "fitness"
    },
    {
        "key": "activity_streak_7d",
        "name": "7-Day Activity Streak",
        "description": "Maintained active step logs for 7 consecutive days",
        "icon": "🔥",
        "category": "consistency"
    },
    {
        "key": "water_champion",
        "name": "Hydration Champion",
        "description": "Reached 100% of your daily water intake goal",
        "icon": "💧",
        "category": "hydration"
    },
    {
        "key": "sleep_champion",
        "name": "Sleep Champion",
        "description": "Logged 8+ hours of restful sleep session",
        "icon": "🌙",
        "category": "sleep"
    },
    {
        "key": "weight_tracker",
        "name": "Weight Tracker",
        "description": "Recorded your body weight measurement",
        "icon": "⚖️",
        "category": "milestone"
    },
    {
        "key": "heart_pioneer",
        "name": "Cardio Tracker",
        "description": "Logged your resting or active heart rate reading",
        "icon": "❤️",
        "category": "fitness"
    }
]


def evaluate_and_award_achievements(user_id):
    """
    Scans the user's database records across all categories and awards milestone badges.
    """
    today_str = date.today().isoformat()
    
    with get_db() as db:
        cursor = db.cursor()

        # 1. Check Steps
        cursor.execute("SELECT MAX(steps) as max_steps FROM daily_activity WHERE user_id = ?;", (user_id,))
        res = cursor.fetchone()
        max_steps = res["max_steps"] if (res and res["max_steps"]) else 0
        if max_steps >= 5000:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'first_5k_steps', 'First 5,000 Steps', 'Walked at least 5,000 steps in a single day', '👟', 'fitness');
            """, (user_id,))
        if max_steps >= 10000:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, '10k_steps_club', '10,000 Steps Club', 'Achieved 10,000 steps in a single day', '🏆', 'fitness');
            """, (user_id,))

        # 2. Check 7-day Streak
        cursor.execute("""
            SELECT COUNT(DISTINCT activity_date) as active_days 
            FROM daily_activity 
            WHERE user_id = ? AND steps >= 2000;
        """, (user_id,))
        streak_row = cursor.fetchone()
        active_days = streak_row["active_days"] if streak_row else 0
        if active_days >= 7:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'activity_streak_7d', '7-Day Activity Streak', 'Maintained active step logs for 7 consecutive days', '🔥', 'consistency');
            """, (user_id,))

        # 3. Check Water Intake
        cursor.execute("""
            SELECT daily_water_goal_ml FROM users WHERE id = ?;
        """, (user_id,))
        u_row = cursor.fetchone()
        water_goal = u_row["daily_water_goal_ml"] if (u_row and u_row["daily_water_goal_ml"]) else 2500

        cursor.execute("""
            SELECT SUM(amount_ml) as total_ml 
            FROM water_intake 
            WHERE user_id = ? AND intake_date = ?;
        """, (user_id, today_str))
        w_row = cursor.fetchone()
        w_total = w_row["total_ml"] if (w_row and w_row["total_ml"]) else 0
        if w_total >= water_goal:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'water_champion', 'Hydration Champion', 'Reached 100% of your daily water intake goal', '💧', 'hydration');
            """, (user_id,))

        # 4. Check Sleep
        cursor.execute("""
            SELECT MAX(duration_minutes) as max_sleep 
            FROM sleep_records 
            WHERE user_id = ?;
        """, (user_id,))
        sl_row = cursor.fetchone()
        max_sleep_mins = sl_row["max_sleep"] if (sl_row and sl_row["max_sleep"]) else 0
        if max_sleep_mins >= 480:  # 8 hours
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'sleep_champion', 'Sleep Champion', 'Logged 8+ hours of restful sleep session', '🌙', 'sleep');
            """, (user_id,))

        # 5. Check Weight
        cursor.execute("SELECT COUNT(*) as count FROM weight_records WHERE user_id = ?;", (user_id,))
        wt_row = cursor.fetchone()
        if wt_row and wt_row["count"] >= 1:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'weight_tracker', 'Weight Tracker', 'Recorded your body weight measurement', '⚖️', 'milestone');
            """, (user_id,))

        # 6. Check Heart Rate
        cursor.execute("SELECT COUNT(*) as count FROM heart_rate WHERE user_id = ?;", (user_id,))
        hr_row = cursor.fetchone()
        if hr_row and hr_row["count"] >= 1:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'heart_pioneer', 'Cardio Tracker', 'Logged your resting or active heart rate reading', '❤️', 'fitness');
            """, (user_id,))


def get_goals_and_achievements_data(user_id):
    """
    Retrieves user goals, today's completion progress, and achievement badges.
    """
    today_str = date.today().isoformat()
    evaluate_and_award_achievements(user_id)

    with get_db() as db:
        cursor = db.cursor()

        # 1. Fetch user targets
        cursor.execute("""
            SELECT daily_step_goal, daily_water_goal_ml, daily_sleep_goal_hours, target_weight_kg, height_cm 
            FROM users WHERE id = ?;
        """, (user_id,))
        user_row = cursor.fetchone()
        step_goal = user_row["daily_step_goal"] or 10000
        water_goal = user_row["daily_water_goal_ml"] or 2500
        sleep_goal = float(user_row["daily_sleep_goal_hours"] or 8.0)
        target_weight = float(user_row["target_weight_kg"]) if user_row["target_weight_kg"] else None

        # 2. Today's actual activity
        cursor.execute("SELECT steps FROM daily_activity WHERE user_id = ? AND activity_date = ?;", (user_id, today_str))
        step_row = cursor.fetchone()
        today_steps = step_row["steps"] if step_row else 0

        # Today's water
        cursor.execute("SELECT SUM(amount_ml) as total_ml FROM water_intake WHERE user_id = ? AND intake_date = ?;", (user_id, today_str))
        water_row = cursor.fetchone()
        today_water = water_row["total_ml"] if (water_row and water_row["total_ml"]) else 0

        # Latest sleep
        cursor.execute("SELECT duration_minutes FROM sleep_records WHERE user_id = ? ORDER BY sleep_date DESC, id DESC LIMIT 1;", (user_id,))
        sleep_row = cursor.fetchone()
        today_sleep_hours = round(sleep_row["duration_minutes"] / 60.0, 1) if sleep_row else 0.0

        # Latest weight
        cursor.execute("SELECT weight_kg FROM weight_records WHERE user_id = ? ORDER BY record_date DESC, id DESC LIMIT 1;", (user_id,))
        weight_row = cursor.fetchone()
        current_weight = float(weight_row["weight_kg"]) if weight_row else 70.0

        # 3. Percentages & completion status
        step_pct = min(100, round((today_steps / step_goal) * 100)) if step_goal > 0 else 0
        water_pct = min(100, round((today_water / water_goal) * 100)) if water_goal > 0 else 0
        sleep_pct = min(100, round((today_sleep_hours / sleep_goal) * 100)) if sleep_goal > 0 else 0

        goals_list = [
            {
                "key": "steps",
                "title": "Daily Step Goal",
                "icon": "👟",
                "target_display": f"{step_goal:,} steps",
                "target_val": step_goal,
                "current_display": f"{today_steps:,} steps",
                "current_val": today_steps,
                "pct": step_pct,
                "is_completed": today_steps >= step_goal,
                "unit": "steps",
                "theme": "emerald"
            },
            {
                "key": "water",
                "title": "Daily Hydration Goal",
                "icon": "💧",
                "target_display": f"{water_goal:,} ml",
                "target_val": water_goal,
                "current_display": f"{today_water:,} ml",
                "current_val": today_water,
                "pct": water_pct,
                "is_completed": today_water >= water_goal,
                "unit": "ml",
                "theme": "cyan"
            },
            {
                "key": "sleep",
                "title": "Daily Sleep Rest Target",
                "icon": "🌙",
                "target_display": f"{sleep_goal} hours",
                "target_val": sleep_goal,
                "current_display": f"{today_sleep_hours} hours",
                "current_val": today_sleep_hours,
                "pct": sleep_pct,
                "is_completed": today_sleep_hours >= sleep_goal,
                "unit": "hours",
                "theme": "purple"
            }
        ]

        if target_weight:
            w_diff = round(current_weight - target_weight, 1)
            goals_list.append({
                "key": "weight",
                "title": "Target Body Weight",
                "icon": "⚖️",
                "target_display": f"{target_weight} kg",
                "target_val": target_weight,
                "current_display": f"{current_weight} kg",
                "current_val": current_weight,
                "pct": 100 if abs(w_diff) <= 0.5 else max(20, min(95, 100 - int(abs(w_diff) * 5))),
                "is_completed": abs(w_diff) <= 0.5,
                "unit": "kg",
                "theme": "amber"
            })

        completed_count = sum(1 for g in goals_list if g["is_completed"])
        overall_completion_pct = round((completed_count / len(goals_list)) * 100) if goals_list else 0

        # 4. Fetch Unlocked Achievements
        cursor.execute("""
            SELECT badge_key, badge_name, badge_description, badge_icon, category, unlocked_at 
            FROM achievements 
            WHERE user_id = ? 
            ORDER BY unlocked_at DESC;
        """, (user_id,))
        unlocked_records = {r["badge_key"]: dict(r) for r in cursor.fetchall()}

        # Build combined achievements view (unlocked vs locked)
        achievements_view = []
        for badge in ACHIEVEMENT_CATALOG:
            b_key = badge["key"]
            if b_key in unlocked_records:
                achievements_view.append({
                    "key": b_key,
                    "name": badge["name"],
                    "description": badge["description"],
                    "icon": badge["icon"],
                    "category": badge["category"],
                    "is_unlocked": True,
                    "unlocked_at": unlocked_records[b_key]["unlocked_at"]
                })
            else:
                achievements_view.append({
                    "key": b_key,
                    "name": badge["name"],
                    "description": badge["description"],
                    "icon": badge["icon"],
                    "category": badge["category"],
                    "is_unlocked": False,
                    "unlocked_at": None
                })

    return {
        "today_str": today_str,
        "goals": goals_list,
        "summary": {
            "total_goals": len(goals_list),
            "completed_goals": completed_count,
            "overall_pct": overall_completion_pct
        },
        "achievements": achievements_view,
        "unlocked_count": len(unlocked_records),
        "total_achievements": len(ACHIEVEMENT_CATALOG)
    }


def update_all_user_goals(user_id, step_goal=None, water_goal=None, sleep_goal=None, target_weight=None):
    """
    Updates user goals in SQLite.
    """
    with get_db() as db:
        cursor = db.cursor()
        if step_goal:
            s_val = int(step_goal)
            cursor.execute("UPDATE users SET daily_step_goal = ? WHERE id = ?;", (s_val, user_id))
        if water_goal:
            w_val = int(water_goal)
            cursor.execute("UPDATE users SET daily_water_goal_ml = ? WHERE id = ?;", (w_val, user_id))
        if sleep_goal:
            sl_val = float(sleep_goal)
            cursor.execute("UPDATE users SET daily_sleep_goal_hours = ? WHERE id = ?;", (sl_val, user_id))
        if target_weight:
            tw_val = float(target_weight)
            cursor.execute("UPDATE users SET target_weight_kg = ? WHERE id = ?;", (tw_val, user_id))

    return True
