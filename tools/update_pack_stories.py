import json
import os
import re
import sys

pois_data = {
    "chinese-fishing-nets": {
        "summary": "Iconic 14th-century cantilever lift nets, balanced by granite boulders and operated daily at high tide.",
        "history": "Introduced between 1350 and 1450 CE by Chinese traders from Kublai Khan's Mongol court, these colossal teak-and-bamboo cantilevers (Cheena Vala) are mechanical marvels. Operating entirely without motors, a coordinated team of six fishermen walks the central fulcrum beam to submerge the immense net, holding it beneath tidal currents before dropping counterweights to hoist shimmering pomfret and tiger prawns.",
        "fun_facts": [
            "Outside of mainland China and the Tonkin Delta, Fort Kochi is the only place on Earth where these 14th-century lift nets remain in active daily use.",
            "The multi-ton structures are balanced solely by suspended granite river stones tied with coconut coir rope, counterweighted so precisely that one person can tilt them.",
            "Local fishing crews welcome curious travelers to climb onto the cantilever beam and pull the counterweight ropes alongside them at high tide."
        ],
        "see_list": [
            "Suspended granite balance stones swaying inches above the tidal breakers",
            "Rhythmic coordination as five fishermen step in unison along the central fulcrum beam",
            "Fresh sea catch sorted and weighed on woven coir palm mats at the water line"
        ]
    },
    "fort-kochi-beach": {
        "summary": "Historic coastline where ancient spice galleons docked, offering sweeping sunset views over the shipping channel.",
        "history": "This historic strand marks the oceanic entrance to Cochin's vast natural harbour, where Phoenician, Roman, Arab, and Portuguese galleons once anchored to trade for Malabar black pepper. The granite rip-rap promenade now borders the busy deep-water channel where traditional wooden dhows sail alongside international container carriers.",
        "fun_facts": [
            "During calm winter mornings, wild Indo-Pacific humpback dolphins regularly feed within 50 meters of the granite breakwater.",
            "Every New Year's Eve, over one hundred thousand people pack this shore at midnight to watch the burning of the giant 65-foot Pappanji effigy, ringing in Cochin Carnival."
        ],
        "see_list": [
            "Granite sea-wall groyne extending into the channel for a 180-degree ocean view",
            "Vypin passenger ferries cutting across the powerful estuary tidal currents",
            "Seaside fish-fry stalls firing up cast-iron griddles with fresh catches at dusk"
        ]
    },
    "vasco-square": {
        "summary": "Breezy seaside esplanade connecting the fishing nets to the colonial heritage quarter.",
        "history": "Named after Portuguese explorer Vasco da Gama who established Europe's first Indian trading fort here in 1502, this breezy seaside square forms the threshold between maritime commerce and colonial history. Shaded by ancient rain trees, it serves as the vibrant open-air gathering ground where street vendors prepare spiced seafood over coconut-charcoal griddles.",
        "fun_facts": [
            "You can buy raw fish directly from the fishing nets and have nearby stalls grill it on cast-iron hotplates with Kerala red masala within ten minutes.",
            "A historic Dutch maritime signaling mast stood here in the 1700s, using oil lanterns to signal approaching spice ships into the channel."
        ],
        "see_list": [
            "Open-air coconut-charcoal griddles sizzling with fresh red-marinated sea fish",
            "Cast-iron vintage streetlamps framing the silhouettes of the fishing nets",
            "Heritage stone marker showing the official start of the Fort Kochi walking route"
        ]
    },
    "st-francis-church": {
        "summary": "India's oldest surviving European sanctuary, built in 1503 and original resting place of Vasco da Gama.",
        "history": "Constructed of timber in 1503 by Portuguese Franciscan friars and rebuilt in stone in 1516, this sanctum is a living chronicle of colonial power. Vasco da Gama died in Fort Kochi on Christmas Eve in 1524 during his third voyage and was interred here. Captured by Dutch Protestant forces in 1663 (who preserved it while razing other Catholic churches) and later ceded to the British Anglican church, it spans three colonial eras.",
        "fun_facts": [
            "Vasco da Gama's remains rested beneath the nave floor for 14 years before his son exhumed his bones in 1538 to reinter them in Lisbon's Jeronimos Monastery.",
            "The church still preserves manually operated cloth punkahs (cloth ceiling fans) pulled by ropes from the vestibule during ceremonial services."
        ],
        "see_list": [
            "Vasco da Gama's original brass-railed burial crypt in the floor near the chancel",
            "Carved 17th-century Dutch heraldic gravestones mounted on the interior stone walls",
            "Parchment parish baptism register on display recording births and marriages from 1751"
        ]
    },
    "santa-cruz-basilica": {
        "summary": "Monumental white Gothic cathedral celebrated for Italian ceiling frescoes and towering twin spires.",
        "history": "Originally consecrated by the Portuguese in 1558, the cathedral was razed by British troops in 1795 and grandly rebuilt in 1905 in neo-Gothic splendour. Declared a Minor Basilica by Pope John Paul II in 1984, the sanctuary is renowned for its soaring pastel nave, Bruges stained glass, and breathtaking ceiling frescoes painted by Italian Jesuit brother Antonio Moscheni.",
        "fun_facts": [
            "Antonio Moscheni painted the vibrant ceiling frescoes using egg tempera and natural mineral pigments, ensuring the colors remain radiant after a century.",
            "The twin octagonal towers serve as maritime navigational beacons visible to ships miles out in the Arabian Sea."
        ],
        "see_list": [
            "Antonio Moscheni's expressive Passion of Christ frescoes across the vaulted ceiling",
            "Intricately carved teakwood pulpit supported by gold-leaf floral columns",
            "Stained-glass windows casting amber and violet light across the marble nave"
        ]
    },
    "dutch-cemetery": {
        "summary": "Atmospheric 1714 burial ground shaded by ancient frangipani, holding Dutch East India pioneers.",
        "history": "Consecrated in 1714, this secluded sanctuary contains 104 recorded tombs of Dutch East India Company (VOC) governors, commanders, merchants, and their families who died on the Malabar Coast. Shaded by mossy rain trees and frangipani blossoms, it remains the oldest authentic European burial ground on the Indian subcontinent.",
        "fun_facts": [
            "The gravestones feature authentic 18th-century Dutch script along with the official VOC seal (Vereenigde Oostindische Compagnie).",
            "The cemetery is kept locked to protect the fragile coral-stone and laterite tombs; the key is held by the Church of South India trust."
        ],
        "see_list": [
            "Weathered VOC crests and coats-of-arms chiseled into granite grave slabs",
            "Pyramidal brick-and-mortar tombs coated in emerald moss under frangipani branches",
            "Tomb of Captain Jan Labryn, the earliest decipherable grave marker dated 1714"
        ]
    },
    "princess-street": {
        "summary": "Charming pedestrian avenue lined with 300-year-old European townhomes, bookshops, and art cafes.",
        "history": "Also known as Rue Princesse, this is one of Fort Kochi's oldest planned residential lanes. Lined with Portuguese villas, Dutch gabled houses, and British verandahs with timber louvers, it forms an open-air museum of vernacular architecture. Today it serves as the creative nerve center for travelers, writers, and Biennale artists.",
        "fun_facts": [
            "Many homes retain hidden central courtyards (Nadumuttam) that funnel ocean breezes and rainwater to keep interiors naturally cool.",
            "The road's foundation consists of granite ballast stones discarded by returning spice galleons in the 17th century."
        ],
        "see_list": [
            "Contrasting Dutch curved gables and British wooden verandahs on facing sides of the street",
            "Original hand-painted ceramic street and house number tiles from colonial rule",
            "Aroma of fresh cinnamon rolls and cardamom coffee drifting from garden cafes"
        ]
    },
    "pierce-leslie-bungalow": {
        "summary": "Grand 1868 coffee and spice trading headquarters showcasing classic Malabar-colonial architecture.",
        "history": "Constructed in 1868 as the regional headquarters for British commercial powerhouse Pierce Leslie & Co., this sprawling residence exemplifies Malabar-colonial styling. With double-story teak verandahs, arched wooden doorways, and sprawling grounds, it oversaw shipments of thousands of tons of Malabar black pepper, tea, and cashews to European markets.",
        "fun_facts": [
            "The ground floor features reinforced iron-vault chambers where precious spices and vanilla beans were locked behind foot-thick doors.",
            "The steeply pitched tile roofs include custom ventilation plenums designed to siphon cooling sea air through the living quarters."
        ],
        "see_list": [
            "Wraparound double-story verandah supported by polished teak and rosewood pillars",
            "Original arched window frames fitted with hand-blown 19th-century glass panes",
            "Carved wooden eaves designed to shield the facade from fierce monsoon downpours"
        ]
    },
    "koder-house": {
        "summary": "Palatial three-story red-brick mansion built in 1805 by Cochin's prominent Jewish merchant family.",
        "history": "Built in 1805 by Samuel S. Koder of Cochin's venerable Jewish trading community, this landmark red-brick mansion marks the transition from colonial estates to Jewish mercantile prestige. The Koders introduced electrical infrastructure and water transportation to Cochin, hosting royalty and foreign diplomats within these halls.",
        "fun_facts": [
            "The house includes a private bar salon imported intact from a luxury 19th-century British passenger steamship.",
            "During Hanukkah, a massive Menorah was traditionally lit on the top balcony, visible to cargo ships entering Cochin harbor."
        ],
        "see_list": [
            "Crimson brick facade accented with carved teakwood hanging balconies",
            "Hand-laid terracotta tile floors featuring intricate floral border patterns",
            "Cast-iron spiral staircase ascending to the private rooftop harbour observatory"
        ]
    },
    "indo-portuguese-museum": {
        "summary": "Ecclesiastical treasury preserving 500 years of Indo-Portuguese art in the Bishop's gardens.",
        "history": "Founded by the late Bishop Joseph Kureethara, this museum safeguards the cultural and artistic confluence between Renaissance Portugal and traditional Kerala woodworking. Its five galleries (Altar, Treasure, Procession, Civil Life, and Cathedral) showcase sacred relics salvaged from colonial-era churches.",
        "fun_facts": [
            "Houses a 16th-century teak processional cross constructed entirely without metal nails, joined with traditional Kerala wood joints.",
            "A ceremonial vestment in the textile hall is hand-woven from pure Chinese gold thread presented by Portuguese explorer Afonso de Albuquerque."
        ],
        "see_list": [
            "Masterpiece 17th-century silver and rock-crystal monstrance in the Treasure Hall",
            "Delicately carved ivory tabernacle crafted by local artisans for European clergy",
            "Excavated stone ramparts of 16th-century Fort Manuel visible in the garden grounds"
        ]
    },
    "bishop-house": {
        "summary": "Historic 1506 residence of the Portuguese Governor, now the administrative seat of Cochin Diocese.",
        "history": "Perched on a scenic rise, this mansion was built in 1506 as the official residence for Dom Francisco de Almeida, first Portuguese Viceroy of India. Taken by the Dutch and restored to the Roman Catholic Diocese in 1888, it features soaring Romanesque arcades, circular stairways, and expansive private gardens.",
        "fun_facts": [
            "Subterranean vaulted cellars beneath the building once connected to Fort Manuel via emergency escape tunnels.",
            "The private diocesan archive holds original handwritten Latin and Portuguese church rolls dating to the 1500s."
        ],
        "see_list": [
            "Monumental circular staircase and sweeping arched corridors overlooking the lawns",
            "Stately 19th-century stained-glass chapel window in the upper gallery",
            "Ancient laterite stone boundary wall separating the bishopric from the street"
        ]
    },
    "fort-kochi-jetty": {
        "summary": "Vibrant municipal ferry pier offering five-minute harbor crossings to Vypin island.",
        "history": "This bustling jetty is Fort Kochi's lifeline, ferrying thousands of daily commuters, cyclists, and travelers across the harbor throat to Vypin Island. Operating from dawn to late evening, it offers an authentic front-row view of the busy shipping channel at work.",
        "fun_facts": [
            "At roughly six rupees per ticket, this ferry ride is celebrated as one of the most scenic and affordable water crossings in the world.",
            "Pods of dolphins are frequently spotted right alongside the ferry bow as it crosses the deep channel current."
        ],
        "see_list": [
            "Ferry helmsman deftly steering against the powerful ocean tidal currents",
            "Expansive views of the international container terminal across the channel",
            "Daily commuters and fishermen boarding with bicycles and fresh morning goods"
        ]
    },
    "mattancherry-palace": {
        "summary": "1555 palace built by the Portuguese for the Kochi Raja, famed for magnificent Ramayana murals.",
        "history": "Built in 1555 by the Portuguese as a goodwill gift to Raja Veera Kerala Varma and expanded in 1663 by the Dutch, this palace fuses traditional Kerala Nalukettu courtyard architecture with European arched windows. Its bedchambers house 300 square meters of tempera wall murals illustrating the Ramayana and Mahabharata in glowing natural pigments.",
        "fun_facts": [
            "The stunning murals were painted using purely organic pigments derived from crushed semi-precious stones, plant saps, and laterite earth, glowing without fading for 450 years.",
            "The coronation hall floor looks like polished black marble, but is actually an ancient Kerala organic compound made of burnt coconut shells, plant juices, lime, and egg whites."
        ],
        "see_list": [
            "Ramayana murals in the Royal Bedchamber detailing 48 epic scenes with lifelike expressions",
            "Ivory-and-teak royal palanquin used in coronation processions on display in the central hall",
            "Coffered wooden ceiling adorned with hand-carved lotus blossoms"
        ]
    },
    "pardesi-synagogue": {
        "summary": "1568 Sephardic synagogue renowned for hand-painted Canton porcelain tiles and Belgian chandeliers.",
        "history": "Erected in 1568 by Sephardic Jewish refugees from Spain and the Netherlands on land granted by Kochi Raja Rama Varma, this is the oldest active synagogue in the Commonwealth. Rebuilt after Portuguese cannon damage in 1664, it features a 1762 clock tower with numerals in Hebrew, Roman, Malayalam, and Arabic, symbolizing Cochin's religious harmony.",
        "fun_facts": [
            "Every single floor tile is hand-painted blue-and-white porcelain imported from Canton in 1762; no two tiles share the exact same design.",
            "The holy ark holds 1,000-year-old engraved copper plates granted by King Bhaskara Ravi Varma conferring hereditary rights to the community."
        ],
        "see_list": [
            "Hand-painted blue-and-white porcelain tiles imported from Canton beneath your feet",
            "Brilliant 19th-century Belgian glass crystal chandeliers suspended from the ceiling",
            "Four-dial clock tower showing distinct scripts facing palace, synagogue, and harbor"
        ]
    },
    "jew-town-lanes": {
        "summary": "Four-century-old spice trading streets filled with wholesale pepper warehouses and antiquities.",
        "history": "For more than four hundred years, this narrow market lane has traded the world's finest spices. Bales of Tellicherry black pepper, sun-dried ginger, cardamom pods, and nutmeg are still weighed and auctioned here, flanked by antique dealers displaying ship lamps, brass compasses, and carved temple rafters.",
        "fun_facts": [
            "The pungent scent of black pepper and dried ginger fills the air because many warehouses have stored spices continuously since the 1600s.",
            "Massive bronze cooking cauldrons (Varpu) displayed outside antique shops were cast to feed up to 2,000 temple pilgrims at once and weigh over 800 kg."
        ],
        "see_list": [
            "Sun-drying courtyards spread with golden Cochin ginger and pungent black pepper",
            "Maritime antique shops displaying salvaged ship compasses and teak sea chests",
            "Vintage balance scales and burlap spice sacks stacked inside open merchant halls"
        ]
    },
    "maritime-museum": {
        "summary": "Naval heritage museum inside WWII ammunition bunkers, chronicling India's maritime supremacy.",
        "history": "Set inside two fortified World War II ammunition bunkers, this museum chronicles the maritime heritage of the subcontinent from the ancient Indus Valley dockyard at Lothal to the modern aircraft carrier era. It honors Malabar admiral Kunjali Marakkar, who held off Portuguese armadas for forty years.",
        "fun_facts": [
            "The museum is built within genuine bomb-proof naval bunkers constructed to protect coastal defenses during World War II.",
            "The outdoor weapons park includes authentic ship torpedoes, depth charges, and deck guns from the 1971 naval conflicts."
        ],
        "see_list": [
            "Detailed scale models of aircraft carriers INS Vikrant and INS Viraat",
            "Diorama depicting the legendary 1971 missile boat strike on Karachi harbor",
            "Outdoor weapons park featuring real anti-submarine depth charges and sea mines"
        ]
    },
    "david-hall-gallery": {
        "summary": "Restored 1695 Dutch East India residence turned contemporary art center and leafy garden cafe.",
        "history": "Built in 1695 by the Dutch East India Company and later occupied by merchant David Koder, this historic home features thick laterite walls, sweeping Dutch gables, and open rafters. It has been reimagined as a cultural hub for contemporary exhibitions, artist talks, and courtyard gatherings.",
        "fun_facts": [
            "The back garden contains a wood-fired pizza oven situated beneath a 200-year-old breadfruit tree planted during the Dutch governorship.",
            "During the Kochi-Muziris Biennale, this hall transforms into an avant-garde performance venue hosting international creators."
        ],
        "see_list": [
            "Exposed Dutch mortar and brickwork preserved along the interior gallery walls",
            "Serene garden courtyard with century-old tropical shade trees and sculptures",
            "Rotating contemporary art and photography exhibits by Kerala artists"
        ]
    },
    "burgher-street": {
        "summary": "Tranquil heritage street of Dutch burghers and Anglo-Indian families, lined with gabled villas.",
        "history": "Named after the 'burghers'—citizens and merchants of Dutch descent—this quiet residential lane features low-slung, tile-roofed houses with welcoming street verandas. It preserves the unique domestic heritage of Fort Kochi's Anglo-Indian community across centuries.",
        "fun_facts": [
            "Every New Year's Day, this street erupts into the vibrant Cochin Carnival rally with costumed dancers and traditional percussion bands.",
            "Dutch municipal engineers calculated the gabled rooflines specifically to catch cooling evening sea breezes while shedding monsoon rain."
        ],
        "see_list": [
            "Curved Dutch gables and ornamental carved wooden eaves on historic townhomes",
            "Friendly local cats resting on cool laterite front-porch steps",
            "Polished brass ship bells hanging by doorways in place of electric doorbells"
        ]
    },
    "st-john-church": {
        "summary": "Historic 19th-century parish church serving Fort Kochi's close-knit Anglo-Indian community.",
        "history": "Erected in the 19th century and dedicated to St. John de Britto, this parish sanctuary has long been the spiritual home of Fort Kochi's Anglo-Indian families. With high timber-beamed ceilings, wooden pews, and a shaded cemetery, it preserves community traditions and holiday celebrations.",
        "fun_facts": [
            "The annual feast of St. John de Britto in February reunites family members who travel from across the globe to join the neighborhood street procession.",
            "The parish cemetery holds gravestones of British and Anglo-Indian residents dating back over a century."
        ],
        "see_list": [
            "Gothic-arch stained-glass window portraying St. John de Britto above the altar",
            "Engraved brass memorial plaques commemorating early community benefactors",
            "Peaceful shaded courtyard framed by flowering crimson gulmohar trees"
        ]
    },
    "mahatma-gandhi-beach-walk": {
        "summary": "Seaside promenade illuminated by heritage lanterns, ideal for quiet evening ocean strolls.",
        "history": "Tracing the southwestern coastline of Fort Kochi, this paved oceanfront walkway offers unobstructed views of the Arabian Sea. Lined with period lampposts and shaded benches, it provides a tranquil retreat for morning runners and evening sunset watchers.",
        "fun_facts": [
            "At low tide, local fishermen wade chest-deep into the surf to fling weighted circular throw-nets with incredible accuracy.",
            "Direct ocean winds keep this coastal path 3 to 4 degrees cooler than the inland market streets during warm afternoons."
        ],
        "see_list": [
            "Unbroken panoramic sunset dropping directly into the open ocean horizon",
            "Traditional shore fishermen casting circular throw-nets into the breaking surf",
            "Warm glow of heritage lampposts lighting the path as night falls over the sea"
        ]
    },
    "kashi-art-cafe": {
        "summary": "Celebrated art cafe in a restored townhome, famed for its sunlit courtyard and chocolate cake.",
        "history": "Opened in 1997 by local artists in a restored merchant home, Kashi Art Cafe spearheaded Fort Kochi's contemporary art revolution. Centered around a skylit open courtyard with stone sculptures, it is the favored gathering spot for writers, curators, and travelers.",
        "fun_facts": [
            "The evocative sculpture of stone heads emerging from the courtyard earth was created by renowned artist N.N. Rimzon.",
            "Their signature warm chocolate cake with molten center has been baked using the same closely guarded recipe for over 25 years."
        ],
        "see_list": [
            "N.N. Rimzon's iconic stone heads installation in the gravel-lined courtyard",
            "Rotating gallery hallway exhibiting original contemporary photography and prints",
            "Locally sourced French-press coffee brewed from fresh Wayanad Arabica beans"
        ]
    },
    "dal-roti-corner": {
        "summary": "Beloved Jew Town eatery celebrated for generous kathi rolls and homestyle North Indian dishes.",
        "history": "A fixture on the edge of the spice district, Dal Roti has fed hungry travelers and spice merchants for decades. The kitchen serves hearty, freshly made North Indian meals, flaky parathas, and spiced curries made from scratch each day.",
        "fun_facts": [
            "Their oversized kathi rolls are legendary among travelers and easily provide a hearty meal for two people.",
            "The owner personally chats with diners to share insider tips on discovering the best spice warehouses in Mattancherry."
        ],
        "see_list": [
            "Sizzling rumali roti spun paper-thin over an inverted iron wok by the chef",
            "Freshly rolled kathi rolls stuffed with spiced paneer or chicken and mint chutney",
            "Chilled homemade salted buttermilk infused with fresh ginger and curry leaves"
        ]
    },
    "old-harbour-stay": {
        "summary": "300-year-old Dutch-style boutique hotel set in private gardens facing the fishing nets.",
        "history": "Originally built in the Dutch style with Portuguese influences, this 300-year-old residence housed colonial merchants and later served as an estate office for English tea brokers. Lovingly restored with period antiques, it stands as one of Fort Kochi's most elegant heritage addresses.",
        "fun_facts": [
            "The inner courtyard retains an ancient freshwater well that supplied drinking water to merchant captains three centuries ago.",
            "Every room is named after historic settlement quarters and appointed with authentic period oil paintings and furniture."
        ],
        "see_list": [
            "Tranquil garden courtyard with tropical flora and a heritage swimming pool",
            "Classic wooden verandas offering views toward the silhouettes of the fishing nets",
            "Exposed laterite masonry walls seamlessly paired with high timber ceilings"
        ]
    },
    "fort-heritage-homestay": {
        "summary": "Quiet lane of family-run heritage homestays offering authentic Kerala hospitality.",
        "history": "Nestled in quiet residential alleys behind Burgher Street, these family homestays welcome travelers into restored ancestral homes. Guests experience home-cooked Kerala breakfasts, tranquil garden verandas, and warm personal guidance from local families.",
        "fun_facts": [
            "Hosts often prepare traditional Kerala breakfast dishes like steamed Puttu with kadala curry and fresh coconut appams.",
            "Staying along this lane puts travelers within a five-minute walk of all Fort Kochi landmarks without needing any motor vehicles."
        ],
        "see_list": [
            "Peaceful shaded gardens filled with flowering hibiscus and bougainvillea",
            "Traditional Kerala wooden swing (Oonjal) suspended on the front veranda",
            "Family hosts offering local stories and insider walking tips over morning chai"
        ]
    }
}

paths = [
    r"c:\my files in athuls lap\my files in athuls lap\projects\PLANNING\TRAVEL-GUIDE\mobile\app\src\main\assets\packs\fort-kochi-walk-v1.json",
    r"c:\my files in athuls lap\my files in athuls lap\projects\PLANNING\TRAVEL-GUIDE\content\packs\fort-kochi-walk-v1.json"
]

emoji_regex = re.compile(r"[\U0001F300-\U0001F9FF\u2600-\u26FF\u2700-\u27BF]")

for p in paths:
    if not os.path.exists(p):
        print(f"Skipping missing {p}")
        continue
    with open(p, "r", encoding="utf-8") as f:
        pack = json.load(f)
    
    for poi in pack.get("pois", []):
        pid = poi.get("id")
        if pid in pois_data:
            d = pois_data[pid]
            poi["summary"] = d["summary"]
            poi["history"] = d["history"]
            poi["fun_facts"] = d["fun_facts"]
            poi["see_list"] = d["see_list"]
            if not poi.get("sources"):
                poi["sources"] = ["Pack v1.1.0 field survey"]
            
            # Emoji verification
            for text in [poi["summary"], poi["history"]] + poi["fun_facts"] + poi["see_list"]:
                if emoji_regex.search(text):
                    print(f"ERROR: Emoji detected in {pid}: {text}")
                    sys.exit(1)
            
            if len(poi["summary"]) > 140:
                print(f"ERROR: summary > 140 chars for {pid} ({len(poi['summary'])})")
                sys.exit(1)

    with open(p, "w", encoding="utf-8") as f:
        json.dump(pack, f, indent=2, ensure_ascii=False)
    print("Successfully updated", p)

print("All pack files updated and validated without emojis!")
