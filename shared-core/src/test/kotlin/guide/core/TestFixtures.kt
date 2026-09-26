package guide.core

/** Shared builders so tests read like the SPEC scenarios, not JSON plumbing. */
internal fun poi(
    id: String,
    lat: Double,
    lng: Double,
    radiusM: Double = 60.0,
    layer: String = "heritage",
    hours: Hours? = null,
): Poi = Poi(
    id = id,
    name = id.uppercase(),
    lat = lat,
    lng = lng,
    radiusM = radiusM,
    category = "test",
    layer = layer,
    summary = "summary",
    history = "history",
    funFacts = listOf("fact"),
    seeList = listOf("look up"),
    hours = hours,
    sources = listOf("test-source"),
    packVersion = "0.0.1",
)

internal fun pack(vararg pois: Poi, routes: List<Route> = emptyList()): Pack = Pack(
    cityId = "test-city",
    cityName = "Test City",
    packVersion = "0.0.1",
    pois = pois.toList(),
    routes = routes,
)
