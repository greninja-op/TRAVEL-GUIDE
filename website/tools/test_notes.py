from playwright.sync_api import sync_playwright
import json

URL = "http://localhost:8080"

with sync_playwright() as p:
    browser = p.chromium.launch(channel="msedge", headless=True)

    for mode in ("anchor", "scroll"):
        page = browser.new_page(viewport={"width": 1440, "height": 1000})
        page.goto(URL + ("#notes" if mode == "anchor" else ""), wait_until="networkidle")
        page.evaluate("document.fonts.ready")
        if mode == "scroll":
            page.evaluate("document.querySelector('#notes').scrollIntoView({block:'center'})")
        page.wait_for_timeout(2200)
        state = page.evaluate("""() => {
          const get = (sel) => {
            const el = document.querySelector(sel);
            if (!el) return null;
            const cs = getComputedStyle(el);
            const r = el.getBoundingClientRect();
            return { cls: el.className, opacity: cs.opacity, color: cs.color, transform: cs.transform, clip: cs.clipPath, y: +r.y.toFixed(1), h: +r.height.toFixed(1) };
          };
          return {
            fieldnotes: get('.fieldnotes'),
            copy: get('.fieldnotes__copy'),
            title: get('.fieldnotes__copy .section-title'),
            cards: [...document.querySelectorAll('.story-card')].map((el) => ({ cls: el.className, opacity: getComputedStyle(el).opacity, y: +el.getBoundingClientRect().y.toFixed(1) })),
            inner: get('.fieldnotes__inner'),
            stack: get('.story-stack'),
            visibleReveals: document.querySelectorAll('.reveal.is-visible').length,
            totalReveals: document.querySelectorAll('.reveal').length
          };
        }""")
        print(f"=== {mode} ===")
        print(json.dumps(state, indent=2))
        page.screenshot(path=f"website/shots/notes-{mode}.png")
        page.close()
    browser.close()
