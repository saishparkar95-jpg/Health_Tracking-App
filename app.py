"""
HealthTrack AI - Main Flask Application
Step 5: Step Tracking Module (Add/Update Steps, Daily Goal Setting, Weekly/Monthly Charts, History)
"""

import os
import functools
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
    ensure_user_has_initial_data,
    log_user_water,
    log_user_sleep,
    log_user_weight,
    log_user_heart_rate
)
from step_service import (
    get_step_module_data,
    add_user_steps,
    update_user_steps,
    set_user_step_goal,
    delete_user_step_record
)

# 1. Initialize Flask Application
app = Flask(__name__)
app.secret_key = os.environ.get("SECRET_KEY", "healthtrack-ai-secret-dev-key-2026-secure-session")


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
    Renders the public landing page.
    """
    app_info = {
        "name": "HealthTrack AI",
        "tagline": "Your Modern Personal Health & Wellness Companion",
        "version": "1.4.0 (Step 5 - Step Tracking Ready)"
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
            
            # Establish user session
            session.clear()
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
    """
    Dedicated Step Tracking Module page:
    - Today's Progress Ring, Distance, Calories, Active Time
    - Goal setting
    - Weekly (7-day) and Monthly (30-day) Chart.js views
    - Activity history table with edit/delete
    """
    user_id = session.get("user_id")
    ensure_user_has_initial_data(user_id)

    step_data = get_step_module_data(user_id)
    return render_template('steps.html', **step_data)


@app.route('/steps/add', methods=['POST'], endpoint='add_steps_route')
@app.route('/log/steps', methods=['POST'], endpoint='log_steps_route')
@login_required
def add_steps_route():
    """
    Adds/increments steps for today or a chosen date.
    Prevents negative or invalid values.
    """
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
    """
    Sets the exact step count for today or a specified date.
    """
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
    """
    Updates the user's daily step goal.
    """
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
    """
    Deletes an activity record belonging to the logged-in user.
    """
    user_id = session.get("user_id")
    success = delete_user_step_record(user_id, record_id)
    if success:
        flash("Activity record deleted successfully.", "info")
    else:
        flash("Record could not be found or unauthorized.", "danger")
    return redirect(url_for("steps_page"))


# 10. Quick Logging Endpoints for Other Modules
@app.route('/log/water', methods=['POST'])
@login_required
def log_water_route():
    user_id = session.get("user_id")
    amount = request.form.get("amount_ml") or 250
    beverage = request.form.get("beverage_type") or "Water"
    try:
        log_user_water(user_id, int(amount), beverage)
        flash(f"Logged +{int(amount)} ml of {beverage}!", "success")
    except Exception as e:
        flash("Failed to log water intake.", "danger")
    return redirect(request.referrer or url_for("dashboard"))


@app.route('/log/sleep', methods=['POST'])
@login_required
def log_sleep_route():
    user_id = session.get("user_id")
    hours = request.form.get("hours") or 8.0
    quality = request.form.get("quality") or 85
    try:
        log_user_sleep(user_id, float(hours), int(quality))
        flash(f"Logged {float(hours)} hrs of sleep (Quality: {int(quality)}%)!", "success")
    except Exception as e:
        flash("Failed to log sleep entry.", "danger")
    return redirect(request.referrer or url_for("dashboard"))


@app.route('/log/weight', methods=['POST'])
@login_required
def log_weight_route():
    user_id = session.get("user_id")
    weight = request.form.get("weight_kg")
    try:
        if weight:
            log_user_weight(user_id, float(weight))
            flash(f"Weight updated to {float(weight):.1f} kg!", "success")
    except Exception as e:
        flash("Failed to update weight.", "danger")
    return redirect(request.referrer or url_for("dashboard"))


@app.route('/log/heart-rate', methods=['POST'])
@login_required
def log_heart_rate_route():
    user_id = session.get("user_id")
    bpm = request.form.get("bpm")
    context = request.form.get("context") or "resting"
    try:
        if bpm:
            log_user_heart_rate(user_id, int(bpm), context)
            flash(f"Heart rate {int(bpm)} BPM ({context}) recorded!", "success")
    except Exception as e:
        flash("Failed to record heart rate.", "danger")
    return redirect(request.referrer or url_for("dashboard"))


# 11. Application Entry Point
if __name__ == '__main__':
    print("-------------------------------------------------------")
    print("[HealthTrack AI] Initializing SQLite database...")
    init_db()
    print("[HealthTrack AI] Server starting...")
    print("[HealthTrack AI] Open your browser at: http://127.0.0.1:5000")
    print("-------------------------------------------------------")
    app.run(host='127.0.0.1', port=5000, debug=True)
