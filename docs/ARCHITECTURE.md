# Architecture (`travel-guide/docs/ARCHITECTURE.md`)

```
GPS + heading → shared-core Geo/Triggers (enter/dwell/exit, cooldown, speed gate)
  → NarrationQueue (one voice: on-route > nearest > event, dedupe)
  → mobile Narrator (on-device TTS; cloud voice only if user opts in)
  → Now-Playing sheet + transcript card → Room visits → Trip timeline
```

## Modules

| Module | Role | Verifiable without device |
|---|---|---|
| `content/` | versioned POI packs + schema | `tools/validate_pack.py` |
| `shared-core/` | pure-Kotlin rules (no Android imports) | `tools/simulate_triggers.py` mirrors them |
| `mobile/` | Compose UI, Room, location service, TTS | Android Studio build |
| `tools/` | stdlib-only mirrors of core rules | `python tools/*.py` |

Pack flow: author JSON in `content/packs/` → validate → copy same bytes to
`mobile/app/src/main/assets/packs/` → PackLoader reads at runtime →
Phase 3 PackManager adds download/delete/sizes around it.
