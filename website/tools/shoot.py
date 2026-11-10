"""Capture desktop/mobile screenshots and surface console/network errors."""

from __future__ import annotations

import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "shots"
OUT.mkdir(exist_ok=True)
URL = "http://localhost:8080"

VIEWPORTS = [
    ("desktop", 1440, 1000, False),
    ("laptop", 1180, 900, False),
    ("mobile", 390, 844, True),
]

with sync_playwright() as p:
    browser = p.chromium.launch(channel="msedge", headless=True)
    for name, width, height, mobile in VIEWPORTS:
        page = browser.new_page(
            viewport={"width": width, "height": height},
            device_scale_factor=1,
            is_mobile=mobile,
            has_touch=mobile,
        )
        errors: list[str] = []
        failed: list[str] = []
        page.on("console", lambda msg: errors.append(f"{msg.type}: {msg.text}") if msg.type == "error" else None)
        page.on("requestfailed", lambda req: failed.append(f"{req.url} :: {req.failure}"))
        page.goto(URL, wait_until="networkidle")
        page.evaluate("document.fonts.ready")

        # Walk the page so every IntersectionObserver reveal fires, then return.
        page.evaluate("window.scrollTo(0, 0)")
        total = page.evaluate("document.body.scrollHeight")
        for y in range(0, total, max(400, height // 2)):
            page.evaluate(f"window.scrollTo(0, {y})")
            page.wait_for_timeout(80)
        page.evaluate("window.scrollTo(0, 0)")
        page.wait_for_timeout(800)

        page.screenshot(path=str(OUT / f"{name}-hero.png"))
        page.screenshot(path=str(OUT / f"{name}-full.png"), full_page=True)
        print(f"{name}: {width}x{height} height={total} console_errors={len(errors)} failed_requests={len(failed)}")
        for line in errors[:10]:
            print("  console:", line)
        for line in failed[:10]:
            print("  failed:", line)
        page.close()

    browser.close()
