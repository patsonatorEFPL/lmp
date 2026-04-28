import subprocess
import sys

try:
    from playwright.sync_api import sync_playwright
except ImportError:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "playwright", "-q"])
    from playwright.sync_api import sync_playwright

import time

BASE_URL = "http://localhost:4200"
ADMIN_EMAIL = "admin@lmp.ca"
ADMIN_PASSWORD = "admin123"

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False, args=["--window-size=1400,900"])
        context = browser.new_context(viewport={"width": 1400, "height": 900})
        page = context.new_page()

        print("[E2E] Navigating to login...")
        page.goto(f"{BASE_URL}/login")
        page.wait_for_timeout(1000)

        # Login
        print("[E2E] Logging in...")
        page.fill('input[type="email"]', ADMIN_EMAIL)
        page.fill('input[type="password"]', ADMIN_PASSWORD)
        page.click('button[type="submit"]')
        page.wait_for_timeout(2500)

        # Screenshot after login
        page.screenshot(path="audit-screenshots/e2e-admin-dashboard.png", full_page=True)
        print("[E2E] Screenshot: audit-screenshots/e2e-admin-dashboard.png")

        # Check dashboard content
        html = page.content()
        checks = {
            "no_mock_spark": "spark" not in html.lower() or "[object Object]" not in html,
            "no_mock_revenue_12840": "12 840" not in html,
            "no_mock_monitoring": "API principale" not in html,
        }
        print("[E2E] Dashboard checks:", checks)

        # Navigate to admin dashboard
        print("[E2E] Navigating to admin dashboard...")
        page.goto(f"{BASE_URL}/admin")
        page.wait_for_timeout(2500)
        page.screenshot(path="audit-screenshots/e2e-admin-dashboard.png", full_page=True)
        print("[E2E] Screenshot: audit-screenshots/e2e-admin-dashboard.png")

        # Navigate to user dashboard
        print("[E2E] Navigating to user dashboard...")
        page.goto(f"{BASE_URL}/dashboard")
        page.wait_for_timeout(2500)
        page.screenshot(path="audit-screenshots/e2e-user-dashboard.png", full_page=True)
        print("[E2E] Screenshot: audit-screenshots/e2e-user-dashboard.png")

        # Check user dashboard
        html_user = page.content()
        checks_user = {
            "no_mock_insight_23": "23 %" not in html_user,
            "no_mock_activity_series": "Activité de vos commandes" in html_user,
        }
        print("[E2E] User dashboard checks:", checks_user)

        context.close()
        browser.close()
        print("[E2E] Done.")

if __name__ == "__main__":
    main()
