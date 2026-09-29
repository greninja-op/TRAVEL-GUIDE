package guide.app.extras

/** Phase 2 extras: phrasebook (10 survival phrases, TTS playback) + events layer. */
object Phrasebook {
    val phrases = listOf(
        "Hello!" to "Namaskaram!",
        "Thank you" to "Nanni!",
        "How much?" to "Ethra aanu?",
        "Where is…?" to "…evideyaanu?",
        "Too spicy" to "Erivu kooduthal!",
        "Bill, please" to "Bill tharumo?",
        "Beautiful place" to "Manoharamaya sthalam!",
        "Help!" to "Sahaayikku!",
        "Water" to "Vellam",
        "Goodbye" to "Pinne kaanam!",
    )
}

data class LocalEvent(
    val id: String,
    val poiId: String,
    val title: String,
    val date: String,
    val note: String? = null,
)

object Events {
    /**
     * Date-aware event filtering.
     * Matches exact yyyy-MM-dd date, wildcards ("*"), or current month.
     * Falls back to recurring cultural events so tourists always have live context.
     */
    fun forDate(all: List<LocalEvent>, yyyyMmDd: String): List<LocalEvent> {
        val exact = all.filter { it.date == yyyyMmDd }
        if (exact.isNotEmpty()) return exact
        val currentMonth = yyyyMmDd.take(7)
        val monthOrWildcard = all.filter { it.date == "*" || it.date.startsWith(currentMonth) }
        return if (monthOrWildcard.isNotEmpty()) monthOrWildcard else all
    }

    /** Loads bundled events from assets. */
    fun loadFromAssets(context: android.content.Context): List<LocalEvent> {
        return try {
            val jsonStr = context.assets.open("events/kochi-events.json").bufferedReader().use { it.readText() }
            val root = org.json.JSONObject(jsonStr)
            val arr = root.optJSONArray("events") ?: return emptyList()
            val list = mutableListOf<LocalEvent>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    LocalEvent(
                        id = obj.getString("id"),
                        poiId = obj.getString("poi_id"),
                        title = obj.getString("title"),
                        date = obj.optString("date", "*"),
                        note = if (obj.has("note")) obj.getString("note") else null,
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
