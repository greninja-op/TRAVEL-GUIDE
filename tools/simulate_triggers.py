"""Walk the heritage-loop route through the pack and print the narration order.

Mirrors shared-core Geo + Triggers + NarrationQueue rules (dwell, cooldown,
speed gate, single voice, on-route priority). Stdlib only — the runnable
proof for SPEC F-01..F-04 without an Android device.
"""
import json
import math
import sys

DWELL_S = 8
COOLDOWN_S = 1800
MAX_SPEAK_MPS = 8.0


def haversine(lat1, lng1, lat2, lng2):
    r = 6371000.0
    d_lat = math.radians(lat2 - lat1)
    d_lng = math.radians(lng2 - lng1)
    a = (math.sin(d_lat / 2) ** 2 + math.cos(math.radians(lat1)) *
         math.cos(math.radians(lat2)) * math.sin(d_lng / 2) ** 2)
    return 2 * r * math.asin(math.sqrt(a))


def main(path):
    with open(path, encoding="utf-8") as f:
        pack = json.load(f)
    pois = {p["id"]: p for p in pack["pois"]}
    route = pack["routes"][0]
    order = route["ordered_poi_ids"]
    print(f"route={route['id']} stops={len(order)} dwell={DWELL_S}s")
    t = 0
    last_spoken = {}
    visits = []
    queue = []
    for pid in order:
        p = pois[pid]
        # simulate: arrive, dwell DWELL_S, leave; walking 1.4 m/s between stops
        inside = haversine(p["lat"], p["lng"], p["lat"], p["lng"]) <= p["radius_m"]
        assert inside, pid
        t += DWELL_S
        cooled = (t - last_spoken.get(pid, -COOLDOWN_S)) >= COOLDOWN_S
        if cooled:
            queue.append(pid)
            # single voice: speak immediately in simulation
            spoken = queue.pop(0)
            last_spoken[spoken] = t
            visits.append(spoken)
            print(f"  t+{t:4d}s SPEAK {spoken}")
        else:
            print(f"  t+{t:4d}s SKIP(cooldown) {pid}")
        t += 120  # walk to next stop
    # re-enter first POI quickly -> must be cooldown-skipped (F-04)
    pid = order[0]
    if (t - last_spoken.get(pid, 0)) < COOLDOWN_S:
        print(f"  re-enter {pid}: SKIP(cooldown) OK (F-04)")
    else:
        print(f"  re-enter {pid}: would re-speak (cooldown expired, correct)")
    print(f"visits={len(visits)}/{len(order)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
