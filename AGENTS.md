# Agent rules

## Android device access — HARD CONSTRAINT

A physical phone may be connected over ADB for one purpose only: **building,
installing, launching, and verifying the Travel Guide app (`guide.app`)**.

While any device is connected, the agent MUST NOT:

- Read, list, pull, copy, back up, or inspect any file, directory, database,
  photo, media, contact, SMS, call log, clipboard, account, or app data on the
  device.
- Run broad `adb shell`, `adb pull`, `adb push`, `adb backup`, `adb exec-out`,
  `dumpsys` outside `guide.app`, `pm list packages` beyond `guide.app`,
  `logcat` without the `guide.app` PID filter, or read device properties.
- Change any system, secure, or global setting, except adding/removing the two
  Travel Guide companion components in
  `enabled_accessibility_services` and `enabled_notification_listeners`.
- Install, uninstall, clear, or modify any package other than `guide.app`.
- Take screenshots or screen recordings that contain anything outside the
  Travel Guide UI.
- Touch another app, the launcher, Google Maps content, notifications, or
  device settings.

Allowed operations (and nothing else):

- `adb devices` to identify the serial (serial + state only).
- Install `mobile/app/build/outputs/apk/debug/app-debug.apk` with
  `adb install -r`.
- Launch/stop/query `guide.app` with `am` / `pidof` / `pm grant guide.app`.
- Enable the Travel Guide accessibility service and notification listener.
- Read only `guide.app` logs via `logcat --pid=<guide.app pid>`.
- `adb reverse tcp:<port> tcp:<port>` only when the user asks for a local
  proxy/debug endpoint.

Use **only** `tools/adb_control.py`, which enforces the allowlist. Never run
ad-hoc `adb` commands. If a request needs data or control outside `guide.app`,
stop and ask the user first.
