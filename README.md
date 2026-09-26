# Travel Guide (`travel-guide/`)

Auto tour-guide mobile app: as you move through a place, it tells you what
you're seeing — history, stories, fun facts, what not to miss — out loud,
like a human guide walking with you.

## Where this lives (repo routing)

Subtree of the planning superset, same pattern as `journal-app/`,
`chronolens/`, `foresight/`, `cli-workflow/`, `echoscribe/`:

- Planning superset: `https://github.com/greninja-op/PROJECT-PLANNING-.git`
  (`PROJECT-PLANNING/` → remote `origin`)
- Standalone: `https://github.com/greninja-op/TRAVEL-GUIDE.git`
  (remote `travel-guide`, prefix `travel-guide/` — renamed from TRAVEL-AGENT)

**Naming:** folder/remote use git-safe `travel-guide` (your asked name,
hyphenated — spaces break `subtree --prefix` matching). The GitHub repo was
`TRAVEL-AGENT` and is now `TRAVEL-GUIDE`; remote already updated.

## Push — the only command (from `PROJECT-PLANNING/`)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\push.ps1 -m "message"
```

`git add -A` + commit, `push origin HEAD` (always), then for every touched
`prefix/*`, `git subtree push --prefix=<prefix> <remote> main` — so
`travel-guide/*` changes land in **both** repos in one command. Never
hand-push the standalone repo. Exit code 1 is normal (git progress on
stderr); verify with `git status`.

## Layout (v0.1 scaffold)

```
travel-guide/
  README.md          this file
  SPEC.md            full Product Specification v0.1 (source of truth)
  DESIGN-TOKENS.md   draft tokens — finalize via DesignSoul before UI build
  .env.example       secret *names* only — never real keys
```

Future (per SPEC §4): `mobile/` (Android native per your call — Kotlin +
Compose), `content/` (POI packs: history/facts/see-list per place),
`shared-core/` (geo triggers, narration queue, offline packs contract).

## Secrets rule

Filled credentials live ONLY at workspace root in `../secrets/`
(not a git repo). This repo commits `.env.example` with names only.

## Current status

- [x] Repo registered + `push.ps1` wiring
- [x] Phase 0: scaffolding, POI pack format, Fort Kochi pack v1 (20 POIs)
- [x] Phase 1: triggers, narration queue, on-device TTS, foreground service
- [x] Phase 2: seeing pick, itineraries, POI detail, routes, voice settings
- [x] Phase 3: packs manager, trip export, battery profiles, QA docs
- Decisions taken: Android-only beta, hand-written packs, on-device TTS,
  OSM/MapLibre offline (no API keys). SPEC §8 answered in code.
- [x] v1.0.0 finished: offline map UI, all screens wired, quiet hours,
  visited-rerank, food/stay layer (pack v1.1.0, 24 POIs), events sample,
  runnable `python tools/demo.py --yes` (full trip + exports in demo-out/)
- [x] Build wired (2026-09-26): shared-core Gradle module, Compose plugin
  (Kotlin 2.0.21), AGP 8.7.3 + Gradle 8.9 wrapper, compileSdk/targetSdk 35,
  gradle.properties (useAndroidX), launcher icon + res/, manifest INTERNET
  perms, MapLibre geojson imports fixed. **First successful build:**
  `:shared-core:test` 25 Kotlin unit tests green (real engine, ports of the
  Python mirrors) + `:app:assembleDebug` → app-debug.apk. CI:
  `PROJECT-PLANNING/.github/workflows/travel-guide-ci.yml`.

## Remaining to release (next up)

- Install the APK on a physical device; run SPEC §6 QA matrix (F/P/C cases)
- Wire live data into MainActivity (nearby rows, packs inventory, history
  from Room, POI detail route, map pins from PackLoader) — screens exist,
  currently fed placeholders
- Offline style URL + tile distribution per pack release (OfflinePackHelper
  bounds are ready; needs a hosted style)
- Release signing config + R8 verification, PDF export, a11y pass on device
- Hosted privacy policy URL + Play listing assets; LICENSE file decision
