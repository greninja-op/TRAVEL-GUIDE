package guide.app.power

/** Phase 3 battery profiles (SPEC §1.3 honesty: cost is always disclosed). */
enum class BatteryProfile(val label: String, val intervalS: Long, val minDisplacementM: Float) {
    PRECISE("Precise (drains faster)", 5, 5f),
    BALANCED("Balanced", 15, 15f),
    SAVER("Saver (may delay stories)", 60, 50f),
}
