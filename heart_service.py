"""
HealthTrack AI - Heart Rate Tracking Service
Step 9: Heart Rate Tracking & Cardiology Habit Monitoring

Handles:
- Adding heart rate logs with date, time, and context (resting, workout, walking, normal, sleeping)
- Computing latest BPM, average recorded BPM, min and max readings
- Aggregating heart rate time-series for Chart.js
- Retrieving and deleting heart rate entries with strict user_id isolation
"""

from datetime import date, timedelta, datetime
from database import get_db


def map_context_to_db(context_str):
    """
    Normalizes context string to schema CHECK constraint values:
    ('resting', 'workout', 'walking', 'normal', 'sleeping')
    """
    ctx = (context_str or "").lower()
    if "rest" in ctx or "morning" in ctx or "wind-down" in ctx or "evening" in ctx:
        return "resting"
    if "workout" in ctx or "gym" in ctx or "run" in ctx or "exercise" in ctx:
        return "workout"
    if "walk" in ctx or "light" in ctx:
        return "walking"
    if "sleep" in ctx:
        return "sleeping"
    return "normal"


def get_heart_module_data(user_id):
    """
    Retrieves heart-rate data and analytics for the user.
    """
    today_str = date.today().isoformat()
    now_time_str = datetime.now().strftime("%H:%M")

    with get_db() as db:
        cursor = db.cursor()

        # Fetch all readings ordered chronologically for charts
        cursor.execute("""
            SELECT id, measured_at, bpm, resting_heart_rate, activity_context, created_at 
            FROM heart_rate 
            WHERE user_id = ? 
            ORDER BY measured_at ASC, id ASC;
        """, (user_id,))
        chrono_records = [dict(r) for r in cursor.fetchall()]

        # Latest reading
        if chrono_records:
            latest_rec = chrono_records[-1]
            latest_bpm = latest_rec["bpm"]
            latest_measured_at = latest_rec["measured_at"]
            latest_context = (latest_rec["activity_context"] or "resting").capitalize()
            
            all_bpms = [r["bpm"] for r in chrono_records]
            avg_bpm = round(sum(all_bpms) / len(all_bpms))
            min_bpm = min(all_bpms)
            max_bpm = max(all_bpms)
        else:
            latest_bpm = 72
            latest_measured_at = f"{today_str} {now_time_str}"
            latest_context = "Resting"
            avg_bpm = 72
            min_bpm = 72
            max_bpm = 72

        # Context Category rating for latest reading
        if latest_bpm < 60:
            rating = "Low / Athletic"
            rating_class = "info"
        elif 60 <= latest_bpm <= 100:
            rating = "Normal Resting"
            rating_class = "success"
        else:
            rating = "Elevated"
            rating_class = "warning"

        # Chart Series Data (Recent 25 readings)
        recent_25 = chrono_records[-25:] if len(chrono_records) > 25 else chrono_records
        chart_labels = []
        chart_bpms = []
        for r in recent_25:
            try:
                dt = datetime.strptime(r["measured_at"], "%Y-%m-%d %H:%M:%S")
                lbl = dt.strftime("%b %d, %H:%M")
            except Exception:
                lbl = r["measured_at"]
            chart_labels.append(lbl)
            chart_bpms.append(r["bpm"])

        # History log (Reverse chronological order for table)
        cursor.execute("""
            SELECT id, measured_at, bpm, resting_heart_rate, activity_context, created_at 
            FROM heart_rate 
            WHERE user_id = ? 
            ORDER BY measured_at DESC, id DESC 
            LIMIT 40;
        """, (user_id,))
        history = []
        for r in cursor.fetchall():
            item = dict(r)
            item["display_context"] = (item.get("activity_context") or "resting").capitalize()
            history.append(item)

    return {
        "today_str": today_str,
        "now_time_str": now_time_str,
        "latest": {
            "bpm": latest_bpm,
            "measured_at": latest_measured_at,
            "context": latest_context,
            "rating": rating,
            "rating_class": rating_class
        },
        "stats": {
            "avg_bpm": avg_bpm,
            "min_bpm": min_bpm,
            "max_bpm": max_bpm,
            "total_readings": len(chrono_records)
        },
        "chart": {
            "labels": chart_labels,
            "bpms": chart_bpms
        },
        "history": history
    }


def add_heart_rate_record(user_id, bpm, measured_date=None, measured_time=None, context="resting", notes=None):
    """
    Inserts a heart rate measurement with validation against schema constraints.
    
    Raises:
        ValueError: If BPM is not a reasonable whole number between 30 and 240.
    """
    try:
        bpm_val = int(bpm)
    except (ValueError, TypeError):
        raise ValueError("Heart rate must be a valid whole number in Beats Per Minute (BPM).")

    if bpm_val < 30 or bpm_val > 240:
        raise ValueError("Heart rate entry must be between 30 BPM and 240 BPM.")

    m_date = (measured_date or date.today().isoformat()).strip()
    m_time = (measured_time or datetime.now().strftime("%H:%M:%S")).strip()
    if len(m_time) == 5:
        m_time += ":00"

    timestamp_str = f"{m_date} {m_time}"
    db_context = map_context_to_db(context)
    resting_hr = bpm_val if db_context == "resting" else None

    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            INSERT INTO heart_rate (user_id, measured_at, bpm, resting_heart_rate, activity_context)
            VALUES (?, ?, ?, ?, ?);
        """, (user_id, timestamp_str, bpm_val, resting_hr, db_context))

        # Award achievement badge
        cursor.execute("SELECT COUNT(*) as count FROM heart_rate WHERE user_id = ?;", (user_id,))
        if cursor.fetchone()["count"] >= 1:
            cursor.execute("""
                INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
                VALUES (?, 'heart_pioneer', 'Cardio Tracker', 'Logged your resting or active heart rate reading', 'heart', 'cardio');
            """, (user_id,))

    return bpm_val


def delete_heart_rate_record(user_id, record_id):
    """
    Deletes a heart rate entry.
    """
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("DELETE FROM heart_rate WHERE id = ? AND user_id = ?;", (record_id, user_id))
        return cursor.rowcount > 0
