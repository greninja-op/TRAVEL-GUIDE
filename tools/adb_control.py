#!/usr/bin/env python3
"""Allowlisted ADB controller for the Travel Guide app only.

HARD POLICY (see AGENTS.md): the connected phone may only be used to install,
launch, and verify ``guide.app``. This script deliberately exposes no command
that reads device files/data, inspects other apps, or changes unrelated
settings.

Usage:
    python tools/adb_control.py doctor
    python tools/adb_control.py setup
    python tools/adb_control.py install|launch|grant|companion|status
    python tools/adb_control.py logs [--lines 200] [--follow]
    python tools/adb_control.py stop|uninstall|clear
    python tools/adb_control.py reverse --port 8080
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

APP_ID = "guide.app"
ACTIVITY = f"{APP_ID}/.MainActivity"
ACCESSIBILITY_COMPONENT = f"{APP_ID}/{APP_ID}.companion.TravelGuideAccessibilityService"
NOTIFICATION_COMPONENT = f"{APP_ID}/{APP_ID}.navigation.MapsCompanionService"

REPO_ROOT = Path(__file__).resolve().parents[1]
APK_PATH = REPO_ROOT / "mobile" / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"

APP_PERMISSIONS = (
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
)
BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"


class AdbError(RuntimeError):
    pass


def find_adb() -> str:
    candidates: list[Path | str] = []
    for env in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        root = os.environ.get(env)
        if root:
            candidates.append(Path(root) / "platform-tools" / "adb.exe")
            candidates.append(Path(root) / "platform-tools" / "adb")
    local_appdata = os.environ.get("LOCALAPPDATA")
    if local_appdata:
        candidates.append(Path(local_appdata) / "Android" / "Sdk" / "platform-tools" / "adb.exe")
    candidates.append("adb")

    for candidate in candidates:
        if isinstance(candidate, Path):
            if candidate.is_file():
                return str(candidate)
        else:
            found = shutil.which(candidate)
            if found:
                return found
    raise AdbError("adb not found. Install Android platform-tools or set ANDROID_HOME.")


ADB = None


def adb(*args: str, serial: str | None = None, check: bool = True) -> subprocess.CompletedProcess:
    if ADB is None:
        raise AdbError("adb was not initialized")
    cmd = [ADB]
    if serial:
        cmd += ["-s", serial]
    cmd += list(args)
    result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if check and result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        raise AdbError(f"adb {' '.join(args)} failed: {detail}")
    return result


def connected_devices() -> list[tuple[str, str]]:
    """Return (serial, state) pairs only. No device data is read."""
    result = adb("devices", serial=None)
    devices: list[tuple[str, str]] = []
    for line in result.stdout.splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] in {"device", "unauthorized", "offline", "bootloader"}:
            devices.append((parts[0], parts[1]))
    return devices


def resolve_serial(explicit: str | None) -> str:
    devices = connected_devices()
    if explicit:
        for serial, state in devices:
            if serial == explicit:
                if state != "device":
                    raise AdbError(
                        f"Device {serial} is '{state}'. Unlock the phone and accept the USB debugging prompt."
                    )
                return serial
        raise AdbError(f"Device {serial} is not attached. Check the USB cable.")

    ready = [serial for serial, state in devices if state == "device"]
    if not ready:
        if devices:
            states = ", ".join(f"{s} ({st})" for s, st in devices)
            raise AdbError(f"No authorized device. Found: {states}. Accept the USB debugging prompt.")
        raise AdbError("No Android device detected. Connect the phone over USB with USB debugging enabled.")
    if len(ready) > 1:
        raise AdbError(f"Multiple devices attached ({', '.join(ready)}). Pass --serial <serial>.")
    return ready[0]


def require_apk() -> Path:
    if not APK_PATH.is_file():
        raise AdbError(
            f"APK not found at {APK_PATH}. Build it first: cd mobile && gradlew.bat :app:assembleDebug"
        )
    return APK_PATH


def cmd_doctor(serial: str | None) -> int:
    print("ADB:", ADB)
    version = adb("version", serial=None).stdout.splitlines()
    if version:
        print("Version:", version[0].strip())

    devices = connected_devices()
    if devices:
        print("Devices:")
        for dev_serial, state in devices:
            print(f"  - {dev_serial} [{state}]")
    else:
        print("Devices: none")
        print("Connect the phone over USB, enable Developer options -> USB debugging, then approve the prompt.")

    if APK_PATH.is_file():
        print(f"APK: {APK_PATH} ({APK_PATH.stat().st_size / (1024 * 1024):.1f} MiB)")
    else:
        print("APK: missing (build with: cd mobile && gradlew.bat :app:assembleDebug)")

    try:
        resolved = resolve_serial(serial)
        print("Target:", resolved)
        return 0
    except AdbError as error:
        print("Target: unavailable —", error)
        return 1


def cmd_install(serial: str) -> int:
    apk = require_apk()
    print(f"Installing {apk.name} on {serial} ...")
    result = adb("install", "-r", str(apk), serial=serial)
    print((result.stdout or result.stderr).strip())
    return 0


def cmd_grant(serial: str) -> int:
    for permission in APP_PERMISSIONS:
        result = adb("shell", "pm", "grant", APP_ID, permission, serial=serial, check=False)
        output = (result.stdout + result.stderr).strip()
        if result.returncode == 0:
            print(f"granted: {permission}")
        elif "Unknown permission" in output:
            print(f"skipped (not present on this Android version): {permission}")
        else:
            print(f"not granted: {permission} (grant it from the app UI)")

    result = adb("shell", "pm", "grant", APP_ID, BACKGROUND_LOCATION, serial=serial, check=False)
    if result.returncode == 0:
        print(f"granted: {BACKGROUND_LOCATION}")
    else:
        print("background location: choose 'Allow all the time' from Travel Guide Settings if needed")
    return 0


def _ensure_component(serial: str, setting: str, component: str, label: str) -> None:
    current = adb("shell", "settings", "get", "secure", setting, serial=serial).stdout.strip()
    entries = [entry for entry in current.split(":") if entry]
    if component in entries:
        print(f"{label}: already enabled")
        return
    entries.append(component)
    # Write the full list back so other apps' entries are preserved. Only the
    # Travel Guide entry and the resulting boolean are ever printed.
    adb("shell", "settings", "put", "secure", setting, ":".join(entries), serial=serial)
    print(f"{label}: enabled")


def cmd_companion(serial: str) -> int:
    _ensure_component(
        serial,
        "enabled_accessibility_services",
        ACCESSIBILITY_COMPONENT,
        "accessibility service",
    )
    adb("shell", "settings", "put", "secure", "accessibility_enabled", "1", serial=serial)
    _ensure_component(
        serial,
        "enabled_notification_listeners",
        NOTIFICATION_COMPONENT,
        "notification listener",
    )
    print("Companion services configured for guide.app only.")
    return 0


def cmd_launch(serial: str, route: str | None = None) -> int:
    command = ["shell", "am", "start", "-W", "-n", ACTIVITY]
    if route:
        command += ["--es", "route", route]
    result = adb(*command, serial=serial)
    print((result.stdout or result.stderr).strip())
    return 0


def cmd_stop(serial: str) -> int:
    adb("shell", "am", "force-stop", APP_ID, serial=serial)
    print(f"Stopped {APP_ID}.")
    return 0


def cmd_clear(serial: str) -> int:
    adb("shell", "pm", "clear", APP_ID, serial=serial)
    print(f"Cleared app data for {APP_ID} only.")
    return 0


def cmd_uninstall(serial: str) -> int:
    adb("uninstall", APP_ID, serial=serial)
    print(f"Uninstalled {APP_ID}.")
    return 0


def app_pid(serial: str) -> str | None:
    result = adb("shell", "pidof", APP_ID, serial=serial, check=False)
    pid = result.stdout.strip()
    return pid or None


def cmd_status(serial: str) -> int:
    installed = adb("shell", "pm", "list", "packages", APP_ID, serial=serial).stdout.strip()
    print(f"installed: {'yes' if installed else 'no'}")
    pid = app_pid(serial)
    print(f"running: {'yes (pid ' + pid + ')' if pid else 'no'}")

    # app-scoped metadata only; no other package is inspected
    dump = adb("shell", "dumpsys", "package", APP_ID, serial=serial, check=False).stdout
    for line in dump.splitlines():
        stripped = line.strip()
        if stripped.startswith("versionName=") or stripped.startswith("versionCode="):
            print(stripped)
    return 0


def cmd_logs(serial: str, lines: int, follow: bool) -> int:
    pid = app_pid(serial)
    if not pid:
        print(f"{APP_ID} is not running. Launch it with: python tools/adb_control.py launch")
        return 1
    cmd = ["shell", "logcat", "--pid", pid, "-v", "time", "-t", str(lines)]
    if not follow:
        cmd.insert(2, "-d")
    print(f"Showing {APP_ID} logs only (pid {pid})...")
    if follow:
        subprocess.run([ADB, "-s", serial, *cmd], check=False)
    else:
        result = adb(*cmd, serial=serial, check=False)
        print(result.stdout.rstrip())
    return 0


def cmd_mock_location(serial: str) -> int:
    """Allow mock locations for guide.app only (app-scoped appop)."""
    adb("shell", "appops", "set", APP_ID, "android:mock_location", "allow", serial=serial)
    print(f"Mock location enabled for {APP_ID}.")
    return 0


def cmd_mock_fix(serial: str, lat: float, lng: float, speed: float) -> int:
    adb(
        "shell", "am", "broadcast", "-p", APP_ID,
        "-a", "guide.app.location.MOCK_FIX",
        "--ef", "lat", f"{lat:.6f}",
        "--ef", "lng", f"{lng:.6f}",
        "--ef", "speed", f"{speed:.1f}",
        serial=serial,
    )
    return 0


def cmd_mock_stop(serial: str, poi_id: str) -> int:
    adb(
        "shell", "am", "broadcast", "-p", APP_ID,
        "-a", "guide.app.location.SIMULATE_STOP",
        "--es", "poi_id", poi_id,
        serial=serial,
    )
    return 0


def cmd_reverse(serial: str, port: int) -> int:
    adb("reverse", f"tcp:{port}", f"tcp:{port}", serial=serial)
    print(f"adb reverse set: device tcp:{port} -> host tcp:{port}")
    return 0


def cmd_setup(serial: str) -> int:
    cmd_install(serial)
    cmd_grant(serial)
    cmd_companion(serial)
    cmd_launch(serial)
    print("Setup complete. The Travel Guide app is installed and launched.")
    return 0


def cmd_wait(serial: str | None, timeout: int, run_setup: bool) -> int:
    """Wait for a USB device, then optionally run the allowlisted setup."""
    print(f"Waiting up to {timeout}s for a USB device (USB debugging enabled) ...")
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            resolved = resolve_serial(serial)
        except AdbError:
            time.sleep(2)
            continue
        print(f"Device connected: {resolved}")
        if run_setup:
            return cmd_setup(resolved)
        return cmd_status(resolved)
    print("Timed out waiting for a device.")
    return 1


def main() -> int:
    global ADB
    parser = argparse.ArgumentParser(description="Travel Guide-only ADB controller")
    parser.add_argument("--serial", help="Target device serial (auto-detected when one device is attached)")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("doctor", help="Show adb, device state, and APK presence")
    sub.add_parser("setup", help="Install, grant app permissions, enable companions, launch")
    sub.add_parser("install", help="Install the debug APK")
    sub.add_parser("grant", help="Grant app runtime permissions")
    sub.add_parser("companion", help="Enable the Travel Guide companion services")
    launch = sub.add_parser("launch", help="Launch guide.app")
    launch.add_argument("--route", help="Optional route deep-link value, e.g. map")
    sub.add_parser("mock-location", help="Allow mock location for guide.app only")
    sub.add_parser("status", help="Show install/running state and app version")
    sub.add_parser("stop", help="Force-stop guide.app")
    sub.add_parser("clear", help="Clear guide.app data")
    sub.add_parser("uninstall", help="Uninstall guide.app")

    logs = sub.add_parser("logs", help="Show guide.app-only logs")
    logs.add_argument("--lines", type=int, default=200)
    logs.add_argument("--follow", action="store_true")

    reverse = sub.add_parser("reverse", help="Set adb reverse for a local debug port")
    reverse.add_argument("--port", type=int, required=True)

    mock_fix = sub.add_parser("mock-fix", help="Send a mock GPS fix to guide.app")
    mock_fix.add_argument("--lat", type=float, required=True)
    mock_fix.add_argument("--lng", type=float, required=True)
    mock_fix.add_argument("--speed", type=float, default=1.4)

    mock_stop = sub.add_parser("mock-stop", help="Simulate arrival at a POI in guide.app")
    mock_stop.add_argument("--poi-id", required=True)

    wait = sub.add_parser("wait", help="Wait for a USB device, then run setup")
    wait.add_argument("--timeout", type=int, default=900)
    wait.add_argument("--no-setup", action="store_true", help="Only report status after connecting")

    args = parser.parse_args()

    try:
        ADB = find_adb()
        if args.command == "doctor":
            return cmd_doctor(args.serial)
        if args.command == "wait":
            return cmd_wait(args.serial, args.timeout, not args.no_setup)

        serial = resolve_serial(args.serial)
        handlers = {
            "setup": lambda: cmd_setup(serial),
            "install": lambda: cmd_install(serial),
            "grant": lambda: cmd_grant(serial),
            "companion": lambda: cmd_companion(serial),
            "launch": lambda: cmd_launch(serial, args.route),
            "mock-location": lambda: cmd_mock_location(serial),
            "mock-fix": lambda: cmd_mock_fix(serial, args.lat, args.lng, args.speed),
            "mock-stop": lambda: cmd_mock_stop(serial, args.poi_id),
            "status": lambda: cmd_status(serial),
            "stop": lambda: cmd_stop(serial),
            "clear": lambda: cmd_clear(serial),
            "uninstall": lambda: cmd_uninstall(serial),
            "logs": lambda: cmd_logs(serial, args.lines, args.follow),
            "reverse": lambda: cmd_reverse(serial, args.port),
        }
        return handlers[args.command]()
    except AdbError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
