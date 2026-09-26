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

data class LocalEvent(val id: String, val poiId: String, val title: String, val date: String)

object Events {
    /** Date-aware event pins; empty until a pack update ships events (SPEC §3.2). */
    fun forDate(all: List<LocalEvent>, yyyyMmDd: String): List<LocalEvent> =
        all.filter { it.date == yyyyMmDd }
}
