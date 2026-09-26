# POI packs (`travel-guide/content/`)

Offline-first content. A pack is one city's stories; the app fully works
from a downloaded pack with zero network (SPEC §1.4).

```
content/
  schema/poi-pack-schema.json   JSON Schema (advisory; enforced by tools/validate_pack.py)
  packs/fort-kochi-walk-v1.json test-city pack v1.1.0 — 24 POIs (20 heritage
    + 2 food + 2 stay), 2 routes
```

## Authoring rules (SPEC §1.5 honesty)

- Every POI needs ≥1 source in `sources[]`.
- Folklore goes in `fun_facts[]` with "Legend says…" wording + `"is_legend": true`
  on the POI when the history itself is disputed. Never present legend as fact.
- `hours: null` = always viewable from outside (exteriors, streets, beach).
- `radius_m` 10–500. Exteriors 40–80, interiors 20–40, streets 60–100.
- Keep `summary` ≤ 140 chars (fits the now-playing sheet).
- `layer` is `heritage` (default) | `food` | `stay` — separate map layers,
  food/stay pins never interrupt heritage narration priority.

## Validation

From `travel-guide/` (stdlib only):

```bash
python tools/validate_pack.py content/packs/fort-kochi-walk-v1.json
python tools/simulate_triggers.py content/packs/fort-kochi-walk-v1.json
```

## Versioning

`pack_version` semver. App shows it on every POI card ("Pack v1.0.0").
Fixes ship as new pack versions; visits store the version that was spoken.
