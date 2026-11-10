"""Read-only layout audit: overlaps, overflow, loaded images, reveal state."""

from __future__ import annotations

import json

from playwright.sync_api import sync_playwright

AUDIT = """
() => {
  const box = (sel) => {
    const el = document.querySelector(sel);
    if (!el) return null;
    const r = el.getBoundingClientRect();
    return { sel, x: +r.x.toFixed(1), y: +(r.y + scrollY).toFixed(1), w: +r.width.toFixed(1), h: +r.height.toFixed(1), right: +(r.right).toFixed(1), bottom: +(r.bottom + scrollY).toFixed(1) };
  };
  const intersect = (a, b) => a && b && a.x < b.right && a.right > b.x && a.y < b.bottom && a.bottom > b.y;
  const pairs = [
    ['.hero__devices .phone:nth-child(1)', '.stats'],
    ['.hero__devices .phone:nth-child(2)', '.stats'],
    ['.hero__devices .phone:nth-child(1)', '.hero__banner-copy'],
    ['.hero__micro', '.hero__banner'],
    ['.stage__phone', '.feature__copy'],
    ['.float-card--a', '.feature__copy'],
    ['.float-card--b', '.feature__copy'],
    ['.float-card--c', '.feature__copy'],
    ['.notif-card', '.companion__media .phone'],
    ['.fieldnotes__copy', '.story-stack'],
  ];
  const overlaps = [];
  for (const [a, b] of pairs) {
    const ba = box(a), bb = box(b);
    if (intersect(ba, bb)) overlaps.push({ a, b, ba, bb });
  }
  const overflows = [];
  document.querySelectorAll('.phone, .stage__phone, .hero__devices, .float-card, .notif-card').forEach((el) => {
    const r = el.getBoundingClientRect();
    if (r.left < -1 || r.right > innerWidth + 1) {
      overflows.push({ cls: el.className, left: +r.left.toFixed(1), right: +r.right.toFixed(1) });
    }
  });
  const sections = [...document.querySelectorAll('main > section')].map((el) => ({ id: el.id || null, cls: el.className, h: +el.getBoundingClientRect().height.toFixed(1) }));
  const imgs = [...document.images].map((img) => ({ src: img.getAttribute('src'), ok: img.complete && img.naturalWidth > 0, w: img.naturalWidth }));
  const revealImg = [...document.querySelectorAll('.reveal-img')].map((el) => ({ cls: el.className, visible: el.classList.contains('is-visible'), clip: getComputedStyle(el).clipPath }));
  return {
    viewport: { w: innerWidth, h: innerHeight },
    doc: { scrollWidth: document.documentElement.scrollWidth, clientWidth: document.documentElement.clientWidth, scrollHeight: document.body.scrollHeight },
    sections,
    overlaps,
    overflows,
    brokenImages: imgs.filter((i) => !i.ok),
    revealImg,
    fonts: { display: getComputedStyle(document.querySelector('.hero__title')).fontFamily, serif: getComputedStyle(document.querySelector('.hero__title em')).fontFamily, body: getComputedStyle(document.body).fontFamily },
  };
}
"""

with sync_playwright() as p:
    browser = p.chromium.launch(channel="msedge", headless=True)
    for name, width, height, mobile in [("desktop", 1440, 1000, False), ("mobile", 390, 844, True)]:
        page = browser.new_page(viewport={"width": width, "height": height}, is_mobile=mobile, has_touch=mobile)
        page.goto("http://localhost:8080", wait_until="networkidle")
        page.evaluate("document.fonts.ready")
        # trigger reveals the way a human would
        total = page.evaluate("document.body.scrollHeight")
        for y in range(0, total, 420):
            page.evaluate(f"window.scrollTo(0, {y})")
            page.wait_for_timeout(120)
        page.evaluate("window.scrollTo(0, 0)")
        page.wait_for_timeout(600)
        result = page.evaluate(AUDIT)
        print(f"\n=== {name} ===")
        print(json.dumps(result, indent=2))
        page.close()
    browser.close()
