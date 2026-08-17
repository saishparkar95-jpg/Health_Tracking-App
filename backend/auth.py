"""
HealthTrack AI - Authentication & User Management Service (Step 3)

Provides secure password hashing, user registration, authentication,
and validation logic.
"""

import re
import sqlite3
from datetime import date
from werkzeug.security import generate_password_hash, check_password_hash
from database import get_db

# Email validation regex pattern
EMAIL_REGEX = re.compile(r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$")


def validate_registration_data(data):
    """
    Performs comprehensive backend validation for user registration.
    
    Returns:
        tuple (bool, str | None): (is_valid, error_message)
    """
    full_name = (data.get("full_name") or "").strip()
    email = (data.get("email") or "").strip().lower()
    password = data.get("password") or ""
    confirm_password = data.get("confirm_password") or ""
    gender = (data.get("gender") or "").strip().lower()
    age_raw = data.get("age")
    height_raw = data.get("height")
    weight_raw = data.get("weight")

    # 1. Required fields check
    if not full_name:
        return False, "Full Name is required."
    if len(full_name) < 2 or len(full_name) > 60:
        return False, "Full Name must be between 2 and 60 characters."

    if not email:
        return False, "Email address is required."
    if not EMAIL_REGEX.match(email):
        return False, "Please enter a valid email address (e.g. user@example.com)."

    if not password:
        return False, "Password is required."
    if len(password) < 6:
        return False, "Password must be at least 6 characters long."
    if confirm_password and password != confirm_password:
        return False, "Passwords do not match."

    # 2. Gender validation
    valid_genders = ["male", "female", "other", "prefer_not_to_say"]
    if gender not in valid_genders:
        return False, f"Please select a valid gender option ({', '.join(valid_genders)})."

    # 3. Numeric range validations
    try:
        age = int(age_raw)
        if age < 10 or age > 120:
            return False, "Age must be a realistic number between 10 and 120 years."
    except (ValueError, TypeError):
        return False, "Age must be a valid whole number."

    try:
        height = float(height_raw)
        if height < 50.0 or height > 260.0:
            return False, "Height must be between 50.0 cm and 260.0 cm."
    except (ValueError, TypeError):
        return False, "Height must be a valid number in centimeters."

    try:
        weight = float(weight_raw)
        if weight < 20.0 or weight > 350.0:
            return False, "Weight must be between 20.0 kg and 350.0 kg."
    except (ValueError, TypeError):
        return False, "Weight must be a valid number in kilograms."

    return True, None


def generate_unique_username(email, full_name):
    """
    Generates a clean, unique username derived from email or name.
    """
    base = email.split("@")[0].lower()
    base = re.sub(r"[^a-z0-9_]", "_", base)[:20]
    if len(base) < 3:
        base = re.sub(r"[^a-z0-9_]", "_", full_name.lower())[:20]
    
    with get_db() as db:
        cursor = db.cursor()
        username = base
        counter = 1
        while True:
            cursor.execute("SELECT id FROM users WHERE username = ?;", (username,))
            if not cursor.fetchone():
                return username
            username = f"{base}_{counter}"
            counter += 1


def register_user(full_name, email, password, age, gender, height, weight):
    """
    Registers a new user into the SQLite database.
    - Hashes password using werkzeug.security.
    - Saves user profile and biometric targets.
    - Logs the initial weight record.
    - Awards the 'first_step' achievement badge.
    
    Returns:
        dict: Newly created user record.
    Raises:
        ValueError: If email is already registered or validation fails.
    """
    email_clean = email.strip().lower()
    full_name_clean = full_name.strip()
    age_val = int(age)
    height_val = float(height)
    weight_val = float(weight)

    # Calculate initial BMI: weight (kg) / [height (m)]^2
    height_m = height_val / 100.0
    bmi_val = round(weight_val / (height_m * height_m), 1) if height_m > 0 else None

    # Check if email is already taken
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT id FROM users WHERE email = ?;", (email_clean,))
        if cursor.fetchone():
            raise ValueError("An account with this email address already exists. Please log in.")

        username = generate_unique_username(email_clean, full_name_clean)
        hashed_password = generate_password_hash(password)

        cursor.execute("""
            INSERT INTO users (
                username, email, password_hash, full_name, age, gender,
                height_cm, target_weight_kg, daily_step_goal, daily_water_goal_ml
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 10000, 2500);
        """, (
            username, email_clean, hashed_password, full_name_clean,
            age_val, gender, height_val, weight_val
        ))
        
        user_id = cursor.lastrowid

        # Insert initial weight log
        today_str = date.today().isoformat()
        cursor.execute("""
            INSERT INTO weight_records (user_id, record_date, weight_kg, bmi, notes)
            VALUES (?, ?, ?, ?, 'Initial weight during registration');
        """, (user_id, today_str, weight_val, bmi_val))

        # Award welcome badge
        cursor.execute("""
            INSERT OR IGNORE INTO achievements (user_id, badge_key, badge_name, badge_description, badge_icon, category)
            VALUES (?, 'welcome', 'Welcome Explorer', 'Successfully joined HealthTrack AI', 'sparkles', 'milestone');
        """, (user_id,))

        cursor.execute("SELECT * FROM users WHERE id = ?;", (user_id,))
        user_row = cursor.fetchone()
        return dict(user_row)


def authenticate_user(identifier, password):
    """
    Verifies user credentials by email or username.
    
    Returns:
        dict | None: User record if valid, otherwise None.
    """
    identifier_clean = identifier.strip().lower()
    
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("""
            SELECT * FROM users 
            WHERE LOWER(email) = ? OR LOWER(username) = ?;
        """, (identifier_clean, identifier_clean))
        user = cursor.fetchone()

        if not user:
            return None

        if check_password_hash(user["password_hash"], password):
            return dict(user)
        
        return None


def get_user_by_id(user_id):
    """
    Fetches user profile by user ID.
    """
    if not user_id:
        return None
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT * FROM users WHERE id = ?;", (user_id,))
        user = cursor.fetchone()
        return dict(user) if user else None
