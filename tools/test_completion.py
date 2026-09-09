"""Tests for quiet hours, visited-rerank, layer filter. Stdlib only.
Mirrors QuietHours.kt / VisitRank.kt.
"""
import sys


def is_quiet(now, window):
    if not window or not window.get("enabled", True):
        return False
    s, e = window["start"], window["end"]
    if s <= e:
        return s <= now <= e
    return now >= s or now <= e


def rank(nearby, visited, now=None, layer=None):
    items = [(p, d) for p, d in nearby if layer is None or p.get("layer", "heritage") == layer]

    def closed(p):
        h = p.get("hours")
        return bool(now and h and not (h["open"] <= now <= h["close"]))

    return [p for p, _ in sorted(items, key=lambda t: (t[0]["id"] in visited, closed(t[0]), t[1]))]


def check(name, cond):
    print(f"  {'PASS' if cond else 'FAIL'} {name}")
    return cond


def main():
    ok = True
    ok &= check("quiet inside window", is_quiet("23:00", {"start": "22:00", "end": "07:00"}))
    ok &= check("not quiet midday", not is_quiet("12:00", {"start": "22:00", "end": "07:00"}))
    ok &= check("overnight wrap (06:00 quiet)", is_quiet("06:00", {"start": "22:00", "end": "07:00"}))
    ok &= check("disabled window never quiet",
                not is_quiet("23:00", {"start": "22:00", "end": "07:00", "enabled": False}))
    pois = [
        ({"id": "visited-near", "hours": None}, 10.0),
        ({"id": "fresh-far", "hours": None}, 200.0),
        ({"id": "fresh-near-closed", "hours": {"open": "09:00", "close": "12:00"}}, 20.0),
    ]
    ranked = [p["id"] for p in rank(pois, {"visited-near"}, now="10:00")]
    ok &= check("visited sinks below unvisited", ranked[-1] == "visited-near")
    ok &= check("nearest open unvisited first", ranked[0] == "fresh-near-closed")
    ranked2 = [p["id"] for p in rank(pois, {"visited-near"}, now="13:00")]
    ok &= check("closed sinks below open", ranked2.index("fresh-far") < ranked2.index("fresh-near-closed"))
    layered = [
        ({"id": "h1", "layer": "heritage"}, 50.0),
        ({"id": "f1", "layer": "food"}, 10.0),
    ]
    ok &= check("layer filter isolates food pins",
                [p["id"] for p in rank(layered, set(), layer="food")] == ["f1"])
    print("ALL PASS" if ok else "SOME FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
