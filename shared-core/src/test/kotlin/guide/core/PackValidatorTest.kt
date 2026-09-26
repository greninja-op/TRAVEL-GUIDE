package guide.core

import kotlin.test.Test
import kotlin.test.assertTrue

/** Content-honesty + schema rules (SPEC §1.5, §4.3), mirrors tools/validate_pack.py. */
class PackValidatorTest {
    @Test
    fun `valid pack has no errors`() {
        val errors = PackValidator.validate(
            pack(poi("a", 9.0, 76.0), routes = listOf(Route("r", "R", listOf("a"), 10))),
        )
        assertTrue(errors.isEmpty(), "unexpected: $errors")
    }

    @Test
    fun `flags duplicate ids, missing sources, bad radius, unknown route poi`() {
        val noSource = poi("b", 9.0, 76.0).copy(sources = emptyList())
        val badRadius = poi("c", 9.0, 76.0).copy(radiusM = 5.0)
        val p = Pack(
            cityId = "city",
            cityName = "City",
            packVersion = "1",
            pois = listOf(poi("a", 9.0, 76.0), poi("a", 9.001, 76.0), noSource, badRadius),
            routes = listOf(Route("r", "R", listOf("a", "ghost"), 10)),
        )
        val errors = PackValidator.validate(p)
        assertTrue(errors.any { "duplicate" in it })
        assertTrue(errors.any { "missing sources" in it })
        assertTrue(errors.any { "radius_m" in it })
        assertTrue(errors.any { "unknown pois" in it })
    }

    @Test
    fun `flags empty pack and bad coords`() {
        assertTrue(PackValidator.validate(pack()).any { "no pois" in it })
        val bad = pack(poi("x", 999.0, 76.0))
        assertTrue(PackValidator.validate(bad).any { "bad coords" in it })
    }
}
