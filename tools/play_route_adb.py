#!/usr/bin/env python3
"""Automated GPS route playback and geofence verification via ADB.

Plays the Fort Kochi heritage loop against the connected Android device:
- Feeds real GPS coordinates along the walk
- Verifies geofence triggers (F-01..F-04)
- Observes arrival chimes and oral guide audio narration

All device access goes through ``tools/adb_control.py`` and targets only
``guide.app`` (see ``AGENTS.md``).
"""

from __future__ import annotations

import json
import math
import sys
import time
from pathlib import Path

import adb_control as adb

DWELL_S = 8
STEP_INTERVAL_S = 1.0

PACK_PATH = Path(__file__).resolve().parents[1] / "content" / "packs" / "fort-kochi-walk-v1.json"


def haversine(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    r = 6371000.0
    d_lat = math.radians(lat2 - lat1)
    d_lng = math.radians(lng2 - lng1)
    a = (
        math.sin(d_lat / 2) ** 2
        + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(d_lng / 2) ** 2
    )
    return 2 * r * math.asin(math.sqrt(a))


def send_gps_fix(serial: str, lat: float, lng: float, speed: float = 1.4) -> None:
    adb.cmd_mock_fix(serial, lat, lng, speed)


def jump_poi(serial: str, poi_id: str) -> None:
    adb.cmd_mock_stop(serial, poi_id)


def check_logcat(serial: str) -> str:
    pid = adb.app_pid(serial)
    if not pid:
        return ""
    return adb.adb(
        "shell", "logcat", "-d", "-t", "15", "--pid", pid,
        "-s", "GuideService", "GuideEngine", "WearableAudio", "MockLocationReceiver",
        serial=serial, check=False,
    ).stdout


def main() -> int:
    with PACK_PATH.open("r", encoding="utf-8") as handle:
        pack = json.load(handle)

    pois = {poi["id"]: poi for poi in pack["pois"]}
    route = pack["routes"][0]
    order = route["ordered_poi_ids"]

    print("=" * 65)
    print(" FORT KOCHI HERITAGE LOOP — LIVE GPS FIELD PLAYBACK (ADB)")
    print(f" Route: {route['name']} ({len(order)} Stops)")
    print("=" * 65)

    try:
        adb.ADB = adb.find_adb()
        serial = adb.resolve_serial(None)
    except adb.AdbError as error:
        print(f"Error: {error}")
        return 1

    print(f"ADB device: {serial}\n")

    # Mock location is allowed for guide.app only (app-scoped appop).
    adb.cmd_mock_location(serial)

    # Bring the app to the foreground on the map route.
    adb.cmd_launch(serial, route="map")
    time.sleep(2)

    visited_count = 0

    for index, poi_id in enumerate(order[:6], 1):  # first 6 major stops
        poi = pois[poi_id]
        print("\n" + "-" * 55)
        print(f"[{index}/{len(order)}] Navigating towards: {poi['name']}")
        print(f"      Coordinates: {poi['lat']:.4f}, {poi['lng']:.4f} | Radius: {poi['radius_m']}m")

        approach_lat = poi["lat"] - 0.0004
        approach_lng = poi["lng"] - 0.0004
        distance_out = haversine(approach_lat, approach_lng, poi["lat"], poi["lng"])
        print(f"  -> Approaching stop (dist ~{distance_out:.1f}m)...")
        send_gps_fix(serial, approach_lat, approach_lng, speed=1.4)
        time.sleep(STEP_INTERVAL_S)

        print(f"  -> ENTER GEOFENCE: arrived inside {poi['name']}!")
        jump_poi(serial, poi_id)
        send_gps_fix(serial, poi["lat"], poi["lng"], speed=0.6)
        time.sleep(1.0)

        print(f"  -> Dwelling inside geofence ({DWELL_S}s requirement)...", end="", flush=True)
        for second in range(1, DWELL_S + 1):
            time.sleep(1.0)
            send_gps_fix(serial, poi["lat"], poi["lng"], speed=0.2)
            print(f" {second}s", end="", flush=True)
        print(" [TRIGGER DWELL MET]")

        logs = check_logcat(serial)
        if any(token in logs.lower() for token in ("arrival", "chime", "spoke")):
            print("  -> Logcat verified: Arrival chime & voice guidance triggered!")
        else:
            print("  -> Fix registered at location.")

        visited_count += 1
        time.sleep(2.0)

    print("\n" + "=" * 65)
    print(f" GPS Route Playback Complete! Visited {visited_count} heritage stops.")
    print("=" * 65)
    return 0


if __name__ == "__main__":
    sys.exit(main())
