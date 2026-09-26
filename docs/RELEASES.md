# Releases (`travel-guide/docs/RELEASES.md`)

## v1.0.0 (beta) — 2026-09-10 — pack v1.1.0

First finished beta: Fort Kochi & Mattancherry, 24 POIs (20 heritage + 2 food
+ 2 stay), 2 routes (Heritage Loop 16 stops, Full Day 10 stops).

- Auto-narration: geofence enter + 8s dwell, 30-min cooldown, single voice,
  on-route priority, speed gate, quiet hours (cards only, no voice)
- On-device TTS, playback speed, auto-play toggle, <500ms mute everywhere
- "What am I seeing?" (heading-aware), day-plan builder (skips closed POIs),
  POI cards with see-lists + sources + pack version, closed badge (C-02)
- Offline: MapLibre tiles 12–16, versioned packs, 500MB LRU (bundled pack
  never evicted), zero-network full loop
- Trip history + notes + Markdown export; phrasebook (10 phrases); events layer
- Consent-first: first-launch rationale, background location separate opt-in,
  battery profile with disclosed cost

Known issues: Android build needs Android Studio (no CI here); event dates
recur yearly — confirm before visiting; food/stay pins are starter data.

Battery: Balanced profile default (15s / 15m); Saver may delay stories.
