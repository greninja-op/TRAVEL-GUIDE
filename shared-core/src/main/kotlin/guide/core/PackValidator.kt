package guide.core

/** Phase 0 pack rules. Mirrored by tools/validate_pack.py. */
object PackValidator {
    fun validate(pack: Pack): List<String> {
        val errors = mutableListOf<String>()
        if (pack.pois.isEmpty()) errors += "pack has no pois"
        val ids = pack.pois.map { it.id }
        if (ids.size != ids.toSet().size) errors += "duplicate poi ids"
        for (p in pack.pois) {
            if (p.lat !in -90.0..90.0 || p.lng !in -180.0..180.0) errors += "${p.id}: bad coords"
            if (p.radiusM !in 10.0..500.0) errors += "${p.id}: radius_m out of 10..500"
            if (p.sources.isEmpty()) errors += "${p.id}: missing sources (SPEC §1.5)"
            if (p.summary.length > 140) errors += "${p.id}: summary > 140 chars"
        }
        val known = ids.toSet()
        for (r in pack.routes) {
            val missing = r.orderedPoiIds.filter { it !in known }
            if (missing.isNotEmpty()) errors += "route ${r.id}: unknown pois $missing"
        }
        return errors
    }
}
