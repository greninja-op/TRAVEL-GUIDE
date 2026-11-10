from playwright.sync_api import sync_playwright

URL = "http://localhost:8080"

with sync_playwright() as p:
    browser = p.chromium.launch(channel="msedge", headless=True)

    page = browser.new_page(viewport={"width": 1440, "height": 1000})
    page.goto(URL, wait_until="networkidle")
    page.evaluate("document.fonts.ready")
    page.evaluate("document.querySelector('#how').scrollIntoView({block:'center'})")
    page.wait_for_timeout(900)
    page.locator("#how").screenshot(path="website/shots/section-how.png")

    page.evaluate("document.querySelector('#companion').scrollIntoView({block:'center'})")
    page.wait_for_timeout(900)
    page.locator("#companion").screenshot(path="website/shots/section-companion.png")

    page.evaluate("document.querySelector('#notes').scrollIntoView({block:'center'})")
    page.wait_for_timeout(900)
    page.locator("#notes").screenshot(path="website/shots/section-notes.png")

    mobile = browser.new_page(viewport={"width": 390, "height": 844}, is_mobile=True, has_touch=True)
    mobile.goto(URL, wait_until="networkidle")
    mobile.evaluate("document.fonts.ready")
    mobile.wait_for_timeout(600)
    mobile.screenshot(path="website/shots/mobile-hero-new.png")
    mobile.evaluate("document.querySelector('#how').scrollIntoView({block:'center'})")
    mobile.wait_for_timeout(900)
    mobile.locator("#how").screenshot(path="website/shots/mobile-how.png")

    browser.close()
