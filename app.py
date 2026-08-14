"""
HealthTrack AI - Main Flask Application
Step 3: User Authentication (Register, Login, Logout, Sessions, Route Guarding)
"""

import os
import functools
from flask import Flask, render_template, request, redirect, url_for, session, flash
from database import init_db
from auth import (
    register_user,
    authenticate_user,
    validate_registration_data,
    get_user_by_id
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
            flash("Please log in to access your dashboard.", "warning")
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
        "version": "1.2.0 (Step 3 - Authentication Ready)"
    }
    return render_template('index.html', info=app_info)


# 5. User Registration Route
@app.route('/register', methods=['GET', 'POST'])
def register():
    """
    Handles user registration:
    - Collects: Full Name, Email, Password, Age, Gender, Height, Weight.
    - Validates fields on backend.
    - Hashes password.
    - Creates database record.
    - Automatically logs in and redirects to /dashboard.
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
    Handles user login:
    - Authenticates credentials against password hash.
    - Sets session and redirects to /dashboard (or requested 'next' URL).
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
            # Establish session
            session.clear()
            session["user_id"] = user["id"]
            session["username"] = user["username"]
            session["full_name"] = user["full_name"]
            session["email"] = user["email"]

            flash(f"Welcome back, {user['full_name']}!", "success")
            
            # Redirect to next URL or dashboard
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
    Protected dashboard route.
    Displays user profile summary and confirmation of Step 3 authentication.
    """
    user_id = session.get("user_id")
    user = get_user_by_id(user_id)
    
    # Calculate BMI
    bmi = None
    if user and user.get("height_cm") and user.get("target_weight_kg"):
        h_m = user["height_cm"] / 100.0
        if h_m > 0:
            bmi = round(user["target_weight_kg"] / (h_m * h_m), 1)

    return render_template('dashboard.html', user=user, bmi=bmi)


# 9. Application Entry Point
if __name__ == '__main__':
    print("-------------------------------------------------------")
    print("[HealthTrack AI] Initializing SQLite database...")
    init_db()
    print("[HealthTrack AI] Server starting...")
    print("[HealthTrack AI] Open your browser at: http://127.0.0.1:5000")
    print("-------------------------------------------------------")
    app.run(host='127.0.0.1', port=5000, debug=True)
