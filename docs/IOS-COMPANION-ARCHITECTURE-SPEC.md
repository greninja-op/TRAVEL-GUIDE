# iOS Native Architecture, Liquid Glass Design System & Screen-Reading Specification

**Document Version:** 3.0.0  
**Target Platform:** iOS 16.0+ / iOS 17.0+ (Swift 5.9+, SwiftUI, Metal, ReplayKit, Vision, ActivityKit, CoreLocation, AVFoundation, CryptoKit, MapLibre Native)  
**Project:** Travel Guide (Offline-First Audio Tour Companion)  
**Author:** DeepMind Antigravity Engineering Architecture Team  
**Status:** Comprehensive Architecture, UI/UX Design System & Implementation Blueprint  

---

## 1. Executive Summary & Architectural Philosophy

### 1.1 Objective & Companion Mission
On Android, Travel Guide operates as an invisible driving and walking companion alongside Google Maps. When the user navigates through historic corridors (such as Fort Kochi and Mattancherry), Travel Guide detects the destination from Google Maps, pre-arms the local tour queue, and guides the traveler along road-snapped geometry with localized audio stories—all while functioning 100% offline with zero cloud tracking.

For iOS, we are designing a **purely native Swift & SwiftUI application** accompanied by a system-level background extension.

### 1.2 The Platform Philosophy: Why Kotlin & Swift (Rejecting the Cross-Platform Trap)
A foundational question in modern mobile engineering is whether to use cross-platform frameworks like **Flutter** or **React Native**. While cross-platform toolkits offer single-codebase convenience, they force both platforms into a **generic, lowest-common-denominator compromise**:
1. **Uncanny Valley & Boring UI**: Flutter renders pixels onto an internal Skia/Impeller canvas. It cannot natively replicate Apple's hardware-accelerated real-time blur (`UIBlurEffect` / `.ultraThinMaterial`), optical vibrancy filters, or subpixel chromatic dispersion. The result is a flat, artificial, imitation interface that feels sluggish and alien to iOS users.
2. **SwiftUI Spring Physics & Gestures**: Apple's interface feel relies on physically-modelled damping springs that conserve gesture velocity. Cross-platform gesture bridges introduce touch latency and unnatural easing curves.
3. **Hard System Barriers**:
   - **Dynamic Island & Live Activities (`ActivityKit`)**: Exclusively native Swift, requiring an iOS App Extension target and WidgetKit. Bridging this from Flutter/React Native requires brittle custom plugins prone to lifecycle crashes.
   - **ReplayKit Zero-Copy Buffering**: Processing `CMSampleBuffer` video streams at 60 FPS under a strict 50 MB memory ceiling requires direct C pointer arithmetic and Apple Vision on the Apple Neural Engine. JavaScript and Dart garbage-collection runtimes trigger instant memory exhaustion.
   - **Audio Ducking (`AVAudioSession`)**: Fine-grained hardware audio session mixing (`.duckOthers`) during Google Maps voice prompts requires native CoreAudio interaction.
4. **Platform-Tailored UI**: Android and iOS users have drastically different mental models. Android leverages elevated Material surfaces and system back buttons. iOS demands floating **Liquid Glass**, translucent navigation capsules floating above the Home Indicator, and Dynamic Island heads-up displays.

**Architectural Decision**: We reject Flutter and React Native. Android remains 100% Kotlin (Jetpack Compose / Material 3), and iOS is engineered 100% in native Swift (SwiftUI / Metal / Liquid Glass).

---

## 2. iOS Screen-Reading & Background Detection Architecture

### 2.1 The Deep Dive: Why `AXRuntime` Cannot Pass App Store Verification

On macOS, `AXUIElement` is a public API (`ApplicationServices.framework`) used by automation tools. On iOS, Apple deliberately moved cross-app accessibility symbols into private frameworks:
* `/System/Library/PrivateFrameworks/AXRuntime.framework`
* `/System/Library/PrivateFrameworks/AccessibilityPlatformTranslation.framework`

#### Why It Fails App Store Review:
1. **Automated Mach-O Ingest Scanners**: When an `.ipa` is uploaded to App Store Connect, Apple's ingest pipeline decompiles the Mach-O binary and scans every dynamic symbol (`otool -L`). Calling `AXUIElementCopyAttributeValue` or referencing `AXRuntime` triggers an instant automated rejection under **Guideline 2.5.1 (Use of Non-Public APIs)**.
2. **Apple Mobile File Integrity (AMFI) Kernel Protection**: Even if symbols are obfuscated via runtime dynamic loading (`dlopen`, `dlsym`, or XOR strings), communicating with the system accessibility daemon (`com.apple.accessibility.gax.client`) requires a private Mach entitlement:
   ```xml
   <key>com.apple.private.accessibility.automation</key>
   <true/>
   ```
   This entitlement is cryptographically signed only by Apple's internal certificate authority. Any third-party provisioning profile fails validation, and the iOS kernel terminates the process with `KERN_PROTECTION_FAILURE`.
3. **Enterprise vs. App Store Release**: While private frameworks function in internal enterprise distribution (MDM in-house provisioning profiles) or sideloaded environments (AltStore / TrollStore), **a public App Store release requires an official, public Apple API path**.

---

### 2.2 The Three Screen-Access Solutions on iOS

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      iOS COMPANION ACCESS MATRIX                            │
├─────────────────────────┬───────────────────────┬──────────────┬────────────┤
│ Solution                │ Technology            │ App Store?   │ Notifs?    │
├─────────────────────────┼───────────────────────┼──────────────┼────────────┤
│ 1. ReplayKit + OCR      │ RPBroadcastExtension  │ YES (Public) │ ZERO       │
│ 2. Back Tap / Action    │ Shortcuts + Vision    │ YES (Public) │ ZERO       │
│ 3. Private Framework    │ AXRuntime / Mach IPC  │ NO (Private) │ ZERO       │
└─────────────────────────┴───────────────────────┴──────────────┴────────────┘
```

---

### Solution 1: ReplayKit Broadcast Extension + On-Device Vision OCR (Recommended App Store Path)

This is the exact architecture deployed by live screen translation and gaming overlay apps currently approved and active on the Apple App Store.

#### Architecture Diagram
```
┌──────────────────────┐         ┌───────────────────────────────┐
│     Google Maps      │         │   Travel Guide Main App       │
│   (Foreground App)   │         │ (Background Location + Audio) │
└──────────┬───────────┘         └───────────────▲───────────────┘
           │ Screen Video Frames                 │
           │ (CMSampleBuffer)                    │ App Group Shared Memory
           ▼                                     │ (UserDefaults / MMKV Container)
┌────────────────────────────────────────────────┴───────────────┐
│  Broadcast Upload Extension (RPBroadcastSampleHandler)         │
│  - Throttled to 0.5 FPS (1 frame every 2.0 seconds)            │
│  - Downsampled to 720p (Memory ceiling < 50MB)                │
│  - On-Device Apple Neural Engine OCR (VNRecognizeTextRequest)  │
│  - Destination regex matcher ("Navigating to ...", "Towards")  │
└────────────────────────────────────────────────────────────────┘
```

#### Production Swift Implementation (`SampleHandler.swift`)

```swift
import ReplayKit
import Vision

public final class SampleHandler: RPBroadcastSampleHandler {
    private var lastProcessTime: TimeInterval = 0
    private let appGroupID = "group.guide.app.companion"
    
    // Lazy Vision text recognition running on Apple Neural Engine
    private lazy var textRecognitionRequest: VNRecognizeTextRequest = {
        let request = VNRecognizeTextRequest { [weak self] (req, error) in
            guard let observations = req.results as? [VNRecognizedTextObservation] else { return }
            self?.extractDestination(from: observations)
        }
        request.recognitionLevel = .fast // Minimal CPU & memory footprint
        request.usesLanguageCorrection = false
        return request
    }()

    public override func processSampleBuffer(_ sampleBuffer: CMSampleBuffer, with sampleBufferType: RPSampleBufferType) {
        guard sampleBufferType == .video else { return }
        
        // 1. Throttle to 0.5 FPS (1 frame every 2 seconds) to preserve battery
        let now = CACurrentMediaTime()
        guard now - lastProcessTime > 2.0 else { return }
        lastProcessTime = now

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        // 2. Execute on background queue within iOS Broadcast 50MB RAM ceiling
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, options: [:])
            try? handler.perform([self.textRecognitionRequest])
        }
    }

    private func extractDestination(from observations: [VNRecognizedTextObservation]) {
        for obs in observations {
            guard let topCandidate = obs.topCandidates(1).first?.string else { continue }
            
            // Regex match navigation headers from Google Maps or Apple Maps
            if topCandidate.localizedCaseInsensitiveContains("Towards") ||
               topCandidate.localizedCaseInsensitiveContains("Navigating to") ||
               topCandidate.localizedCaseInsensitiveContains("Head") ||
               topCandidate.localizedCaseInsensitiveContains("Destination:") {
                
                // Write destination to App Group container for main app
                if let defaults = UserDefaults(suiteName: appGroupID) {
                    defaults.set(topCandidate, forKey: "companion_active_destination")
                    defaults.set(Date().timeIntervalSince1970, forKey: "companion_timestamp")
                }
                break
            }
        }
    }
}
```

---

### Solution 2: Native iOS "Back Tap" / Action Button Snapshot OCR

For travelers who prefer zero status-bar indicators and zero background memory consumption:
1. The user installs an iOS Shortcut: `Travel Guide Sync`.
2. The shortcut runs: `Take Screenshot` -> `Extract Text from Image` -> `Open Travel Guide Deep Link (travelguide://sync?text=...)`.
3. In iOS Settings (`Accessibility -> Touch -> Back Tap`), the user assigns **Double Tap** to this shortcut (or maps it to the iPhone 15/16 Action Button).
4. When looking at Google Maps, the user double-taps the back of their iPhone.
5. iOS takes an instant screenshot, extracts destination text on the Neural Engine, and arms the tour queue.

---

### Solution 3: The Private Framework Hook (`AXRuntime`)
* Linked against `/System/Library/PrivateFrameworks/AXRuntime.framework`.
* Traverses the live UI hierarchy using `AXUIElementCopyAttributeValue`.
* Reads UI text directly from Google Maps without OCR or screen recording.
* **Strict Constraint**: For Enterprise MDM or developer sideloading (AltStore / TrollStore). Completely blocked from the public App Store.

---

### 2.3 The Notification Resilience Matrix (Handling 100% Turned-Off Notifications)

In consumer travel apps, between 10% and 20% of users turn off notifications entirely. Our iOS architecture is engineered so that **turning off notifications does NOT degrade the audio tour or companion experience**:

| Capability | Behavior when Notifications are DISABLED | System Alternative Deployed |
| :--- | :--- | :--- |
| **Active Turn Tracking** | System alert banners dropped | **Dynamic Island & Live Activity (`ActivityKit`)** permanently visible on screen and Lock Screen. |
| **Audio Tour Triggers** | No notification chime | **Continuous GPS (`CoreLocation`)** triggers **`AVAudioSession` spoken narration** directly in car speakers or headphones. |
| **Navigation Ducking** | Not notification-dependent | **Auto-Ducking (`.duckOthers`)** automatically softens tour audio when Google Maps speaks a direction. |
| **Destination Detection** | Not notification-dependent | **ReplayKit / Vision OCR** writes to shared App Group memory, triggering tour pre-arming silently. |

---

## 3. The "Liquid Glass" Visual Design System (Apple iOS HIG)

### 3.1 Principles of Liquid Glass
Unlike flat 2013 frosted glass (`UIVisualEffectView`), Apple's **Liquid Glass** treats UI elements as optical lenses:
* **Specular Highlight**: A 1px curved inner gradient catching light at the top-left edge (135° key light).
* **Chromatic Refraction**: A subtle subpixel tint shift at the perimeter simulating physical glass dispersion.
* **Backdrop Saturation Boost**: `saturate(180%) blur(24px)` to make underlying map features glow through the glass without reducing text contrast.
* **Dynamic Material Adaptation**: Automatically blends between light and dark material schemes based on system luminance.

```
┌───────────────────────────────────────────────────────────────┐
│               LIQUID GLASS COMPOSITING STACK                  │
├───────────────────────────────────────────────────────────────┤
│  Top: Specular Rim (1px Gradient Stroke: White 65% -> 10%)    │
│  Layer 1: Optical Foreground Content (SF Symbols / Typography)│
│  Layer 2: Vibrancy Filter (.vibrancy color blending)          │
│  Layer 3: Ultra-Thin Material Blur (.ultraThinMaterial)       │
│  Base: Ambient Drop Shadow (Color.black.opacity(0.12), r: 16) │
└───────────────────────────────────────────────────────────────┘
```

---

### 3.2 Production SwiftUI Liquid Glass Kit (`LiquidGlassKit.swift`)

```swift
import SwiftUI

// MARK: - Liquid Glass View Modifier
public struct LiquidGlassModifier: ViewModifier {
    public var cornerRadius: CGFloat = 28
    public var isElevated: Bool = true
    public var hasChromaticRim: Bool = true

    public func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(.ultraThinMaterial)
                    .background(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .fill(Color.white.opacity(0.12))
                    )
            )
            .overlay(
                // Top-Left Specular Light Catch
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(
                        LinearGradient(
                            stops: [
                                .init(color: Color.white.opacity(0.70), location: 0.0),
                                .init(color: Color.white.opacity(0.25), location: 0.35),
                                .init(color: Color.clear, location: 0.65),
                                .init(color: Color.white.opacity(0.12), location: 1.0)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.2
                    )
            )
            .overlay(
                // Subtle Chromatic Edge Dispersion
                Group {
                    if hasChromaticRim {
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .strokeBorder(
                                LinearGradient(
                                    colors: [
                                        Color(red: 1.0, green: 0.4, blue: 0.6, opacity: 0.08),
                                        Color.clear,
                                        Color(red: 0.3, green: 0.7, blue: 1.0, opacity: 0.08)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 1.0
                            )
                    }
                }
            )
            .shadow(
                color: Color.black.opacity(isElevated ? 0.14 : 0.05),
                radius: isElevated ? 18 : 8,
                x: 0,
                y: isElevated ? 9 : 4
            )
    }
}

public extension View {
    func liquidGlass(
        cornerRadius: CGFloat = 28,
        isElevated: Bool = true,
        hasChromaticRim: Bool = true
    ) -> some View {
        modifier(LiquidGlassModifier(
            cornerRadius: cornerRadius,
            isElevated: isElevated,
            hasChromaticRim: hasChromaticRim
        ))
    }
}
```

---

## 4. Design Tokens: Colors, Typography & SF Symbols

### 4.1 Semantic Color Token System (`DesignTokens.swift`)

All colors are defined as dynamic semantic tokens supporting automatic Light and Dark mode transitions:

```swift
import SwiftUI

public enum DesignColors {
    // Brand Accent: Sunset Coral
    public static let coralPrimary = Color("CoralPrimary", bundle: nil) // #FF5A36 (Light) / #FF6B4A (Dark)
    public static let coralGlow = Color(red: 1.0, green: 0.35, blue: 0.21, opacity: 0.25)

    // Primary & Secondary Inks
    public static let carbonInk = Color("CarbonInk", bundle: nil) // #121826 (Light) / #F8F9FA (Dark)
    public static let mineralMuted = Color("MineralMuted", bundle: nil) // #64748B (Light) / #94A3B8 (Dark)
    
    // Map Navigation Polyline (Google Blue)
    public static let navBlueCore = Color(red: 0.26, green: 0.52, blue: 0.96) // #4285F4
    public static let navBlueBorder = Color(red: 0.10, green: 0.45, blue: 0.91) // #1A73E8

    // Compass Needles
    public static let compassNorth = Color(red: 0.92, green: 0.26, blue: 0.21) // #EA4335 Crimson
    public static let compassSouth = Color(red: 0.56, green: 0.64, blue: 0.68) // #90A4AE Slate Steel
    public static let compassPivot = Color.white

    // Categories
    public static let categoryHeritage = Color(red: 0.96, green: 0.62, blue: 0.04) // Amber #F59E0B
    public static let categoryDining = Color(red: 0.06, green: 0.73, blue: 0.51) // Emerald #10B981
    public static let categoryScenic = Color(red: 0.39, green: 0.40, blue: 0.95) // Indigo #6366F1
}
```

### 4.2 SF Symbols 5/6 Variable Color & Symbol Effects
Instead of static SVGs, iOS deploys Apple's vector SF Symbols with native variable color and bounce/pulse animations:
* **Compass Snap**: `"safari.fill"` or custom dual-needle vector with `.symbolEffect(.bounce)`.
* **Recenter Button**: `"location.fill"` with `.symbolEffect(.pulse, isActive: isTrackingLocation)`.
* **Audio Waveform**: `"waveform"` with `.symbolEffect(.variableColor.iterative)`.
* **Tour Queues**: `"headphones"` and `"sparkles"`.

---

## 5. Apple Spring Physics & CoreHaptics Engine

### 5.1 Spring Animation Tokens (`MotionTokens.swift`)

Android uses duration-based linear/bezier interpolation. iOS strictly relies on **undamped/damped harmonic oscillators** that track physical user gesture speed:

```swift
import SwiftUI

public enum MotionTokens {
    /// Interactive gesture drags, sheet panning, tab presses
    public static let interactiveSpring = Animation.spring(response: 0.32, dampingFraction: 0.82, blendDuration: 0)
    
    /// Camera movements, card arrivals, modal popups
    public static let settleSpring = Animation.spring(response: 0.44, dampingFraction: 0.76, blendDuration: 0)
    
    /// Tab bar active indicator pill sliding
    public static let dockSlideSpring = Animation.spring(response: 0.36, dampingFraction: 0.72, blendDuration: 0)
    
    /// Micro-interactions, button presses, badge pop
    public static let quickBounce = Animation.spring(response: 0.24, dampingFraction: 0.64, blendDuration: 0)
}
```

### 5.2 Taptic Engine Haptic Profiles
Integrated via `CoreHaptics` and `UIFeedbackGenerator`:
* **Nav Bar Tab Selection**: `UIImpactFeedbackGenerator(style: .light).impactOccurred()`
* **Compass North Snap**: `UIImpactFeedbackGenerator(style: .rigid).impactOccurred()`
* **Geofence Story Arrival**: `UINotificationFeedbackGenerator().notificationOccurred(.success)`
* **Interactive Button Press**: `UIImpactFeedbackGenerator(style: .soft).impactOccurred()`

---

## 6. Screen Breakdown & UI Spatial Placements

```
┌─────────────────────────────────────────────────────────────────┐
│                    iOS FULL VIEWPORT COMPOSITION                │
├─────────────────────────────────────────────────────────────────┤
│ [Top Left: Glass Header Pill]       [Top Right: Compass Button] │
│ "Fort Kochi • 24 stops"             (Red/Steel Needle 44pt)     │
│                                     [Recenter Button]           │
│                                     (Blue Location Disc 44pt)   │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │               FULL-BLEED MAP CANVASC (MapLibre / Metal)     │ │
│ │               - Road-Snapped Polyline (#1A73E8 / #4285F4)   │ │
│ │               - Discovery Corridor Dots (< zoom 16.0)       │ │
│ │               - Expanded Non-Stacking Glass Cards (zoom 16+)│ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ [Floating Next-Stop Card] (16pt above dock)                     │
│ "Chinese Fishing Nets" [▶ Play Story] [What am I seeing?]       │
│                                                                 │
│ [FLOATING LIQUID GLASS DOCK] (16pt above Home Indicator)        │
│   (Map)   (Nearby)   (Routes)   (Packs)   (History)  (Settings) │
└─────────────────────────────────────────────────────────────────┘
```

---

### 6.1 The Floating Liquid Glass Bottom Nav Dock (`FloatingGlassDock.swift`)

Unlike Android's full-width bottom bar or Flutter's standard navigation bar, the iOS Nav Dock is an **elevated floating capsule**:
* **Position**: Anchored `16pt` above the Apple Home Indicator safe area (`.padding(.bottom, safeAreaInsets.bottom > 0 ? 0 : 16)`).
* **Dimensions**: Height `64pt`, horizontal margin `20pt`, corner radius `32pt`.
* **Material**: `.ultraThinMaterial` with specular 1px border.
* **Matched Geometry Sliding Pill**: A glowing Coral pill (`#FF5A36` at 20% opacity + 1px border) smoothly slides behind the active tab icon using `@Namespace` and `.matchedGeometryEffect(id: "activeTab", in: namespace)`.
* **Micro-interaction**: Tapping an icon triggers an instantaneous scale bounce (`0.88 -> 1.08 -> 1.0`).

```swift
import SwiftUI

public enum NavigationTab: String, CaseIterable, Identifiable {
    case map = "Map"
    case nearby = "Nearby"
    case routes = "Routes"
    case packs = "Packs"
    case history = "History"
    case settings = "Settings"

    public var id: String { rawValue }

    public var iconName: String {
        switch self {
        case .map: return "map.fill"
        case .nearby: return "location.north.circle.fill"
        case .routes: return "point.topleft.down.curvedto.point.bottomright.up"
        case .packs: return "arrow.down.circle.fill"
        case .history: return "clock.arrow.circlepath"
        case .settings: return "gearshape.fill"
        }
    }
}

public struct FloatingGlassDock: View {
    @Binding public var selectedTab: NavigationTab
    @Namespace private var dockNamespace

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(NavigationTab.allCases) { tab in
                let isSelected = selectedTab == tab
                Button {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    withAnimation(MotionTokens.dockSlideSpring) {
                        selectedTab = tab
                    }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tab.iconName)
                            .font(.system(size: isSelected ? 20 : 18, weight: isSelected ? .bold : .regular))
                            .foregroundColor(isSelected ? DesignColors.coralPrimary : DesignColors.mineralMuted)
                        
                        Text(tab.rawValue)
                            .font(.system(size: 10, weight: isSelected ? .bold : .medium))
                            .foregroundColor(isSelected ? DesignColors.coralPrimary : DesignColors.mineralMuted)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background {
                        if isSelected {
                            Capsule()
                                .fill(DesignColors.coralGlow)
                                .overlay(
                                    Capsule()
                                        .strokeBorder(DesignColors.coralPrimary.opacity(0.4), lineWidth: 1)
                                )
                                .matchedGeometryEffect(id: "activeTabIndicator", in: dockNamespace)
                        }
                    }
                }
                .buttonStyle(TabBounceButtonStyle())
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .liquidGlass(cornerRadius: 34, isElevated: true)
        .padding(.horizontal, 18)
    }
}

private struct TabBounceButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.90 : 1.0)
            .animation(MotionTokens.quickBounce, value: configuration.isPressed)
    }
}
```

---

### 6.2 Top Liquid Glass Header Pill & Category Filter
* **Position**: Pinned `8pt` below the Dynamic Island / Status Bar.
* **Layout**:
  - Left pill: `"Fort Kochi Heritage Loop • 24 Stops"`.
  - Horizontal scrolling frosted chips: `All`, `Historic`, `Art`, `Dining`, `Scenic`.
  - Right circular button: Companion mode toggle with status indicator dot (Green = Screen Active, Amber = Geofence Only).

---

### 6.3 Top-Right Elevated Floating Map Controls (Compass & Recenter)
* Positioned vertically on the top right, `80pt` below the status bar:
  1. **Dual-Needle Navigation Compass**:
     - Circular `44x44pt` Liquid Glass button.
     - Canvas-drawn dual needle:
       - **North**: Crimson `#EA4335` triangle with white highlight.
       - **South**: Slate Steel `#90A4AE` triangle with shadow.
       - **Center**: Pivot disc in pure white with subtle drop shadow.
     - Rotating needle follows device/camera bearing; tapping rotates map smoothly to True North (`0.0°`).
  2. **Recenter Control**:
     - Circular `44x44pt` Liquid Glass button.
     - Tinted in Google Navigation Blue (`#1A73E8`).
     - Tapping snaps camera to GPS location and engages orientation follower mode.

---

### 6.4 Zoom-Adaptive Discovery Corridor & Spatial Collision Guard
Directly matching our Android breakthrough in `MapScreen.kt`:
* **Zoom < 16.0**: Displays minimal `CorridorDot` markers (scaling from `9pt` down to `4.5pt` with crisp white border), preventing card clutter over streets.
* **Zoom >= 16.0**: Gracefully expands into full `CalloutCard` overlays.
* **95pt Spatial Collision Guard**: If two pins are within `95pt` of screen space, only the nearest priority spot expands—the neighboring spot stays a dot, completely eliminating card overlap.

---

### 6.5 Dynamic Island & Live Activities (`ActivityKit`)
Implemented in `TravelGuideLiveActivity.swift` for iPhone 14 Pro, 15, and 16 series:
* **Compact Leading**: Mini Coral pin icon + `"120m"`.
* **Compact Trailing**: Animated sound wave icon indicating background audio guide.
* **Expanded Banner**:
  - Landmark Title (`"Vasco da Gama Square"`).
  - Subtitle (`"Next stop on Heritage Loop"`).
  - Interactive Action Button (`[▶ Play Story]`).
* **Lock Screen Widget**: Full-bleed Liquid Glass card with upcoming corridor timeline.

---

## 7. Complete iOS Technology Stack & Framework Inventory

| Framework / Kit | Technology Version | Core Responsibility |
| :--- | :--- | :--- |
| **SwiftUI** | iOS 16.0+ / iOS 17.0+ | Reactive declarative UI, Liquid Glass view modifiers, matched geometry |
| **ReplayKit** | `RPBroadcastSampleHandler` | Ingests live Google Maps screen buffers under 50 MB RAM ceiling |
| **Vision** | `VNRecognizeTextRequest` | On-device text OCR using Apple Neural Engine hardware |
| **ActivityKit** | iOS 16.1+ | Dynamic Island HUD and Lock Screen persistent Live Activity |
| **CoreLocation** | `CLLocationManager` | Continuous background GPS tracking, heading, and circular geofences |
| **AVFoundation** | `AVAudioSession` | Local audio playback with automatic `.duckOthers` over Google Maps speech |
| **CryptoKit** | `SecureEnclave.P256` | Hardware-backed cryptographic keys matching Android's KeyStore |
| **MapLibre Native iOS** | Metal Engine (SPM) | Hardware-accelerated offline vector/raster map rendering |
| **AppGroups / MMKV** | `group.guide.app.companion` | Zero-copy shared IPC memory between broadcast extension and main app |

---

## 8. Step-by-Step Xcode Implementation Blueprint & File Tree

### 8.1 Xcode Project Directory Structure
```
TravelGuide-iOS/
├── TravelGuide.xcodeproj
├── TravelGuide/
│   ├── App/
│   │   ├── TravelGuideApp.swift
│   │   └── AppEnvironment.swift
│   ├── DesignSystem/
│   │   ├── LiquidGlassKit.swift
│   │   ├── DesignTokens.swift
│   │   ├── MotionTokens.swift
│   │   └── HapticsEngine.swift
│   ├── UI/
│   │   ├── MainContainerView.swift
│   │   ├── Components/
│   │   │   ├── FloatingGlassDock.swift
│   │   │   ├── TopHeaderPill.swift
│   │   │   ├── CompassButton.swift
│   │   │   └── RecenterButton.swift
│   │   ├── Map/
│   │   │   ├── MapScreenView.swift
│   │   │   ├── MapLibreContainer.swift
│   │   │   ├── RoadGeometry.swift
│   │   │   └── CorridorCalloutCard.swift
│   │   └── Audio/
│   │       ├── AudioPlayerSheet.swift
│   │       └── NarrationManager.swift
│   ├── Core/
│   │   ├── LocationService.swift
│   │   ├── AudioDuckingManager.swift
│   │   ├── CompanionSyncReceiver.swift
│   │   └── SecureVault.swift
├── TravelGuideBroadcast/ (ReplayKit Upload Extension)
│   ├── Info.plist
│   └── SampleHandler.swift
└── TravelGuideWidgets/ (ActivityKit Live Activity)
    ├── Info.plist
    └── TravelGuideLiveActivity.swift
```

---

### 8.2 Execution Phases for Native Build

1. **Phase 1: Project Scaffolding & Entitlements**
   - Initialize Xcode project with Bundle Identifier `guide.app.travel`.
   - Add App Extension: `Broadcast Upload Extension` (`guide.app.travel.broadcast`).
   - Add Widget Extension: `ActivityKit Widget Extension` (`guide.app.travel.widgets`).
   - Enable App Group `group.guide.app.companion` on all three targets.
   - Configure `UIBackgroundModes`: `location`, `audio`.

2. **Phase 2: Liquid Glass Design System Primitives**
   - Implement `LiquidGlassKit.swift` with dynamic material stacking and specular highlights.
   - Implement `DesignTokens.swift` and `MotionTokens.swift` with spring curves.
   - Build `FloatingGlassDock.swift` with `.matchedGeometryEffect` tab pill indicator.

3. **Phase 3: Map Engine & Road Geometry**
   - Integrate `maplibre-gl-native-distribution` via Swift Package Manager.
   - Port `RoadGeometry.kt` Fort Kochi coordinates into `RoadGeometry.swift`.
   - Implement `MapScreenView.swift` with zoom-adaptive pins and 95pt collision protection.
   - Build custom Canvas `CompassButton` (Crimson North `#EA4335`, Steel South `#90A4AE`).

4. **Phase 4: Screen Companion & Live Activity**
   - Implement `SampleHandler.swift` with `VNRecognizeTextRequest` throttled to 0.5 FPS.
   - Implement `TravelGuideLiveActivity.swift` for Dynamic Island and Lock Screen.
   - Configure `AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])`.

5. **Phase 5: Secure Enclave & Final Polish**
   - Implement `SecureVault.swift` using `CryptoKit.SecureEnclave`.
   - Verify zero-network offline guarantee, dark mode adaptation, and dynamic font scaling.

---

*This document serves as the permanent, authoritative implementation specification for the Travel Guide iOS Companion.*
