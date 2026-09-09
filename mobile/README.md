# mobile (`travel-guide/mobile/`)

Android native, Kotlin + Compose (SPEC §4.1). Offline-first; no account.

## Open in Android Studio

Open `travel-guide/mobile/` (Gradle root). Run `app` on a device with GPS
(emulator: extended controls → location → route playback for the loop).

## Where things are

- `app/src/main/AndroidManifest.xml` — fine/coarse now; background location
  only requested from Settings (Phase 1), never at launch
- `app/.../MainActivity.kt` — consent gate + 6 tabs + detail/phrasebook,
  MapView lifecycle forwarded, always-visible mute
- `app/.../ui/` — MapScreen (MapLibre offline), NearbyScreen (radar + layers
  + events + seeing), PoiDetailScreen (hours/closed badge C-02), RoutesScreen
  (day-plan picker), PacksScreen, HistoryScreen (notes + export), SettingsScreen
  (battery picker, bg-location opt-in, quiet hours), ConsentScreen, VoiceSettings,
  PhrasebookScreen, NowPlayingSheet
- `app/.../location/` — GuideService (foreground) + LocationTracker (battery
  profiles) + HeadingSensor (seeing)
- `app/.../voice/Narrator.kt` — on-device TTS, single voice, <500ms mute
- `app/.../map/OfflinePackHelper.kt` — MapLibre tile-pyramid download (12..16)
- `app/.../packs|export|power|extras|trip/` — PackManager, TripExport,
  BatteryProfiles, phrasebook/events, TripModes
- `app/.../ui/theme/Tokens.kt` — DESIGN-TOKENS.md as Compose values
- `app/.../data/Entities.kt` — Room mirror of shared-core Models
- `app/.../data/PackLoader.kt` — bundled pack reader (Phase 3: packs manager)
- Phase 1: `location/GuideService.kt` + `voice/Narrator.kt` + `trip/` modes
- Phase 2: POI detail see-lists, itineraries, phrasebook, notes, events
- Phase 3: PackManager, TripExport, BatteryProfiles, a11y pass

Bundled pack copy: `app/src/main/assets/packs/fort-kochi-walk-v1.json`
is synced from `content/packs/` at release time (same bytes, verified).
