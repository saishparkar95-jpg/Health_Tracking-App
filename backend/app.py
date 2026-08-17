"""
HealthTrack AI - Main Flask Application
Step 9: Heart Rate Tracking, Goals Dashboard & Dynamic Achievements
"""

import os
import functools
from datetime import timedelta
from flask import Flask, render_template, request, redirect, url_for, session, flash, jsonify
from database import init_db
from auth import (
    register_user,
    authenticate_user,
    validate_registration_data,
    get_user_by_id
)
from dashboard_service import (
    get_user_dashboard_metrics,
    ensure_user_has_initial_data
)
from step_service import (
    get_step_module_data,
    add_user_steps,
    update_user_steps,
    set_user_step_goal,
    delete_user_step_record
)
from water_service import (
    get_water_module_data,
    add_water_intake,
    set_user_water_goal,
    delete_user_water_log
)
from sleep_service import (
    get_sleep_module_data,
    add_sleep_record,
    set_user_sleep_goal,
    delete_user_sleep_record
)
from weight_service import (
    get_weight_module_data,
    add_weight_record,
    update_user_height,
    set_user_target_weight,
    delete_user_weight_record
)
from heart_service import (
    get_heart_module_data,
    add_heart_rate_record,
    delete_heart_rate_record
)
from goals_service import (
    get_goals_and_achievements_data,
    update_all_user_goals
)
from insights_service import (
    analyze_user_wellness_insights
)

# 1. Initialize Flask Application
BACKEND_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(BACKEND_DIR)
app = Flask(
    __name__,
    template_folder=os.path.join(PROJECT_ROOT, "frontend", "templates"),
    static_folder=os.path.join(PROJECT_ROOT, "frontend", "static")
)
app.secret_key = os.environ.get("SECRET_KEY", "healthtrack-ai-secret-dev-key-2026-secure-session")
app.config['PERMANENT_SESSION_LIFETIME'] = timedelta(days=365)


# 2. Global Template Context
@app.context_processor
def inject_current_user():
    """
    Injects logged-in user information into all templates.
    """
    user_id = session.get("user_id")
    current_user = get_user_by_id(user_id) if user_id else None
    return dict(current_user=current_user)


# 3. Authentication Route Guard Decorator
def login_required(view):
    """
    Decorator to restrict access to authenticated users only.
    Redirects unauthenticated visitors to /login.
    """
    @functools.wraps(view)
    def wrapped_view(**kwargs):
        if "user_id" not in session:
            flash("Please log in to access your health dashboard.", "warning")
            return redirect(url_for("login", next=request.url))
        return view(**kwargs)
    return wrapped_view


# 4. Public Landing Page Route
@app.route('/')
def home():
    """
    Renders the public landing page, or redirects already logged in users directly to dashboard.
    """
    if "user_id" in session:
        return redirect(url_for("dashboard"))

    app_info = {
        "name": "HealthTrack AI",
        "tagline": "Your Modern Personal Health & Wellness Companion",
        "version": "1.8.0 (Step 9 - Cardio & Goals Ready)"
    }
    return render_template('index.html', info=app_info)


# 5. User Registration Route
@app.route('/register', methods=['GET', 'POST'])
def register():
    """
    Handles user registration with biometrics and redirects to /dashboard.
    """
    if "user_id" in session:
        return redirect(url_for("dashboard"))

    if request.method == 'POST':
        form_data = {
            "full_name": request.form.get("full_name"),
            "email": request.form.get("email"),
            "password": request.form.get("password"),
            "confirm_password": request.form.get("confirm_password"),
            "age": request.form.get("age"),
            "gender": request.form.get("gender"),
            "height": request.form.get("height"),
            "weight": request.form.get("weight")
        }

        # Backend Validation
        is_valid, error_msg = validate_registration_data(form_data)
        if not is_valid:
            flash(error_msg, "danger")
            return render_template("register.html", form=form_data), 400

        try:
            user = register_user(
                full_name=form_data["full_name"],
                email=form_data["email"],
                password=form_data["password"],
                age=form_data["age"],
                gender=form_data["gender"],
                height=form_data["height"],
                weight=form_data["weight"]
            )
            
            # Establish user session (persistent for 365 days)
            session.clear()
            session.permanent = True
            session["user_id"] = user["id"]
            session["username"] = user["username"]
            session["full_name"] = user["full_name"]
            session["email"] = user["email"]

            # Initialize realistic tracking history for immediate chart visualization
            ensure_user_has_initial_data(user["id"])

            flash(f"Welcome to HealthTrack AI, {user['full_name']}! Your account is ready.", "success")
            return redirect(url_for("dashboard"))

        except ValueError as err:
            flash(str(err), "danger")
            return render_template("register.html", form=form_data), 400
        except Exception as e:
            flash("An unexpected error occurred during registration. Please try again.", "danger")
            return render_template("register.html", form=form_data), 500

    return render_template('register.html', form={})


# 6. User Login Route
@app.route('/login', methods=['GET', 'POST'])
def login():
    """
    Handles user login.
    """
    if "user_id" in session:
        return redirect(url_for("dashboard"))

    if request.method == 'POST':
        identifier = (request.form.get("identifier") or "").strip()
        password = request.form.get("password") or ""
        next_url = request.form.get("next") or request.args.get("next")

        if not identifier or not password:
            flash("Please enter both email/username and password.", "danger")
            return render_template("login.html", identifier=identifier), 400

        user = authenticate_user(identifier, password)
        if user:
            session.clear()
            session.permanent = True
            session["user_id"] = user["id"]
            session["username"] = user["username"]
            session["full_name"] = user["full_name"]
            session["email"] = user["email"]

            ensure_user_has_initial_data(user["id"])

            flash(f"Welcome back, {user['full_name']}!", "success")
            
            if next_url and next_url.startswith('/'):
                return redirect(next_url)
            return redirect(url_for("dashboard"))
        else:
            flash("Invalid email/username or password. Please try again.", "danger")
            return render_template("login.html", identifier=identifier), 401

    return render_template('login.html', identifier="")


# 7. User Logout Route
@app.route('/logout', methods=['GET', 'POST'])
def logout():
    """
    Logs out user by clearing the session.
    """
    session.clear()
    flash("You have been logged out successfully.", "info")
    return redirect(url_for("login"))


# 8. User Dashboard Route (Protected)
@app.route('/dashboard')
@login_required
def dashboard():
    """
    Main HealthTrack AI Dashboard.
    """
    user_id = session.get("user_id")
    ensure_user_has_initial_data(user_id)
    
    data = get_user_dashboard_metrics(user_id)
    if not data:
        flash("User profile could not be loaded. Please re-login.", "danger")
        return redirect(url_for("logout"))

    return render_template('dashboard.html', **data)


# =============================================================================
# 9. STEP TRACKING MODULE ROUTES (STEP 5)
# =============================================================================

@app.route('/steps')
@login_required
def steps_page():
    user_id = session.get("user_id")
    ensure_user_has_initial_data(user_id)
    step_data = get_step_module_data(user_id)
    return render_template('steps.html', **step_data)


@app.route('/steps/add', methods=['POST'], endpoint='add_steps_route')
@app.route('/log/steps', methods=['POST'], endpoint='log_steps_route')
@login_required
def add_steps_route():
    user_id = session.get("user_id")
    steps = request.form.get("steps")
    target_date = request.form.get("date") or None
    notes = request.form.get("notes") or None
    redirect_target = request.form.get("redirect") or request.referrer or url_for("dashboard")

    try:
        new_total = add_user_steps(user_id, steps, target_date=target_date, notes=notes)
        flash(f"Successfully added +{int(steps):,} steps! Today's total is now {new_total:,} steps.", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("An error occurred while logging steps.", "danger")

    return redirect(redirect_target)


@app.route('/steps/update', methods=['POST'])
@login_required
def update_steps_route():
    user_id = session.get("user_id")
    steps = request.form.get("steps")
    target_date = request.form.get("date") or None
    notes = request.form.get("notes") or None
    redirect_target = request.form.get("redirect") or url_for("steps_page")

    try:
        updated_val = update_user_steps(user_id, steps, target_date=target_date, notes=notes)
        flash(f"Step count updated to {updated_val:,} steps.", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("Failed to update step count.", "danger")

    return redirect(redirect_target)


@app.route('/steps/goal', methods=['POST'])
@login_required
def set_step_goal_route():
    user_id = session.get("user_id")
    new_goal = request.form.get("daily_step_goal")
    redirect_target = request.form.get("redirect") or url_for("steps_page")

    try:
        saved_goal = set_user_step_goal(user_id, new_goal)
        flash(f"Daily step goal updated to {saved_goal:,} steps!", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("Failed to update daily step goal.", "danger")

    return redirect(redirect_target)


@app.route('/steps/delete/<int:record_id>', methods=['POST'])
@login_required
def delete_step_record_route(record_id):
    user_id = session.get("user_id")
    success = delete_user_step_record(user_id, record_id)
    if success:
        flash("Activity record deleted successfully.", "info")
    else:
        flash("Record could not be found or unauthorized.", "danger")
    return redirect(url_for("steps_page"))


# =============================================================================
# 10. WATER TRACKING MODULE ROUTES (STEP 6)
# =============================================================================

@app.route('/water')
@login_required
def water_page():
    user_id = session.get("user_id")
    ensure_user_has_initial_data(user_id)
    water_data = get_water_module_data(user_id)
    return render_template('water.html', **water_data)


@app.route('/water/add', methods=['POST'], endpoint='add_water_route')
@app.route('/log/water', methods=['POST'], endpoint='log_water_route')
@login_required
def add_water_route():
    user_id = session.get("user_id")
    amount = request.form.get("amount_ml")
    beverage = request.form.get("beverage_type") or "Water"
    target_date = request.form.get("date") or None
    redirect_target = request.form.get("redirect") or request.referrer or url_for("water_page")

    try:
        new_total = add_water_intake(user_id, amount, beverage_type=beverage, intake_date=target_date)
        flash(f"Logged +{int(amount):,} ml of {beverage}! Today's hydration is {new_total:,} ml.", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("An error occurred while logging water intake.", "danger")

    return redirect(redirect_target)


@app.route('/water/goal', methods=['POST'])
@login_required
def set_water_goal_route():
    user_id = session.get("user_id")
    new_goal = request.form.get("daily_water_goal_ml")
    redirect_target = request.form.get("redirect") or url_for("water_page")

    try:
        saved_goal = set_user_water_goal(user_id, new_goal)
        flash(f"Daily water goal updated to {saved_goal:,} ml!", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("Failed to update daily water goal.", "danger")

    return redirect(redirect_target)


@app.route('/water/delete/<int:log_id>', methods=['POST'])
@login_required
def delete_water_log_route(log_id):
    user_id = session.get("user_id")
    success = delete_user_water_log(user_id, log_id)
    if success:
        flash("Water intake record deleted successfully.", "info")
    else:
        flash("Water log could not be found or unauthorized.", "danger")
    return redirect(url_for("water_page"))


# =============================================================================
# 11. SLEEP TRACKING MODULE ROUTES (STEP 7)
# =============================================================================

@app.route('/sleep')
@login_required
def sleep_page():
    user_id = session.get("user_id")
    ensure_user_has_initial_data(user_id)
    sleep_data = get_sleep_module_data(user_id)
    return render_template('sleep.html', **sleep_data)


@app.route('/sleep/add', methods=['POST'], endpoint='add_sleep_route')
@app.route('/log/sleep', methods=['POST'], endpoint='log_sleep_route')
@login_required
def add_sleep_route():
    user_id = session.get("user_id")
    bedtime = request.form.get("bedtime")
    wake_time = request.form.get("wake_time")
    hours_direct = request.form.get("hours")
    sleep_date = request.form.get("date") or None
    quality = request.form.get("quality") or 85
    notes = request.form.get("notes") or None
    redirect_target = request.form.get("redirect") or request.referrer or url_for("sleep_page")

    try:
        if not bedtime or not wake_time:
            if hours_direct:
                h_val = float(hours_direct)
                bedtime = "23:00"
                wake_mins = int((23 * 60 + h_val * 60) % 1440)
                wake_time = f"{wake_mins // 60:02d}:{wake_mins % 60:02d}"
            else:
                raise ValueError("Please provide both bedtime and wake-up time.")

        duration_mins = add_sleep_record(
            user_id=user_id,
            bedtime_str=bedtime,
            wake_time_str=wake_time,
            sleep_date_str=sleep_date,
            quality_score=quality,
            notes=notes
        )
        h_res = duration_mins // 60
        m_res = duration_mins % 60
        flash(f"Successfully logged {h_res}h {m_res}m of sleep (Quality: {quality}%)!", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("An error occurred while saving sleep record.", "danger")

    return redirect(redirect_target)


@app.route('/sleep/goal', methods=['POST'])
@login_required
def set_sleep_goal_route():
    user_id = session.get("user_id")
    new_goal = request.form.get("daily_sleep_goal_hours")
    redirect_target = request.form.get("redirect") or url_for("sleep_page")

    try:
        saved_goal = set_user_sleep_goal(user_id, new_goal)
        flash(f"Daily sleep goal updated to {saved_goal} hours!", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("Failed to update daily sleep goal.", "danger")

    return redirect(redirect_target)


@app.route('/sleep/delete/<int:record_id>', methods=['POST'])
@login_required
def delete_sleep_record_route(record_id):
    user_id = session.get("user_id")
    success = delete_user_sleep_record(user_id, record_id)
    if success:
        flash("Sleep record deleted successfully.", "info")
    else:
        flash("Sleep record could not be found or unauthorized.", "danger")
    return redirect(url_for("sleep_page"))


# =============================================================================
# 12. WEIGHT TRACKING & BMI MODULE ROUTES (STEP 8)
# =============================================================================

@app.route('/weight')
@login_required
def weight_page():
    user_id = session.get("user_id")
    ensure_user_has_initial_data(user_id)
    weight_data = get_weight_module_data(user_id)
    return render_template('weight.html', **weight_data)


@app.route('/weight/add', methods=['POST'], endpoint='add_weight_route')
@app.route('/log/weight', methods=['POST'], endpoint='log_weight_route')
@login_required
def add_weight_route():
    user_id = session.get("user_id")
    weight = request.form.get("weight_kg")
    record_date = request.form.get("date") or None
    notes = request.form.get("notes") or None
    redirect_target = request.form.get("redirect") or request.referrer or url_for("weight_page")

    try:
        w_val, bmi_val = add_weight_record(user_id, weight, record_date=record_date, notes=notes)
        flash(f"Logged weight: {w_val:.1f} kg (BMI: {bmi_val}) successfully!", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("An error occurred while logging weight.", "danger")

    return redirect(redirect_target)


@app.route('/weight/height', methods=['POST'])
@login_required
def update_height_route():
    user_id = session.get("user_id")
    height = request.form.get("height_cm")
    redirect_target = request.form.get("redirect") or url_for("weight_page")

    try:
        h_val = update_user_height(user_id, height)
        flash(f"Profile height updated to {h_val:.1f} cm! BMI values recalculated.", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("Failed to update profile height.", "danger")

    return redirect(redirect_target)


@app.route('/weight/target', methods=['POST'])
@login_required
def set_target_weight_route():
    user_id = session.get("user_id")
    target_weight = request.form.get("target_weight_kg")
    redirect_target = request.form.get("redirect") or url_for("weight_page")

    try:
        saved_target = set_user_target_weight(user_id, target_weight)
        flash(f"Target body weight updated to {saved_target:.1f} kg!", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("Failed to update target weight.", "danger")

    return redirect(redirect_target)


@app.route('/weight/delete/<int:record_id>', methods=['POST'])
@login_required
def delete_weight_record_route(record_id):
    user_id = session.get("user_id")
    success = delete_user_weight_record(user_id, record_id)
    if success:
        flash("Weight entry deleted successfully.", "info")
    else:
        flash("Weight record could not be found or unauthorized.", "danger")
    return redirect(url_for("weight_page"))


# =============================================================================
# 13. HEART RATE TRACKING MODULE ROUTES (STEP 9)
# =============================================================================

@app.route('/heart')
@login_required
def heart_page():
    """
    Dedicated Heart Rate Tracking Module page:
    - Latest BPM reading & Context (Resting, Post-Workout, etc.)
    - Average, min, max recorded BPM statistics
    - Heart rate trend line chart (Chart.js)
    - Manual entry with date, time, context, and notes
    - Medical disclaimer banner
    """
    user_id = session.get("user_id")
    ensure_user_has_initial_data(user_id)
    heart_data = get_heart_module_data(user_id)
    return render_template('heart.html', **heart_data)


@app.route('/heart/add', methods=['POST'], endpoint='add_heart_route')
@app.route('/log/heart-rate', methods=['POST'], endpoint='log_heart_rate_route')
@login_required
def add_heart_route():
    """
    Records a manual heart rate measurement in BPM.
    """
    user_id = session.get("user_id")
    bpm = request.form.get("bpm")
    m_date = request.form.get("date") or None
    m_time = request.form.get("time") or None
    context = request.form.get("context") or "Resting"
    notes = request.form.get("notes") or None
    redirect_target = request.form.get("redirect") or request.referrer or url_for("heart_page")

    try:
        saved_bpm = add_heart_rate_record(
            user_id=user_id,
            bpm=bpm,
            measured_date=m_date,
            measured_time=m_time,
            context=context,
            notes=notes
        )
        flash(f"Recorded heart rate: {saved_bpm} BPM ({context}) successfully!", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("An error occurred while saving heart rate reading.", "danger")

    return redirect(redirect_target)


@app.route('/heart/delete/<int:record_id>', methods=['POST'])
@login_required
def delete_heart_route(record_id):
    """
    Deletes a heart rate log entry.
    """
    user_id = session.get("user_id")
    success = delete_heart_rate_record(user_id, record_id)
    if success:
        flash("Heart rate measurement deleted successfully.", "info")
    else:
        flash("Record could not be found or unauthorized.", "danger")
    return redirect(url_for("heart_page"))


# =============================================================================
# 14. GOALS DASHBOARD & ACHIEVEMENTS SYSTEM (STEP 9)
# =============================================================================

@app.route('/goals')
@login_required
def goals_page():
    """
    Dedicated Goals Dashboard & Achievements Gallery:
    - Daily Step Goal, Daily Water Goal, Daily Sleep Goal, Target Weight
    - Today's completion percentages & progress status bars
    - Dynamic Achievement Badges (unlocked vs locked with progress indicators)
    """
    user_id = session.get("user_id")
    ensure_user_has_initial_data(user_id)
    goals_data = get_goals_and_achievements_data(user_id)
    return render_template('goals.html', **goals_data)


@app.route('/goals/update', methods=['POST'])
@login_required
def update_goals_route():
    """
    Updates all daily goals (Steps, Water, Sleep, Target Weight).
    """
    user_id = session.get("user_id")
    step_goal = request.form.get("daily_step_goal")
    water_goal = request.form.get("daily_water_goal_ml")
    sleep_goal = request.form.get("daily_sleep_goal_hours")
    target_weight = request.form.get("target_weight_kg")

    try:
        update_all_user_goals(
            user_id=user_id,
            step_goal=step_goal,
            water_goal=water_goal,
            sleep_goal=sleep_goal,
            target_weight=target_weight
        )
        flash("All daily health goals have been updated successfully!", "success")
    except ValueError as err:
        flash(str(err), "danger")
    except Exception as e:
        flash("Failed to update health goals.", "danger")

    return redirect(url_for("goals_page"))


# =============================================================================
# 15. AI WELLNESS INSIGHTS MODULE (STEP 10)
# =============================================================================

@app.route('/insights')
@login_required
def insights_page():
    """
    Dedicated AI Wellness Insights Module:
    - Multi-metric analysis across Steps, Water Intake, Sleep, Weight, and Heart Rate
    - Simple, understandable wellness observations and progression highlights
    - Safe non-diagnostic guidelines with neutral advisory banners
    """
    user_id = session.get("user_id")
    ensure_user_has_initial_data(user_id)
    insights_data = analyze_user_wellness_insights(user_id)
    return render_template('insights.html', **insights_data)


# =============================================================================
# 16. PROGRESSIVE WEB APP (PWA) ROUTES
# =============================================================================

@app.route('/manifest.json')
def manifest():
    """Serves PWA manifest configuration."""
    response = app.send_static_file('manifest.json')
    response.headers['Content-Type'] = 'application/manifest+json; charset=utf-8'
    return response


@app.route('/sw.js')
@app.route('/service-worker.js')
def service_worker():
    """Serves PWA Service Worker with full domain scope."""
    response = app.send_static_file('sw.js')
    response.headers['Content-Type'] = 'application/javascript; charset=utf-8'
    response.headers['Service-Worker-Allowed'] = '/'
    response.headers['Cache-Control'] = 'no-cache, no-store, must-revalidate'
    return response


# 17. Application Entry Point
if __name__ == '__main__':
    print("-------------------------------------------------------")
    print("[HealthTrack AI] Initializing SQLite database...")
    init_db()
    print("[HealthTrack AI] Server starting...")
    print("[HealthTrack AI] Open your browser at: http://127.0.0.1:5000")
    print("-------------------------------------------------------")
    app.run(host='127.0.0.1', port=5000, debug=True)
