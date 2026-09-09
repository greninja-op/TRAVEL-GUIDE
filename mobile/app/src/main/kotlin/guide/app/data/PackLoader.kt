package guide.app.data

import android.content.Context
import org.json.JSONObject

/** Phase 0: loads the bundled v1 pack from assets. Phase 3 adds the
 *  packs manager (download/delete/sizes) around this loader. */
object PackLoader {
    data class PoiCard(
        val id: String, val name: String, val summary: String,
        val history: String, val funFacts: List<String>,
        val seeList: List<String>, val sources: List<String>,
        val packVersion: String,
    )

    fun load(context: Context, asset: String = "packs/fort-kochi-walk-v1.json"): Pair<String, List<PoiCard>> {
        val raw = context.assets.open(asset).bufferedReader().readText()
        val o = JSONObject(raw)
        val version = o.getString("pack_version")
        val cards = mutableListOf<PoiCard>()
        val arr = o.getJSONArray("pois")
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            cards += PoiCard(
                id = p.getString("id"), name = p.getString("name"),
                summary = p.getString("summary"), history = p.getString("history"),
                funFacts = List(p.getJSONArray("fun_facts").length()) { k -> p.getJSONArray("fun_facts").getString(k) },
                seeList = List(p.getJSONArray("see_list").length()) { k -> p.getJSONArray("see_list").getString(k) },
                sources = List(p.getJSONArray("sources").length()) { k -> p.getJSONArray("sources").getString(k) },
                packVersion = version,
            )
        }
        return version to cards
    }
}
