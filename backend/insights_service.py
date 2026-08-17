"""
HealthTrack AI - AI Wellness Insights Service
Analyzes user activity, hydration, sleep, weight trends, and heart rate records
to generate clear, understandable, and safe personalized wellness observations.

Safety & Compliance Guardrails:
- Does NOT diagnose diseases.
- Does NOT claim a user is healthy or unhealthy.
- Does NOT provide medical treatment recommendations.
- Uses neutral, objective language for concerning patterns and advises consulting a qualified healthcare professional.
"""

from datetime import date, timedelta
from database import get_db


def analyze_user_wellness_insights(user_id):
    """
    Analyzes user's multi-metric health records (steps, water, sleep, weight, heart rate)
    over 7-day and 30-day windows to produce structured, safe, and actionable wellness insights.
    
    Returns:
        dict: Categorized insights, summary statistics, metric comparison cards,
              and health disclaimer metadata.
    """
    today = date.today()
    today_str = today.isoformat()
    past_7_days = [(today - timedelta(days=i)).isoformat() for i in range(6, -1, -1)]
    prev_7_days = [(today - timedelta(days=i)).isoformat() for i in range(13, 6, -1)]
    past_30_days_start = (today - timedelta(days=29)).isoformat()

    insights = []
    
    with get_db() as db:
        cursor = db.cursor()

        # 1. Fetch User Goals & Profile
        cursor.execute("""
            SELECT id, full_name, daily_step_goal, daily_water_goal_ml, 
                   daily_sleep_goal_hours, target_weight_kg, height_cm 
            FROM users WHERE id = ?;
        """, (user_id,))
        user = cursor.fetchone()
        if not user:
            return None

        step_goal = user["daily_step_goal"] or 10000
        water_goal = user["daily_water_goal_ml"] or 2500
        sleep_goal = float(user["daily_sleep_goal_hours"] or 8.0)
        target_weight = float(user["target_weight_kg"]) if user["target_weight_kg"] else None
        height_cm = user["height_cm"] or 175.0

        # =========================================================================
        # 2. STEP & ACTIVITY ANALYSIS
        # =========================================================================
        # Past 7 days steps
        cursor.execute("""
            SELECT activity_date, steps, distance_km, calories_burned, active_minutes
            FROM daily_activity
            WHERE user_id = ? AND activity_date IN ({});
        """.format(','.join('?' for _ in past_7_days)), [user_id] + past_7_days)
        recent_act_rows = cursor.fetchall()
        recent_act_dict = {r["activity_date"]: r["steps"] for r in recent_act_rows}
        
        recent_steps_list = [recent_act_dict.get(d, 0) for d in past_7_days]
        recent_steps_sum = sum(recent_steps_list)
        recent_steps_avg = round(recent_steps_sum / 7) if recent_steps_list else 0
        step_goal_met_days = sum(1 for s in recent_steps_list if s >= step_goal)

        # Previous 7 days steps (for week-over-week comparison)
        cursor.execute("""
            SELECT activity_date, steps
            FROM daily_activity
            WHERE user_id = ? AND activity_date IN ({});
        """.format(','.join('?' for _ in prev_7_days)), [user_id] + prev_7_days)
        prev_act_rows = cursor.fetchall()
        prev_act_dict = {r["activity_date"]: r["steps"] for r in prev_act_rows}
        prev_steps_list = [prev_act_dict.get(d, 0) for d in prev_7_days]
        prev_steps_avg = round(sum(prev_steps_list) / 7) if prev_steps_list else 0

        # Check consecutive activity streak
        cursor.execute("""
            SELECT activity_date, steps 
            FROM daily_activity 
            WHERE user_id = ? AND activity_date >= ?
            ORDER BY activity_date DESC;
        """, (user_id, past_30_days_start))
        all_recent_steps = {r["activity_date"]: r["steps"] for r in cursor.fetchall()}
        
        consecutive_active_days = 0
        for i in range(30):
            check_date = (today - timedelta(days=i)).isoformat()
            if all_recent_steps.get(check_date, 0) >= 2000:
                consecutive_active_days += 1
            else:
                break

        # Generate Step Insights
        if consecutive_active_days >= 7:
            insights.append({
                "category": "steps",
                "title": "Activity Streak Milestone",
                "text": f"You have maintained your activity goal for {consecutive_active_days} consecutive days.",
                "type": "positive",
                "icon": "🔥",
                "metric_label": f"{consecutive_active_days}-Day Streak"
            })
        
        if prev_steps_avg > 0:
            step_diff_pct = round(((recent_steps_avg - prev_steps_avg) / prev_steps_avg) * 100)
            if step_diff_pct >= 5:
                insights.append({
                    "category": "steps",
                    "title": "Weekly Step Progression",
                    "text": f"Your average steps increased this week by {abs(step_diff_pct)}% (from {prev_steps_avg:,} to {recent_steps_avg:,} daily steps).",
                    "type": "positive",
                    "icon": "📈",
                    "metric_label": f"+{abs(step_diff_pct)}% vs Last Week"
                })
            elif step_diff_pct <= -10:
                insights.append({
                    "category": "steps",
                    "title": "Step Volume Variation",
                    "text": f"Your daily step average was lower this week ({recent_steps_avg:,} steps/day) compared to the previous week ({prev_steps_avg:,} steps/day).",
                    "type": "neutral",
                    "icon": "👟",
                    "metric_label": f"{recent_steps_avg:,} Steps Avg"
                })
            else:
                insights.append({
                    "category": "steps",
                    "title": "Steady Activity Rhythm",
                    "text": f"Your daily step count remained steady this week, averaging {recent_steps_avg:,} steps per day.",
                    "type": "neutral",
                    "icon": "👟",
                    "metric_label": f"{recent_steps_avg:,} Steps Avg"
                })
        else:
            insights.append({
                "category": "steps",
                "title": "Weekly Activity Average",
                "text": f"Your daily step count averaged {recent_steps_avg:,} steps over the past 7 days against your {step_goal:,} step goal.",
                "type": "positive" if recent_steps_avg >= step_goal else "neutral",
                "icon": "👟",
                "metric_label": f"{recent_steps_avg:,} Steps Avg"
            })

        if step_goal_met_days >= 5:
            insights.append({
                "category": "steps",
                "title": "Consistent Step Target Completion",
                "text": f"You reached your daily step goal on {step_goal_met_days} of the last 7 days.",
                "type": "positive",
                "icon": "🎯",
                "metric_label": f"{step_goal_met_days}/7 Days Met"
            })

        # =========================================================================
        # 3. WATER INTAKE ANALYSIS
        # =========================================================================
        cursor.execute("""
            SELECT intake_date, SUM(amount_ml) as daily_total
            FROM water_intake
            WHERE user_id = ? AND intake_date IN ({})
            GROUP BY intake_date;
        """.format(','.join('?' for _ in past_7_days)), [user_id] + past_7_days)
        water_rows = cursor.fetchall()
        water_dict = {r["intake_date"]: r["daily_total"] for r in water_rows}
        
        water_days_list = [water_dict.get(d, 0) for d in past_7_days]
        water_avg_ml = round(sum(water_days_list) / 7)
        water_goal_completed_days = sum(1 for w in water_days_list if w >= water_goal)

        if water_goal_completed_days >= 5:
            insights.append({
                "category": "water",
                "title": "Hydration Goal Consistency",
                "text": f"You completed your water goal on {water_goal_completed_days} of the last 7 days.",
                "type": "positive",
                "icon": "💧",
                "metric_label": f"{water_goal_completed_days}/7 Days Met"
            })
        elif water_goal_completed_days >= 3:
            insights.append({
                "category": "water",
                "title": "Hydration Progress",
                "text": f"You reached your full water target on {water_goal_completed_days} of the last 7 days, averaging {water_avg_ml:,} ml daily.",
                "type": "neutral",
                "icon": "💧",
                "metric_label": f"{water_avg_ml:,} ml Daily Avg"
            })
        else:
            insights.append({
                "category": "water",
                "title": "Hydration Opportunity",
                "text": f"Your recorded hydration averaged {water_avg_ml:,} ml per day over the last week. Keeping a water bottle nearby can help you stay on track towards your {water_goal:,} ml goal.",
                "type": "suggestion",
                "icon": "💧",
                "metric_label": f"{water_avg_ml:,} ml / {water_goal:,} ml Goal"
            })

        # =========================================================================
        # 4. SLEEP & REST ANALYSIS
        # =========================================================================
        cursor.execute("""
            SELECT sleep_date, SUM(duration_minutes) as daily_duration_minutes, AVG(sleep_quality_score) as avg_quality
            FROM sleep_records
            WHERE user_id = ? AND sleep_date IN ({})
            GROUP BY sleep_date
            ORDER BY sleep_date ASC;
        """.format(','.join('?' for _ in past_7_days)), [user_id] + past_7_days)
        sleep_rows = cursor.fetchall()
        
        sleep_durations_hrs = [round(r["daily_duration_minutes"] / 60.0, 1) for r in sleep_rows]
        sleep_qualities = [r["avg_quality"] for r in sleep_rows if r["avg_quality"]]
        
        days_below_sleep_target = sum(1 for h in sleep_durations_hrs if h < (sleep_goal - 0.2))
        avg_sleep_hrs = round(sum(sleep_durations_hrs) / len(sleep_durations_hrs), 1) if sleep_durations_hrs else 0.0
        avg_sleep_quality = round(sum(sleep_qualities) / len(sleep_qualities)) if sleep_qualities else 0

        if days_below_sleep_target >= 3:
            insights.append({
                "category": "sleep",
                "title": "Sleep Duration Pattern",
                "text": f"Your recorded sleep was below your personal target on {days_below_sleep_target} of the last 7 days (averaging {avg_sleep_hrs} hours vs your {sleep_goal} hour target).",
                "type": "suggestion",
                "icon": "🌙",
                "metric_label": f"{avg_sleep_hrs}h Avg vs {sleep_goal}h Goal"
            })
        elif sleep_durations_hrs and avg_sleep_hrs >= sleep_goal:
            insights.append({
                "category": "sleep",
                "title": "Restful Sleep Consistency",
                "text": f"Your recorded sleep averaged {avg_sleep_hrs} hours over the past week, successfully meeting your {sleep_goal} hour target.",
                "type": "positive",
                "icon": "🌙",
                "metric_label": f"{avg_sleep_hrs}h Sleep Average"
            })
        elif sleep_durations_hrs:
            insights.append({
                "category": "sleep",
                "title": "Weekly Sleep Balance",
                "text": f"You logged an average of {avg_sleep_hrs} hours of rest over the past week with an average sleep quality score of {avg_sleep_quality}%.",
                "type": "neutral",
                "icon": "🌙",
                "metric_label": f"{avg_sleep_hrs}h Avg / {avg_sleep_quality}% Quality"
            })

        # Check for severe short sleep pattern (< 5 hours on consecutive recorded sessions)
        short_sleep_consecutive = sum(1 for h in sleep_durations_hrs[-3:] if h < 5.0)
        if short_sleep_consecutive >= 3:
            insights.append({
                "category": "sleep",
                "title": "Consecutive Short Sleep Recorded",
                "text": "Your recent logs record multiple nights under 5 hours of sleep. Adequate rest is essential for daily recovery. If you experience persistent sleep difficulties, you may wish to discuss sleep habits with a qualified healthcare professional.",
                "type": "advisory",
                "icon": "🩺",
                "metric_label": "Rest Advisory"
            })

        # =========================================================================
        # 5. WEIGHT & BMI TRENDS ANALYSIS
        # =========================================================================
        cursor.execute("""
            SELECT record_date, weight_kg, bmi
            FROM weight_records
            WHERE user_id = ?
            ORDER BY record_date DESC, id DESC
            LIMIT 10;
        """, (user_id,))
        weight_rows = cursor.fetchall()

        if len(weight_rows) >= 2:
            latest_w = float(weight_rows[0]["weight_kg"])
            earliest_w = float(weight_rows[-1]["weight_kg"])
            weight_diff = round(latest_w - earliest_w, 1)

            if target_weight:
                diff_to_target = round(abs(latest_w - target_weight), 1)
                if diff_to_target <= 0.5:
                    insights.append({
                        "category": "weight",
                        "title": "Target Weight Alignment",
                        "text": f"Your current recorded weight of {latest_w} kg is aligned with your target goal of {target_weight} kg.",
                        "type": "positive",
                        "icon": "⚖️",
                        "metric_label": f"{latest_w} kg (On Target)"
                    })
                elif abs(weight_diff) >= 0.3:
                    direction_text = "decreased" if weight_diff < 0 else "increased"
                    insights.append({
                        "category": "weight",
                        "title": "Weight Trend Observation",
                        "text": f"Your recorded body weight has {direction_text} by {abs(weight_diff)} kg across your recent logged measurements (currently {latest_w} kg).",
                        "type": "neutral",
                        "icon": "⚖️",
                        "metric_label": f"{'+' if weight_diff > 0 else ''}{weight_diff} kg Trend"
                    })
                else:
                    insights.append({
                        "category": "weight",
                        "title": "Stable Weight Trend",
                        "text": f"Your body weight measurements have remained steady across recent logs, averaging around {latest_w} kg.",
                        "type": "neutral",
                        "icon": "⚖️",
                        "metric_label": f"{latest_w} kg Stable"
                    })
            else:
                if abs(weight_diff) >= 0.5:
                    direction = "a decrease" if weight_diff < 0 else "an increase"
                    insights.append({
                        "category": "weight",
                        "title": "Weight Progression Record",
                        "text": f"Your recorded entries reflect {direction} of {abs(weight_diff)} kg across recent entries (latest: {latest_w} kg).",
                        "type": "neutral",
                        "icon": "⚖️",
                        "metric_label": f"{latest_w} kg"
                    })
                else:
                    insights.append({
                        "category": "weight",
                        "title": "Consistent Weight Baseline",
                        "text": f"Your weight measurements remain steady at approximately {latest_w} kg across recent logs.",
                        "type": "neutral",
                        "icon": "⚖️",
                        "metric_label": f"{latest_w} kg"
                    })
        elif len(weight_rows) == 1:
            latest_w = float(weight_rows[0]["weight_kg"])
            insights.append({
                "category": "weight",
                "title": "Weight Baseline Logged",
                "text": f"Your initial weight entry is recorded at {latest_w} kg. Logging measurements consistently helps visualize long-term patterns.",
                "type": "neutral",
                "icon": "⚖️",
                "metric_label": f"{latest_w} kg Baseline"
            })

        # =========================================================================
        # 6. HEART RATE & CARDIO ANALYSIS
        # =========================================================================
        cursor.execute("""
            SELECT measured_at, bpm, resting_heart_rate, activity_context
            FROM heart_rate
            WHERE user_id = ?
            ORDER BY measured_at DESC, id DESC
            LIMIT 15;
        """, (user_id,))
        hr_rows = cursor.fetchall()

        if hr_rows:
            bpms = [r["bpm"] for r in hr_rows]
            resting_bpms = [r["bpm"] for r in hr_rows if (r["activity_context"] or "").lower() == "resting"]
            
            avg_all_bpm = round(sum(bpms) / len(bpms))
            avg_resting_bpm = round(sum(resting_bpms) / len(resting_bpms)) if resting_bpms else avg_all_bpm
            latest_bpm = bpms[0]

            # Check for neutral resting advisory if consistently high or low
            if avg_resting_bpm > 100:
                insights.append({
                    "category": "heart_rate",
                    "title": "Elevated Resting Pulse Observation",
                    "text": f"Your recorded resting heart rate has averaged {avg_resting_bpm} BPM across entries. Heart rate varies with caffeine, stress, and hydration. If you notice persistent high resting pulse or feel unwell, consider discussing your readings with a qualified healthcare professional.",
                    "type": "advisory",
                    "icon": "🩺",
                    "metric_label": f"{avg_resting_bpm} BPM Resting Avg"
                })
            elif avg_resting_bpm < 50:
                insights.append({
                    "category": "heart_rate",
                    "title": "Low Resting Pulse Observation",
                    "text": f"Your recorded resting heart rate averaged {avg_resting_bpm} BPM. While low resting pulse is common in conditioned individuals, feel free to review any questions with a qualified healthcare professional.",
                    "type": "neutral",
                    "icon": "❤️",
                    "metric_label": f"{avg_resting_bpm} BPM Resting Avg"
                })
            else:
                insights.append({
                    "category": "heart_rate",
                    "title": "Heart Rate Pattern",
                    "text": f"Your average resting heart rate recorded across recent entries is {avg_resting_bpm} BPM (latest reading: {latest_bpm} BPM).",
                    "type": "positive",
                    "icon": "❤️",
                    "metric_label": f"{avg_resting_bpm} BPM Average"
                })

        # =========================================================================
        # 7. MULTI-METRIC SUMMARY CARDS
        # =========================================================================
        metrics_summary = {
            "steps": {
                "avg_daily": recent_steps_avg,
                "prev_avg": prev_steps_avg,
                "goal": step_goal,
                "goal_met_days": step_goal_met_days,
                "streak": consecutive_active_days
            },
            "water": {
                "avg_daily_ml": water_avg_ml,
                "goal_ml": water_goal,
                "goal_met_days": water_goal_completed_days
            },
            "sleep": {
                "avg_daily_hrs": avg_sleep_hrs,
                "goal_hrs": sleep_goal,
                "avg_quality": avg_sleep_quality,
                "days_below_goal": days_below_sleep_target
            },
            "weight": {
                "latest_kg": weight_rows[0]["weight_kg"] if weight_rows else None,
                "target_kg": target_weight
            },
            "heart_rate": {
                "latest_bpm": hr_rows[0]["bpm"] if hr_rows else None,
                "total_records": len(hr_rows)
            }
        }

    return {
        "user": dict(user),
        "today_str": today_str,
        "insights": insights,
        "metrics_summary": metrics_summary,
        "total_insights": len(insights),
        "positive_count": sum(1 for i in insights if i["type"] == "positive"),
        "suggestion_count": sum(1 for i in insights if i["type"] == "suggestion"),
        "advisory_count": sum(1 for i in insights if i["type"] == "advisory")
    }
