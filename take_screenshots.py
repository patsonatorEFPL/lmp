from playwright.sync_api import sync_playwright
import os

output_dir = 'preview_screenshots'
os.makedirs(output_dir, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    
    # Light mode screenshots
    for page_name, url in [
        ('cancel_light', 'http://localhost:8080/payment/cancel'),
        ('success_light', 'http://localhost:8080/payment/success'),
    ]:
        page = browser.new_page()
        page.set_viewport_size({'width': 1280, 'height': 900})
        try:
            page.goto(url, wait_until='networkidle', timeout=30000)
            page.wait_for_timeout(2000)
            screenshot_path = f'{output_dir}/{page_name}.png'
            page.screenshot(path=screenshot_path, full_page=True)
            print(f'Saved: {screenshot_path}')
        except Exception as e:
            print(f'Error capturing {page_name}: {e}')
        page.close()
    
    # Dark mode screenshots
    for page_name, url in [
        ('cancel_dark', 'http://localhost:8080/payment/cancel'),
        ('success_dark', 'http://localhost:8080/payment/success'),
    ]:
        page = browser.new_page()
        page.set_viewport_size({'width': 1280, 'height': 900})
        try:
            page.goto(url, wait_until='networkidle', timeout=30000)
            page.evaluate("document.documentElement.classList.add('dark')")
            page.wait_for_timeout(2000)
            screenshot_path = f'{output_dir}/{page_name}.png'
            page.screenshot(path=screenshot_path, full_page=True)
            print(f'Saved: {screenshot_path}')
        except Exception as e:
            print(f'Error capturing {page_name}: {e}')
        page.close()
    
    browser.close()
    print(f'Screenshots saved in: {output_dir}/')
import os

output_dir = 'preview_screenshots'
os.makedirs(output_dir, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    
    # Light mode screenshots
    for page_name, url in [
        ('cancel_light', 'http://localhost:8080/payment/cancel'),
        ('success_light', 'http://localhost:8080/payment/success'),
    ]:
        page = browser.new_page()
        page.set_viewport_size({'width': 1280, 'height': 900})
        try:
            page.goto(url, wait_until='networkidle', timeout=30000)
            page.wait_for_timeout(2000)
            screenshot_path = f'{output_dir}/{page_name}.png'
            page.screenshot(path=screenshot_path, full_page=True)
            print(f'Saved: {screenshot_path}')
        except Exception as e:
            print(f'Error capturing {page_name}: {e}')
        page.close()
    
    # Dark mode screenshots
    for page_name, url in [
        ('cancel_dark', 'http://localhost:8080/payment/cancel'),
        ('success_dark', 'http://localhost:8080/payment/success'),
    ]:
        page = browser.new_page()
        page.set_viewport_size({'width': 1280, 'height': 900})
        try:
            page.goto(url, wait_until='networkidle', timeout=30000)
            page.evaluate("document.documentElement.classList.add('dark')")
            page.wait_for_timeout(2000)
            screenshot_path = f'{output_dir}/{page_name}.png'
            page.screenshot(path=screenshot_path, full_page=True)
            print(f'Saved: {screenshot_path}')
        except Exception as e:
            print(f'Error capturing {page_name}: {e}')
        page.close()
    
    browser.close()
    print(f'Screenshots saved in: {output_dir}/')

output_dir = 'preview_screenshots'
os.makedirs(output_dir, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    
    # Light mode screenshots
    for page_name, url in [
        ('cancel_light', 'http://localhost:8080/payment/cancel'),
        ('success_light', 'http://localhost:8080/payment/success'),
    ]:
        page = browser.new_page()
        page.set_viewport_size({'width': 1280, 'height': 900})
        try:
            page.goto(url, wait_until='networkidle', timeout=30000)
            page.wait_for_timeout(2000)
            screenshot_path = f'{output_dir}/{page_name}.png'
            page.screenshot(path=screenshot_path, full_page=True)
            print(f'Saved: {screenshot_path}')
        except Exception as e:
            print(f'Error capturing {page_name}: {e}')
        page.close()
    
    # Dark mode screenshots
    for page_name, url in [
        ('cancel_dark', 'http://localhost:8080/payment/cancel'),
        ('success_dark', 'http://localhost:8080/payment/success'),
    ]:
        page = browser.new_page()
        page.set_viewport_size({'width': 1280, 'height': 900})
        try:
            page.goto(url, wait_until='networkidle', timeout=30000)
            page.evaluate("document.documentElement.classList.add('dark')")
            page.wait_for_timeout(2000)
            screenshot_path = f'{output_dir}/{page_name}.png'
            page.screenshot(path=screenshot_path, full_page=True)
            print(f'Saved: {screenshot_path}')
        except Exception as e:
            print(f'Error capturing {page_name}: {e}')
        page.close()
    
    browser.close()
    print(f'Screenshots saved in: {output_dir}/')
