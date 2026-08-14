"""
HealthTrack AI - Main Application (Step 1: Project Setup)

This file initializes the Flask web server and defines our first route (the home page).
"""

from flask import Flask, render_template

# 1. Initialize the Flask application
# Flask uses '__name__' to determine the root path of the application
app = Flask(__name__)

# 2. Define the Home Page Route
# The '@app.route('/')' decorator tells Flask to trigger this function when someone visits the root URL (e.g., http://127.0.0.1:5000/)
@app.route('/')
def home():
    """
    Renders the landing/home page template for HealthTrack AI.
    """
    app_info = {
        "name": "HealthTrack AI",
        "tagline": "Your Modern Personal Health & Wellness Companion",
        "version": "1.0.0 (Step 1 - Project Setup)"
    }
    return render_template('index.html', info=app_info)

# 3. Application Entry Point
# Ensures the web server only runs when this script is executed directly (not when imported as a module)
if __name__ == '__main__':
    print("-------------------------------------------------------")
    print("[HealthTrack AI] Server starting...")
    print("[HealthTrack AI] Open your browser at: http://127.0.0.1:5000")
    print("-------------------------------------------------------")
    # 'debug=True' enables automatic reloading when code changes and provides interactive error pages
    app.run(host='127.0.0.1', port=5000, debug=True)
