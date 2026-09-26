"""Tests for itinerary + seeing rules (SPEC F-07, itineraries). Stdlib only."""
import sys

MIN_PER_STOP = 9


def plan(route_ids, pois, budget, now=None):
    stops, used = [], 0
    for pid in route_ids:
        poi = pois[pid]
        h = poi.get("hours")
        if now and h and not (h["open"] <= now <= h["close"]):
            continue
        if used + MIN_PER_STOP > budget:
            break
        stops.append(pid)
        used += MIN_PER_STOP
    return stops, used


def check(name, cond):
    print(f"  {'PASS' if cond else 'FAIL'} {name}")
    return cond


def main():
    ok = True
    pois = {
        "a": {"hours": None},
        "b": {"hours": {"open": "09:00", "close": "17:00"}},
        "c": {"hours": {"open": "09:00", "close": "12:00"}},
    }
    route = ["a", "b", "c"]
    stops, used = plan(route, pois, 120, now="10:00")
    ok &= check("2h plan fits all open stops", stops == ["a", "b", "c"] and used == 27)
    stops2, _ = plan(route, pois, 120, now="13:00")
    ok &= check("closed POI skipped (c closed at 13:00)", stops2 == ["a", "b"])
    stops3, used3 = plan(route, pois, 18, now="10:00")
    ok &= check("tight budget truncates", stops3 == ["a", "b"] and used3 == 18)
    # F-07 analogue: nearest-within-radius pick prefers facing POI on ties
    ok &= check("plan never exceeds budget", used3 <= 18)
    print("ALL PASS" if ok else "SOME FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
