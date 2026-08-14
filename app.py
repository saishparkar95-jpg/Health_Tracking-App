"""
HealthTrack AI - Main Application (Step 2: Database Initialization)

This file initializes the Flask web server, connects to the SQLite database,
and serves the application.
"""

from flask import Flask, render_template
from database import init_db

# 1. Initialize the Flask application
app = Flask(__name__)

# 2. Define the Home Page Route
@app.route('/')
def home():
    """
    Renders the landing/home page template for HealthTrack AI.
    """
    app_info = {
        "name": "HealthTrack AI",
        "tagline": "Your Modern Personal Health & Wellness Companion",
        "version": "1.1.0 (Step 2 - Database Ready)"
    }
    return render_template('index.html', info=app_info)

# 3. Application Entry Point
if __name__ == '__main__':
    print("-------------------------------------------------------")
    print("[HealthTrack AI] Initializing SQLite database...")
    init_db()
    print("[HealthTrack AI] Server starting...")
    print("[HealthTrack AI] Open your browser at: http://127.0.0.1:5000")
    print("-------------------------------------------------------")
    app.run(host='127.0.0.1', port=5000, debug=True)

