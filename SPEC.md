# Travel Guide — Automatic Tour-Guide App
## Product Specification v0.1 (Beta Scope)

Source of truth for `travel-guide/`. Built from your brief; you said to
figure out the rest — §8 marks what still needs your call. Update here
first, then code.

---

## 0. Product Summary

A mobile app that does what a human tour guide does — automatically. As you
walk (or ride) through a place, it notices where you are and tells you about
it out loud: what you're seeing, its history, stories and fun facts, what to
look at, what most people miss, what's next nearby.

**Core promise:** put your phone in your pocket — the place explains itself.

Beta = one platform (Android, per your call), offline-capable, location-triggered
audio narration + readable cards.

**Scope: worldwide.** The engine is city-agnostic. Fort Kochi & Mattancherry is
the bundled **reference pack** — sample content that exercises every code path —
not the product's boundary. A place with no pack must still be explainable, which
is what the AI layer and corridor delivery are for (§4.5,
`docs/ARCHITECTURE.md`).

---

## 1. Non-Negotiable Constraints

1. **Location only with explicit permission.** Foreground location to start;
   background location only after a separate, plain-language opt-in. Never
   track silently.
2. **User controls the voice.** Narration speaks only when the user enables
   audio for the trip (start/stop, mute instantly). No surprise audio.
3. **Battery honesty.** No constant high-accuracy GPS polling in background
   without telling the user the cost + offering power modes.
4. **Offline-first.** A downloaded city pack (map pins + stories + audio or
   TTS cache) must fully work with zero network.
5. **Content honesty.** History vs legend vs fun-fact are labeled as such.
   Never present folklore as fact. Every POI cites its source pack version.
6. **Minimal, consistent components** (tokens in `DESIGN-TOKENS.md`).

---

## 2. Phased Roadmap

### Phase 0 — Foundations (Weeks 1–2)

- Repo scaffolding (`mobile/`, `content/`, `shared-core/`)
- POI pack format locked (JSON schema: id, name, coords, radius, story,
  facts, see-list, sources, audio/TTS text)
- One test-city pack (~20 POIs on one walkable route)
- Map + location permission flow + basic "nearby list"

### Phase 1 — Auto-Guide Loop (Weeks 3–6)

- Geofence enter/dwell triggers → narration queue ("you're passing X")
- TTS narration (on-device first) + now-playing sheet + transcript card
- Walk mode: ordered route with "next up" + off-route handling
- Wander mode: whatever is near you, nearest-first, no fixed route

### Phase 2 — Guide Brain (Weeks 7–10)

- "What am I seeing?" button: nearest POI + direction-facing pick
- See-list per POI ("look up at the arch, find the mason's mark…")
- Don't-miss + skip logic (closed hours, already visited)
- Day plan: 2h / half-day / full-day auto-itineraries from packs
- Language + voice pick, playback speed, auto-play toggles

### Phase 3 — Polish & Beta Hardening (Weeks 11–13)

- Offline packs manager (download/delete/sizes), sync conflicts
- Export trip (map + visited + notes) as PDF/Markdown
- Accessibility (TalkBack labels, font scaling, contrast)
- Battery profiles (precise / balanced / saver)

### Phase 4 — Post-Beta (noted only)

- iOS, Wear OS / watch companion, car mode
- Community packs + creator review pipeline
- Group/family trips, shared live location (opt-in)

---

## 3. Feature Spec

### 3.1 Core Features

| Feature | Description |
|---|---|
| Nearby radar | Live list of POIs around you, sorted by distance, with "2 min walk" hints |
| Auto-narration | Entering a POI radius (with dwell anti-spam) speaks its 30–60s story; queues if several trigger |
| POI card | Photo/illustration, 3-line summary, history, 2–3 fun facts, see-list, sources |
| Walk mode | Fixed route: step-by-step, "next: X, 150m ahead", reroutes if you stray |
| Wander mode | No route: nearest-first narration as you roam |
| What am I seeing? | One tap: best-guess POI in front of you (location + heading) |
| Offline packs | Per-city download: POIs, text, map tiles ref, pre-rendered or TTS-cached audio |
| Trip history | Visited POIs with timestamps, per-day timeline |

### 3.2 Add-On Features

| Feature | Description |
|---|---|
| Itineraries | Auto day-plans (2h / half / full day) from pack + opening hours |
| Food/stay pins | Secondary layer (eat/rest), clearly separated from heritage POIs |
| Phrasebook | 10 survival phrases per city with TTS playback |
| Notes | Per-POI personal note + photo, kept local |
| Share trip | Export visited route + notes as link/PDF |
| Events layer | Recurring festivals bundled with the city; live one-offs (today/tomorrow) fetched early for the whole route and TTL-cached |

---

## 4. Technical Architecture

### 4.1 Platform choices

- **Beta: Android native** — Kotlin + Jetpack Compose (best background
  location + notification + TTS control). iOS later.
- **Maps:** OSM + MapLibre for offline tiles preferred; Google Maps SDK is
  the fallback if you want Places data richness (costs + offline limits).
- **Shared logic:** `shared-core/` holds pure Kotlin: geofence evaluation,
  narration queue, pack schema validation, itinerary picking — no Android
  imports, unit-testable.

### 4.2 Pipeline

```
GPS + heading → geofence eval (enter/dwell/exit, hysteresis, cooldown)
→ narration queue (priority: on-route > nearest > event, dedupe, max 1 voice)
→ TTS (on-device cached audio first, cloud voice opt-in fallback)
→ now-playing + transcript card → visited log → trip timeline
```

Trigger guards: minimum radius, dwell 5–10s, per-POI cooldown (e.g. 30 min),
speed gate (don't narrate new POIs above cycling speed unless user allows),
quiet hours per user setting.

### 4.3 Data model (core objects)

- `Poi`: id, name, lat, lng, radius_m, category, summary, history,
  fun_facts[], see_list[], hours, sources[], pack_version
- `Pack`: city_id, version, pois[], routes[], audio_cache_ref
- `Route`: id, name, ordered_poi_ids[], est_minutes
- `TriggerEvent`: poi_id, entered_at, dwelled_s, spoken (bool)
- `Visit`: poi_id, arrived_at, left_at, transport_mode
- `Trip`: date, visits[], notes[]

### 4.4 Storage & sync

- Local: Room (SQLite) + DataStore; audio cache in app files, LRU-capped
- Packs versioned; user content (notes/photos/visits) local-first, export
  only — no account needed for beta

### 4.5 Content delivery (worldwide, corridor-based)

Full design in `docs/ARCHITECTURE.md`; the short version:

- **Cells, not cities.** The world is addressed in ~600 m tiles at zoom 16. A
  request is a set of cells, a response is a delta against the hashes already on
  the phone.
- **Three lanes.** A = index (every POI the corridor touches, whole route),
  B = text (whole route), C = pre-rendered audio (rolling window only).
- **Route funnel, not radius.** L0 chosen route → L1 router alternatives →
  L2 junction fan → L3 wide net, weighted and deduplicated by cell.
- **Watch the path.** Sustained off-route (>80 m for 15 s while moving) replans
  from the real position; a 2-minute detour promotes the observed path.
- **Live layer.** AI answers stream in the traveller's language; events are
  fetched early for the whole route, TTL-cached, and served stale only with an
  "as of" timestamp. Google place data may not be stored — discovery and baking
  come from OSM / Wikidata / Wikipedia, which permit it.

---

## 5. UI/UX (summary — tokens in `DESIGN-TOKENS.md`)

Screens: Map (hero) / Nearby / Now Playing sheet / POI Detail / Routes /
Packs (offline) / Trip History / Settings. Map-first, big tap targets
outdoors, high-contrast sun-readable text, one-tap mute always visible.

---

## 6. Test Plan

### 6.1 Functional

| ID | Case | Expected |
|---|---|---|
| F-01 | Enter POI radius, dwell | Narration starts ≤3s after dwell, card appears |
| F-02 | Two POIs overlap | One voice at a time, second queued, no overlap |
| F-03 | Leave mid-narration | Finishes current sentence, marks partial visit |
| F-04 | Re-enter same POI in 5 min | No repeat (cooldown), card only |
| F-05 | Offline, pack downloaded | Full loop works, zero network calls |
| F-06 | Walk mode off-route 100m | "Back on route" hint, next POI recomputed |
| F-07 | What am I seeing? | Correct POI top-1 in test route ≥8/10 |
| F-08 | Mute mid-trip | Audio stops <500ms, queue paused |

### 6.2 Permissions/battery

| ID | Case | Expected |
|---|---|---|
| P-01 | Deny background location | Foreground-only mode, clear explainer, no crash |
| P-02 | Battery saver on | Reduced GPS rate disclosed, triggers still fire |
| P-03 | First launch | Permission rationale before system dialog |

### 6.3 Content

| ID | Case | Expected |
|---|---|---|
| C-01 | Every POI | Has sources + pack version, legend labeled as legend |
| C-02 | Closed POI | Shows "closed now", still narrates exterior story |

---

## 7. Docs To Produce Alongside Build

README (run/build), pack-schema reference, architecture diagram, privacy &
permissions doc, QA test scripts, release notes template.

---

## 8. Open Questions For You

Answered during build (kept for the record):

- ~~Android-only beta confirmed?~~ Android first; iOS designed separately in
  `docs/IOS-COMPANION-ARCHITECTURE-SPEC.md`.
- ~~Test city + 20-POI route: which city?~~ Fort Kochi & Mattancherry, 24 POIs,
  now the reference pack rather than the product scope.
- ~~TTS: on-device vs cloud?~~ On-device is the default; cloud voice is an
  explicit opt-in.

Still open:

- Discovery source for baking: OSM + Wikidata only, or OSM plus a curated
  editorial layer for launch cities? (Google place data cannot be stored.)
- Server stack for content delivery (edge worker + object store vs a small
  service + CDN).
- How much depth is baked per POI (one 60 s story, or also a 15 s teaser and a
  longer "walk with me" variant).
- Repo rename: `TRAVEL-AGENT` → ? (tell me, I'll update the remote)
