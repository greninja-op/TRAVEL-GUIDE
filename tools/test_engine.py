"""Tests for GuideEngine: ordering, arrive/depart, cooldown, off-route."""
import math
import sys

DWELL, COOLDOWN = 8, 1800


def dist(a, b):
    r = 6371000.0
    d_lat, d_lng = math.radians(b[0] - a[0]), math.radians(b[1] - a[1])
    x = math.sin(d_lat / 2) ** 2 + math.cos(math.radians(a[0])) * math.cos(math.radians(b[0])) * math.sin(d_lng / 2) ** 2
    return 2 * r * math.asin(math.sqrt(x))


class Engine:
    def __init__(self, pois, route):
        self.pois = {p["id"]: p for p in pois}
        self.route = route
        self.inside, self.visited = set(), set()
        self.since, self.spoken = {}, {}

    def on_fix(self, lat, lng, t):
        ordered = [i for i in self.route if i not in self.visited]
        ordered += sorted([i for i in self.pois if i not in ordered],
                          key=lambda i: dist((lat, lng), (self.pois[i]["lat"], self.pois[i]["lng"])))
        spoke = []
        for pid in ordered:
            p = self.pois[pid]
            is_in = dist((lat, lng), (p["lat"], p["lng"])) <= p["radius_m"]
            if not is_in:
                self.since.pop(pid, None)
                continue
            s = self.since.setdefault(pid, t)
            if t - s >= DWELL and (t - self.spoken.get(pid, -COOLDOWN)) >= COOLDOWN:
                spoke.append(pid)
                self.spoken[pid] = t
                self.since.pop(pid, None)
        now_in = {i for i, p in self.pois.items()
                  if dist((lat, lng), (p["lat"], p["lng"])) <= p["radius_m"]}
        arrived, departed = sorted(now_in - self.inside), sorted(self.inside - now_in)
        self.inside = now_in
        self.visited |= set(arrived)
        remaining = [i for i in self.route if i not in self.visited]
        off = bool(self.route) and bool(remaining) and all(
            dist((lat, lng), (self.pois[i]["lat"], self.pois[i]["lng"])) > 100 for i in remaining)
        return spoke, arrived, departed, off


POIS = [
    {"id": "a", "lat": 9.0, "lng": 76.0, "radius_m": 60},
    {"id": "b", "lat": 9.001, "lng": 76.0, "radius_m": 60},
]


def check(name, cond):
    print(f"  {'PASS' if cond else 'FAIL'} {name}")
    return cond


def main():
    ok = True
    e = Engine(POIS, ["a", "b"])
    # dwell at a: first fix starts dwell, second completes it
    s1, a1, d1, _ = e.on_fix(9.0, 76.0, 0)
    s2, a2, d2, _ = e.on_fix(9.0, 76.0, DWELL)
    ok &= check("dwell then speak", s1 == [] and s2 == ["a"])
    ok &= check("arrival recorded on enter", a1 == ["a"])
    # leave a -> departure closes visit
    _, _, d3, off = e.on_fix(9.005, 76.005, 2 * DWELL)
    ok &= check("departure recorded on exit", d3 == ["a"])
    # immediate re-enter: no re-speak (cooldown)
    e.on_fix(9.0, 76.0, 2 * DWELL + 1)
    s4, _, _, _ = e.on_fix(9.0, 76.0, 2 * DWELL + 1 + DWELL)
    ok &= check("cooldown blocks re-speak", s4 == [])
    # far from remaining route stop -> off-route
    _, _, _, off2 = e.on_fix(9.05, 76.05, 3 * DWELL)
    ok &= check("off-route detected when far", off2)
    print("ALL PASS" if ok else "SOME FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
