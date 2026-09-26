# shared-core (`travel-guide/shared-core/`)

Pure Kotlin, no Android imports — the testable brain. Android (`mobile/`)
and the Python mirrors (`tools/`) implement the same rules; this folder is
the spec of record.

```
shared-core/
  src/main/kotlin/guide/core/Models.kt        SPEC §4.3 data model
  src/main/kotlin/guide/core/Geo.kt           haversine, bearing, inside-radius
  src/main/kotlin/guide/core/PackValidator.kt pack rules (Phase 0)
  src/main/kotlin/guide/core/Triggers.kt      enter/dwell/exit + guards (Phase 1)
  src/main/kotlin/guide/core/NarrationQueue.kt single-voice queue (Phase 1)
  src/main/kotlin/guide/core/Seeing.kt        "what am I seeing?" pick (Phase 2)
  src/main/kotlin/guide/core/Itineraries.kt   day-plan builder (Phase 2)
  src/main/kotlin/guide/core/QuietHours.kt    quiet-hours voice gate
  src/main/kotlin/guide/core/VisitRank.kt     visited-rerank + closed-sink + layer filter
```

## Rules (mirrored in tools/*.py so CI-free verification works)

- Distance: haversine metres. Inside = d <= radius_m.
- Dwell: must stay inside continuously for dwell_s (default 8s) before speak.
- Cooldown: same POI never re-speaks within cooldown_s (default 1800s).
- Speed gate: above maxSpeakSpeedMps (default 8 m/s ≈ cycling) new POIs queue
  silently as cards only, unless user allows.
- Queue: one voice at a time; priority on-route > nearest > event; dedupe by poi id.
- Seeing: nearest POI within 1.5× radius, boosted if bearing within ±35°.
