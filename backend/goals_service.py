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
        "category": "fitness",
        "target": 5000,
        "unit": "steps"
    },
    {
        "key": "10k_steps_club",
        "name": "10,000 Steps Club",
        "description": "Achieved the gold standard of 10,000 steps in a single day",
        "icon": "🏆",
        "category": "fitness",
        "target": 10000,
        "unit": "steps"
    },
    {
        "key": "activity_streak_7d",
        "name": "7-Day Activity Streak",
        "description": "Maintained active step logs for 7 consecutive days",
        "icon": "🔥",
        "category": "consistency",
        "target": 7,
        "unit": "days"
    },
    {
        "key": "water_champion",
        "name": "Water Goal Completed",
        "description": "Reached 100% of your daily water intake goal",
        "icon": "💧",
        "category": "hydration",
        "target": 100,
        "unit": "%"
    },
    {
        "key": "sleep_champion",
        "name": "Sleep Goal Completed",
        "description": "Reached 100% of your daily sleep rest goal (or 8+ hours)",
        "icon": "🌙",
        "category": "sleep",
        "target": 100,
        "unit": "%"
    },
    {
        "key": "perfect_day",
        "name": "Perfect Health Day",
        "description": "Completed all 3 daily goals (Steps, Water, Sleep) in a single day",
        "icon": "⭐",
        "category": "milestone",
        "target": 3,
        "unit": "goals"
    },
    {
        "key": "weight_tracker",
        "name": "Weight Tracker",
        "description": "Recorded your body weight measurement",
        "icon": "⚖️",
        "category": "milestone",
        "target": 1,
        "unit": "entry"
    },
    {
        "key": "heart_pioneer",
        "name": "Cardio Tracker",
        "description": "Logged your resting or active heart rate reading",
        "icon": "❤️",
        "category": "fitness",
        "target": 1,
        "unit": "entry"
    }
]


def evaluate_and_award_achievements(user_id):
    """
    Scans the user's database records across all categories and awards milestone badges.
    """
    today_str = date.today().isoformat()
    
    with get_db() as db:
        cursor = db.cursor()

        # Fetch user target goals for comparison
        cursor.execute("SELECT daily_step_goal, daily_water_goal_ml, daily_sleep_goal_hours FROM users WHERE id = ?;", (user_id,))
        u_goals = cursor.fetchone()
        water_goal = u_goals["daily_water_goal_ml"] if (u_goals and u_goals["daily_water_goal_ml"]) else 2500
        sleep_goal = float(u_goals["daily_sleep_goal_hours"] or 8.0)
        step_goal = u_goals["daily_step_goal"] if (u_goals and u_goals["daily_step_goal"]) else 10000

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

        # 3. Check Water Intake (Today or any past day achieving goal)
        cursor.execute("""
            SELECT MAX(daily_total) as max_water FROM (
                SELECT SUM(amount_ml) as daily_total 
                FROM water_intake 
                WHERE user_id = ? 
                GROUP BY intake_date
            );
        """, (user_id,))
        w_row = cursor.fetchone()
        max_water = w_row["max_water"] if (w_row and w_row["max_water"]) else 0
        if max_water >= water_goal:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'water_champion', 'Water Goal Completed', 'Reached 100% of your daily water intake goal', '💧', 'hydration');
            """, (user_id,))

        # 4. Check Sleep (Today or any session meeting goal / 8+ hours)
        cursor.execute("""
            SELECT MAX(duration_minutes) as max_sleep 
            FROM sleep_records 
            WHERE user_id = ?;
        """, (user_id,))
        sl_row = cursor.fetchone()
        max_sleep_mins = sl_row["max_sleep"] if (sl_row and sl_row["max_sleep"]) else 0
        if max_sleep_mins >= int(sleep_goal * 60) or max_sleep_mins >= 480:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'sleep_champion', 'Sleep Goal Completed', 'Reached 100% of your daily sleep rest goal (or 8+ hours)', '🌙', 'sleep');
            """, (user_id,))

        # 5. Check Perfect Day (Steps + Water + Sleep all achieved today)
        cursor.execute("SELECT steps FROM daily_activity WHERE user_id = ? AND activity_date = ?;", (user_id, today_str))
        st_row = cursor.fetchone()
        today_st = st_row["steps"] if st_row else 0

        cursor.execute("SELECT SUM(amount_ml) as total_ml FROM water_intake WHERE user_id = ? AND intake_date = ?;", (user_id, today_str))
        wt_today = cursor.fetchone()
        today_wt = wt_today["total_ml"] if (wt_today and wt_today["total_ml"]) else 0

        cursor.execute("SELECT duration_minutes FROM sleep_records WHERE user_id = ? ORDER BY sleep_date DESC, id DESC LIMIT 1;", (user_id,))
        sl_today = cursor.fetchone()
        today_sl_hrs = (sl_today["duration_minutes"] / 60.0) if sl_today else 0.0

        if today_st >= step_goal and today_wt >= water_goal and today_sl_hrs >= sleep_goal:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'perfect_day', 'Perfect Health Day', 'Completed all 3 daily goals (Steps, Water, Sleep) in a single day', '⭐', 'milestone');
            """, (user_id,))

        # 6. Check Weight Record
        cursor.execute("SELECT COUNT(*) as count FROM weight_records WHERE user_id = ?;", (user_id,))
        w_rec = cursor.fetchone()
        if w_rec and w_rec["count"] >= 1:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'weight_tracker', 'Weight Tracker', 'Recorded your body weight measurement', '⚖️', 'milestone');
            """, (user_id,))

        # 7. Check Heart Rate Record
        cursor.execute("SELECT COUNT(*) as count FROM heart_rate WHERE user_id = ?;", (user_id,))
        hr_rec = cursor.fetchone()
        if hr_rec and hr_rec["count"] >= 1:
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

        # Active streak calculation
        cursor.execute("""
            SELECT COUNT(DISTINCT activity_date) as active_days 
            FROM daily_activity 
            WHERE user_id = ? AND steps >= 2000;
        """, (user_id,))
        streak_row = cursor.fetchone()
        active_streak = streak_row["active_days"] if streak_row else 0

        # Max steps recorded
        cursor.execute("SELECT MAX(steps) as max_steps FROM daily_activity WHERE user_id = ?;", (user_id,))
        res_st = cursor.fetchone()
        max_recorded_steps = res_st["max_steps"] if (res_st and res_st["max_steps"]) else 0

        # 3. Percentages & completion status
        step_pct = min(100, round((today_steps / step_goal) * 100)) if step_goal > 0 else 0
        water_pct = min(100, round((today_water / water_goal) * 100)) if water_goal > 0 else 0
        sleep_pct = min(100, round((today_sleep_hours / sleep_goal) * 100)) if sleep_goal > 0 else 0

        step_remaining = max(0, step_goal - today_steps)
        water_remaining = max(0, water_goal - today_water)
        sleep_remaining = max(0.0, round(sleep_goal - today_sleep_hours, 1))

        goals_list = [
            {
                "key": "steps",
                "title": "Daily Step Goal",
                "icon": "👟",
                "target_display": f"{step_goal:,} steps",
                "target_val": step_goal,
                "current_display": f"{today_steps:,} steps",
                "current_val": today_steps,
                "remaining_val": step_remaining,
                "remaining_display": f"{step_remaining:,} steps remaining" if step_remaining > 0 else "Goal met!",
                "pct": step_pct,
                "is_completed": today_steps >= step_goal,
                "status_text": "Completed" if today_steps >= step_goal else f"{step_pct}% completed",
                "unit": "steps",
                "theme": "emerald",
                "quick_add_amount": 1000,
                "quick_add_label": "+1,000 Steps"
            },
            {
                "key": "water",
                "title": "Daily Water Goal",
                "icon": "💧",
                "target_display": f"{water_goal:,} ml",
                "target_val": water_goal,
                "current_display": f"{today_water:,} ml",
                "current_val": today_water,
                "remaining_val": water_remaining,
                "remaining_display": f"{water_remaining:,} ml remaining" if water_remaining > 0 else "Goal met!",
                "pct": water_pct,
                "is_completed": today_water >= water_goal,
                "status_text": "Completed" if today_water >= water_goal else f"{water_pct}% completed",
                "unit": "ml",
                "theme": "cyan",
                "quick_add_amount": 250,
                "quick_add_label": "+250 ml Glass"
            },
            {
                "key": "sleep",
                "title": "Daily Sleep Goal",
                "icon": "🌙",
                "target_display": f"{sleep_goal} hours",
                "target_val": sleep_goal,
                "current_display": f"{today_sleep_hours} hours",
                "current_val": today_sleep_hours,
                "remaining_val": sleep_remaining,
                "remaining_display": f"{sleep_remaining} hrs remaining" if sleep_remaining > 0 else "Goal met!",
                "pct": sleep_pct,
                "is_completed": today_sleep_hours >= sleep_goal,
                "status_text": "Completed" if today_sleep_hours >= sleep_goal else f"{sleep_pct}% completed",
                "unit": "hours",
                "theme": "purple",
                "quick_add_amount": 1.0,
                "quick_add_label": "Log Sleep"
            }
        ]

        if target_weight:
            w_diff = round(current_weight - target_weight, 1)
            is_w_completed = abs(w_diff) <= 0.5
            goals_list.append({
                "key": "weight",
                "title": "Target Body Weight",
                "icon": "⚖️",
                "target_display": f"{target_weight} kg",
                "target_val": target_weight,
                "current_display": f"{current_weight} kg",
                "current_val": current_weight,
                "remaining_val": abs(w_diff),
                "remaining_display": f"{abs(w_diff):.1f} kg to target" if not is_w_completed else "Target achieved!",
                "pct": 100 if is_w_completed else max(20, min(95, 100 - int(abs(w_diff) * 5))),
                "is_completed": is_w_completed,
                "status_text": "On Target" if is_w_completed else f"{abs(w_diff):.1f} kg difference",
                "unit": "kg",
                "theme": "amber",
                "quick_add_amount": 0,
                "quick_add_label": "Update Weight"
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

        # Build combined achievements view (unlocked vs locked with live progress)
        achievements_view = []
        for badge in ACHIEVEMENT_CATALOG:
            b_key = badge["key"]
            is_unlocked = b_key in unlocked_records

            # Calculate badge progress
            prog_val = 0
            prog_max = badge.get("target", 100)
            prog_text = ""

            if is_unlocked:
                prog_val = prog_max
                prog_text = "Completed & Unlocked"
            else:
                if b_key == "first_5k_steps":
                    prog_val = min(5000, max_recorded_steps)
                    prog_text = f"{prog_val:,} / 5,000 steps"
                elif b_key == "10k_steps_club":
                    prog_val = min(10000, max_recorded_steps)
                    prog_text = f"{prog_val:,} / 10,000 steps"
                elif b_key == "activity_streak_7d":
                    prog_val = min(7, active_streak)
                    prog_text = f"{prog_val} / 7 days active"
                elif b_key == "water_champion":
                    prog_val = min(water_goal, today_water)
                    prog_text = f"{prog_val:,} / {water_goal:,} ml ({water_pct}%)"
                elif b_key == "sleep_champion":
                    prog_val = min(sleep_goal, today_sleep_hours)
                    prog_text = f"{prog_val} / {sleep_goal} hrs ({sleep_pct}%)"
                elif b_key == "perfect_day":
                    goals_met_today = (1 if today_steps >= step_goal else 0) + (1 if today_water >= water_goal else 0) + (1 if today_sleep_hours >= sleep_goal else 0)
                    prog_val = goals_met_today
                    prog_text = f"{goals_met_today} / 3 goals completed today"
                else:
                    prog_val = 0
                    prog_text = "In Progress"

            badge_pct = min(100, round((prog_val / prog_max) * 100)) if prog_max > 0 else 0

            achievements_view.append({
                "key": b_key,
                "name": badge["name"],
                "description": badge["description"],
                "icon": badge["icon"],
                "category": badge["category"],
                "is_unlocked": is_unlocked,
                "unlocked_at": unlocked_records[b_key]["unlocked_at"] if is_unlocked else None,
                "progress_val": prog_val,
                "progress_max": prog_max,
                "progress_text": prog_text,
                "progress_pct": badge_pct
            })

    return {
        "today_str": today_str,
        "goals": goals_list,
        "user_targets": {
            "step_goal": step_goal,
            "water_goal": water_goal,
            "sleep_goal": sleep_goal,
            "target_weight": target_weight
        },
        "summary": {
            "total_goals": len(goals_list),
            "completed_goals": completed_count,
            "overall_pct": overall_completion_pct,
            "active_streak": active_streak
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
