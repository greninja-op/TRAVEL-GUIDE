<h1 align="center">Travel Guide</h1>

<p align="center">
  <strong>Walk in. Listen.</strong><br />
  A proximity-triggered audio companion for Fort Kochi’s old streets.
</p>

<p align="center">
  <a href="website/README.md">Website</a> ·
  <a href="docs/ARCHITECTURE.md">Architecture</a> ·
  <a href="SPEC.md">Product spec</a> ·
  <a href="docs/PRIVACY.md">Privacy</a>
</p>

<p align="center">
  <img alt="CI" src="https://github.com/greninja-op/TRAVEL-GUIDE/actions/workflows/ci.yml/badge.svg" />
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" />
  <img alt="Kotlin 2.4.21" src="https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white" />
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Compose-2026.09-4285F4?logo=jetpackcompose&logoColor=white" />
  <img alt="License" src="https://img.shields.io/badge/License-Apache%202.0%20%2F%20MIT-lightgrey" />
</p>

<p align="center">
  <img src="website/assets/img/kochi_hero.jpg" alt="Sunset over the Chinese fishing nets and the Fort Kochi shoreline" width="100%" />
</p>

<p align="center"><em>Fort Kochi, Kerala — the 24-stop heritage pack.</em></p>

---

## What it is

Travel Guide is an Android audio guide that speaks when you arrive. Walk into a heritage stop and the story begins on its own — no searching, no reading, no screen. The app is built around the Fort Kochi & Mattancherry heritage pack: **24 curated stops, two self-guided walks**, and a voice that keeps working when the signal doesn’t.

It is local-first by design: no account, no ads, no analytics. Location is processed on the phone, notes are encrypted, and the whole guide is available offline.

## At a glance

| Area | Detail |
|---|---|
| **Platform** | Android 8.0+ (API 26–37) |
| **Language / UI** | Kotlin 2.4.21 · Jetpack Compose |
| **Build** | Gradle 9.9 · AGP 9.5.1 · compileSdk 37 |
| **Guide** | Fort Kochi & Mattancherry · 24 stops · 2 walks |
| **Voice** | On-device TTS · optional Sarvam neural voice |
| **Privacy** | No account · no ads · no analytics |
| **Release** | App v1.0.0 · Pack v1.1.0 |

## What it does

| Feature | What it does |
|---|---|
| **Ambient narration** | Stories start after an 8-second dwell, so passing traffic doesn’t trigger chatter. A 30-minute cooldown keeps the same story from repeating. |
| **What am I seeing?** | One tap uses position and heading to name the closest landmark, then answers from the pack already on the phone. |
| **Curated walks** | A 16-stop Heritage Loop (~2.5 hours) and a 10-stop full-day route. Choose 2 hours, half a day, or a full day — the guide picks the order and skips closed stops. |
| **Offline first** | The Fort Kochi pack, map tiles, and on-device voice work without a signal. Tiles are capped at 500 MB; the Fort Kochi pack is never evicted. |
| **Google Maps companion** | Optional. Follows an active Google Maps route and pre-loads stops within 450 m of the path, then clears itself when navigation ends. |
| **Trip history & notes** | Every visit is logged with a timestamp. Add a note, attach a photo, and export the whole trip as a Markdown file. |
| **Private by default** | No account, ads, or analytics. Notes are sealed with a hardware-backed Android Keystore key (AES-256-GCM). One tap erases everything. |
| **Multilingual voice** | English, Malayalam, Hindi, and Tamil, with adjustable speed and quiet hours from 10 PM to 7 AM. |
| **Battery profiles** | Saver (~30 s), Balanced (~10 s), and Precise (~3 s) GPS intervals, with background location as a separate opt-in. |

## The app

Five tabs, one job each:

| Screen | Purpose |
|---|---|
| **Map** | The Fort Kochi route, heritage pins, next-stop card, and “What am I seeing?” |
| **Nearby** | Stops sorted by distance, layer filters, today’s events, and the seeing card. |
| **Packs** | The built-in Fort Kochi pack, offline map tiles, and storage status. |
| **History** | Visits, timestamps, personal notes, photos, and Markdown export. |
| **Settings** | Voice, quiet hours, battery profile, Maps companion, app lock, and data controls. |

## How it works

| Stage | Responsibility |
|---|---|
| **Input** | GPS + heading, plus optional Google Maps companion cues |
| **Engine** | `shared-core` — geofencing, dwell/cooldown, route order, narration queue |
| **Voice** | On-device TTS by default; opt-in Sarvam neural voice |
| **Storage** | Room — visits and notes, encrypted at rest |
| **Output** | Earbuds or speaker; one voice at a time, mute in under 500 ms |

- **`shared-core`** is pure Kotlin: geofencing, dwell/cooldown rules, route ordering, quiet hours, and the narration queue. It has no Android imports and is tested on the JVM.
- **`mobile`** is the Android app: Compose UI, Room, Fused Location, MapLibre, foreground service, and TTS.
- **`content`** holds the versioned Fort Kochi pack and JSON schema. The same bytes are bundled into the app assets.
- **`tools`** mirrors the core logic in Python for pack validation, trigger simulation, and ADB field playback.

## Project structure

```text
mobile/        Android app — Compose UI, Room, location, voice
shared-core/   Pure Kotlin engine — geofencing, queue, routes
content/       Fort Kochi pack + schema (24 POIs, 2 routes)
website/       Static marketing site (Vercel-ready)
tools/         Python pack validation, simulation, ADB helpers
docs/          Architecture, privacy, QA, release notes
```

## Getting started

### Android

Requirements: **JDK 17**, **Android SDK 37**, and optionally Android Studio.

```powershell
cd mobile
.\gradlew.bat :shared-core:test
.\gradlew.bat :app:assembleDebug
```

The APK is written to:

```text
mobile/app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```powershell
adb install -r mobile/app/build/outputs/apk/debug/app-debug.apk
```

`mobile/local.properties` (git-ignored) points Gradle at the local SDK. Optional API keys live in `secrets/api-keys.env`, also git-ignored.

### Website

```powershell
python -m http.server 8080 --directory website
# open http://localhost:8080
```

The site is static — no build step. Vercel configuration and deployment steps are in [`website/README.md`](website/README.md).

## Testing

| Layer | Command | Result |
|---|---|---|
| Core engine | `cd mobile && .\gradlew.bat :shared-core:test` | **29 tests passing** |
| Pack schema | `python tools/validate_pack.py content/packs/fort-kochi-walk-v1.json` | valid pack |
| Logic mirrors | `python tools/test_engine.py` · `test_queue.py` · `test_itinerary.py` · `test_completion.py` | all pass |
| Website QA | `python website/tools/qa_audit.py` | 1440 / 1180 / 768 / 390 / 320 — 0 errors, 0 overflow |
| Lighthouse | landing + privacy pages | Accessibility 100 · Best Practices 100 · SEO 100 |

CI runs the Kotlin tests, the debug APK build, pack validation, and the Python logic mirrors on every push.

## Fort Kochi pack

| Pack detail | Value |
|---|---|
| **Pack** | Fort Kochi & Mattancherry · v1.1.0 |
| **Stops** | 24 — 20 heritage, 2 food & cafés, 2 stays |
| **Heritage Loop** | 16 stops · ~2.5 hours on foot |
| **Full Day** | 10 stops · ~4 hours, Fort Kochi + Mattancherry |
| **Schema** | [`content/schema/poi-pack-schema.json`](content/schema/poi-pack-schema.json) |

Highlights include the Chinese Fishing Nets, St. Francis Church, Santa Cruz Basilica, the Dutch Cemetery, Princess Street, Mattancherry Palace, the Pardesi Synagogue, and Jew Town’s spice lanes.

## Privacy by design

- **Location stays local.** GPS fixes are used in memory to match nearby stops and are not uploaded. Background location is a separate, revocable opt-in.
- **Maps companion is scoped.** The optional accessibility/notification listener is restricted to Google Maps, ignores editable and password fields, and drops every other app.
- **Notes are encrypted.** Trip notes are sealed with an Android Keystore key using AES-256-GCM.
- **Cloud is opt-in.** The offline voice is the default. Cloud narration and the optional AI guide only run when enabled, and can be turned off without losing the offline guide.
- **You control the data.** Export the trip as Markdown, or erase the local database and notes from Settings.

Read the full policies in [`docs/PRIVACY.md`](docs/PRIVACY.md) and [`website/privacy.html`](website/privacy.html).

## Documentation

| Document | What it covers |
|---|---|
| [`SPEC.md`](SPEC.md) | Product and engineering specification |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Module boundaries and data flow |
| [`docs/QA.md`](docs/QA.md) | Test commands and manual QA checklist |
| [`docs/PRIVACY.md`](docs/PRIVACY.md) | Privacy and data handling |
| [`DESIGN-TOKENS.md`](DESIGN-TOKENS.md) | Palette, type, spacing, motion, components |
| [`website/README.md`](website/README.md) | Website structure, assets, and Vercel deployment |

## Credits

Fort Kochi photography is part of the project’s own app asset set. Map data © OpenStreetMap contributors (ODbL). Icons from [Lucide](https://lucide.dev) (ISC). Archivo, Inter, Instrument Serif, DM Mono, and Lora are licensed under the SIL Open Font License. Full provenance: [`website/credits.html`](website/credits.html).

## License

Code is released under the project’s **Apache 2.0 / MIT** dual license. Heritage stories, routes, and photography are shared under **CC BY-SA 4.0**.
