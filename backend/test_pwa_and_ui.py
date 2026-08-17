"""
HealthTrack AI - PWA & UI/UX Test Suite
Verifies:
1. PWA Web App Manifest (/manifest.json) accessibility and structure.
2. Service Worker (/sw.js and /service-worker.js) headers and content.
3. High-resolution PNG App Icons (192x192, 512x512, maskable) accessibility.
4. Mobile & PWA meta tags in HTML templates.
5. All application pages rendering and navigation integrity.
"""

import sys
import json
from app import app
from database import init_db, get_db

def run_pwa_and_ui_tests():
    print("=" * 70)
    print("      HEALTHTRACK AI - PWA & UI/UX INTEGRATION TEST SUITE")
    print("=" * 70)

    init_db(reset=True)
    app.config["TESTING"] = True
    client = app.test_client()

    # 1. Test /manifest.json endpoint
    print("\n[Test 1] Testing PWA Web App Manifest (/manifest.json)...")
    res_manifest = client.get('/manifest.json')
    assert res_manifest.status_code == 200
    assert 'manifest' in res_manifest.content_type
    manifest_json = json.loads(res_manifest.data.decode('utf-8'))
    assert manifest_json["name"] == "HealthTrack AI - Smart Health & Fitness Companion"
    assert manifest_json["short_name"] == "HealthTrack AI"
    assert manifest_json["display"] == "standalone"
    assert manifest_json["theme_color"] == "#10b981"
    assert len(manifest_json["icons"]) >= 2
    assert len(manifest_json["shortcuts"]) >= 4
    print("  [PASSED] Manifest configuration verified.")

    # 2. Test /sw.js & /service-worker.js endpoints
    print("\n[Test 2] Testing Service Worker endpoints (/sw.js & /service-worker.js)...")
    res_sw1 = client.get('/sw.js')
    assert res_sw1.status_code == 200
    assert 'javascript' in res_sw1.content_type
    assert res_sw1.headers.get('Service-Worker-Allowed') == '/'
    assert b'healthtrack-ai' in res_sw1.data

    res_sw2 = client.get('/service-worker.js')
    assert res_sw2.status_code == 200
    assert 'javascript' in res_sw2.content_type
    print("  [PASSED] Service worker endpoints verified with full domain scope.")

    # 3. Test App Icons static accessibility
    print("\n[Test 3] Testing PWA App Icons (/static/images/icon-*.png)...")
    for icon_name in ['icon-192.png', 'icon-512.png', 'icon-maskable-192.png', 'icon-maskable-512.png']:
        res_icon = client.get(f'/static/images/{icon_name}')
        assert res_icon.status_code == 200
        assert res_icon.content_type == 'image/png'
    print("  [PASSED] 192x192 and 512x512 standard & maskable PNG icons verified.")

    # 4. Register test user and test all authenticated views
    print("\n[Test 4] Testing User Authentication & Full Application Route Rendering...")
    reg = client.post('/register', data={
        "full_name": "Elena Rostova",
        "email": "elena@example.com",
        "password": "Password123!",
        "confirm_password": "Password123!",
        "age": 27,
        "gender": "female",
        "height": 168.0,
        "weight": 56.5
    }, follow_redirects=True)
    assert reg.status_code == 200

    pages_to_test = [
        ('/', b'HealthTrack'),
        ('/dashboard', b'Personal Health Dashboard'),
        ('/steps', b'Step Tracking'),
        ('/water', b'Water Tracking'),
        ('/sleep', b'Sleep Tracking'),
        ('/weight', b'Weight &amp; BMI Tracking'),
        ('/heart', b'Heart Rate Tracking'),
        ('/goals', b'Goals &amp; Achievements'),
        ('/insights', b'AI Wellness Insights')
    ]

    for path, expected_text in pages_to_test:
        res = client.get(path)
        assert res.status_code == 200, f"Failed on path {path} with status {res.status_code}"
        assert expected_text in res.data, f"Expected text missing on path {path}"
        assert b'rel="manifest"' in res.data, f"Manifest link missing on path {path}"
        assert b'theme-color' in res.data, f"Theme color meta tag missing on path {path}"
        assert b'pwa-install-banner' in res.data or b'btn-pwa-install' in res.data, f"Install prompt missing on path {path}"
        print(f"  [PASSED] Route {path:12} rendered with PWA capabilities & UI elements.")

    print("\n" + "=" * 70)
    print(" ALL PWA & UI/UX TESTS PASSED SUCCESSFULLY!")
    print("=" * 70)
    return True

if __name__ == '__main__':
    success = run_pwa_and_ui_tests()
    sys.exit(0 if success else 1)
