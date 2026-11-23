---
name: Travel Guide device control
description: Install, launch, and verify the Travel Guide app on a USB-connected Android phone through a strictly allowlisted ADB script. Use for any device, install, launch, logcat, companion-service, or adb reverse request.
---

# Travel Guide device control

## Hard boundary

The connected phone is used **only** for the Travel Guide app (`guide.app`).
Never read files/data from it, never inspect other apps, and never change
settings unrelated to Travel Guide. The full policy is in `AGENTS.md`.

## Use the controller, never raw adb

All device work goes through `tools/adb_control.py`. It refuses anything
outside the allowlist and always targets one explicit device serial.

```powershell
# 1. Check adb + the connected device (serial/state and APK only)
python tools/adb_control.py doctor

# 2. Build the APK first (repo root -> mobile)
cd mobile
.\gradlew.bat :app:assembleDebug
cd ..

# 3. Install, grant app permissions, enable companions, launch
python tools/adb_control.py setup

# Or wait for the phone to be plugged in, then run setup automatically
python tools/adb_control.py wait --timeout 900

# 4. Verify the app process and version
python tools/adb_control.py status

# 5. App-only logs (filtered to the guide.app PID)
python tools/adb_control.py logs --lines 200

# 6. Optional: local proxy/debug port
python tools/adb_control.py reverse --port 8080

# Stop / reinstall / remove only the app
python tools/adb_control.py stop
python tools/adb_control.py install
python tools/adb_control.py uninstall
```

If more than one device is attached, pass `--serial <serial>`.
If the device shows `unauthorized`, ask the user to tap **Allow USB debugging**
on the phone; do not try to bypass it.

## What `setup` does

1. `adb install -r` the debug APK.
2. Grants `POST_NOTIFICATIONS`, `ACCESS_FINE_LOCATION`,
   `ACCESS_COARSE_LOCATION` (and tries background location).
3. Appends the Travel Guide accessibility service and notification listener to
   their secure settings lists **without removing other entries**, and prints
   only whether the Travel Guide entry was added (never the other entries).
4. Launches `guide.app/.MainActivity`.

## Never do

- `adb pull`, `adb push`, `adb backup`, `adb exec-out`, or `adb shell ls/cat`.
- General `logcat`, `dumpsys` outside `guide.app`, or reading properties.
- `pm`/`settings` changes for anything except `guide.app` and the two
  companion entries above.
- Screenshots or screen recordings outside the Travel Guide UI.

If the user asks for a disallowed action, refuse and explain that it is outside
the Travel Guide–only device policy.
