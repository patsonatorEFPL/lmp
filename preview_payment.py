from playwright.sync_api import sync_playwright
import os

# Create output directory
output_dir = "preview_screenshots"
os.makedirs(output_dir, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    
    # Take screenshots of payment pages
    for page_name, url in [
        ("cancel_light", "http://localhost:8080/payment/cancel"),
        ("success_light", "http://localhost:8080/payment/success"),
    ]:
        page = browser.new_page()
        page.set_viewport_size({"width": 1280, "height": 900})
        
        try:
            page.goto(url, wait_until="networkidle", timeout=30000)
            page.wait_for_timeout(2000)  # Wait for animations
            screenshot_path = f"{output_dir}/{page_name}.png"
            page.screenshot(path=screenshot_path, full_page=True)
            print(f"Saved: {screenshot_path}")
        except Exception as e:
            print(f"Error capturing {page_name}: {e}")
            # Take screenshot of error state
            page.screenshot(path=f"{output_dir}/{page_name}_error.png")
        
        page.close()
    
    # Take screenshots in dark mode
    for page_name, url in [
        ("cancel_dark", "http://localhost:8080/payment/cancel"),
        ("success_dark", "http://localhost:8080/payment/success"),
    ]:
        page = browser.new_page()
        page.set_viewport_size({"width": 1280, "height": 900})
        
        try:
            page.goto(url, wait_until="networkidle", timeout=30000)
            # Force dark mode via JavaScript
            page.evaluate("document.documentElement.classList.add('dark')")
            page.wait_for_timeout(2000)  # Wait for theme change
            screenshot_path = f"{output_dir}/{page_name}.png"
            page.screenshot(path=screenshot_path, full_page=True)
            print(f"Saved: {screenshot_path}")
        except Exception as e:
            print(f"Error capturing {page_name}: {e}")
            page.screenshot(path=f"{output_dir}/{page_name}_error.png")
        
        page.close()
    
    browser.close()
    print(f"\nScreenshots saved in: {output_dir}/")
import os

# Create output directory
output_dir = "preview_screenshots"
os.makedirs(output_dir, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    
    # Take screenshots of payment pages
    for page_name, url in [
        ("cancel_light", "http://localhost:8080/payment/cancel"),
        ("success_light", "http://localhost:8080/payment/success"),
    ]:
        page = browser.new_page()
        page.set_viewport_size({"width": 1280, "height": 900})
        
        try:
            page.goto(url, wait_until="networkidle", timeout=30000)
            page.wait_for_timeout(2000)  # Wait for animations
            screenshot_path = f"{output_dir}/{page_name}.png"
            page.screenshot(path=screenshot_path, full_page=True)
            print(f"Saved: {screenshot_path}")
        except Exception as e:
            print(f"Error capturing {page_name}: {e}")
            # Take screenshot of error state
            page.screenshot(path=f"{output_dir}/{page_name}_error.png")
        
        page.close()
    
    # Take screenshots in dark mode
    for page_name, url in [
        ("cancel_dark", "http://localhost:8080/payment/cancel"),
        ("success_dark", "http://localhost:8080/payment/success"),
    ]:
        page = browser.new_page()
        page.set_viewport_size({"width": 1280, "height": 900})
        
        try:
            page.goto(url, wait_until="networkidle", timeout=30000)
            # Force dark mode via JavaScript
            page.evaluate("document.documentElement.classList.add('dark')")
            page.wait_for_timeout(2000)  # Wait for theme change
            screenshot_path = f"{output_dir}/{page_name}.png"
            page.screenshot(path=screenshot_path, full_page=True)
            print(f"Saved: {screenshot_path}")
        except Exception as e:
            print(f"Error capturing {page_name}: {e}")
            page.screenshot(path=f"{output_dir}/{page_name}_error.png")
        
        page.close()
    
    browser.close()
    print(f"\nScreenshots saved in: {output_dir}/")
    
    browser.close()
    print(f"\nScreenshots saved in: {output_dir}/")import os

# Create output directory
output_dir = "preview_screenshots"
os.makedirs(output_dir, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    
    # Take screenshots of payment pages
    for page_name, url in [
        ("cancel_light", "http://localhost:8080/payment/cancel"),
        ("success_light", "http://localhost:8080/payment/success"),
    ]:
        page = browser.new_page()
        page.set_viewport_size({"width": 1280, "height": 900})
        
        try:
            page.goto(url, wait_until="networkidle", timeout=30000)
            page.wait_for_timeout(2000)  # Wait for animations
            screenshot_path = f"{output_dir}/{page_name}.png"
            page.screenshot(path=screenshot_path, full_page=True)
            print(f"Saved: {screenshot_path}")
        except Exception as e:
            print(f"Error capturing {page_name}: {e}")
            # Take screenshot of error state
            page.screenshot(path=f"{output_dir}/{page_name}_error.png")
        
        page.close()
    
    # Take screenshots in dark mode
    for page_name, url in [
        ("cancel_dark", "http://localhost:8080/payment/cancel"),
        ("success_dark", "http://localhost:8080/payment/success"),
    ]:
        page = browser.new_page()
        page.set_viewport_size({"width": 1280, "height": 900})
        
        # Set dark mode by adding class to html
        page.add_style_tag(content=":root { color-scheme: dark; }")
        
        try:
            page.goto(url, wait_until="networkidle", timeout=30000)
            # Force dark mode via JavaScript
            page.evaluate("document.documentElement.classList.add('dark')")
            page.wait_for_timeout(2000)  # Wait for theme change
            screenshot_path = f"{output_dir}/{page_name}.png"
            page.screenshot(path=screenshot_path, full_page=True)
            print(f"Saved: {screenshot_path}")
        except Exception as e:
            print(f"Error capturing {page_name}: {e}")
            page.screenshot(path=f"{output_dir}/{page_name}_error.png")
        
        page.close()
    
    browser.close()
    print(f"\nScreenshots saved in: {output_dir}/")
