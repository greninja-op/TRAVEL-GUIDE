from playwright.sync_api import sync_playwright
import json

URL = "http://localhost:8080"

with sync_playwright() as p:
    browser = p.chromium.launch(channel="msedge", headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 1000})
    errors = []
    page.on("console", lambda m: errors.append(m.text) if m.type == "error" else None)
    page.goto(URL, wait_until="networkidle")
    page.evaluate("document.fonts.ready")

    pill_before = page.evaluate("""() => {
      const b = document.querySelector('#audioPill');
      return { exists: !!b, pressed: b?.getAttribute('aria-pressed'), cls: b?.className, label: b?.innerText };
    }""")
    page.click("#audioPill")
    page.wait_for_timeout(250)
    pill_after = page.evaluate("""() => {
      const b = document.querySelector('#audioPill');
      return { pressed: b?.getAttribute('aria-pressed'), cls: b?.className, bars: getComputedStyle(document.querySelector('.audio-pill .wave-bar') || document.querySelector('.audio-pill span')).animationName };
    }""")

    # Scroll feature 1 into view and sample dot position twice
    page.evaluate("document.querySelector('#how').scrollIntoView({block:'center'})")
    page.wait_for_timeout(500)
    t1 = page.evaluate("""() => {
      const d = document.querySelector('.trail-dot');
      return d ? { cx: d.getAttribute('cx'), cy: d.getAttribute('cy'), hits: document.querySelectorAll('.float-card.is-hit').length } : null;
    }""")
    page.evaluate("window.scrollBy(0, 260)")
    page.wait_for_timeout(500)
    t2 = page.evaluate("""() => {
      const d = document.querySelector('.trail-dot');
      return d ? { cx: d.getAttribute('cx'), cy: d.getAttribute('cy'), hits: document.querySelectorAll('.float-card.is-hit').length, nodes: document.querySelectorAll('.trail-node.is-hit').length } : null;
    }""")

    # Overflow at 390
    page.set_viewport_size({"width": 390, "height": 844})
    page.wait_for_timeout(300)
    mobile = page.evaluate("""() => ({ sw: document.documentElement.scrollWidth, cw: document.documentElement.clientWidth, iw: innerWidth, pillW: document.querySelector('#audioPill')?.getBoundingClientRect().width })""")

    print(json.dumps({"pill_before": pill_before, "pill_after": pill_after, "trail_1": t1, "trail_2": t2, "mobile": mobile, "console": errors[:5]}, indent=2))
    browser.close()
