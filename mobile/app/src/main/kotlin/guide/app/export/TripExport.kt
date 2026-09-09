package guide.app.export

/** Phase 3 trip export: Markdown now (shares anywhere), PDF via Android
 *  Print framework rendering the same Markdown (WebView print adapter). */
object TripExport {
    fun toMarkdown(
        date: String,
        cityName: String,
        stops: List<Stop>,
        notes: Map<String, String>,
    ): String = buildString {
        appendLine("# Trip — $cityName ($date)")
        appendLine()
        stops.forEachIndexed { i, s ->
            appendLine("## ${i + 1}. ${s.name}")
            appendLine()
            appendLine(s.summary)
            appendLine()
            notes[s.poiId]?.let {
                appendLine("> My note: $it")
                appendLine()
            }
        }
    }

    data class Stop(val poiId: String, val name: String, val summary: String)
}
