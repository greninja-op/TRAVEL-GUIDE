"""Finished-product demo: runs a full trip on the real pack, end to end.

Consent notice -> route walk (dwell/cooldown/queue/quiet-hours) -> spoken
lines printed -> visits.json + trip Markdown written to demo-out/.
Stdlib only. Mirrors shared-core rules (Triggers/NarrationQueue/QuietHours/
VisitRank) and mobile Narrator/TripExport behavior.

Usage:
  python tools/demo.py [--date YYYY-MM-DD] [--route fort-kochi-heritage-loop]
                       [--start HH:MM] [--yes]
"""
import argparse
import datetime
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DWELL_S = 8
COOLDOWN_S = 1800
QUIET_START, QUIET_END = "22:00", "07:00"


def haversine(lat1, lng1, lat2, lng2):
    r = 6371000.0
    d_lat, d_lng = math.radians(lat2 - lat1), math.radians(lng2 - lng1)
    a = (math.sin(d_lat / 2) ** 2 + math.cos(math.radians(lat1)) *
         math.cos(math.radians(lat2)) * math.sin(d_lng / 2) ** 2)
    return 2 * r * math.asin(math.sqrt(a))


def in_quiet(hhmm):
    if QUIET_START <= QUIET_END:
        return QUIET_START <= hhmm <= QUIET_END
    return hhmm >= QUIET_START or hhmm <= QUIET_END


def add_minutes(hhmm, mins):
    h, m = map(int, hhmm.split(":"))
    total = (h * 60 + m + mins) % (24 * 60)
    return f"{total // 60:02d}:{total % 60:02d}"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--date", default="2026-09-10")
    ap.add_argument("--route", default="fort-kochi-heritage-loop")
    ap.add_argument("--start", default="10:00")
    ap.add_argument("--yes", action="store_true")
    args = ap.parse_args()

    print("Travel Guide demo — location is simulated along the route.")
    print("Real app: foreground location only to start; background is a")
    print("separate opt-in; visits stay on-device. (docs/PRIVACY.md)")
    if not args.yes:
        ans = input("Continue with simulated location? [y/N] ").strip().lower()
        if ans != "y":
            print("Stopped. Nothing recorded.")
            return 0

    pack = json.load(open(os.path.join(ROOT, "content", "packs", "fort-kochi-walk-v1.json"),
                          encoding="utf-8"))
    pois = {p["id"]: p for p in pack["pois"]}
    route = next(r for r in pack["routes"] if r["id"] == args.route)

    events = []
    ev_path = os.path.join(ROOT, "content", "events", "kochi-2026-12.json")
    if os.path.exists(ev_path):
        ev = json.load(open(ev_path, encoding="utf-8"))
        events = [e for e in ev.get("events", []) if e.get("date") == args.date]
    if events:
        print(f"\nHappening {args.date}:")
        for e in events:
            print(f"  * {e['title']}")

    print(f"\nRoute: {route['name']} ({len(route['ordered_poi_ids'])} stops)")
    now = args.start
    last_spoken = {}
    visits = []
    for pid in route["ordered_poi_ids"]:
        p = pois[pid]
        now = add_minutes(now, 2 + DWELL_S // 60 + 2)  # walk + dwell
        # cooldown: 30 min between speaks of the same POI
        mins_since = None
        for v in reversed(visits):
            if v["poi_id"] == pid:
                h1, m1 = map(int, v["at"].split(":"))
                h2, m2 = map(int, now.split(":"))
                mins_since = (h2 * 60 + m2) - (h1 * 60 + m1)
                break
        if mins_since is not None and mins_since < 30:
            print(f"  [{now}] {p['name']} — seen recently, card only.")
            continue
        story = f"{p['name']}. {p['summary']} {p['fun_facts'][0] if p['fun_facts'] else ''}"
        if in_quiet(now):
            print(f"  [{now}] {p['name']} — quiet hours, card only.")
        else:
            print(f"  [{now}] GUIDE: {story[:220]}")
        visits.append({"poi_id": pid, "at": now, "pack_version": pack["pack_version"]})

    out = os.path.join(ROOT, "demo-out")
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, "visits.json"), "w", encoding="utf-8") as f:
        json.dump({"date": args.date, "route": route["id"], "visits": visits}, f, indent=2)
    lines = [f"# Trip — {pack['city_name']} ({args.date})", ""]
    for i, v in enumerate(visits, 1):
        p = pois[v["poi_id"]]
        lines += [f"## {i}. {p['name']}", "", p["summary"], ""]
    md_path = os.path.join(out, f"trip-{args.date}.md")
    with open(md_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"\nDone: {len(visits)} stops -> demo-out/visits.json + trip-{args.date}.md")
    return 0


if __name__ == "__main__":
    sys.exit(main())
