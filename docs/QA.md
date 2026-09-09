# Beta QA (`travel-guide/docs/QA.md`)

Map to SPEC §6. Device-needed rows are manual; logic rows run in `tools/`.

## Runnable now

```bash
python tools/validate_pack.py content/packs/fort-kochi-walk-v1.json
python tools/simulate_triggers.py content/packs/fort-kochi-walk-v1.json
python tools/test_queue.py        # F-02, F-04, F-08
python tools/test_itinerary.py    # itineraries, closed-skip, budget
python tools/export_trip.py content/packs/fort-kochi-walk-v1.json 2026-09-10 \
  vasco-square chinese-fishing-nets st-francis-church
```

## Manual (device)

- F-01/F-03/F-05/F-06/F-07, P-01..P-03, C-01/C-02: walk the loop once with a
  checklist build; record narration timing + accuracy.
- U-01..U-06: dropdown/calendar/button consistency, TalkBack pass, 130% font,
  min-width resize. Every Compose screen has contentDescriptions (a11y).
- E-01..E-05: 3h soak, noisy street, start/stop spam, storage-full, mic-denied.

## Release notes template (`docs/RELEASE-NOTES-TEMPLATE.md`)

Version / pack version / what's new / known issues / battery notes.
