"""Download the latin/latin-ext woff2 files for the site and write fonts.css."""

import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FONT_DIR = ROOT / "assets" / "fonts"
FONT_DIR.mkdir(parents=True, exist_ok=True)

CSS_URL = (
    "https://fonts.googleapis.com/css2?"
    "family=Archivo:wdth,wght@62..125,100..900&"
    "family=DM+Mono:wght@400;500&"
    "family=Instrument+Serif:ital@0;1&"
    "family=Inter:opsz,wght@14..32,400..700&"
    "display=swap"
)

UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
)

request = urllib.request.Request(CSS_URL, headers={"User-Agent": UA})
css = urllib.request.urlopen(request, timeout=60).read().decode("utf-8")

block_re = re.compile(r"/\*\s*([\w-]+)\s*\*/\s*@font-face\s*\{(.*?)\}", re.S)
blocks = block_re.findall(css)
print(f"blocks: {len(blocks)}")

keep_subsets = {"latin"}
out_rules = []

for subset, body in blocks:
    if subset not in keep_subsets:
        continue
    family = re.search(r"font-family:\s*'([^']+)'", body)
    style = re.search(r"font-style:\s*([^;]+);", body)
    weight = re.search(r"font-weight:\s*([^;]+);", body)
    stretch = re.search(r"font-stretch:\s*([^;]+);", body)
    url = re.search(r"src:\s*url\((https://[^)]+)\)", body)
    if not (family and style and weight and url):
        continue

    fam = family.group(1)
    sty = style.group(1).strip()
    wgt = weight.group(1).strip()
    url_value = url.group(1)

    name = re.sub(r"[^a-z0-9]+", "-", f"{fam}-{sty}-{wgt}-{subset}".lower()).strip("-")
    filename = f"{name}.woff2"
    target = FONT_DIR / filename
    if not target.exists():
        req = urllib.request.Request(url_value, headers={"User-Agent": UA})
        target.write_bytes(urllib.request.urlopen(req, timeout=60).read())
        print("downloaded", filename, target.stat().st_size)
    else:
        print("exists", filename)

    rule = [
        "@font-face {",
        f"  font-family: '{fam}';",
        f"  font-style: {sty};",
        f"  font-weight: {wgt};",
    ]
    if stretch:
        rule.append(f"  font-stretch: {stretch.group(1).strip()};")
    rule += [
        f"  src: url('{filename}') format('woff2');",
        "  font-display: swap;",
        "}",
    ]
    out_rules.append("\n".join(rule))

(FONT_DIR / "fonts.css").write_text(
    "/* Self-hosted Google Fonts (OFL). Latin + latin-ext subsets only. */\n\n"
    + "\n\n".join(out_rules)
    + "\n",
    encoding="utf-8",
)
print(f"wrote fonts.css with {len(out_rules)} faces")
