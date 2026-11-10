from playwright.sync_api import sync_playwright

URL = "http://localhost:8080"


def overlap(a, b):
    left = max(a["x"], b["x"])
    top = max(a["y"], b["y"])
    right = min(a["x"] + a["w"], b["x"] + b["w"])
    bottom = min(a["y"] + a["h"], b["y"] + b["h"])
    if right <= left or bottom <= top:
        return 0.0
    return (right - left) * (bottom - top)


with sync_playwright() as p:
    browser = p.chromium.launch(channel="msedge", headless=True)

    for name, w, h, mobile in [("1180", 1180, 900, False), ("390", 390, 844, True), ("320", 320, 740, True), ("1440", 1440, 1000, False)]:
        page = browser.new_page(viewport={"width": w, "height": h}, is_mobile=mobile, has_touch=mobile)
        page.goto(URL, wait_until="networkidle")
        page.evaluate("document.fonts.ready")
        page.evaluate("document.querySelector('#how').scrollIntoView({block:'center', behavior:'instant'})")
        page.wait_for_timeout(1200)
        data = page.evaluate("""() => {
          const box = (sel) => {
            const el = document.querySelector(sel);
            if (!el) return null;
            const r = el.getBoundingClientRect();
            return { x: r.x, y: r.y, w: r.width, h: r.height };
          };
          const title = document.querySelector('.float-card--a b');
          const range = document.createRange();
          range.selectNodeContents(title);
          const rects = [...range.getClientRects()].map(r => ({ x: +r.x.toFixed(1), y: +r.y.toFixed(1), w: +r.width.toFixed(1), h: +r.height.toFixed(1) }));
          return {
            sw: document.documentElement.scrollWidth,
            cw: document.documentElement.clientWidth,
            cardC: box('.float-card--c'),
            previewHead: box('.app-preview__head'),
            previewTitle: box('.app-preview h4'),
            previewActions: box('.app-preview__actions'),
            cardTitleRects: rects,
            cardTitleText: title.textContent,
            cardWidth: box('.float-card--a')?.w,
          };
        }""")
        for key in ("previewHead", "previewTitle", "previewActions"):
            if data["cardC"] and data[key]:
                a = data["cardC"]; b = data[key]
                area = max(b["w"] * b["h"], 1)
                data[key + "OverlapPct"] = round(overlap(a, b) / area * 100, 1)
        print(name, data)
        page.locator("#how").screenshot(path=f"website/shots/card-{name}.png")
        page.close()
    browser.close()
