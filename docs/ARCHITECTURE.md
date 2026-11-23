# Architecture (`travel-guide/docs/ARCHITECTURE.md`)

Travel Guide is a **worldwide** audio guide. A city pack is not the product — it
is one way content arrives. The product is: *the phone knows roughly where you
are and where you are going next, so the depth for the next few hundred metres
is already on the phone before you get there.*

Fort Kochi is the reference pack (24 stops, v1.1.0). Everything below must work
for a city nobody has authored yet, where the only inputs are open geo data and
a server that can write about a place on demand.

```
GPS + heading + route
  → region index (cheap: every pin in range, already local)
  → prefetch planner (predicted corridor, not a radius)
  → deep bundles (story, facts, audio) fetched ahead of arrival
  → geofence triggers → local audio playback or on-device TTS
  → live layer (AI answers, today's events) on demand, streamed
```

## 1. The three layers

| Layer | What it is | Size | Where it lives | Offline |
|---|---|---|---|---|
| **Discovery** | Every POI in the region: id, name, coords, radius, category, 8-word teaser | ~200 B/POI | Phone, one slice per corridor | Yes — this is why the map is never blank |
| **Deep** | Story text, facts, see-list, hours, sources, pre-rendered audio per language | 15–40 KB text, 150–400 KB audio per POI | Phone, prefetched by corridor; CDN behind it | Yes, once prefetched |
| **Live** | AI answers, today/tomorrow events | small, per response | Server or on-device fallback | Degrades to cached + "as of HH:MM" |

The split exists for one reason: **depth is expensive and predictable, live is
cheap and not.** A 60-second story in four languages with a neural voice is a
few megabytes; a question about the building in front of you is a few hundred
tokens. Prefetch the first, stream the second.

## 2. What "instant" means (latency budgets)

"Instant" is not one number. It is four, and each has a different mechanism.

| Moment | Budget | Mechanism |
|---|---|---|
| Tap a pin → card on screen | **≤ 100 ms** | Card is composed from the discovery index already in memory. No network on the tap path, ever |
| Arrive in radius → first audible word | **≤ 300 ms** | Audio file already local. Playback, not generation |
| Arrive → first word, *cold* (nothing prefetched) | **≤ 900 ms** | On-device TTS speaks the teaser immediately; pre-baked audio swaps in mid-sentence if it arrives |
| "What is this?" / AI answer | **≤ 1.2 s to first spoken clause** | Streamed tokens, sentence-by-sentence TTS; not a full answer before speaking |

Rule that follows from the budgets: **nothing on the trigger path is allowed to
block on the network.** The network only ever moves the *next* stop closer.

## 3. How content reaches the phone

### 3.1 Cells, not cities

The world is addressed in map tiles at **z16** (≈ 600 m at the equator,
~530 m in Kerala). A cell is the unit of caching, requesting and invalidating.

- `GET /v1/cells/{z}/{x}/{y}/index` → the discovery slice: every POI in the
  cell. ~6 KB gzipped for a dense heritage quarter.
- A corridor request is 10–40 cells. At 100 m/s of walking, that is minutes of
  coverage for one round trip.

Cities stay as a *grouping* concept (a downloadable pack = a set of cells plus a
curated order), not as the addressing scheme. This is what makes "anywhere"
possible without uploading a planet-sized file.

### 3.2 Route-aware: the whole path, in three lanes

A radius prefetches in all directions, which wastes most of the bytes — you
walk one way. A **fixed route is better still**: it is the exact set of places
you will pass, known in advance, so it can be loaded *before* you start
walking instead of being chased as you go.

Two modes, one planner:

- **Free roam (no route).** Predict forward from position + bearing + speed:
  next ~500 m walking, ~3 km driving, buffered ±250 m and snapped to roads.
- **Fixed route** (an in-app walk, or a Google Maps navigation session). Take
  the **entire** path at once: resolve the polyline, buffer it by the corridor
  radius, convert to cells, and prefetch every cell the corridor touches —
  start to finish, not just the next few minutes. The companion service
  already produces this corridor from an active Google Maps route, and
  `RoadRouter` already turns origin → destination into road geometry.

The whole path is affordable only because it is split by cost:

| Lane | Contents | Coverage | Rough size |
|---|---|---|---|
| **A — index + events** | every POI the corridor touches, plus the events for those cells | full route, requested immediately | a few hundred KB |
| **B — text** | story, facts, see-list, sources | full route, in the background | ~1 MB for 50 stops |
| **C — audio** | pre-rendered voice | rolling window along the path (~10 min walking, ~5 km driving), plus everything behind you for re-listening | ~0.3 MB per stop |

Lane A is what makes a surprise stop possible at all: every pin the corridor
passes has its record local, so the card is instant even if you leave the
route. Lane B means the full story is readable the moment you stop, before any
audio exists. Lane C is the only genuinely heavy lane, so it follows you
rather than the route — and an explicit *download this walk* promotion turns
the whole route into lane C when on Wi-Fi.

A cell is in when the **buffered path intersects it**, not when its centre
happens to be close. Buffer: 250 m walking, 500 m driving (more look-ahead and
straighter geometry at speed).

Priority within a route: next stop → next 3 stops → current corridor → the
rest in route order. Reroute is cheap by construction: the new path's cells
minus the cells already held usually returns only the changed segment.

Three pieces of this already exist in the app and should be reused rather than
rebuilt:

- `navigation/MapsCompanionService.kt` + `MapsCompanionState` — an active Google
  Maps route, already turned into a corridor of POIs within 350–450 m.
- `map/RoadRouter.kt` — road-snapped geometry (currently OSRM demo server).
- `packs/PackManager.kt` — the 500 MB LRU budget and eviction policy.

What is missing is the planner that turns those into request batches, and a
store that can hold partial regions. Today `AppState` loads exactly one bundled
asset pack at construction and derives every list from it.

### 3.3 The route funnel (not a wider circle)

Widening the radius is the expensive way to survive a wrong guess: area grows
with r², and the extra bytes are places you will never see. But a route is not
one line either — it is a small set of plausible lines that share most of their
length and split at a few junctions. Prefetch the **union**, weighted by how
likely each cell is to be walked.

| Level | Source | Confidence | Lanes | Coverage |
|---|---|---|---|---|
| **L0 — chosen route** | in-app route, or the path resolved for the Maps destination | certain | A + B + C | full path |
| **L1 — alternatives** | up to 3 router alternatives, same origin → destination | high near the start, decays with distance | A + B | full path each, while they share the corridor |
| **L2 — junction fan** | every road leaving each junction in the next ~3 junctions | medium | A only | 400 m past each junction |
| **L3 — widened net** | L0 buffered wide (1.5 km) | low, but cheap | A only | coarse coverage |

Cells are deduplicated, so the union of three routes is typically **1.2–1.6×**
the cost of one rather than 3× — alternatives diverge only at junctions. That
is the argument against widening the radius: the same protection for a fraction
of the bytes.

Lane C (audio) stays on L0 only, with the L0 ordering above. If the traveller
actually turns onto an alternative, L2 has already given that branch its index,
so the card is instant, text lands in seconds and audio follows — a degraded
start, not a dead one.

### 3.4 Watch the path, don't trust it

The funnel is a prediction, so it needs a sensor. Every fix is compared against
the active path:

- **off-route** when perpendicular distance > 80 m for > 15 s while moving
  (> 1 m/s) — not for one noisy fix indoors;
- on deviation, plan immediately from the **actual** position to the next
  waypoint and request lane A for the next ~5 minutes of travel; lane B follows
  if the deviation persists;
- **keep** L2/L3 cells for ~10 minutes — rejoining is common, and those cells
  are already paid for;
- debounce to at most one replan a minute, so a jittery fix cannot cause a
  fetch storm;
- if still off-route after ~2 minutes and moving away, promote the observed
  path to L0 and stop feeding audio for the abandoned one.

**A Google Maps session gives one extra signal worth using.** The companion
listener already reads the turn prompt ("in 250 m, turn right onto Bazaar
Road"). That names the next maneuver and the road, which is exactly what
decides which branch at the next junction is real — so the junction fan can be
biased toward that road instead of fanning equally. It narrows the cone with
information already arriving.

### 3.5 Manifests and deltas

The phone never asks "send me everything". It asks **what changed**:

```
POST /v1/prefetch
{ "cells": ["16/38123/45678", …], "lang": "ml", "voice": "ananya",
  "have": { "chinese-fishing-nets": "b7f1…", … } }

200 { "items": [ { "poi": "…", "version": "b7f1…",
                   "audio": "…/b7f1.opus", "text": "…/b7f1.json" } ],
      "events": [ … ], "next": "…" }
```

Steady state, a corridor re-check is a **~400-byte response**. That is the whole
trick behind "it takes no time": after the first visit, the phone is mostly
saying "I already have these".

### 3.6 Pre-baking and the CDN

Deep bundles are generated **once per (POI, language, voice, content version)**
and never regenerated for a reader:

- content-addressed filenames (`b7f1….opus`, `b7f1….json`),
- `Cache-Control: public, max-age=31536000, immutable`,
- served from an edge CDN; the phone's second request for the same bundle is a
  local cache hit, the CDN's second request is an edge hit.

Baking is **lazy, per city**. The planet cannot be pre-baked ahead of time at
any sane cost. When a cell is requested for the first time:

1. pipeline pulls the POIs for that cell from open data (see §6),
2. writes the story per requested language (LLM + human-reviewed template),
3. renders audio per voice,
4. publishes immutable bundles and updates the cell index.

First visitor to a city waits a few seconds for the teaser and a few seconds
more for audio. **Every visitor after that gets the instant path**, which is the
correct trade: cost is paid per city once, not per user per walk.

## 4. The live layer

Two things cannot live inside a pack — one because it must be written for the
moment, one because it did not exist when the pack was baked. They are also the
two the product differentiates on.

### 4.1 AI answers, in the traveller's language

`voice/SpontaneousGuideAiEngine.kt` is the seed of this, but it is currently
bound to a pack card and hardcodes "Fort Kochi and Mattancherry, Kerala" in
every prompt — outside Kochi it would confidently describe the wrong city.

The shape it needs:

- input: coordinates, the discovery record if any, language, time, weather,
  active event, the question;
- output: **streamed** sentences, each sent to TTS as soon as it is complete;
- language is a **BCP-47 tag**, not a 4-value enum. The model can answer in any
  language; only the *spoken* side is limited by the TTS voice catalogue
  (Sarvam ~11 Indic languages + English; on-device Android TTS varies).
- fallback ladder: streamed cloud answer → local templated answer from the
  discovery record → "no connection, here is what I know offline".

Never let this path block narration: if an answer takes more than ~1.5 s to
start, say something locally and let the full answer arrive as a card.

### 4.2 Events: recurring vs live

| Kind | Example | Source | Delivery |
|---|---|---|---|
| **Recurring** | Onam, Kochi-Muziris Biennale, annual feast day | bundled with the city, date-aware | pre-baked, offline |
| **Live** | a parish fete tomorrow, a one-night exhibition, a road closure | fetched, TTL-cached | online, "as of 18:40" when stale |

`extras/Extras.kt` today reads a bundled JSON keyed by `poi_id` and, on a day
with no exact match, returns *the whole list* — which would show every festival
in the pack as if it were happening now. Live events need:

- a per-cell endpoint (`GET /v1/events?cell=…&from=…&to=…`), one request per
  corridor rather than per POI — a route request covers every cell on the path
  in one call,
- TTL 10–30 min, one cached copy served offline with a visible timestamp,
- never invent: an event with no source is not shown, same honesty rule as POI
  sources (SPEC §1.5).

**Fetch early, not on arrival.** Live events cannot be *baked* into a pack —
this year's fete did not exist when the city was baked — but they can be
fetched before they are needed:

- the moment a route is fixed, request the event set for **every cell on the
  path**, in the same call that fetches lane A. One request, whole walk;
- refresh on a timer while the route is active (10–15 min) and once at start,
  so "happening tomorrow" does not silently become yesterday's data;
- if the route was planned days ahead, fetch for the trip window (today and
  tomorrow — exactly what the UI shows) rather than "now";
- on arrival at a POI, the event is already in memory, so the card and the
  narration mention it with zero wait — which was the point of fetching early.


## 5. API sketch

```
GET  /v1/cells/{z}/{x}/{y}/index?lang=xx        discovery slice for one cell
POST /v1/prefetch                               delta manifest for a corridor or a whole route
GET  /v1/bundles/{poi}/{lang}/{voice}/{ver}.json.gz
GET  /v1/bundles/{poi}/{lang}/{voice}/{ver}.opus     immutable, CDN
GET  /v1/events?cells=a,b,c&from=…&to=…         one call for the whole route, TTL 10–30 min
POST /v1/ask                                    streamed answer (SSE)
POST /v1/bake                                   operator: enqueue a city
```

Everything except `/v1/ask` is a cacheable GET. `/v1/bake` is the only endpoint
that spends real money, and it is an operator action, not a user one.

## 6. Constraints that cannot be designed around

- **Google Places/Geocoding terms forbid pre-fetching, caching or storing their
  content** beyond limited display use. So Google cannot be the source for
  baked bundles — only for live display, if used at all. Discovery and baking
  must come from sources that permit storage: **OpenStreetMap** (ODbL),
  **Wikidata** (CC0), **Wikipedia/Wikivoyage** (CC BY-SA, attribution
  required), plus curated and user-contributed content.
- **OSM tile servers** are community infrastructure with a usage policy; a
  shipping build needs self-hosted or commercial tiles (already noted in
  `map/MapStyle.kt`). Same applies to the OSRM demo router in `RoadRouter.kt`.
- **Attribution is a licence condition**, not a courtesy: every POI already
  carries `sources[]`; the delivery layer must preserve source and licence per
  bundle, and the credits screen must stay in sync.
- **AI cost is per interaction, not per user.** Bake is cheap and amortised;
  live answers are not. Budget them separately.

## 7. What the phone stores

| Store | Contents | Budget | Eviction |
|---|---|---|---|
| Region index | lane A: discovery slices + event sets per cell | ~2 MB per city | LRU by cell |
| Text bundles | lane B: story, facts, sources | ~20 MB per city | LRU by POI, last |
| Audio bundles | lane C: pre-rendered voice | rest of the 500 MB cap (existing) | LRU by POI, oldest first |
| Manifests | ETags/content hashes per POI | tiny | per walk, then LRU |
| Live cache | AI answers, stale event sets | ~20 MB | TTL, then LRU |

Text outlives audio deliberately: if the cap is hit, the walk still reads
without a voice, rather than going silent with a voice and no words.

The bundled Kochi pack stays as the permanent offline reference and as the
demo: with no network at all, the app must still do a complete loop.

## 8. Failure modes (honest degradation)

| Failure | Behaviour |
|---|---|
| Predictive miss (you turn left) | Card is instant from the index; audio starts from TTS within a second and upgrades if the bundle lands |
| You take an alternative route | L2 already holds that branch's index; text lands in seconds, audio follows. Never a blank screen |
| Dead zone | Last-known bundles play; nothing claims to be live |
| Cold cell (never baked) | Teaser immediately, deep content streams; spoken fallback so silence never happens |
| Prefetch overshoots data cap | Queue stops at the user's limit, discloses it, keeps the next stop only |
| Event source stale | Shown as "as of HH:MM", never as "today" |
| AI unavailable | Local templated answer, labelled as offline |

No path is allowed to show a spinner where it could show a card.

## 9. What changed in this codebase

Landed with this design (all JVM-tested, `:shared-core:test`):

| Piece | Where | What it does |
|---|---|---|
| Cell addressing | `shared-core/.../delivery/Cells.kt` | z16 tiles, bounds/centre, corridor stroking, path projection |
| Route funnel | `delivery/RouteFunnel.kt` | L0–L3 candidates, weights, buffers, free-roam corridor, junction stubs |
| Planner | `delivery/PrefetchPlanner.kt` | ranked cells per lane, audio window, budgets |
| Deviation | `delivery/DeviationMonitor.kt` | sustained off-route → replan → promote, with debounce |
| Store | `delivery/BundleStore.kt` | lanes, manifest, LRU, audio-before-text eviction, pinned reference pack |
| Content contract | `delivery/ContentSource.kt` | request/response model, fixture source, `PrefetchCoordinator` |
| HTTP | `mobile/.../delivery/HttpContentSource.kt` | `/v1/prefetch`, `/v1/events`, bundle GETs |
| Route supply | `mobile/.../delivery/RouteProvider.kt` | OSRM alternatives (L1) + junction fan (L2) |
| Loop | `mobile/.../delivery/ContentPrefetcher.kt` | fix → funnel → plan → fetch → events, on IO, never on the trigger path |

Wired into `data/AppState.kt`: `onFix` now forwards speed and the active route.
It stays **dormant** unless a content server is configured
(`content_base_url` in `guide_prefs`), so the bundled-pack guide is unchanged
until a backend exists.

Still to change:

| Today | Becomes |
|---|---|
| `data/PackLoader.kt` — one bundled JSON | `RegionStore` — index + bundles + LRU, pack is a special case |
| `voice/Narrator.kt` — TTS only, cloud default on | local audio file first, TTS fallback, cloud last |
| `extras/Extras.kt` — bundled events | bundled recurring + fetched live, TTL (`ContentPrefetcher.liveEvents` exists; the UI does not read it yet) |
| `voice/SpontaneousGuideAiEngine.kt` — pack card, Kochi prompts | coordinates + language in, streamed sentences out |

The geofence, dwell, queue, quiet-hours and itinerary rules in `shared-core`
do not change. They are the part that is already right.

## 10. Open decisions

1. **Server stack** — one of: Cloudflare Workers + R2 (cheapest at the edge for
   immutable bundles), a small Go/Node service + S3/CDN, or Firebase (fastest
   to start, weakest cold-start control).
2. **Discovery source** — OSM + Wikidata only (legal to store), or OSM plus a
   curated editorial layer for quality in launch cities.
3. **Voice catalogue** — Sarvam's 11 Indic languages + English as the baked
   set, with on-device TTS as the universal fallback, or a broader TTS vendor.
4. **How much depth is baked per POI** — one 60-second story + facts, or also a
   longer "walk with me" variant and a 15-second teaser variant. Triples bake
   cost and storage; buys a lot of perceived quality.
