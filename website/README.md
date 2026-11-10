# Travel Guide — website

A static marketing site for the Travel Guide Android app. No framework, no build
step, no external runtime dependencies. Open `index.html` through a local web
server so the external SVG icon sprite and self-hosted fonts resolve correctly.

```bash
# from the repository root
python -m http.server 8080 --directory website
# open http://localhost:8080
```

## What is here

```text
website/
├── index.html              # landing page
├── privacy.html            # local-first privacy policy
├── credits.html            # photo / map / icon / type provenance
├── css/
│   ├── styles.css          # landing page system, layout, motion
│   └── app.css             # phone mockups (app tokens, Inter + Lora)
├── js/
│   └── main.js             # nav state, mobile menu, scroll reveals
├── assets/
│   ├── favicon.svg
│   ├── icons.svg           # Lucide outline sprite (ISC)
│   ├── fonts/              # self-hosted woff2 + Lora TTFs + fonts.css
│   ├── img/                # real Fort Kochi photography + OSM map crop
│   └── icons-inline.html   # icon preview sheet (dev only)
└── tools/
    ├── fetch_fonts.py      # re-download the Google Fonts latin subsets
    ├── fetch_icons.py      # re-download the Lucide sprite
    └── stitch_map.py       # stitch the 3×3 OSM tile crop
```

## Design system

Two layers, deliberately separate:

**Website** — alabaster `#F4F1EA`, paper `#FFFFFF`, sunset orange `#F26419`,
ink `#141414`, slate `#66645F`, laterite `#B8430B` for small orange text.
Display: Archivo 850–880, uppercase, tight tracking. Body: Inter. Editorial
accent: Instrument Serif italic. Labels/metrics: DM Mono. Radii are ranked:
999 pills, 14 controls, 24 media/cards, 54 devices. Motion is one gesture —
a 640ms fade-up on cards and images, plus a 1.5s SVG trail draw, both fired once
by IntersectionObserver and frozen under `prefers-reduced-motion`.

**Phone mockups** — the shipped app contract from
`design-reference/reference.html` and `ui/theme/Tokens.kt`: coral `#FF5A36`,
midnight `#121826`, amber `#F59E0B`, emerald `#10B981`, Inter + Lora, 18px cards,
14px buttons, 24px sheets, 999 pills, 5-tab dock (Map / Nearby / Packs / History /
Settings). The screens use real pack strings, stop names, distances, and route
order from `content/packs/fort-kochi-walk-v1.json`.

## Provenance / anti-slop rules

- Photography is from the project’s own Fort Kochi app assets — no AI images,
  no stock “lone hiker” hero.
- The map is a real OpenStreetMap crop of Fort Kochi (© OpenStreetMap
  contributors, ODbL).
- Icons are one family, one stroke: Lucide, 24px box, 2px stroke.
- No decorative gradients. Photo scrims and the device material are the only
  places a gradient appears.
- No fake reviews, star ratings, user counts, or company logos. The “field
  notes” section uses real story lines from the pack.
- Every number is checkable: 24 stops, 2 walks, 16-stop Heritage Loop, 8s dwell,
  30 min cooldown, 450m corridor, pack v1.1.0.
- Copy avoids “seamlessly”, “elevate”, “unleash”, “curated”, and similar filler.

## Regenerate assets

```bash
python website/tools/fetch_fonts.py
python website/tools/fetch_icons.py
python website/tools/stitch_map.py
```

The OSM tiles are already committed under `assets/img/osm_tiles/`; the stitched
crop is `assets/img/fort-kochi-map.png`.

## Deploy to Vercel

### Option A — native Vercel Git integration (recommended)

1. Vercel → **Add New → Project** → import `greninja-op/TRAVEL-GUIDE`.
2. Leave **Root Directory** empty (repo root). `vercel.json` sets
   `"outputDirectory": "website"`.
3. Framework Preset: **Other**. Build Command: **empty**. Output Directory:
   `website` (or leave it to `vercel.json`).
4. Deploy. Every push to `main` now deploys automatically.

If you prefer **Root Directory = `website`**, use the `website/vercel.json`
config instead of the root one and leave Output Directory empty.

### Option B — GitHub Actions CLI deploy

`.github/workflows/vercel.yml` deploys on every push to `main` that touches
`website/**` or the Vercel configs. Add these repository secrets first
(GitHub → Settings → Secrets and variables → Actions):

| Secret | Where to get it |
|---|---|
| `VERCEL_TOKEN` | <https://vercel.com/account/tokens> |
| `VERCEL_ORG_ID` | Vercel → Project → Settings → General → Team ID |
| `VERCEL_PROJECT_ID` | Vercel → Project → Settings → General → Project ID |

Then run the workflow once with **Run workflow**. If Vercel's native Git
integration is already connected, disable one of the two mechanisms — otherwise
every push deploys twice. Until the three secrets exist, the workflow exits with
a warning instead of a red failure.

### What the config does

- `outputDirectory: "website"` — deploys only the static site.
- `cleanUrls` + `trailingSlash: false` — `/privacy` instead of `/privacy.html`.
- Immutable one-year cache for fonts; one-week cache for images and the icon
  sprite.
- Security headers: `nosniff`, frame denial, strict referrer, no geolocation /
  camera / microphone permissions.
- `.vercelignore` keeps the Android app, build caches, screenshots, and dev tools
  out of the upload.


## Accessibility

- Semantic landmarks, one `h1`, visible focus rings, skip link.
- Body text 16–17px; small orange text uses `#B8430B` (AA) instead of the display
  orange.
- 44px+ targets; keyboard-operable mobile menu with Escape to close.
- `prefers-reduced-motion` freezes reveals, trail draws, and hero entrances.
- Images have descriptive alt text; decorative phone UI is `aria-hidden`.
- Lighthouse: **Accessibility 100 · Best Practices 100 · SEO 100** on the
  landing page and privacy page.

## Verification

The repeatable audit is `tools/qa_audit.py`. It checks 1440 / 1180 / 768 / 390 /
320 for console errors, horizontal overflow, six phone screens, reveal images,
image upscaling, the audio pill state, the trail progress states, reduced motion,
and keyboard focus. Latest run: 0 errors, 0 horizontal overflow, 18/18 images
loaded, 0 upscaled images, 2/2 reveals visible, trail progress 0 → 0.5 → 1.0.
