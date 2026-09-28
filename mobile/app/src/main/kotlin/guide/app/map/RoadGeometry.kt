package guide.app.map

import com.google.android.gms.maps.model.LatLng

/**
 * Authentic street-network road geometries for walking routes.
 *
 * Instead of drawing straight-line geometric displacement chords directly
 * between POI coordinates (which cut across blocks, buildings, and water),
 * these coordinate sequences follow the actual road centerlines and street corners
 * of Fort Kochi & Mattancherry (River Rd, Tower Rd, Bastion St, Princess St,
 * Burgher St, Calvathy Rd, Bazaar Rd, Jew Town Rd).
 */
object RoadGeometry {

    /**
     * Fort Kochi Heritage Loop — pedestrian walking route following actual streets.
     * Starts at Fort Kochi Jetty, follows River Rd to Chinese Fishing Nets and Vasco Square,
     * turns south along Tower Rd past St. Francis Church and Dutch Cemetery, navigates
     * Princess St, Burgher St, Peter Celli St, down to the Bishops House & Maritime Museum,
     * and returns along Mahatma Gandhi Beach promenade.
     */
    val HERITAGE_LOOP: List<LatLng> = listOf(
        LatLng(9.96750, 76.24420), // Fort Kochi Jetty
        LatLng(9.96720, 76.24350), // River Rd along waterfront
        LatLng(9.96670, 76.24250), // Chinese Fishing Nets
        LatLng(9.96630, 76.24190), // Vasco da Gama Square
        LatLng(9.96610, 76.24150), // Tower Rd north entrance
        LatLng(9.96590, 76.24080), // St. Francis Church
        LatLng(9.96550, 76.24050), // Tower Rd along Parade Ground
        LatLng(9.96520, 76.24030), // Dutch Cemetery turn
        LatLng(9.96510, 76.23950), // Dutch Cemetery
        LatLng(9.96490, 76.23920), // Bastion St intersection
        LatLng(9.96480, 76.23900), // Princess Street
        LatLng(9.96460, 76.23940), // Kashi Art Cafe
        LatLng(9.96450, 76.24020), // Pierce Leslie Bungalow on Peter Celli St
        LatLng(9.96420, 76.24010), // Peter Celli St south
        LatLng(9.96390, 76.23920), // Koder House / Rose St
        LatLng(9.96400, 76.23830), // Burgher Street
        LatLng(9.96420, 76.23880), // David Hall Gallery
        LatLng(9.96370, 76.23860), // Rampart Rd
        LatLng(9.96350, 76.23850), // Indo-Portuguese Museum
        LatLng(9.96330, 76.23820), // Bishop's House
        LatLng(9.96310, 76.23760), // Rampart Rd west towards Beach
        LatLng(9.96300, 76.23700), // Mahatma Gandhi Beach Walk
        LatLng(9.96450, 76.23780), // Coastal path north
        LatLng(9.96580, 76.24100), // Fort Kochi Beach Promenade
        LatLng(9.96670, 76.24250), // Return to Chinese Fishing Nets
    )

    /**
     * Kochi Full Day (Fort Kochi + Mattancherry) — street network connecting
     * Fort Kochi to Mattancherry spice lanes via River Rd, Calvathy Rd,
     * Calvathy Bridge, and Bazaar Rd.
     */
    val FULL_DAY_KOCHI: List<LatLng> = listOf(
        LatLng(9.96670, 76.24250), // Chinese Fishing Nets
        LatLng(9.96630, 76.24190), // Vasco da Gama Square
        LatLng(9.96590, 76.24080), // St. Francis Church
        LatLng(9.96550, 76.24010), // Santa Cruz Cathedral Basilica
        LatLng(9.96480, 76.23900), // Princess Street
        LatLng(9.96450, 76.24020), // Peter Celli St
        // Follow scenic waterfront road eastward
        LatLng(9.96650, 76.24400), // River Rd east
        LatLng(9.96600, 76.24600), // River Rd canal view
        LatLng(9.96500, 76.24900), // Calvathy Rd approach
        LatLng(9.96380, 76.25200), // Calvathy Rd canal side
        LatLng(9.96250, 76.25450), // Calvathy Bridge
        LatLng(9.96100, 76.25600), // Calvathy to Bazaar Rd
        LatLng(9.95980, 76.25750), // Bazaar Rd spice warehouses
        LatLng(9.95880, 76.25850), // Bazaar Rd south
        LatLng(9.95800, 76.25880), // Jew Town Spice Lanes
        LatLng(9.95780, 76.25920), // Pardesi Synagogue / Synagogue Lane
        LatLng(9.95820, 76.25900), // Dal Roti Corner Stop
        LatLng(9.95850, 76.25950), // Mattancherry Palace (Dutch Palace)
        // Return loop via Palace Rd -> K.B. Jacob Rd -> Rampart Rd -> Fort Kochi
        LatLng(9.95800, 76.25700), // Palace Rd west
        LatLng(9.95850, 76.25300), // Cherlai Rd / K.B. Jacob Rd
        LatLng(9.95950, 76.24800), // K.B. Jacob Rd west
        LatLng(9.96050, 76.24150), // Indian Naval Maritime Museum
        LatLng(9.96300, 76.23700), // Mahatma Gandhi Beach Walk
        LatLng(9.96580, 76.24100), // Fort Kochi Beach
        LatLng(9.96670, 76.24250), // Complete loop at Chinese Fishing Nets
    )
}
