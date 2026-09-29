import subprocess
import sys

def run_adb(cmd):
    full_cmd = f"adb -s 8TCABAIFWOZTDICI {cmd}"
    res = subprocess.run(full_cmd, capture_output=True, text=True, shell=True)
    return res.stdout.strip()

print("Enabling Travel Guide Companion permissions via adb...")

curr_acc = run_adb("shell settings get secure enabled_accessibility_services")
svc_acc = "guide.app/guide.app.companion.TravelGuideAccessibilityService"
if svc_acc not in curr_acc:
    new_acc = f"{curr_acc}:{svc_acc}" if curr_acc and curr_acc != "null" else svc_acc
    run_adb(f'shell settings put secure enabled_accessibility_services "{new_acc}"')
    run_adb("shell settings put secure accessibility_enabled 1")
    print("Added Accessibility Service:", svc_acc)
else:
    print("Accessibility Service was already registered.")

curr_notif = run_adb("shell settings get secure enabled_notification_listeners")
svc_notif = "guide.app/guide.app.navigation.MapsCompanionService"
if svc_notif not in curr_notif:
    new_notif = f"{curr_notif}:{svc_notif}" if curr_notif and curr_notif != "null" else svc_notif
    run_adb(f'shell settings put secure enabled_notification_listeners "{new_notif}"')
    print("Added Notification Listener:", svc_notif)
else:
    print("Notification Listener was already registered.")

print("Verification:")
print("Enabled Accessibility:", run_adb("shell settings get secure enabled_accessibility_services"))
print("Enabled Notification Listeners:", run_adb("shell settings get secure enabled_notification_listeners"))
