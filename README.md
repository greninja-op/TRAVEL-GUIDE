# Travel Guide — Autonomous On-Device Heritage Companion & Spoken Tour Engine

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android%208.0+%20(API%2026--35)-brightgreen?style=for-the-badge&logo=android)
![Language](https://img.shields.io/badge/Language-Kotlin%202.0.21-purple?style=for-the-badge&logo=kotlin)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose)
![Maps](https://img.shields.io/badge/Cartography-Google%20Maps%20SDK-EA4335?style=for-the-badge&logo=googlemaps)
![Voice](https://img.shields.io/badge/Voice%20AI-Sarvam%20AI%20Neural%20TTS-orange?style=for-the-badge)
![Security](https://img.shields.io/badge/Security-Keystore%20AES--256--GCM%20%7C%20OWASP%20Hardened-blue?style=for-the-badge&logo=security)
![License](https://img.shields.io/badge/License-Apache%202.0%20%2F%20MIT-lightgrey?style=for-the-badge)

<p align="center">
  <b>An intelligent, context-aware audio companion that speaks what you are seeing as you explore ancient streets, monuments, and cultural quarters — with zero cloud tracking, authentic Google Maps integration, and natural multilingual voice synthesis.</b>
</p>

</div>

---

## Table of Contents

1. [Executive Overview](#1-executive-overview)
2. [Core Innovations & Architecture](#2-core-innovations--architecture)
3. [Deep-Dive System Components](#3-deep-dive-system-components)
   - [Google Maps Navigation Companion & Corridor Discovery](#google-maps-navigation-companion--corridor-discovery)
   - [Next-Gen Voice Synthesis & Multilingual Pipeline](#next-gen-voice-synthesis--multilingual-pipeline)
   - [Google Maps SDK Vector Cartography & Night Mode](#google-maps-sdk-vector-cartography--night-mode)
   - [System Drawer & Background Tracking Engine](#system-drawer--background-tracking-engine)
   - [Luxury Design System & Obsidian Carbon Dark Mode](#luxury-design-system--obsidian-carbon-dark-mode)
4. [Complete SDK & Technology Stack](#4-complete-sdk--technology-stack)
5. [Repository Structure](#5-repository-structure)
6. [Security Architecture & Threat Vulnerability Report](#6-security-architecture--threat-vulnerability-report)
   - [OWASP Mobile Top 10 (2024 Edition) Mitigation Matrix](#owasp-mobile-top-10-2024-edition-mitigation-matrix)
   - [High-Privilege Service Isolation & Anti-Scraping Defenses](#high-privilege-service-isolation--anti-scraping-defenses)
   - [Hardware-Backed Cryptography & Data at Rest](#hardware-backed-cryptography--data-at-rest)
   - [Runtime UI Protections (Tapjacking, Overlays & Recents Leakage)](#runtime-ui-protections-tapjacking-overlays--recents-leakage)
   - [Network Security & Transit Lockdown](#network-security--transit-lockdown)
   - [Intent Validation & Injection Safeguards](#intent-validation--injection-safeguards)
   - [Threat Modeling Matrix](#threat-modeling-matrix)
7. [Verification & Penetration Test Suite](#7-verification--penetration-test-suite)
8. [Permissions & Privacy Ledger](#8-permissions--privacy-ledger)
9. [Physical Device Testing & Field Verification](#9-physical-device-testing--field-verification)
10. [License & Ethical Standards](#10-license--ethical-standards)

---

## 1. Executive Overview

Traditional travel applications fall into two broken paradigms:
1. **Static tourist guides** requiring travelers to stare at a smartphone screen, read tiny text under bright sunlight, and follow rigid pre-planned pins.
2. **Cloud-monitored trackers** that monetize traveler location traces, continuously beam GPS telemetry to remote servers, and drain mobile battery while roaming without data.

**Travel Guide** reimagines exploring by turning your smartphone into an invisible companion walking by your side. As you wander through historic alleys, the app quietly detects heritage landmarks, maritime relics, colonial facades, and local artisans. Through wireless earbuds, it narrates their stories, architectural subtleties, and forgotten legends in crisp, natural spoken voice.

Whether you are taking a self-guided walking loop or driving via Google Maps turn-by-turn navigation, Travel Guide operates seamlessly in the background — **without transmitting your location coordinates or identity to any remote surveillance system.**

---

## 2. Core Innovations & Architecture

- **Ambient Spoken Narration**: Hands-free storytelling that activates when entering historical geo-fences or when approaching stops along walking paths.
- **Sensory Trigger ("What am I seeing?")**: Instant one-tap tactile control on the UI that resolves the closest landmark and delivers a concise audio summary.
- **Google Maps Navigation Companion**: Real-time integration with Google Maps turn-by-turn navigation, calculating discovery corridors (450m radius) to uncover cultural stops along your active drive or walk.
- **Sarvam AI Indic Speech Synthesis**: High-fidelity Indian English and native Malayalam voice delivery powered by Sarvam AI neural models, with automatic zero-network local TTS failover.
- **Adaptive Obsidian Dark Mode**: High-contrast, luxury design system adhering to strict WCAG AAA guidelines with deep obsidian `#0B0F17` surfaces, luminous coral highlights, and native Google Maps night styling.
- **Zero-Trust Local Privacy**: Zero user tracking, zero account registration, Android KeyStore AES-256-GCM hardware-backed encryption, and one-tap Markdown trip export.
- **Interactive System Drawer Notification**: Native `NotificationCompat.BigTextStyle` notification with theme-adaptive contrast and instant inline action controls (`Mute Audio`, `Explore Place`).
- **Battery-Optimized Profiles**: Three intelligent GPS polling levels (`Saver`, `Balanced`, `Precise`) delivering all-day walk endurance.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID OPERATING SYSTEM                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
         ┌──────────────────────────┴──────────────────────────┐
         │                                                     │
         ▼                                                     ▼
┌─────────────────────────────────┐           ┌─────────────────────────────────┐
│  TravelGuideAccessibilityService│           │      FusedLocationProvider      │
│  (Monitors Google Maps On-Screen│           │   (Background GPS Tracking)     │
│   Turn Cues & Destination Info) │           └────────────────┬────────────────┘
└────────────────┬────────────────┘                            │
                 │                                             │
                 ▼                                             ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    GuideService (Foreground Engine)                    │
│  • MapsCompanionState (Corridor 450m POI Projection)                   │
│  • TriggerEngine (Entry, Exit, Directional Approach)                   │
│  • NarrationQueue (Priority Resolution & Audio Ducking)                │
│  • Interactive Notification Drawer Controller                          │
└───────────────┬───────────────────────────────┬────────────────────────┘
                │                               │
                ▼                               ▼
┌───────────────────────────────┐   ┌────────────────────────────────────┐
│      SarvamAudioService / TTS │   │         Local Room Database        │
│  • Sarvam AI bulbul:v3 API    │   │  • AES-256-GCM Encrypted Visits    │
│  • Offline Android TTS Backup │   │  • Hardware KeyStore Security Key  │
└───────────────┬───────────────┘   └────────────────┬───────────────────┘
                │                                    │
                └─────────────────┬──────────────────┘
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│               Jetpack Compose UI (120 FPS High Refresh)                │
│  • MapScreen (Google Maps SDK Vector Cartography & Night Style)        │
│  • NearbyScreen (Distance-sorted POIs & "What am I seeing?")           │
│  • PacksScreen (Offline City Packs & Storage Inspection)               │
│  • HistoryScreen (Visited Stops, Personal Notes, Markdown Export)       │
│  • SettingsScreen (Biometric App Lock, Audio Voices, Privacy Shred)   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Deep-Dive System Components

### Google Maps Navigation Companion & Corridor Discovery
Travel Guide runs side-by-side with official Google Maps turn-by-turn navigation without requiring account integration:
- Intercepts navigation cues via dedicated companion services (`TravelGuideAccessibilityService` and `MapsCompanionService`).
- Calculates an active corridor along the journey vector.
- POIs within 450 meters of the path are dynamically highlighted on the map and pre-cached in the narration queue.
- Automatically clears polylines and corridor states when Google Maps navigation ends.

### Next-Gen Voice Synthesis & Multilingual Pipeline
- **Primary Cloud Engine**: Sarvam AI `bulbul:v3` model delivering natural Indian English and Malayalam voice inflection.
- **Audio Cache**: Generated speech is saved to internal cache (`cacheDir/sarvam_tts/`), eliminating redundant network requests.
- **Local Fallback**: Instant switch to Android native `TextToSpeech` when disconnected or in flight mode.
- **Input Sanitization**: All speech strings are stripped of SSML tags and control codes before synthesis.

### Google Maps SDK Vector Cartography & Night Mode
- Official Google Maps Android SDK (v18.2.0) with custom Night Mode styling JSON.
- 3D building extrusions, vector road meshes, dynamic labeling, and 120 FPS gesture support.
- Fully hardware-accelerated map view with memory management in `onLowMemory()` and `onDestroy()`.

### System Drawer & Background Tracking Engine
- Persistent foreground service (`GuideService`) with a low-importance notification channel.
- Rich expanded notification (`NotificationCompat.BigPictureStyle` or `BigTextStyle`) with instant audio toggle and explore buttons.
- Fully functional when device screen is turned off or phone is placed in a pocket.

### Luxury Design System & Obsidian Carbon Dark Mode
The UI adheres strictly to custom tokens with zero generic styling:

| Token | Light Mode (Warm Ivory) | Dark Mode (Obsidian Carbon) | Visual Purpose |
| :--- | :--- | :--- | :--- |
| `Bg` | `#F8F9FA` | `#0B0F17` | Canvas background |
| `Surface` | `#FFFFFF` | `#131A26` | Card and sheet resting surface |
| `Surface2` | `#F1F3F5` | `#182232` | Raised elements & active row wash |
| `Border` | `#E5E7EB` | `#232F42` | Hairline structural separation |
| `Primary` | `#FF5A36` | `#FF6D4D` | Sunset coral brand accent |
| `Highlight`| `#F59E0B` | `#FBBF24` | Radiant amber for audio & ratings |
| `Success` | `#10B981` | `#34D399` | Mint emerald for active/open status |
| `Text` | `#111827` | `#F8FAFC` | High-contrast primary text |
| `Text2` | `#6B7280` | `#94A3B8` | Secondary metadata slate |

---

## 4. Complete SDK & Technology Stack

| Layer | Component | Version | Security & Operational Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin | `2.0.21` | Type safety, coroutine concurrency, memory efficiency |
| **Android Build** | Android Gradle Plugin (AGP) | `8.7.3` | R8 optimization, DEX compilation, manifest packaging |
| **SDK Levels** | Compile: `API 35` / Min: `API 26` | 35 / 26 | Modern Android 15 features with Android 8.0+ backwards compatibility |
| **UI Toolkit** | Jetpack Compose (BOM) | `2024.10.01` | Declarative UI, 120 FPS animations, reactive state |
| **Cartography** | Google Play Services Maps | `18.2.0` | Vector rendering, 3D buildings, night styling |
| **Location** | FusedLocationProviderClient | `21.3.0` | Power-balanced GPS polling, geofencing |
| **Biometrics** | AndroidX Biometric | `1.2.0-alpha05` | Hardware-backed biometric / device credential authentication |
| **Database** | AndroidX Room | `2.6.1` | Encrypted SQLite database for visits and personal notes |
| **Crypto** | AndroidKeyStore (Hardware TEE) | Native | AES-256-GCM authenticated encryption |
| **Audio AI** | Sarvam AI REST API | `v3` | Indic neural voice synthesis (English & Malayalam) |
| **Networking** | OkHttp & HttpURLConnection | `4.12.0` | TLS 1.3 only, certificate trust verification |

---

## 5. Repository Structure

```
TRAVEL-GUIDE/
├── README.md                      # Master Documentation & Security Specification
├── SPEC.md                        # Formal Engineering & Product Specification v1.0
├── DESIGN-TOKENS.md               # DesignSoul token definitions & color scales
│
├── mobile/                        # Android Native Project
│   ├── app/                       # Main Android Application Module
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml # Permissions, hardened components, security config
│   │   │   ├── kotlin/guide/app/
│   │   │   │   ├── MainActivity.kt # Root navigation host, biometric gate, overlay defense
│   │   │   │   ├── companion/      # TravelGuideAccessibilityService with anti-scraping
│   │   │   │   ├── data/           # AppState, Room DB, Encrypted Notes, Pack Loader
│   │   │   │   ├── export/         # Markdown trip exporter (scoped cache provider)
│   │   │   │   ├── location/       # GuideService foreground worker & LocationTracker
│   │   │   │   ├── map/            # Google Maps vector styles & custom pins
│   │   │   │   ├── navigation/     # MapsCompanionState & 450m corridor calculations
│   │   │   │   ├── security/       # CryptoVault (AES-256-GCM hardware KeyStore)
│   │   │   │   ├── voice/          # SarvamAudioService, Narrator (SSML sanitization)
│   │   │   │   └── ui/             # Jetpack Compose screens, theme tokens, components
│   │   │   └── res/
│   │   │       ├── xml/
│   │   │       │   ├── network_security_config.xml # TLS enforcement, cleartext block
│   │   │       │   ├── data_extraction_rules.xml   # Backup and transfer lockdown
│   │   │       │   ├── file_paths.xml              # Scoped cache provider paths
│   │   │       │   └── accessibility_service_config.xml # Scope limited to Google Maps
│   │   └── build.gradle.kts        # App dependencies and compiler configurations
│   │
│   └── shared-core/               # Pure Kotlin Core Engine
│       ├── src/main/kotlin/        # POI triggers, priority queues, and geo algorithms
│       └── src/test/kotlin/        # Comprehensive Unit Test Suite
│
├── content/                       # Heritage Packs
│   └── fort-kochi/                # Fort Kochi & Mattancherry Heritage Pack v1.1.0
│       ├── pack.json              # 24 Curated POIs (stories, coordinates, hours, tips)
│       └── routes.json            # Curated walking routes
│
└── docs/                          # Technical Architecture & QA Specifications
    ├── QA.md                      # Test commands & manual QA checklists
    ├── UI-AUDIT-2026-09-26.md     # Design token audit
    └── IOS-ARCHITECTURE.md        # SwiftUI & CoreLocation iOS companion blueprint
```

---

## 6. Security Architecture & Threat Vulnerability Report

Because Travel Guide utilizes high-privilege Android features (`AccessibilityService`, `NotificationListenerService`, background location, and hardware biometrics), it is architected under an adversarial threat model. The system defends against device theft, rogue third-party applications, network eavesdropping, and injection attacks.

### OWASP Mobile Top 10 (2024 Edition) Mitigation Matrix

| OWASP Risk | Threat Description | Travel Guide Defense & Mitigation | Status |
| :--- | :--- | :--- | :--- |
| **M1: Improper Credential Usage** | Hardcoded API keys in APK or repository leakage | Secrets decoupled via `secrets.properties`; runtime credentials isolated; zero private keys stored in code. | [SECURE] |
| **M2: Inadequate Supply Chain** | Malicious third-party SDK dependencies | Zero ad trackers, zero analytics SDKs; strictly official Google Play Services, AndroidX, and Kotlin stdlib. | [SECURE] |
| **M3: Insecure Authentication** | Biometric bypass or screen lock evasion | `BiometricPrompt` requiring `BIOMETRIC_STRONG` or `DEVICE_CREDENTIAL`. App locks on `onStop()`; UI obscured with `FLAG_SECURE`. | [SECURE] |
| **M4: Insufficient Input Validation** | Intent injection, format strings, SSML abuse | Strict coordinate bounds (-90..90, -180..180); string clamping (80 chars); SSML tag stripping in TTS engine. | [SECURE] |
| **M5: Insecure Communication** | Cleartext interception or rogue Wi-Fi MitM | `network_security_config.xml` disables cleartext; trusts system CAs only; rejects user-installed proxy certs. | [SECURE] |
| **M6: Inadequate Privacy Controls** | GPS telemetry exfiltration or tracking | No remote telemetry server exists. GPS coordinates are processed exclusively in device memory. | [SECURE] |
| **M7: Insufficient Binary Protections** | ADB backup extraction or physical memory dumping | `android:allowBackup="false"`, `android:dataExtractionRules` exclude all databases, cache, and preferences. | [SECURE] |
| **M8: Security Misconfiguration** | Tapjacking via floating windows or overlay attacks | `window.decorView.filterTouchesWhenObscured = true` and `setHideOverlayWindows(true)` prevent UI redress. | [SECURE] |
| **M9: Insecure Data Storage** | SQLite database reading on rooted devices | User travel notes are encrypted via `CryptoVault` using AES-256-GCM before writing to Room database. | [SECURE] |
| **M10: Insufficient Cryptography** | Weak cipher modes (ECB/CBC) or static IVs | Android KeyStore master key generates unique 12-byte IV per encryption; 128-bit authentication tag verification. | [SECURE] |

---

### High-Privilege Service Isolation & Anti-Scraping Defenses

#### 1. Accessibility Service Isolation (`TravelGuideAccessibilityService`)
- **Package Lockdown**: Hardcoded filter in both XML configuration (`android:packageNames="com.google.android.apps.maps"`) and runtime (`onAccessibilityEvent`) drops any event not originating from Google Maps.
- **Anti-Keylogger Guard**: Explicit check:
  ```kotlin
  if (node.isPassword || node.isEditable) return
  ```
  The service refuses to inspect password fields, search bars, or user input text fields, ensuring zero scraping of sensitive inputs.
- **Zero Logging / Dumping**: Node texts and view hierarchies are never logged to Logcat, written to disk, or sent across IPC.
- **Bounds Checking**: Extracted destination strings are clamped to 80 characters and stripped of control characters.

#### 2. Notification Listener Isolation (`MapsCompanionService`)
- **Strict Package Verification**: Drops all incoming status bar notifications unless `sbn.packageName == "com.google.android.apps.maps"`.
- **Zero Exposure**: Banking alerts, SMS 2FA codes, messaging apps, and email notifications are immediately ignored without inspecting extras.

---

### Hardware-Backed Cryptography & Data at Rest

- **Master Key Generation**: Stored in `AndroidKeyStore` with `PURPOSE_ENCRYPT | PURPOSE_DECRYPT`.
- **Cipher Specification**: `AES/GCM/NoPadding` (256-bit key, 12-byte randomized IV, 128-bit authentication tag).
- **Enclave Verification**: Verified via `KeyInfo.isInsideSecureHardware` to ensure keys reside in the device Trusted Execution Environment (TEE) or StrongBox.
- **Database Note Protection**:
  ```kotlin
  // AppState.kt note storage
  val encryptedText = CryptoVault.encrypt(userNote)
  db.dao().upsertNote(NoteEntity(poiId, encryptedText, timestamp))
  ```

---

### Runtime UI Protections (Tapjacking, Overlays & Recents Leakage)

- **Tapjacking Prevention**:
  ```kotlin
  window.decorView.filterTouchesWhenObscured = true
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      runCatching {
          val method = window.javaClass.getMethod("setHideOverlayWindows", Boolean::class.javaPrimitiveType)
          method.invoke(window, true)
      }
  }
  ```
  Blocks touch events when any third-party floating overlay (e.g. chat bubbles, screen dimmers) is drawn on top of the app.
- **Task Switcher / Recents Privacy Protection**:
  When App Lock is enabled, `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` is applied during backgrounding and lock states. The Android OS renders a blank preview in the Recents task switcher, preventing unauthorized viewing of visited locations or travel notes.

---

### Network Security & Transit Lockdown

Configured in `res/xml/network_security_config.xml`:
```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.sarvam.ai</domain>
        <domain includeSubdomains="true">maps.googleapis.com</domain>
    </domain-config>
</network-security-config>
```
- Disallows all cleartext HTTP traffic across the app (`usesCleartextTraffic="false"`).
- Rejects user-installed certificates to block Man-in-the-Middle (MitM) inspection via rogue corporate or proxy CAs.

---

### Intent Validation & Injection Safeguards

`MainActivity` handles external `geo:` and `ACTION_SEND` intents with defensive bounds:
- **Coordinate Validation**:
  ```kotlin
  val validLat = rawLat?.takeIf { !it.isNaN() && !it.isInfinite() && it in -90.0..90.0 }
  val validLng = rawLng?.takeIf { !it.isNaN() && !it.isInfinite() && it in -180.0..180.0 }
  ```
- **Text Sanitization**:
  Inputs are clamped to 250 characters; destination names are clamped to 80 characters and filtered with regex `[^\w\s.,'#\-]` to eliminate script tags and control characters.
- **Scoped FileProvider**:
  FileProvider is scoped strictly to `cacheDir/exports/`, preventing path traversal (`../`) attacks when sharing Markdown exports.

---

### Threat Modeling Matrix

```
[Attacker Vector]                 [Target Component]                [Engine Defense]
─────────────────────────────────────────────────────────────────────────────────────────────
Rogue Overlay Window        ───►  Biometric / UI Buttons     ───►  filterTouchesWhenObscured
ADB Data Extraction         ───►  Local SQLite / SharedPreferences──► allowBackup=false + DataExtractionRules
Physical Device Snooping    ───►  Recents Task Switcher      ───►  FLAG_SECURE Window Gating
Stolen Database File        ───►  Personal Trip Notes        ───►  AES-256-GCM Hardware Keystore
Rogue App Intent Spoofing   ───►  MainActivity Intent Handler───►  Strict Coordinate & String Bounds
Hostile Wi-Fi Eavesdrop     ───►  Neural Audio Streaming     ───►  TLS 1.3 System Trust Anchors Only
Malicious Input / Prompt    ───►  TTS Speech Synthesis Queue ───►  SSML Tag Stripping & Length Clamping
Accessibility Abuse Attempt ───►  Screen Inspection Engine   ───►  isPassword & isEditable Rejection
```

---

## 7. Verification & Penetration Test Suite

The security architecture is verified through automated and manual test suites:

### Automated Test Cases

| Test Suite | Scope | Target | Result |
| :--- | :--- | :--- | :--- |
| `TriggerEngineTest` | Proximity & Geofencing | POI arrival and departure radius calculations | [PASS] 25/25 |
| `NarrationQueueTest` | Audio Priority | Queue priority, mute response (<500ms), deduplication | [PASS] |
| `CryptoVaultTest` | Cryptography | AES-256-GCM encryption/decryption, hardware backing verification | [PASS] |
| `IntentSanitizerTest` | Input Validation | Geo coordinate bounds checking, null byte stripping | [PASS] |
| `NetworkConfigTest` | Network Security | Verify cleartext rejection and TLS enforcement | [PASS] |

### Manual Penetration Testing Checklist

- [x] **ADB Backup Extraction**: Executed `adb backup guide.app`. Verified Android rejected extraction due to `allowBackup="false"`.
- [x] **Cleartext Traffic Interception**: Monitored network traffic via proxy; verified all plain HTTP requests are dropped by the OS network stack.
- [x] **Overlay / Tapjacking Test**: Ran floating alert overlay on test device; verified touch events are blocked when obscured.
- [x] **Recents Snapshot Leakage**: Sent app to background while locked; verified task switcher displays an obscured black window without exposing private data.
- [x] **Keylogger Prevention**: Inspected Google Maps search and password input fields; verified Accessibility Service skips editable and password nodes immediately.
- [x] **Malformed Intent Fuzzing**: Fired `geo:999,999` and `geo:NaN,NaN` intents; verified app gracefully handles bounds without crashing or creating corrupt sessions.

---

## 8. Permissions & Privacy Ledger

| Permission | Android Level | Reason for Request | Privacy Safeguard |
| :--- | :--- | :--- | :--- |
| `ACCESS_FINE_LOCATION` | Runtime | Detect proximity to heritage landmarks. | Processed entirely on-device; never uploaded to any remote server. |
| `ACCESS_BACKGROUND_LOCATION`| Explicit Opt-In | Keep spoken tour active when phone is in your pocket. | Separate plain-language prompt; can be toggled off at any time. |
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Display active companion card in the notification drawer. | Can be muted or disabled entirely in the app Settings screen. |
| `BIND_ACCESSIBILITY_SERVICE`| User-Granted | Synchronize turn cues with Google Maps navigation. | Restricted strictly to `com.google.android.apps.maps`; never reads editable fields. |
| `BIND_NOTIFICATION_LISTENER`| User-Granted | Detect active navigation session status. | Drops all non-Google Maps notifications immediately. |
| `INTERNET` | Install-Time | Fetch neural voice audio from Sarvam AI when online. | Automatically switches to local on-device TTS when disconnected. |

---

## 9. Physical Device Testing & Field Verification

The engine and design system have been tested directly on physical hardware:

- **Target Device**: Xiaomi HyperOS / Android 14 (`Device ID: 8TCABAIFWOZTDICI`).
- **Verified Workflows**:
  - **Google Maps Navigation Sync**: Intercepted turn cues and projected 450m corridor discovery markers.
  - **Notification Drawer Readability**: Verified high-contrast typography across both system light and dark modes with responsive `Mute Audio` and `Explore Place` actions.
  - **Sarvam AI Audio Playback**: High-definition English and Malayalam spoken tours verified through device speaker and Bluetooth earbuds.
  - **Hardware Keystore Data Protection**: Verified visit logs persist across app restarts and export cleanly to Markdown.
  - **Adaptive Dark Mode**: Instant theme switching between System Default, Warm Light, and Obsidian Carbon without animation frame drops or white flashes.
  - **120 FPS High Refresh Rate**: Verified smooth animations at full display panel refresh rate without MIUI dynamic display throttling.

---

## 10. License & Ethical Standards

Travel Guide is built under the **Apache License 2.0 / MIT Dual License**.

- **Open Cultural Data**: All curated heritage stories, architectural notes, and walking routes for Fort Kochi and Mattancherry are licensed under **Creative Commons Attribution-ShareAlike 4.0 (CC BY-SA 4.0)**.
- **Privacy Covenant**: Travel Guide pledges never to include ad tracking networks, telemetry beacons, or user fingerprinting technologies in its codebase. Your travels belong to you alone.

---

<div align="center">
  <b>Built with craft, cultural reverence, and absolute respect for traveler privacy.</b><br>
  <i>Fort Kochi • Mattancherry • Kerala, India</i>
</div>
