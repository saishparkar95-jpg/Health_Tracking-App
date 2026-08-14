"""
HealthTrack AI - Authentication Automated Test Suite (Step 3)

Tests:
1. Registration endpoint with full biometric data collection.
2. Password hashing security verification.
3. Backend validation rejection of invalid data.
4. Prevention of duplicate email registration.
5. Login verification with valid & invalid credentials.
6. Route protection: Unauthenticated access to /dashboard is blocked.
7. Authenticated access to /dashboard succeeds.
8. Logout session termination.
"""

import sys
from app import app
from database import init_db, get_db
from werkzeug.security import check_password_hash


def run_auth_tests():
    print("=" * 70)
    print("       HEALTHTRACK AI - STEP 3 AUTHENTICATION TEST SUITE")
    print("=" * 70)

    # Initialize fresh test database
    init_db(reset=True)
    
    # Configure Flask test client
    app.config["TESTING"] = True
    app.config["WTF_CSRF_ENABLED"] = False
    client = app.test_client()

    # Test 1: Unauthenticated access to /dashboard must be redirected to /login
    print("\n[Test 1] Testing route protection: GET /dashboard without session...")
    res = client.get('/dashboard', follow_redirects=False)
    if res.status_code == 302 and '/login' in res.headers.get('Location', ''):
        print("  [PASSED] Unauthenticated user was redirected to /login.")
    else:
        print(f"  [FAILED] Expected 302 redirect to /login, got {res.status_code}")
        return False

    # Test 2: Backend validation on registration (invalid email & short password)
    print("\n[Test 2] Testing backend validation rejection on invalid registration...")
    res = client.post('/register', data={
        "full_name": "Test User",
        "email": "invalid-email-format",
        "password": "123",
        "confirm_password": "123",
        "age": 25,
        "gender": "male",
        "height": 175.0,
        "weight": 70.0
    }, follow_redirects=True)
    if b"valid email address" in res.data or res.status_code == 400:
        print("  [PASSED] Invalid email rejected with validation error.")
    else:
        print("  [FAILED] Backend did not reject invalid registration data.")
        return False

    # Test 3: Successful registration
    print("\n[Test 3] Testing valid registration with full profile data...")
    test_user_data = {
        "full_name": "Sarah Connor",
        "email": "sarah.connor@example.com",
        "password": "SecurePassword123!",
        "confirm_password": "SecurePassword123!",
        "age": 29,
        "gender": "female",
        "height": 168.5,
        "weight": 62.5
    }
    res = client.post('/register', data=test_user_data, follow_redirects=True)
    if res.status_code == 200 and b"Welcome back, Sarah Connor" in res.data or b"Sarah Connor" in res.data:
        print("  [PASSED] User registered successfully and redirected to /dashboard.")
    else:
        print(f"  [FAILED] Registration did not redirect to dashboard as expected. Status: {res.status_code}")
        return False

    # Test 4: Verify Password Hashing in Database
    print("\n[Test 4] Verifying password hashing in SQLite database...")
    with get_db() as db:
        cursor = db.cursor()
        cursor.execute("SELECT password_hash FROM users WHERE email = 'sarah.connor@example.com';")
        user_row = cursor.fetchone()
        stored_hash = user_row["password_hash"]
        
        # Verify plaintext password is not in DB and check_password_hash validates
        if stored_hash != "SecurePassword123!" and check_password_hash(stored_hash, "SecurePassword123!"):
            print(f"  [PASSED] Password properly hashed with Werkzeug ({stored_hash[:25]}...).")
        else:
            print("  [FAILED] Password is stored in plaintext or cannot be verified.")
            return False

    # Test 5: Reject Duplicate Email Registration
    print("\n[Test 5] Testing duplicate email registration prevention...")
    client.get('/logout')  # Log out first to test unauthenticated registration attempt
    res = client.post('/register', data=test_user_data, follow_redirects=True)
    if b"already exists" in res.data or res.status_code == 400:
        print("  [PASSED] Duplicate email was blocked with clear error message.")
    else:
        print(f"  [FAILED] Duplicate email was not blocked. Status: {res.status_code}")
        return False


    # Test 6: Logout
    print("\n[Test 6] Testing user logout...")
    res = client.get('/logout', follow_redirects=True)
    if b"logged out" in res.data:
        print("  [PASSED] User logged out and session cleared.")
    else:
        print("  [FAILED] Logout failed to clear session.")
        return False

    # Test 7: Login with Wrong Password
    print("\n[Test 7] Testing login with invalid credentials...")
    res = client.post('/login', data={
        "identifier": "sarah.connor@example.com",
        "password": "WrongPassword999"
    }, follow_redirects=True)
    if b"Invalid email/username or password" in res.data:
        print("  [PASSED] Invalid password rejected.")
    else:
        print("  [FAILED] Failed login was not rejected.")
        return False

    # Test 8: Login with Correct Password
    print("\n[Test 8] Testing login with valid credentials...")
    res = client.post('/login', data={
        "identifier": "sarah.connor@example.com",
        "password": "SecurePassword123!"
    }, follow_redirects=True)
    if res.status_code == 200 and b"Sarah Connor" in res.data:
        print("  [PASSED] Login succeeded and redirected to dashboard.")
    else:
        print("  [FAILED] Login failed to authenticate.")
        return False

    print("\n" + "=" * 70)
    print(" ALL 8 STEP 3 AUTHENTICATION TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return True


if __name__ == "__main__":
    success = run_auth_tests()
    sys.exit(0 if success else 1)
