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
    page.wait_for_timeout(500)

    initial = page.evaluate("""() => {
      const t = document.querySelector('.scroll-thumb');
      return {
        exists: !!t,
        cls: t?.className,
        opacity: t ? getComputedStyle(t).opacity : null,
        nativeHidden: document.documentElement.classList.contains('has-custom-scrollbar'),
        scrollbarWidth: window.innerWidth - document.documentElement.clientWidth
      };
    }""")

    page.mouse.move(700, 500)
    page.mouse.wheel(0, 1200)
    page.wait_for_timeout(450)
    after_wheel = page.evaluate("""() => {
      const t = document.querySelector('.scroll-thumb');
      return { cls: t.className, opacity: getComputedStyle(t).opacity, transform: getComputedStyle(t).transform, h: t.style.height, scrollY: window.scrollY };
    }""")

    page.wait_for_timeout(1200)
    after_idle = page.evaluate("""() => {
      const t = document.querySelector('.scroll-thumb');
      return { cls: t.className, opacity: getComputedStyle(t).opacity };
    }""")

    page.mouse.move(1430, 500)
    page.wait_for_timeout(320)
    edge_show = page.evaluate("""() => {
      const t = document.querySelector('.scroll-thumb');
      return { cls: t.className, opacity: getComputedStyle(t).opacity };
    }""")

    page.mouse.move(700, 500)
    page.wait_for_timeout(360)
    edge_hide = page.evaluate("""() => {
      const t = document.querySelector('.scroll-thumb');
      return { cls: t.className, opacity: getComputedStyle(t).opacity };
    }""")

    # Drag the thumb down.
    page.evaluate("window.scrollTo(0,0)")
    page.wait_for_timeout(300)
    page.mouse.move(1430, 500)
    page.wait_for_timeout(250)
    box = page.evaluate("""() => {
      const r = document.querySelector('.scroll-thumb').getBoundingClientRect();
      return { x: r.x, y: r.y, w: r.width, h: r.height };
    }""")
    start_scroll = page.evaluate("window.scrollY")
    page.mouse.move(box["x"] + box["w"] / 2, box["y"] + box["h"] / 2)
    page.mouse.down()
    page.mouse.move(box["x"] + box["w"] / 2, box["y"] + box["h"] / 2 + 220, steps=12)
    page.mouse.up()
    page.wait_for_timeout(250)
    drag = page.evaluate("({ scrollY: window.scrollY, cls: document.querySelector('.scroll-thumb').className })")

    page.evaluate("window.getSelection().removeAllRanges()"); page.screenshot(path="website/shots/scrollbar.png")

    # Mobile fallback
    mobile = browser.new_page(viewport={"width": 390, "height": 844}, is_mobile=True, has_touch=True)
    mobile.goto(URL, wait_until="networkidle")
    mobile.wait_for_timeout(400)
    mob = mobile.evaluate("""() => ({
      thumb: !!document.querySelector('.scroll-thumb'),
      custom: document.documentElement.classList.contains('has-custom-scrollbar'),
      sw: document.documentElement.scrollWidth,
      cw: document.documentElement.clientWidth
    })""")

    # Reduced motion
    rm = browser.new_page(viewport={"width": 1440, "height": 1000})
    rm.emulate_media(reduced_motion="reduce")
    rm.goto(URL, wait_until="networkidle")
    rm.mouse.move(700, 500)
    rm.mouse.wheel(0, 900)
    rm.wait_for_timeout(200)
    red = rm.evaluate("""() => {
      const t = document.querySelector('.scroll-thumb');
      return { cls: t.className, opacity: getComputedStyle(t).opacity, transform: getComputedStyle(t).transform };
    }""")

    print(json.dumps({
        "initial": initial,
        "after_wheel": after_wheel,
        "after_idle": after_idle,
        "edge_show": edge_show,
        "edge_hide": edge_hide,
        "drag_start_scroll": start_scroll,
        "drag": drag,
        "mobile": mob,
        "reduced": red,
        "console": errors[:5]
    }, indent=2))
    browser.close()
