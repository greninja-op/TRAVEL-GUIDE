"""Fetch the exact Lucide outline icons we use and emit an inline SVG sprite."""

import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "icons.svg"
PREVIEW = ROOT / "assets" / "icons-inline.html"

ICONS = {
    "map": "map",
    "map-pin": "map-pin",
    "compass": "compass",
    "navigation": "navigation",
    "locate-fixed": "locate-fixed",
    "route": "route",
    "headphones": "headphones",
    "sliders": "sliders-horizontal",
    "shield": "shield",
    "shield-check": "shield-check",
    "lock": "lock",
    "user": "user",
    "clock": "clock",
    "download": "download",
    "star": "star",
    "check": "check",
    "book-open": "book-open",
    "arrow-right": "arrow-right",
    "arrow-up-right": "arrow-up-right",
    "chevron-right": "chevron-right",
    "chevron-down": "chevron-down",
    "menu": "menu",
    "x": "x",
    "play": "play",
    "pause": "pause",
    "volume": "volume-2",
    "volume-x": "volume-x",
    "mic": "mic",
    "ear": "ear",
    "eye": "eye",
    "footprints": "footprints",
    "waves": "waves",
    "landmark": "landmark",
    "utensils": "utensils",
    "bed": "bed-double",
    "battery": "battery-full",
    "wifi-off": "wifi-off",
    "smartphone": "smartphone",
    "bell": "bell",
    "globe": "globe",
    "file-text": "file-text",
    "key": "key-round",
    "github": None,
    "sun": "sun",
    "mountain": "mountain",
    "wind": "wind",
    "coffee": "coffee",
    "heart": "heart",
    "info": "info",
    "external-link": "external-link",
    "circle-check": "circle-check",
    "layers": "layers",
    "audio-lines": "audio-lines",
    "speech": "speech",
    "languages": "languages",
    "timer": "timer",
    "gauge": "gauge",
    "scan-eye": "scan-eye",
    "waypoints": "waypoints",
    "train-front": "train-front",
    "signal": "signal",
    "wifi": "wifi",
    "chevron-left": "chevron-left",
    "more-horizontal": "more-horizontal",
    "settings": "settings",
    "skip-forward": "skip-forward",
    "skip-back": "skip-back",
    "circle-stop": "circle-stop",
    "expand": "expand",
}

UA = {"User-Agent": "TravelGuideLanding/1.0"}


def fetch(name: str) -> str:
    url = f"https://unpkg.com/lucide-static@latest/icons/{name}.svg"
    req = urllib.request.Request(url, headers=UA)
    svg = urllib.request.urlopen(req, timeout=60).read().decode("utf-8")
    inner = re.sub(r"^.*?<svg[^>]*>", "", svg, flags=re.S)
    inner = re.sub(r"</svg>\s*$", "", inner, flags=re.S)
    inner = re.sub(r"\s+", " ", inner).strip()
    return inner


symbols = []
for symbol_id, lucide_name in ICONS.items():
    if not lucide_name:
        continue
    try:
        inner = fetch(lucide_name)
        symbols.append(f'  <symbol id="i-{symbol_id}" viewBox="0 0 24 24">{inner}</symbol>')
        print("ok", symbol_id, "<-", lucide_name)
    except Exception as error:  # noqa: BLE001
        print("FAIL", symbol_id, lucide_name, error)

sprite = (
    "<!-- Lucide icons (ISC). https://lucide.dev -->\n"
    '<svg xmlns="http://www.w3.org/2000/svg" style="display:none" aria-hidden="true">\n'
    + "\n".join(symbols)
    + "\n</svg>\n"
)
OUT.write_text(sprite, encoding="utf-8")

PREVIEW.write_text(
    "<!doctype html><meta charset='utf-8'><body style='font-family:sans-serif;padding:24px'>"
    "<h1>Icon sprite</h1><p>Use <code>&lt;use href=\"#i-name\"/&gt;</code>.</p>"
    "<div style='display:flex;flex-wrap:wrap;gap:24px'>"
    + "".join(
        f"<figure style='margin:0;text-align:center'><svg viewBox='0 0 24 24' width='28' height='28' fill='none' stroke='#141414' stroke-width='1.75' stroke-linecap='round' stroke-linejoin='round'><use href='#i-{sid}'/></svg><figcaption style='font-size:12px'>{sid}</figcaption></figure>"
        for sid in ICONS
    )
    + "</div>"
    + sprite
    + "</body>",
    encoding="utf-8",
)
print("wrote", OUT, "and", PREVIEW)
