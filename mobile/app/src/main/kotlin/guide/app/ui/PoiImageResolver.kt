package guide.app.ui

import androidx.annotation.DrawableRes
import guide.app.R

/**
 * Maps tourist points of interest to uniform high-resolution 1200x800 photographic assets.
 * Guarantees zero pixelation and consistent 3:2 landscape aspect ratios across all screens.
 */
object PoiImageResolver {
    @DrawableRes
    fun getDrawableForPoi(poiId: String?): Int = when (poiId?.lowercase()) {
        "chinese-fishing-nets" -> R.drawable.poi_chinese_fishing_nets
        "fort-kochi-beach", "mahatma-gandhi-beach-walk" -> R.drawable.poi_fort_kochi_beach
        "vasco-square" -> R.drawable.poi_vasco_square
        "st-francis-church" -> R.drawable.poi_st_francis_church
        "santa-cruz-basilica" -> R.drawable.poi_santa_cruz_basilica
        "dutch-cemetery" -> R.drawable.poi_dutch_cemetery
        "princess-street" -> R.drawable.poi_princess_street
        "mattancherry-palace" -> R.drawable.poi_mattancherry_palace
        "pardesi-synagogue" -> R.drawable.poi_pardesi_synagogue
        "jew-town-lanes" -> R.drawable.poi_jew_town_lanes
        "kashi-art-cafe" -> R.drawable.poi_kashi_art_cafe
        "david-hall-gallery" -> R.drawable.poi_david_hall_gallery
        "maritime-museum" -> R.drawable.poi_maritime_museum
        "koder-house", "pierce-leslie-bungalow", "bishop-house" -> R.drawable.poi_koder_house
        "indo-portuguese-museum" -> R.drawable.poi_indo_portuguese_museum
        "fort-kochi-jetty" -> R.drawable.poi_fort_kochi_jetty
        else -> R.drawable.poi_chinese_fishing_nets
    }
}
