"""
HealthTrack AI - Sleep Tracking Service
Step 7: Sleep Tracking Module

Handles:
- Bedtime and Wake-up time calculation with robust cross-midnight handling
- Automatic duration calculation in minutes and hours
- Sleep quality scores and sleep phases (Deep, REM, Light)
- Daily sleep goal setting and progress percentage
- Weekly (7-day) sleep time series for Chart.js
- Sleep history log and record deletion with strict user_id isolation
"""

from datetime import date, timedelta, datetime, time
from database import get_db


def calculate_sleep_duration(bedtime_str, wake_time_str, sleep_date_str=None):
    """
    Calculates sleep duration handling cross-midnight periods.
    
    Accepts:
    - Times like '23:00', '07:30'
    - Full datetimes like '2026-08-14 23:00', '2026-08-14T23:00'
    
    Returns:
        tuple: (duration_minutes, bedtime_iso, wake_time_iso)
    """
    target_date = datetime.strptime(sleep_date_str, "%Y-%m-%d").date() if sleep_date_str else date.today()

    # Parse Bedtime
    bedtime_dt = None
    wake_dt = None

    # Try parsing full datetime first
    for fmt in ["%Y-%m-%dT%H:%M", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M"]:
        try:
            bedtime_dt = datetime.strptime(bedtime_str.strip(), fmt)
            break
        except (ValueError, TypeError):
            continue

    for fmt in ["%Y-%m-%dT%H:%M", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M"]:
        try:
            wake_dt = datetime.strptime(wake_time_str.strip(), fmt)
            break
        except (ValueError, TypeError):
            continue

    # If simple time format (e.g. '23:15' and '07:30')
    if not bedtime_dt:
        try:
            t_parts = bedtime_str.strip().split(":")
            b_time = time(int(t_parts[0]), int(t_parts[1]))
        except Exception:
            raise ValueError("Bedtime format is invalid. Please use HH:MM (e.g. 23:00).")
    else:
        b_time = bedtime_dt.time()

    if not wake_dt:
        try:
            t_parts = wake_time_str.strip().split(":")
            w_time = time(int(t_parts[0]), int(t_parts[1]))
        except Exception:
            raise ValueError("Wake-up time format is invalid. Please use HH:MM (e.g. 07:30).")
    else:
        w_time = wake_dt.time()

    # Determine cross-midnight:
    # If wake time is earlier in the clock than bedtime (e.g. bedtime 23:00, wake 07:00),
    # bedtime was on (target_date - 1 day) and wake was on target_date morning.
    if not bedtime_dt or not wake_dt:
        if w_time <= b_time:
            bedtime_dt = datetime.combine(target_date - timedelta(days=1), b_time)
            wake_dt = datetime.combine(target_date, w_time)
        else:
            # Same day nap or night shift (e.g. 13:00 to 15:00 or 01:00 to 08:00)
            bedtime_dt = datetime.combine(target_date, b_time)
            wake_dt = datetime.combine(target_date, w_time)

    # Ensure wake_dt is after bedtime_dt
    if wake_dt <= bedtime_dt:
        wake_dt += timedelta(days=1)

    duration_seconds = (wake_dt - bedtime_dt).total_seconds()
    duration_minutes = int(round(duration_seconds / 60.0))

    if duration_minutes < 15:
        raise ValueError("Sleep duration must be at least 15 minutes.")
    if duration_minutes > 1440:  # 24 hours
        raise ValueError("Sleep duration cannot exceed 24 hours.")

    return duration_minutes, bedtime_dt.strftime("%Y-%m-%d %H:%M:%S"), wake_dt.strftime("%Y-%m-%d %H:%M:%S")


def get_sleep_module_data(user_id):
    """
    Retrieves all sleep tracking metrics for the logged-in user:
    - Today's / latest night's sleep record, hours, quality, sleep stages
    - Goal progress & remaining hours
    - Weekly (past 7 days) chart data
    - Sleep history log
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

        # 1. Fetch User Sleep Goal
        cursor.execute("SELECT daily_sleep_goal_hours FROM users WHERE id = ?;", (user_id,))
        user_row = cursor.fetchone()
        sleep_goal_hours = float(user_row["daily_sleep_goal_hours"] or 8.0)

        # 2. Today's or Latest Sleep Record
        cursor.execute("""
            SELECT * FROM sleep_records 
            WHERE user_id = ? AND sleep_date = ?
            ORDER BY id DESC LIMIT 1;
        """, (user_id, today_str))
        today_record = cursor.fetchone()

        if not today_record:
            # Fallback to most recent record
            cursor.execute("""
                SELECT * FROM sleep_records 
                WHERE user_id = ?
                ORDER BY sleep_date DESC, id DESC LIMIT 1;
            """, (user_id,))
            today_record = cursor.fetchone()

        if today_record:
            duration_mins = today_record["duration_minutes"]
            sleep_hours = round(duration_mins / 60.0, 1)
            hours_int = duration_mins // 60
            mins_int = duration_mins % 60
            duration_formatted = f"{hours_int}h {mins_int}m" if mins_int > 0 else f"{hours_int}h"
            quality_score = today_record["sleep_quality_score"] or 85
            deep_mins = today_record["deep_sleep_minutes"] or int(duration_mins * 0.22)
            rem_mins = today_record["rem_sleep_minutes"] or int(duration_mins * 0.20)
            light_mins = today_record["light_sleep_minutes"] or (duration_mins - deep_mins - rem_mins)
            bedtime_display = today_record["bedtime"]
            wake_display = today_record["wake_time"]
            notes = today_record["notes"]
            record_date = today_record["sleep_date"]
        else:
            duration_mins = 0
            sleep_hours = 0.0
            duration_formatted = "0h 0m"
            quality_score = 0
            deep_mins = 0
            rem_mins = 0
            light_mins = 0
            bedtime_display = "—"
            wake_display = "—"
            notes = ""
            record_date = today_str

        # Goal progress calculation
        goal_pct = min(100, round((sleep_hours / sleep_goal_hours) * 100)) if sleep_goal_hours > 0 else 0
        raw_goal_pct = round((sleep_hours / sleep_goal_hours) * 100, 1) if sleep_goal_hours > 0 else 0
        remaining_hours = max(0.0, round(sleep_goal_hours - sleep_hours, 1))
        is_goal_met = sleep_hours >= sleep_goal_hours

        # 3. Weekly Sleep Data (Past 7 Days)
        cursor.execute("""
            SELECT sleep_date, duration_minutes, sleep_quality_score 
            FROM sleep_records 
            WHERE user_id = ? AND sleep_date >= ? AND sleep_date <= ?;
        """, (user_id, days_7_strs[0], days_7_strs[-1]))
        w_records = {r["sleep_date"]: dict(r) for r in cursor.fetchall()}

        weekly_sleep_hours = []
        weekly_qualities = []
        for d in days_7_strs:
            if d in w_records:
                h = round(w_records[d]["duration_minutes"] / 60.0, 1)
                q = w_records[d]["sleep_quality_score"] or 80
            else:
                h = 0.0
                q = 0
            weekly_sleep_hours.append(h)
            weekly_qualities.append(q)

        weekly_total_hours = round(sum(weekly_sleep_hours), 1)
        valid_days = [h for h in weekly_sleep_hours if h > 0]
        weekly_avg_hours = round(sum(valid_days) / len(valid_days), 1) if valid_days else 0.0

        # 4. Full Sleep History (Recent 30 entries)
        cursor.execute("""
            SELECT id, sleep_date, bedtime, wake_time, duration_minutes, 
                   sleep_quality_score, deep_sleep_minutes, rem_sleep_minutes, light_sleep_minutes, notes, created_at 
            FROM sleep_records 
            WHERE user_id = ? 
            ORDER BY sleep_date DESC, id DESC 
            LIMIT 30;
        """, (user_id,))
        history = []
        for r in cursor.fetchall():
            item = dict(r)
            mins = item["duration_minutes"]
            h_int = mins // 60
            m_int = mins % 60
            item["formatted_duration"] = f"{h_int}h {m_int}m"
            item["hours"] = round(mins / 60.0, 1)
            history.append(item)

    return {
        "today_str": today_str,
        "today": {
            "duration_minutes": duration_mins,
            "hours": sleep_hours,
            "formatted_duration": duration_formatted,
            "quality_score": quality_score,
            "goal_hours": sleep_goal_hours,
            "goal_pct": goal_pct,
            "raw_goal_pct": raw_goal_pct,
            "remaining_hours": remaining_hours,
            "is_goal_met": is_goal_met,
            "bedtime": bedtime_display,
            "wake_time": wake_display,
            "deep_mins": deep_mins,
            "rem_mins": rem_mins,
            "light_mins": light_mins,
            "notes": notes,
            "record_date": record_date
        },
        "weekly": {
            "labels": days_7_short,
            "full_labels": days_7_labels,
            "hours": weekly_sleep_hours,
            "qualities": weekly_qualities,
            "total_hours": weekly_total_hours,
            "avg_hours": weekly_avg_hours,
            "goal_hours": sleep_goal_hours
        },
        "history": history
    }


def add_sleep_record(user_id, bedtime_str, wake_time_str, sleep_date_str=None, quality_score=85, notes=None):
    """
    Logs a sleep record in SQLite with automatic cross-midnight duration calculation.
    
    Raises:
        ValueError: If dates/times are invalid or duration is unrealistic.
    """
    duration_mins, bedtime_iso, wake_iso = calculate_sleep_duration(bedtime_str, wake_time_str, sleep_date_str)
    
    try:
        quality_val = int(quality_score)
        quality_val = max(1, min(100, quality_val))
    except (ValueError, TypeError):
        quality_val = 85

    date_str = sleep_date_str or date.today().isoformat()
    
    # Calculate sleep stages
    deep_mins = int(duration_mins * 0.22)
    rem_mins = int(duration_mins * 0.20)
    light_mins = duration_mins - deep_mins - rem_mins

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            INSERT INTO sleep_records (
                user_id, sleep_date, bedtime, wake_time, duration_minutes,
                sleep_quality_score, deep_sleep_minutes, rem_sleep_minutes, light_sleep_minutes, notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """, (user_id, date_str, bedtime_iso, wake_iso, duration_mins, quality_val, deep_mins, rem_mins, light_mins, notes))

        # Award sleep milestone badges
        hours = duration_mins / 60.0
        if hours >= 8.0:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'sleep_champion', 'Sleep Champion', 'Logged 8+ hours of restful sleep', 'moon', 'sleep');
            """, (user_id,))
        if quality_val >= 90:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'rested_master', 'Deep Rest Master', 'Achieved a 90%+ sleep quality score', 'sparkles', 'sleep');
            """, (user_id,))

    return duration_mins


def set_user_sleep_goal(user_id, new_goal_hours):
    """
    Updates the user's daily sleep target in hours.
    
    Raises:
        ValueError: If goal is outside realistic range (4.0 to 14.0 hours).
    """
    try:
        goal_val = round(float(new_goal_hours), 1)
    except (ValueError, TypeError):
        raise ValueError("Sleep goal must be a valid number of hours.")

    if goal_val < 4.0 or goal_val > 14.0:
        raise ValueError("Daily sleep goal must be between 4.0 and 14.0 hours.")

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("UPDATE users SET daily_sleep_goal_hours = ? WHERE id = ?;", (goal_val, user_id))

        today_str = date.today().isoformat()
        cursor.execute("""
            INSERT INTO goals (user_id, goal_type, title, target_value, unit, start_date, status)
            VALUES (?, 'sleep', 'Daily Sleep Goal', ?, 'hours', ?, 'in_progress');
        """, (user_id, goal_val, today_str))

    return goal_val


def delete_user_sleep_record(user_id, record_id):
    """
    Deletes a sleep record belonging to the user.
    """
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("DELETE FROM sleep_records WHERE id = ? AND user_id = ?;", (record_id, user_id))
        return cursor.rowcount > 0
