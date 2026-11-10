from playwright.sync_api import sync_playwright
import json

with sync_playwright() as p:
    browser = p.chromium.launch(channel="msedge", headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 1000})
    page.goto("http://localhost:8080", wait_until="networkidle")
    page.evaluate("document.fonts.ready")

    data = page.evaluate("""() => {
      const info = (sel) => {
        const el = document.querySelector(sel);
        if (!el) return null;
        const cs = getComputedStyle(el);
        const r = el.getBoundingClientRect();
        return {
          sel,
          class: el.className,
          clipPath: cs.clipPath,
          opacity: cs.opacity,
          display: cs.display,
          visibility: cs.visibility,
          height: r.height,
          top: r.top + window.scrollY,
          childCount: el.children.length,
          text: (el.textContent || '').trim().slice(0, 80)
        };
      };
      return {
        notes: info('.fieldnotes'),
        notesImg: info('.fieldnotes > img'),
        notesCopy: info('.fieldnotes__copy'),
        cta: info('.cta__media'),
        ctaImg: info('.cta__media img'),
        offline: info('#offline'),
        privacy: info('#privacy'),
        docHeight: document.body.scrollHeight
      };
    }""")
    print(json.dumps(data, indent=2))

    page.evaluate("document.querySelector('#notes').scrollIntoView()")
    page.wait_for_timeout(1800)
    data2 = page.evaluate("""() => {
      const el = document.querySelector('.fieldnotes');
      const cs = getComputedStyle(el);
      return { class: el.className, clipPath: cs.clipPath, rect: el.getBoundingClientRect().toJSON() };
    }""")
    print("after scroll:", json.dumps(data2, indent=2))
    page.screenshot(path="website/shots/notes-section.png")
    browser.close()
