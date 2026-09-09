package guide.core

/** SPEC §4.3 data model. Room entities in mobile/ mirror these 1:1. */
data class Hours(val open: String, val close: String)

data class Poi(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Double,
    val category: String,
    val summary: String,
    val history: String,
    val isLegend: Boolean = false,
    val funFacts: List<String>,
    val seeList: List<String>,
    val hours: Hours?,
    val sources: List<String>,
    val packVersion: String,
)

data class Pack(
    val cityId: String,
    val cityName: String,
    val packVersion: String,
    val pois: List<Poi>,
    val routes: List<Route>,
)

data class Route(
    val id: String,
    val name: String,
    val orderedPoiIds: List<String>,
    val estMinutes: Int,
)

data class TriggerEvent(val poiId: String, val enteredAt: Long, val dwelledS: Long, val spoken: Boolean)
data class Visit(val poiId: String, val arrivedAt: Long, val leftAt: Long?, val packVersion: String)
data class Trip(val date: String, val visits: List<Visit>, val notes: Map<String, String> = emptyMap())
