package guide.app.data

/**
 * High-craftsmanship multi-language localization dictionary for Travel Guide.
 * Synchronizes UI layout text, navigation bars, buttons, and place stories across:
 * - English (en)
 * - Malayalam (ml)
 * - Hindi (hi)
 * - Tamil (ta)
 *
 * Strict zero-emoji compliance.
 */
object AppStrings {

    // ---- Navigation Tabs ---------------------------------------------------
    fun tabMap(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ഭൂപടം"
        AppLanguage.HINDI -> "मानचित्र"
        AppLanguage.TAMIL -> "வரைபடம்"
        AppLanguage.ENGLISH -> "Map"
    }

    fun tabNearby(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "അടുത്തുള്ളവ"
        AppLanguage.HINDI -> "आस-पास"
        AppLanguage.TAMIL -> "அருகில்"
        AppLanguage.ENGLISH -> "Nearby"
    }

    fun tabPacks(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "പാക്കുകൾ"
        AppLanguage.HINDI -> "पैक्स"
        AppLanguage.TAMIL -> "பேக்குகள்"
        AppLanguage.ENGLISH -> "Packs"
    }

    fun tabHistory(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ചരിത്രം"
        AppLanguage.HINDI -> "इतिहास"
        AppLanguage.TAMIL -> "வரலாறு"
        AppLanguage.ENGLISH -> "History"
    }

    fun tabSettings(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ക്രമീകരണങ്ങൾ"
        AppLanguage.HINDI -> "सेटिंग्स"
        AppLanguage.TAMIL -> "அமைப்புகள்"
        AppLanguage.ENGLISH -> "Settings"
    }

    // ---- Primary Actions ----------------------------------------------------
    fun whatAmISeeing(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "എന്താണ് മുന്നിൽ?"
        AppLanguage.HINDI -> "क्या दिख रहा है?"
        AppLanguage.TAMIL -> "என்ன தெரிகிறது?"
        AppLanguage.ENGLISH -> "What am I seeing?"
    }

    fun nearbyTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "അടുത്തുള്ളവ"
        AppLanguage.HINDI -> "आस-पास के स्थान"
        AppLanguage.TAMIL -> "அருகிலுள்ள இடங்கள்"
        AppLanguage.ENGLISH -> "Nearby"
    }

    fun nearbySubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "നിങ്ങളുടെ വഴിക്കരികിലുള്ള സ്ഥലങ്ങൾ, ദൂര ക്രമത്തിൽ."
        AppLanguage.HINDI -> "आपके रास्ते के पास के स्थान, दूरी के अनुसार।"
        AppLanguage.TAMIL -> "உங்கள் பாதைக்கு அருகிலுள்ள இடங்கள், தொலைவு வரிசையில்."
        AppLanguage.ENGLISH -> "Places near your path, sorted by distance."
    }

    fun filterByLayer(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "വിഭാഗം തിരഞ്ഞെടുക്കുക"
        AppLanguage.HINDI -> "श्रेणी चुनें"
        AppLanguage.TAMIL -> "வகை வடிகட்டு"
        AppLanguage.ENGLISH -> "Filter by layer"
    }

    fun explore(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "കാണുക"
        AppLanguage.HINDI -> "देखें"
        AppLanguage.TAMIL -> "பார்"
        AppLanguage.ENGLISH -> "Explore"
    }

    fun stopsInRange(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.MALAYALAM -> "$count സ്ഥലങ്ങൾ പരിധിയിലുണ്ട്"
        AppLanguage.HINDI -> "$count स्थान सीमा में हैं"
        AppLanguage.TAMIL -> "$count இடங்கள் வரம்பில் உள்ளன"
        AppLanguage.ENGLISH -> "$count stops in range"
    }

    fun seeingPrompt(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ഫോൺ മുന്നിലെ കാഴ്ചയിലേക്ക് തിരിക്കുക. നിങ്ങൾ നിൽക്കുന്നിടത്തുനിന്ന് ഗൈഡ് വിവരങ്ങൾ പറയും."
        AppLanguage.HINDI -> "फ़ोन को अपने सामने की ओर करें। गाइड स्थान का विवरण देगा।"
        AppLanguage.TAMIL -> "தொலைபேசியை முன்நோக்கி திருப்பவும். நீங்கள் இருக்கும் இடத்திலிருந்து வழிகாட்டி விவரிக்கும்."
        AppLanguage.ENGLISH -> "Point the phone at what's in front of you. The guide names it from where you're standing."
    }

    fun gpsWaiting(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ജിപിഎസ് സിഗ്നലിനായി കാത്തിരിക്കുന്നു..."
        AppLanguage.HINDI -> "जीपीएस सिग्नल की प्रतीक्षा कर रहा है..."
        AppLanguage.TAMIL -> "ஜிபிஎஸ் சிக்னலுக்காக காத்திருக்கிறது..."
        AppLanguage.ENGLISH -> "Waiting on a GPS fix — this answers once location is locked."
    }

    fun distanceMeters(lang: AppLanguage, meters: Int): String = when (lang) {
        AppLanguage.MALAYALAM -> "$meters മീറ്റർ ദൂരം"
        AppLanguage.HINDI -> "$meters मीटर दूर"
        AppLanguage.TAMIL -> "$meters மீட்டர் தொலைவு"
        AppLanguage.ENGLISH -> "$meters m away"
    }

    fun distanceKm(lang: AppLanguage, kmFormatted: String): String = when (lang) {
        AppLanguage.MALAYALAM -> "$kmFormatted കി.മീ ദൂരം"
        AppLanguage.HINDI -> "$kmFormatted किमी दूर"
        AppLanguage.TAMIL -> "$kmFormatted கி.மீ தொலைவு"
        AppLanguage.ENGLISH -> "$kmFormatted km away"
    }

    fun distanceGpsWaiting(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ജിപിഎസ് തിരയുന്നു..."
        AppLanguage.HINDI -> "जीपीएस की प्रतीक्षा..."
        AppLanguage.TAMIL -> "ஜிபிஎஸ் காத்திருக்கிறது..."
        AppLanguage.ENGLISH -> "Waiting for GPS..."
    }

    fun story(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "വിവരണം"
        AppLanguage.HINDI -> "कहानी"
        AppLanguage.TAMIL -> "கதை"
        AppLanguage.ENGLISH -> "Story"
    }

    fun startAudioStory(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ഓഡിയോ കേൾക്കുക"
        AppLanguage.HINDI -> "ऑडियो कहानी सुनें"
        AppLanguage.TAMIL -> "ஆடியோ தொடங்கு"
        AppLanguage.ENGLISH -> "Start Audio Story"
    }

    fun nextStop(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "അടുത്ത സ്ഥലം"
        AppLanguage.HINDI -> "अगला पड़ाव"
        AppLanguage.TAMIL -> "அடுத்த இடம்"
        AppLanguage.ENGLISH -> "Next Stop"
    }

    fun hiddenSecret(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "രഹസ്യങ്ങളും കൗതുകങ്ങളും"
        AppLanguage.HINDI -> "रहस्य और अनोखी बातें"
        AppLanguage.TAMIL -> "மறைக்கப்பட்ட ரகசியங்கள்"
        AppLanguage.ENGLISH -> "HIDDEN SECRET & CURIOSITY"
    }

    fun livingStory(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ജീവിക്കുന്ന ചരിത്രം"
        AppLanguage.HINDI -> "जीवंत इतिहास"
        AppLanguage.TAMIL -> "வாழும் வரலாறு"
        AppLanguage.ENGLISH -> "The living story"
    }

    fun whatToSpot(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "നേരിട്ട് കണ്ടറിയേണ്ടവ"
        AppLanguage.HINDI -> "यहाँ क्या देखें"
        AppLanguage.TAMIL -> "நேரில் பார்க்க வேண்டியவை"
        AppLanguage.ENGLISH -> "What to spot in person"
    }

    fun heritageLore(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "പൈതൃക കഥകളും കൗതുകങ്ങളും"
        AppLanguage.HINDI -> "विरासत की कहानियाँ"
        AppLanguage.TAMIL -> "பாரம்பரிய கதைகள்"
        AppLanguage.ENGLISH -> "Heritage lore & curiosities"
    }

    fun openAlways(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "എപ്പോഴും തുറന്നിരിക്കുന്നു"
        AppLanguage.HINDI -> "हमेशा खुला है"
        AppLanguage.TAMIL -> "எப்போதும் திறந்திருக்கும்"
        AppLanguage.ENGLISH -> "Open 24/7 · Always accessible"
    }

    fun openNow(lang: AppLanguage, hours: String): String = when (lang) {
        AppLanguage.MALAYALAM -> "തുറന്നിരിക്കുന്നു · $hours"
        AppLanguage.HINDI -> "अभी खुला है · $hours"
        AppLanguage.TAMIL -> "திறந்துள்ளது · $hours"
        AppLanguage.ENGLISH -> "Open now · $hours"
    }

    fun closedNow(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "അടച്ചിരിക്കുന്നു"
        AppLanguage.HINDI -> "बंद है"
        AppLanguage.TAMIL -> "மூடப்பட்டுள்ளது"
        AppLanguage.ENGLISH -> "Closed"
    }

    // ---- Filter Chips -------------------------------------------------------
    fun chipAllStops(lang: AppLanguage, count: Int): String = when (lang) {
        AppLanguage.MALAYALAM -> "എല്ലാം ($count)"
        AppLanguage.HINDI -> "सभी पड़ाव ($count)"
        AppLanguage.TAMIL -> "அனைத்தும் ($count)"
        AppLanguage.ENGLISH -> "All Stops ($count)"
    }

    fun chipHeritage(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "പൈതൃകം"
        AppLanguage.HINDI -> "विरासत"
        AppLanguage.TAMIL -> "பாரம்பரியம்"
        AppLanguage.ENGLISH -> "Heritage"
    }

    fun chipFood(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ഭക്ഷണം & കഫേകൾ"
        AppLanguage.HINDI -> "खान-पान & कैफे"
        AppLanguage.TAMIL -> "உணவு & கஃபே"
        AppLanguage.ENGLISH -> "Food & Cafes"
    }

    fun chipStays(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "താമസം"
        AppLanguage.HINDI -> "ठहरने की जगह"
        AppLanguage.TAMIL -> "தங்குமிடம்"
        AppLanguage.ENGLISH -> "Stays"
    }

    // ---- Google Maps Companion ----------------------------------------------
    fun companionActive(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "മാപ്സ് വഴികാട്ടി സജീവം"
        AppLanguage.HINDI -> "मैप्स कंपैनियन सक्रिय"
        AppLanguage.TAMIL -> "மேப்ஸ் வழிகாட்டி செயலில்"
        AppLanguage.ENGLISH -> "MAPS COMPANION ACTIVE"
    }

    fun navigatingTo(lang: AppLanguage, dest: String): String = when (lang) {
        AppLanguage.MALAYALAM -> "യാത്ര: $dest"
        AppLanguage.HINDI -> "की ओर यात्रा: $dest"
        AppLanguage.TAMIL -> "செல்லுமிடம்: $dest"
        AppLanguage.ENGLISH -> "Navigating to $dest"
    }

    fun spotsAlongPath(lang: AppLanguage, count: Int, eta: String?): String = when (lang) {
        AppLanguage.MALAYALAM -> "$count സ്ഥലങ്ങൾ തയാറാണ് • ${eta ?: "സജീവം"}"
        AppLanguage.HINDI -> "मार्ग में $count पड़ाव तैयार • ${eta ?: "सक्रिय"}"
        AppLanguage.TAMIL -> "வழியில் $count இடங்கள் தயார் • ${eta ?: "செயலில்"}"
        AppLanguage.ENGLISH -> "$count spots along path pre-loaded • ${eta ?: "Active"}"
    }

    fun exitNav(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "നിർത്തുക"
        AppLanguage.HINDI -> "नेविगेशन छोड़ें"
        AppLanguage.TAMIL -> "வெளியேறு"
        AppLanguage.ENGLISH -> "Exit Nav"
    }

    // ---- Earbud & Spoken Guide ----------------------------------------------
    fun spontaneousGuideTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "തത്സമയ സംസാര ഗൈഡ്"
        AppLanguage.HINDI -> "सहज मौखिक गाइड"
        AppLanguage.TAMIL -> "உடனடி குரல் வழிகாட்டி"
        AppLanguage.ENGLISH -> "Spontaneous Spoken Guide"
    }

    fun spontaneousGuideSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ഇവിടെ നിൽക്കുമ്പോൾ വയർലെസ് ഇയർബഡിൽ രണ്ട് തവണ തൊട്ടാൽ തത്സമയ ചരിത്ര വിവരണം കേൾക്കാം."
        AppLanguage.HINDI -> "यहाँ खड़े होकर ईयरबड पर दो बार टैप करें और जीवंत मौखिक कहानी सुनें।"
        AppLanguage.TAMIL -> "இங்கு நிற்கும்போது உங்கள் இயர்பட்டை இருமுறை தட்டினால் உடனடி கதையைக் கேட்கலாம்."
        AppLanguage.ENGLISH -> "Double-tap your wireless earbud anytime while standing here for unscripted oral storytelling."
    }

    // ---- Settings Screen ---------------------------------------------------
    fun languageSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ഭാഷയും സംസാര ശബ്ദവും"
        AppLanguage.HINDI -> "भाषा और ऑडियो आवाज"
        AppLanguage.TAMIL -> "மொழி மற்றும் ஆடியோ குரல்"
        AppLanguage.ENGLISH -> "Language & Spoken Voice"
    }

    fun languageSectionDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "സർവം എഐ ന്യൂറൽ ശബ്ദവും ആപ്പ് യുഐയും ഒരേ സമയം മാറുന്നു."
        AppLanguage.HINDI -> "सर्वम न्यूरल वॉयस और ऐप इंटरफेस दोनों एक साथ बदलेंगे।"
        AppLanguage.TAMIL -> "சர்வம் நியூரல் குரலும் திரை மொழியும் ஒரே நேரத்தில் மாறும்."
        AppLanguage.ENGLISH -> "Synchronizes Sarvam AI neural speech synthesis and complete app interface."
    }

    fun testVoiceBtn(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "ശബ്ദ സാമ്പിൾ കേൾക്കുക"
        AppLanguage.HINDI -> "आवाज का नमूना सुनें"
        AppLanguage.TAMIL -> "குரல் மாதிரியைக் கேளுங்கள்"
        AppLanguage.ENGLISH -> "Listen to Voice Sample"
    }

    fun sampleVoiceText(lang: AppLanguage): String = when (lang) {
        AppLanguage.MALAYALAM -> "നമസ്കാരം, ഫോർട്ട് കൊച്ചിയിലേക്ക് സ്വാഗതം. നിങ്ങളുടെ സർവം എഐ യാത്രാ സഹായിയുടെ ശബ്ദ പരിശോധനയാണിത്."
        AppLanguage.HINDI -> "नमस्ते, फोर्ट कोच्चि में आपका स्वागत है। यह आपके सर्वम एआई यात्रा साथी का वॉयस टेस्ट है।"
        AppLanguage.TAMIL -> "வணக்கம், போர்ட் கொச்சிக்கு உங்களை வரவேற்கிறோம். இது உங்கள் சர்வம் ஏஐ பயண வழிகாட்டியின் குரல் சோதனை."
        AppLanguage.ENGLISH -> "Welcome to Fort Kochi. This is a voice test of your Sarvam AI travel companion."
    }

    // ---- Localized POI Names & Summaries ------------------------------------
    data class LocalizedPoi(
        val name: String,
        val summary: String,
        val secret: String,
        val history: String,
    )

    fun getLocalizedPoi(id: String, lang: AppLanguage): LocalizedPoi? {
        if (lang == AppLanguage.ENGLISH) return null
        return when (lang) {
            AppLanguage.MALAYALAM -> POIS_ML[id]
            AppLanguage.HINDI -> POIS_HI[id]
            AppLanguage.TAMIL -> POIS_TA[id]
            else -> null
        }
    }

    private val POIS_ML = mapOf(
        "vasco-square" to LocalizedPoi(
            name = "വാസ്കോ ഡ ഗാമ സ്ക്വയർ",
            summary = "പോർച്ചുഗീസ് നാവികന്റെ പേരിൽ അറിയപ്പെടുന്ന കടൽത്തീര ചത്വരം; സജീവമായ വഴിയോര ഭക്ഷണശാലകൾ.",
            secret = "1700-കളിൽ ഇവിടെയുണ്ടായിരുന്ന ഡച്ച് വിളക്കുമാടത്തിൽ എണ്ണവിളക്കുകൾ കത്തിച്ചാണ് കപ്പലുകൾക്ക് വഴികാട്ടിയിരുന്നത്.",
            history = "1502-ൽ ആദ്യത്തെ വ്യാപാര കോട്ട സ്ഥാപിച്ച പോർച്ചുഗീസ് നാവികൻ വാസ്കോ ഡ ഗാമയുടെ സ്മരണാർത്ഥം നാമകരണം ചെയ്ത ചത്വരം. തണൽ വിരിക്കുന്ന മഴമരങ്ങൾക്ക് കീഴെ വൈകുന്നേരങ്ങളിൽ മീൻ വിഭവങ്ങൾ തയ്യാറാക്കുന്ന വഴിയോരക്കടകൾ ഇവിടെ സജീവമാണ്.",
        ),
        "chinese-fishing-nets" to LocalizedPoi(
            name = "ചീനവലകൾ",
            summary = "14-ാം നൂറ്റാണ്ടിൽ കുബ്ലൈ ഖാന്റെ കാലത്ത് എത്തിയ ഭീമൻ തടിയും മുളയും കൊണ്ടുണ്ടാക്കിയ ചീനവലകൾ.",
            secret = "ചൈനയ്ക്കും വിയറ്റ്നാമിനും പുറത്ത് ഈ 14-ാം നൂറ്റാണ്ടിലെ ചീനവലകൾ ഇന്നും പ്രവർത്തിക്കുന്ന ഒരേയൊരു സ്ഥലം ഫോർട്ട് കൊച്ചിയാണ്. കല്ലുകളുടെ കൃത്യമായ ഭാരം കൊണ്ടാണ് ഇത് പൊങ്ങുന്നത്.",
            history = "1350 നും 1450 നും ഇടയിൽ ചൈനീസ് വ്യാപാരികൾ സ്ഥാപിച്ച ഈ വലകൾ ഇന്നും വേലിയേറ്റ സമയത്ത് പരമ്പരാഗത മീൻപിടുത്തക്കാർ പ്രവർത്തിപ്പിക്കുന്നു. യാതൊരു മോട്ടോറുമില്ലാതെ കയറും കരിങ്കല്ലും കൊണ്ട് മാത്രമാണ് ഈ ഭീമൻ യന്ത്രം സന്തുലിതമാക്കിയിരിക്കുന്നത്.",
        ),
        "fort-kochi-beach" to LocalizedPoi(
            name = "ഫോർട്ട് കൊച്ചി ബീച്ച് വാക്ക്",
            summary = "പുരാതന കപ്പലുകൾ വന്നിറങ്ങിയ തീരം; മനോഹരമായ സൂര്യാസ്തമയ കാഴ്ചകൾ.",
            secret = "ശൈത്യകാല പ്രഭാതങ്ങളിൽ കടൽത്തീരത്തുനിന്ന് വെറും 50 മീറ്റർ അകലെ ഡോൾഫിനുകൾ നീന്തുന്നത് ഇവിടെ നേരിട്ട് കാണാം.",
            history = "കുരുമുളകും ഏലവും വാങ്ങാൻ റോമൻ, അറബ്, പോർച്ചുഗീസ് പായ്ക്കപ്പലുകൾ എത്തിയ ചരിത്രതീരമാണിത്. പടിഞ്ഞാറോട്ട് ദർശനമുള്ള ഈ തീരത്ത് വലിയ ചരക്കുകപ്പലുകളും നാടൻ വഞ്ചികളും ഒരേ കടലിടുക്കിലൂടെ നീങ്ങുന്നത് കാണാം.",
        ),
        "st-francis-church" to LocalizedPoi(
            name = "സെന്റ് ഫ്രാൻസിസ് പള്ളി",
            summary = "ഇന്ത്യയിലെ ഏറ്റവും പഴക്കം ചെന്ന യൂറോപ്യൻ ദേവാലയം; വാസ്കോ ഡ ഗാമയെ ആദ്യം അടക്കം ചെയ്ത ഇടം.",
            secret = "വാസ്കോ ഡ ഗാമയുടെ ഭൗതികശരീരം ലിസ്ബണിലേക്ക് കൊണ്ടുപോകുന്നതിന് മുൻപ് 14 വർഷം ഈ പള്ളിയുടെ തറയിലാണ് സൂക്ഷിച്ചിരുന്നത്.",
            history = "1503-ൽ പോർച്ചുഗീസ് ഫ്രാൻസിസ്കൻ സന്യാസിമാർ മരം കൊണ്ട് നിർമ്മിച്ച് പിന്നീട് 1516-ൽ കല്ലിൽ പുനർനിർമ്മിച്ച ചരിത്രപ്രസിദ്ധമായ ദേവാലയം. പോർച്ചുഗീസ്, ഡച്ച്, ബ്രിട്ടീഷ് ഭരണകാലങ്ങളെ അതിജീവിച്ച ഈ പള്ളിയിൽ പഴയ കാലത്തെ കയർ വലിക്കുന്ന പങ്കകൾ ഇന്നും കാണാം.",
        ),
        "santa-cruz-basilica" to LocalizedPoi(
            name = "സാന്താക്രൂസ് കത്തീഡ്രൽ ബസിലിക്ക",
            summary = "ഇറ്റാലിയൻ ചുമർചിത്രങ്ങളും വെൺമയാർന്ന ഗോപുരങ്ങളുമുള്ള പ്രസിദ്ധമായ ക്രിസ്തീയ ദേവാലയം.",
            secret = "അൾത്താരയ്ക്ക് താഴെയുള്ള അവസാനത്തെ അത്താഴ ചിത്രം മുട്ടയുടെ മഞ്ഞക്കരുവും പ്രകൃതിദത്ത ധാതുക്കളും ചേർത്താണ് ഇറ്റാലിയൻ കലാകാരൻ വരച്ചത്.",
            history = "1558-ൽ പോർച്ചുഗീസുകാർ സ്ഥാപിച്ച ഈ ദേവാലയം 1905-ൽ അതിമനോഹരമായ ഗോതിക് ശൈലിയിൽ പുനർനിർമ്മിക്കപ്പെട്ടു. ഇറ്റാലിയൻ ഈശോസഭാ ബ്രദർ അന്തോണിയോ മൊഷെനി വരച്ച വർണ്ണാഭമായ മച്ചിലെ ചിത്രങ്ങൾ ഇന്നും തിളങ്ങിനിൽക്കുന്നു.",
        ),
        "mattancherry-palace" to LocalizedPoi(
            name = "മട്ടാഞ്ചേരി കൊട്ടാരം (ഡച്ച് പാലസ്)",
            summary = "1555-ൽ കൊച്ചി രാജാവിനായി നിർമ്മിച്ച കൊട്ടാരം; പ്രസിദ്ധമായ രാമായണ ചുവർചിത്രങ്ങൾ.",
            secret = "കൊട്ടാരത്തിലെ കൊറോണേഷൻ ഹാളിന്റെ കറുത്ത തറ മാർബിളല്ല, മറിച്ച് ചിരട്ടക്കരിയും കുമ്മായവും മുട്ടയുടെ വെള്ളയും ചേർത്തുണ്ടാക്കിയ പരമ്പരാഗത കേരള മിശ്രിതമാണ്.",
            history = "1555-ൽ പോർച്ചുഗീസുകാർ വീരകേരള വർമ്മ രാജാവിന് സമ്മാനിച്ച കൊട്ടാരം പിന്നീട് ഡച്ചുകാർ പുതുക്കിപ്പണിതു. നാലുകെട്ട് മാതൃകയിലുള്ള ഈ കൊട്ടാരത്തിലെ കിടപ്പുമുറിയിൽ രാമായണവും മഹാഭാരതവും ചിത്രീകരിക്കുന്ന 300 ചതുരശ്ര മീറ്റർ ചുമർചിത്രങ്ങളുണ്ട്.",
        ),
        "pardesi-synagogue" to LocalizedPoi(
            name = "പരദേശി സിനഗോഗ്",
            summary = "ചൈനീസ് കളിമൺ ടൈലുകളും ബെൽജിയം സ്ഫടിക വിളക്കുകളുമുള്ള പുരാതന ജൂതപ്പള്ളി.",
            secret = "തറയിൽ പതിച്ചിരിക്കുന്ന നൂറുകണക്കിന് നീലയും വെളുപ്പുമുള്ള ചൈനീസ് പോഴ്സലൈൻ ടൈലുകളിൽ ഒരെണ്ണത്തിന്റെ പോലും ചിത്രം മറ്റൊന്നിനോട് സമാനമല്ല.",
            history = "1568-ൽ സ്പെയിനിൽ നിന്നും നെതർലാൻഡിൽ നിന്നും അഭയാർത്ഥികളായെത്തിയ സെഫാർഡിക് ജൂതന്മാർ രാജാവ് അനുവദിച്ച സ്ഥലത്ത് നിർമ്മിച്ച ദേവാലയമാണിത്. ഇതിന്റെ ക്ലോക്ക് ടവറിൽ ഹീബ്രു, ലാറ്റിൻ, മലയാളം, അറബി അക്കങ്ങൾ കാണാം.",
        ),
        "jew-town-lanes" to LocalizedPoi(
            name = "ജ്യൂ ടൗൺ സുഗന്ധവ്യഞ്ജന തെരുവ്",
            summary = "കുരുമുളകും ചുക്കും പുരാവസ്തുക്കളും നിറഞ്ഞ 400 വർഷം പഴക്കമുള്ള വ്യാപാര തെരുവ്.",
            secret = "നൂറ്റാണ്ടുകളായി സുഗന്ധവ്യഞ്ജനങ്ങൾ സൂക്ഷിക്കുന്നതുമൂലം ഈ തെരുവിലെ വായുവിൽ ഇപ്പോഴും കുരുമുളകിന്റെയും ചുക്കിന്റെയും സുഗന്ധം സ്വാഭാവികമായി തങ്ങിനിൽക്കുന്നു.",
            history = "നാല് നൂറ്റാണ്ടിലേറെയായി മലബാർ കുരുമുളകും ചുക്കും ലോകമെമ്പാടും കയറ്റി അയക്കുന്ന പ്രധാന കേന്ദ്രമാണിത്. കപ്പൽ വിളക്കുകളും പിച്ചള വസ്‌തുക്കളും 2,000 പേർക്ക് ഭക്ഷണം വിളമ്പാൻ ഉപയോഗിച്ചിരുന്ന ഭീമൻ വാർപ്പുകളും ഇവിടെ കാണാം.",
        ),
    )

    private val POIS_HI = mapOf(
        "vasco-square" to LocalizedPoi(
            name = "वास्को द गामा स्क्वायर",
            summary = "पुर्तगाली खोजकर्ता के नाम पर बना तटीय चौक; शाम के समय ताज़ा समुद्री भोजन के स्टॉल।",
            secret = "1700 के दशक में यहाँ एक डच नौसैनिक संकेत स्तंभ था, जो तेल के दीयों से मसाला जहाजों को रास्ता दिखाता था।",
            history = "1502 में यूरोप का पहला भारतीय व्यापारिक किला स्थापित करने वाले पुर्तगाली नाविक वास्को द गामा के नाम पर बना यह चौक फोर्ट कोच्चि का मुख्य प्रवेश बिंदु है।",
        ),
        "chinese-fishing-nets" to LocalizedPoi(
            name = "चीनी मछली पकड़ने के जाल",
            summary = "14वीं सदी के विशाल कैंटिलीवर जाल, जो भारी पत्थरों के संतुलन पर चलते हैं।",
            secret = "चीन और वियतनाम के बाहर, फोर्ट कोच्चि दुनिया की एकमात्र जगह है जहाँ ये प्राचीन जाल आज भी रोज इस्तेमाल होते हैं।",
            history = "कुबलई खान के दरबार से आए चीनी व्यापारियों द्वारा 14वीं शताब्दी में शुरू किए गए ये जाल वास्तुकला और संतुलन का चमत्कार हैं। 6 मछुआरों की टीम मिलकर भारी पत्थरों की मदद से इन्हें पानी में उतारती और उठाती है।",
        ),
        "fort-kochi-beach" to LocalizedPoi(
            name = "फोर्ट कोच्चि बीच वॉक",
            summary = "मसाला व्यापार का ऐतिहासिक तट और अरब सागर के सूर्यास्त का विहंगम दृश्य।",
            secret = "सर्दियों की शांत सुबहों में ब्रेकवाटर पत्थरों से केवल 50 मीटर की दूरी पर जंगली हंपबैक डॉल्फ़िन तैरती हुई देखी जा सकती हैं।",
            history = "यह तट सदियों से रोमन, अरब और पुर्तगाली मसाला जहाजों का स्वागत करता रहा है। शाम के समय यहाँ गहरे समुद्र में जाते विशाल मालवाहक जहाजों और नौकाओं का सुंदर दृश्य दिखाई देता है।",
        ),
        "st-francis-church" to LocalizedPoi(
            name = "सेंट फ्रांसिस चर्च",
            summary = "भारत का सबसे पुराना यूरोपीय चर्च; वास्को द गामा का पहला समाधि स्थल।",
            secret = "वास्को द गामा का पार्थिव शरीर लिस्बन ले जाने से पहले 14 वर्षों तक इसी चर्च के फर्श के नीचे विश्राम कर रहा था।",
            history = "1503 में पुर्तगाली फ्रांसिस्कन भिक्षुओं द्वारा निर्मित यह चर्च औपनिवेशिक इतिहास का जीवंत प्रमाण है। इसमें आज भी रस्सियों से खींचे जाने वाले कपड़े के पारंपरिक हाथ-पंखा देखे जा सकते हैं।",
        ),
        "santa-cruz-basilica" to LocalizedPoi(
            name = "सांताक्रूज़ कैथेड्रल बेसिलिका",
            summary = "शानदार गोथिक शैली और इटालियन भित्तिचित्रों से सजा ऐतिहासिक कैथेड्रल।",
            secret = "मुख्य वेदी के नीचे अंतिम भोज (द लास्ट सपर) की पेंटिंग अंडे की जर्दी और प्राकृतिक खनिजों से इटालियन कलाकार द्वारा बनाई गई थी।",
            history = "1558 में पुर्तगालियों द्वारा स्थापित यह बेसिलिका अपनी भव्य श्वेत मीनारों और इटालियन भ्राता अंतोनियो मोशेनी द्वारा चित्रित छतों के लिए विश्व प्रसिद्ध है।",
        ),
        "mattancherry-palace" to LocalizedPoi(
            name = "मट्टनचेरी पैलेस (डच पैलेस)",
            summary = "1555 में कोच्चि राजा के लिए निर्मित महल; प्रसिद्ध रामायण भित्तिचित्र।",
            secret = "महल के राज्याभिषेक कक्ष का काला फर्श संगमरमर नहीं, बल्कि नारियल के छिलके, चूना और अंडे की सफेदी का अनोखा पारंपरिक मिश्रण है।",
            history = "पुर्तगालियों द्वारा 1555 में कोच्चि के राजा वीर केरल वर्मा को उपहार में दिया गया यह महल पारंपरिक केरल नलूकेट्टू शैली में बना है। इसके कमरों में रामायण के 48 जीवंत दृश्य चित्रित हैं।",
        ),
        "pardesi-synagogue" to LocalizedPoi(
            name = "परदेसी सिनागॉग",
            summary = "हाथ से चित्रित चीनी टाइलों और बेल्जियम झूमरों से सुसज्जित प्राचीन यहूदी प्रार्थना स्थल।",
            secret = "फर्श पर बिछाई गई सैकड़ों नीली-सफेद चीनी चीनीमिट्टी की टाइलों में से किन्हीं भी दो टाइलों का डिज़ाइन एक जैसा नहीं है।",
            history = "1568 में स्पेन और नीदरलैंड के यहूदी शरणार्थियों द्वारा निर्मित यह कॉमनवेल्थ का सबसे पुराना सक्रिय सिनागॉग है। इसके घंटाघर में हिब्रू, लैटिन, मलयालम और अरबी अंक अंकित हैं।",
        ),
        "jew-town-lanes" to LocalizedPoi(
            name = "ज्यू टाउन मसाला बाज़ार",
            summary = "काली मिर्च, सोंठ और प्राचीन वस्तुओं से भरी चार सौ साल पुरानी ऐतिहासिक गलियाँ।",
            secret = "सैकड़ों वर्षों से मसालों के गोदाम रहने के कारण इस पूरी गली की हवा में आज भी ताज़ी कुटी काली मिर्च और अदरक की तीखी खुशबू बसी रहती है।",
            history = "चार सदियों से यह गली वैश्विक मसाला व्यापार का केंद्र रही है। यहाँ दुकानों के बाहर 2,000 लोगों का खाना पकाने वाली विशालकाय पीतल की कड़ाही (वार्पू) देखी जा सकती है।",
        ),
    )

    private val POIS_TA = mapOf(
        "vasco-square" to LocalizedPoi(
            name = "வாஸ்கோட காமா சதுக்கம்",
            summary = "போர்த்துகீசிய மாலுமியின் பெயரில் அமைந்த கடற்கரை சதுக்கம்; கடல் உணவு அங்காடிகள்.",
            secret = "1700 களில் இங்கு அமைந்திருந்த டச்சு விளக்குக்கம்பம் எண்ணெய் விளக்குகளால் கப்பல்களுக்கு வழிகாட்டியது.",
            history = "1502 இல் முதல் வர்த்தகக் கோட்டையை அமைத்த வாஸ்கோட காமாவின் நினைவாக பெயரிடப்பட்ட இந்த கடலோர சதுக்கம் வரலாற்று நடைபயணத்தின் தொடக்க புள்ளியாகும்.",
        ),
        "chinese-fishing-nets" to LocalizedPoi(
            name = "சீன மீன்பிடி வலைகள்",
            summary = "14 ஆம் நூற்றாண்டின் பழமையான சமநிலை பாறை மீன்பிடி வலைகள்.",
            secret = "சீனா மற்றும் வியட்நாமுக்கு வெளியே, இந்த 14 ஆம் நூற்றாண்டு வலைகள் இன்றும் இயங்கும் ஒரே இடம் போர்ட் கொச்சி மட்டுமே.",
            history = "குப்லாய் கானின் சீன வணிகர்களால் அறிமுகப்படுத்தப்பட்ட இந்த பெரிய வலைகள் எந்திரங்கள் ஏதுமின்றி வெறும் கயிறுகளாலும் கருங்கற்களின் சமநிலையாலும் இயக்கப்படுகின்றன.",
        ),
        "fort-kochi-beach" to LocalizedPoi(
            name = "போர்ட் கொச்சி கடற்கரை நடைபாதை",
            summary = "வரலாற்று சிறப்புமிக்க அரபிக்கடல் துறைமுக முகத்துவாரம்.",
            secret = "குளிர்கால காலை வேளையில் கடற்கரை பாறைகளில் இருந்து 50 மீட்டர் தொலைவில் டால்பின்கள் நீந்துவதை இங்கிருந்து காணலாம்.",
            history = "மிளகு மற்றும் ஏலக்காய் வணிகத்திற்காக ரோமானிய, அரபு, போர்த்துகீசிய கப்பல்கள் வந்து நின்ற வரலாற்று கடற்கரை இதுவாகும்.",
        ),
        "st-francis-church" to LocalizedPoi(
            name = "செயின்ட் பிரான்சிஸ் சர்ச்",
            summary = "இந்தியாவின் பழமையான ஐரோப்பிய தேவாலயம்; வாஸ்கோட காமாவின் முதல் அடக்கத்தலம்.",
            secret = "வாஸ்கோட காமாவின் உடல் லிஸ்பனுக்கு கொண்டு செல்லப்படுவதற்கு முன்பு 14 ஆண்டுகள் இந்த தேவாலயத்தின் தரையின் கீழ்தான் இருந்தது.",
            history = "1503 இல் போர்த்துகீசியர்களால் கட்டப்பட்ட இந்த பழமையான தேவாலயத்தில் கையால் இழுக்கப்படும் துணி விசிறிகள் இன்றும் பாதுகாக்கப்படுகின்றன.",
        ),
        "santa-cruz-basilica" to LocalizedPoi(
            name = "சாந்தா குரூஸ் பசிலிக்கா",
            summary = "இத்தாலிய ஓவியங்கள் மற்றும் வெள்ளை கோபுரங்கள் கொண்ட பிரமாண்ட கதீட்ரல்.",
            secret = "கடைசி விருந்து ஓவியம் முட்டையின் மஞ்சள் கரு மற்றும் இயற்கை தாதுக்களைக் கொண்டு இத்தாலிய கலைஞரால் வரையப்பட்டது.",
            history = "1558 இல் தொடங்கப்பட்டு பின்னர் நேர்த்தியான கோதிக் பாணியில் புதுப்பிக்கப்பட்ட இந்த பசிலிக்கா அதன் கூரை ஓவியங்களுக்கு புகழ்பெற்றது.",
        ),
        "mattancherry-palace" to LocalizedPoi(
            name = "மட்டான்சேரி அரண்மனை (டச்சு அரண்மனை)",
            summary = "1555 இல் கொச்சி மன்னருக்காக கட்டப்பட்ட அரண்மனை; புகழ்பெற்ற ராமாயண சுவரோவியங்கள்.",
            secret = "அரண்மனையின் தரை பளிங்குக்கல் அல்ல, எரிந்த தேங்காய் சிரட்டை, சுண்ணாம்பு மற்றும் முட்டையின் வெள்ளைக்கரு கொண்டு செய்யப்பட்ட பழங்கால கலவை.",
            history = "போர்த்துகீசியர்களால் கொச்சி மன்னர் வீர கேரள வர்மாவிற்கு பரிசளிக்கப்பட்ட இந்த அரண்மனையில் 300 சதுர மீட்டர் பரப்பளவிலான வண்ணமயமான சுவரோவியங்கள் உள்ளன.",
        ),
        "pardesi-synagogue" to LocalizedPoi(
            name = "பரதேசி யூத தொழுகைக்கூடம்",
            summary = "சீன பீங்கான் ஓடுகள் மற்றும் பெல்ஜியம் விளக்குகள் கொண்ட பழமையான யூத ஆலயம்.",
            secret = "தரையில் பதிக்கப்பட்ட நூற்றுக்கணக்கான சீன பீங்கான் ஓடுகளில் எந்த இரண்டு ஓடுகளின் வடிவமைப்பும் ஒரே மாதிரியாக இருக்காது.",
            history = "1568 இல் கட்டப்பட்ட இந்த யூத ஆலயத்தின் கடிகார கோபுரத்தில் ஹீப்ரு, ரோமன், மலையாளம் மற்றும் அரபு எண்கள் உள்ளன.",
        ),
        "jew-town-lanes" to LocalizedPoi(
            name = "யூத தெரு வாசனை திரவிய சந்தை",
            summary = "மிளகு, சுக்கு மற்றும் பழங்கால பொருட்கள் நிறைந்த 400 ஆண்டுகள் பழமையான தெரு.",
            secret = "நூற்றாண்டுகளாக மசாலா கிடங்குகள் இருந்ததால் இந்த தெருவின் காற்றில் இன்றும் மிளகு மற்றும் சுக்கின் நறுமணம் இயற்கையாகவே வீசுகிறது.",
            history = "நான்கு நூற்றாண்டுகளாக உலகளாவிய வாசனை திரவிய வர்த்தகத்தின் மையமாக விளங்கும் இந்த தெருவில் பழங்கால பொருட்கள் ஏராளமாக கிடைக்கின்றன.",
        ),
    )
}
