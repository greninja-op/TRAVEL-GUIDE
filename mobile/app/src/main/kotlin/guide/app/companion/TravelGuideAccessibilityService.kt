package guide.app.companion

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import guide.app.navigation.CompanionSource
import guide.app.navigation.MapsCompanionState

/**
 * Travel Guide Navigation Companion — Accessibility Service.
 *
 * Silently, locally, and privately synchronizes active turn-by-turn navigation
 * from Google Maps (com.google.android.apps.maps).
 *
 * Reactive Lifecycle:
 * - When navigation starts in Google Maps: detects destination, activates corridor polyline.
 * - When navigation ends or is canceled in Google Maps: immediately calls onNavEnded(),
 *   clearing the route polyline while leaving all tourist pins and GPS marker visible.
 */
class TravelGuideAccessibilityService : AccessibilityService() {

    private var lastExtractedDestination: String? = null
    private var lastExtractTimeMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            packageNames = arrayOf("com.google.android.apps.maps")
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 150
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName != "com.google.android.apps.maps") return

        // 1. Detect explicit Exit Navigation button clicks immediately
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val clickedText = event.text?.joinToString(" ").orEmpty()
            val clickedDesc = event.contentDescription?.toString().orEmpty()
            val combined = "$clickedText $clickedDesc"
            if (combined.contains("Exit navigation", ignoreCase = true) ||
                combined.contains("Close navigation", ignoreCase = true) ||
                combined.contains("End route", ignoreCase = true) ||
                combined.contains("Cancel route", ignoreCase = true)
            ) {
                lastExtractedDestination = null
                MapsCompanionState.onNavEnded()
                return
            }
        }

        // 2. Throttle tree inspection to 1.2 Hz
        val now = System.currentTimeMillis()
        if (now - lastExtractTimeMs < 800) return
        lastExtractTimeMs = now

        val rootNode = rootInActiveWindow ?: return
        try {
            val navState = inspectNavigationState(rootNode)

            if (navState.isNavigating && !navState.destination.isNullOrBlank()) {
                if (navState.destination != lastExtractedDestination || MapsCompanionState.currentSession == null) {
                    lastExtractedDestination = navState.destination
                    MapsCompanionState.onNavStarted(
                        destinationName = navState.destination,
                        etaOrDistance = navState.eta ?: "Google Maps Active Route",
                        source = CompanionSource.ACCESSIBILITY,
                    )
                }
            } else if (!navState.isNavigating) {
                // If Google Maps is no longer in navigation mode (e.g. back to search / explore screen)
                if (MapsCompanionState.currentSession != null &&
                    (MapsCompanionState.currentSession?.source == CompanionSource.ACCESSIBILITY || navState.isStandardSearchScreen)
                ) {
                    lastExtractedDestination = null
                    MapsCompanionState.onNavEnded()
                }
            }
        } catch (_: Exception) {
            // Defensive traversal
        }
    }

    private data class NavScanResult(
        val isNavigating: Boolean = false,
        val destination: String? = null,
        val eta: String? = null,
        val isStandardSearchScreen: Boolean = false,
    )

    private fun inspectNavigationState(root: AccessibilityNodeInfo): NavScanResult {
        var hasExitBtn = false
        var hasNavCue = false
        var isSearchScreen = false
        var foundDest: String? = null
        var foundEta: String? = null

        fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 7) return

            // Security Hardening (OWASP M1, M2):
            // 1. Strict Keylogger & Credential Scraping Prevention: Disallow reading password fields or input fields
            if (node.isPassword || node.isEditable) return

            // 2. Process package isolation: Drop any node from untrusted origins
            val pkg = node.packageName?.toString().orEmpty()
            if (pkg.isNotEmpty() && pkg != "com.google.android.apps.maps") return

            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()
            val content = "$text $desc"

            if (content.isNotBlank()) {
                // Exit navigation button detection
                if (content.contains("Exit navigation", ignoreCase = true) ||
                    content.contains("Close navigation", ignoreCase = true) ||
                    content.contains("Re-center", ignoreCase = true)
                ) {
                    hasExitBtn = true
                }

                // Standard non-navigating Google Maps screen indicators
                if (content.contains("Search here", ignoreCase = true) ||
                    content.contains("Search Google Maps", ignoreCase = true) ||
                    content.equals("Explore", ignoreCase = true)
                ) {
                    isSearchScreen = true
                }

                // Navigation cues
                if (content.startsWith("Directions to ", ignoreCase = true)) {
                    hasNavCue = true
                    foundDest = content.substringAfter("Directions to ").trim()
                } else if (content.startsWith("Navigating to ", ignoreCase = true)) {
                    hasNavCue = true
                    foundDest = content.substringAfter("Navigating to ").trim()
                } else if (content.startsWith("Head ", ignoreCase = true) ||
                    content.contains("toward ", ignoreCase = true) ||
                    content.contains("Turn ", ignoreCase = true) ||
                    content.contains("Destination will be", ignoreCase = true)
                ) {
                    hasNavCue = true
                }

                // ETA line: e.g. "15 min (4.2 km)" or "5:30 PM • 12 min"
                if (content.contains("min") && (content.contains("km") || content.contains("m") || content.contains("hr"))) {
                    hasNavCue = true
                    foundEta = content
                    // Often the destination is a sibling or parent text
                    val parent = node.parent
                    if (parent != null && foundDest == null) {
                        for (i in 0 until parent.childCount) {
                            val sib = parent.getChild(i)
                            if (sib == null || sib.isPassword || sib.isEditable) continue
                            val sibText = sib.text?.toString()?.trim()
                            if (!sibText.isNullOrBlank() && sibText != content && sibText.length in 3..50) {
                                foundDest = sibText
                                break
                            }
                        }
                    }
                }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i), depth + 1)
            }
        }

        traverse(root, depth = 0)

        val isNav = (hasExitBtn || hasNavCue) && !isSearchScreen
        val sanitizedDest = foundDest
            ?.replace(Regex("""[^\w\s.,'#\-]"""), " ")
            ?.trim()
            ?.take(80)

        return NavScanResult(
            isNavigating = isNav,
            destination = sanitizedDest ?: if (isNav) "Active Google Maps Route" else null,
            eta = foundEta?.take(40),
            isStandardSearchScreen = isSearchScreen,
        )
    }

    override fun onInterrupt() {
        // Required callback
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${TravelGuideAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabledServices.contains(expectedServiceName) || enabledServices.contains(TravelGuideAccessibilityService::class.java.simpleName)
        }
    }
}
