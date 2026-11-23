package com.example.core.localization

import androidx.compose.runtime.mutableStateOf

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val shortBadge: String,
    val bibleTranslationCode: String
) {
    ENGLISH("en", "English", "English", "EN", "KJV"),
    YORUBA("yo", "Yoruba", "Yorùbá", "YO", "Bibeli Mimo (YO)"),
    HAUSA("ha", "Hausa", "Harshen Hausa", "HA", "Littafi Mai Tsarki (HA)"),
    IGBO("ig", "Igbo", "Asụsụ Igbo", "IG", "Baịbụl Nsọ (IG)");

    companion object {
        fun fromCodeOrName(value: String?): AppLanguage {
            if (value.isNullOrBlank()) return ENGLISH
            return values().firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                    it.code.equals(value, ignoreCase = true) ||
                    it.displayName.equals(value, ignoreCase = true)
            } ?: ENGLISH
        }
    }
}

data class LocalizedAppStrings(
    val navProgrammes: String,
    val navCommunity: String,
    val navGames: String,
    val navSpiritual: String,
    val navProfile: String,
    val tabUnits: String,
    val tabAttendance: String,
    val tabUnitReports: String,
    val tabForum: String,
    val tabDevotionals: String,
    val tabBibleExplorer: String,
    val tabMyProfile: String,
    val tabGiving: String,
    val tabCommandCentre: String,
    val englishSwitchLabel: String,
    val usingNormalLanguageLabel: String,
    val bibleSearchPlaceholder: String,
    val bibleAutoSyncedLabel: String,
    val bibleAllLanguagesLabel: String,
    val scriptureOfTheDayBadge: String,
    val upcomingProgrammesTitle: String,
    val postScriptureAction: String,
    val postProgrammeAction: String,
    val quizAutoSyncedBadge: String
)

object LanguageManager {
    /**
     * The user's normal/default language selected at signup and stored in the database.
     */
    val preferredLanguage = mutableStateOf(AppLanguage.ENGLISH)

    /**
     * Whether the user has temporarily toggled the "Switch to English" override.
     */
    val isEnglishOverride = mutableStateOf(false)

    /**
     * The currently active UI & content language (either [preferredLanguage] or [AppLanguage.ENGLISH] when toggled).
     */
    val currentLanguage = mutableStateOf(AppLanguage.ENGLISH)

    /**
     * Sets the user's normal language (from signup or loaded session) and resets the English override.
     */
    fun setPreferredLanguage(lang: AppLanguage) {
        preferredLanguage.value = lang
        isEnglishOverride.value = false
        currentLanguage.value = lang
    }

    /**
     * Sets the active language directly (also updates preferredLanguage if not English so toggle works intuitively).
     */
    fun setLanguage(lang: AppLanguage) {
        currentLanguage.value = lang
        if (lang != AppLanguage.ENGLISH) {
            preferredLanguage.value = lang
            isEnglishOverride.value = false
        } else if (preferredLanguage.value != AppLanguage.ENGLISH) {
            isEnglishOverride.value = true
        }
    }

    /**
     * One-tap toggle between the user's signup preferred language and English.
     */
    fun toggleEnglishOverride() {
        val normalLang = preferredLanguage.value
        if (normalLang == AppLanguage.ENGLISH) {
            // If user registered with English, keep English active
            isEnglishOverride.value = false
            currentLanguage.value = AppLanguage.ENGLISH
        } else {
            val newOverride = !isEnglishOverride.value
            isEnglishOverride.value = newOverride
            currentLanguage.value = if (newOverride) AppLanguage.ENGLISH else normalLang
        }
    }

    fun strings(lang: AppLanguage = currentLanguage.value): LocalizedAppStrings {
        return when (lang) {
            AppLanguage.ENGLISH -> LocalizedAppStrings(
                navProgrammes = "Home",
                navCommunity = "Community",
                navGames = "Games",
                navSpiritual = "Spiritual",
                navProfile = "Profile",
                tabUnits = "Units",
                tabAttendance = "Attendance",
                tabUnitReports = "Unit Reports",
                tabForum = "Forum",
                tabDevotionals = "Devotionals & Sermons",
                tabBibleExplorer = "Bible Explorer",
                tabMyProfile = "My Profile",
                tabGiving = "Giving & Pledges",
                tabCommandCentre = "Command Centre",
                englishSwitchLabel = "Switch to English",
                usingNormalLanguageLabel = "Normal Language",
                bibleSearchPlaceholder = "Search scripture reference (e.g. Daniel 11:32) or keyword...",
                bibleAutoSyncedLabel = "Synced: English (KJV)",
                bibleAllLanguagesLabel = "All Languages",
                scriptureOfTheDayBadge = "SCRIPTURE FOR THE DAY",
                upcomingProgrammesTitle = "UPCOMING YOUTH PROGRAMMES",
                postScriptureAction = "Post Scripture for the Day",
                postProgrammeAction = "Post Upcoming Programme",
                quizAutoSyncedBadge = "Quiz Language: English"
            )
            AppLanguage.YORUBA -> LocalizedAppStrings(
                navProgrammes = "Ilé",
                navCommunity = "Àwùjọ",
                navGames = "Àwọn Eré",
                navSpiritual = "Ẹ̀mí",
                navProfile = "Ìrísí Mi",
                tabUnits = "Ẹ̀ka Iṣẹ́",
                tabAttendance = "Àkọsílẹ̀ Ìpàdé",
                tabUnitReports = "Ìròyìn Ẹ̀ka",
                tabForum = "Ìjíròrò",
                tabDevotionals = "Ìwé Ìgbàgbọ́ & Ìwàásù",
                tabBibleExplorer = "Bíbélì Mímọ́",
                tabMyProfile = "Ìrísí Mi",
                tabGiving = "Ìdámẹ́wàá & Ọrẹ",
                tabCommandCentre = "Ilé Àṣẹ",
                englishSwitchLabel = "Yí sí Gẹ̀ẹ́sì (EN)",
                usingNormalLanguageLabel = "Èdè Yorùbá",
                bibleSearchPlaceholder = "Wá ẹsẹ Bíbélì (bíi Danieli 11:32) tàbí ọ̀rọ̀...",
                bibleAutoSyncedLabel = "Èdè: Bibeli Mimo (YO)",
                bibleAllLanguagesLabel = "Gbogbo Èdè",
                scriptureOfTheDayBadge = "Ọ̀RỌ̀ ỌLỌ́RUN FÚN ÒNÍ",
                upcomingProgrammesTitle = "ÀWỌN ÈTÒ Ọ̀DỌ́ TÍ Ń BỌ̀",
                postScriptureAction = "Fi Ẹsẹ Bíbélì Òní Sílẹ̀",
                postProgrammeAction = "Fi Ètò Ọ̀dọ́ Sílẹ̀",
                quizAutoSyncedBadge = "Èdè Eré: Yorùbá (YO)"
            )
            AppLanguage.HAUSA -> LocalizedAppStrings(
                navProgrammes = "Gida",
                navCommunity = "Al'umma",
                navGames = "Wasanni",
                navSpiritual = "Ruhaniya",
                navProfile = "Bayani Na",
                tabUnits = "Sassa",
                tabAttendance = "Halarta",
                tabUnitReports = "Rahoton Sassa",
                tabForum = "Tattaunawa",
                tabDevotionals = "Wa'azi & Karatu",
                tabBibleExplorer = "Littafi Mai Tsarki",
                tabMyProfile = "Bayani Na",
                tabGiving = "Zakkar & Sadaka",
                tabCommandCentre = "Cibiyar Gudanarwa",
                englishSwitchLabel = "Koma Turanci (EN)",
                usingNormalLanguageLabel = "Harshen Hausa",
                bibleSearchPlaceholder = "Nemi ayar Littafi Mai Tsarki (misali Daniyel 11:32)...",
                bibleAutoSyncedLabel = "Harshe: Littafi Mai Tsarki (HA)",
                bibleAllLanguagesLabel = "Dukkan Harsuna",
                scriptureOfTheDayBadge = "AYAR LITTAFI TA YAU",
                upcomingProgrammesTitle = "SHIRYE-SHIRYEN MATASA MASU ZUWA",
                postScriptureAction = "Saka Ayar Yau",
                postProgrammeAction = "Saka Sabon Shiri",
                quizAutoSyncedBadge = "Harshen Wasa: Hausa (HA)"
            )
            AppLanguage.IGBO -> LocalizedAppStrings(
                navProgrammes = "Ụlọ",
                navCommunity = "Ọgbakọ",
                navGames = "Egwuregwu",
                navSpiritual = "Ime Mmụọ",
                navProfile = "Profaịlụ M",
                tabUnits = "Ngalaba",
                tabAttendance = "Ọbịbịa",
                tabUnitReports = "Akụkọ Ngalaba",
                tabForum = "Mkparịta ụka",
                tabDevotionals = "Ozizi & Okwuchukwu",
                tabBibleExplorer = "Baịbụl Nsọ",
                tabMyProfile = "Profaịlụ M",
                tabGiving = "Onyinye & Nkwa",
                tabCommandCentre = "Ọdụ Nchịkwa",
                englishSwitchLabel = "Gbanwee gaa Bekee (EN)",
                usingNormalLanguageLabel = "Asụsụ Igbo",
                bibleSearchPlaceholder = "Chọọ amaokwu Baịbụl (dịka Daniel 11:32) ma ọ bụ okwu...",
                bibleAutoSyncedLabel = "Asụsụ: Baịbụl Nsọ (IG)",
                bibleAllLanguagesLabel = "Asụsụ Niile",
                scriptureOfTheDayBadge = "OKWU CHINEKE NKE TAATA",
                upcomingProgrammesTitle = "MMEMME NDỊ NTOROBỊA NA-ABỊA",
                postScriptureAction = "Tinye Amaokwu Taata",
                postProgrammeAction = "Tinye Mmemme Ọhụrụ",
                quizAutoSyncedBadge = "Asụsụ Egwuregwu: Igbo (IG)"
            )
        }
    }

    fun getMemoryVerse(
        reference: String,
        fallbackText: String = "",
        lang: AppLanguage = currentLanguage.value
    ): String {
        val refLower = reference.lowercase()
        return when {
            refLower.contains("11:32") -> when (lang) {
                AppLanguage.ENGLISH -> "...but the people that do know their God shall be strong, and do exploits."
                AppLanguage.YORUBA -> "...ṣùgbọ́n àwọn ènìyàn tí ó mọ Ọlọ́run wọn yóò lágbára, wọn yóò sì ṣe àwọn iṣẹ́ agbára."
                AppLanguage.HAUSA -> "...amma mutanen da suka san Allahnsu za su kasance da ƙarfi, su kuma yi manyan ayyuka."
                AppLanguage.IGBO -> "...ma ndị maara Chineke ha ga-esi ike, meekwa ihe dị ukwuu."
            }
            refLower.contains("6:12") -> when (lang) {
                AppLanguage.ENGLISH -> "The LORD is with thee, thou mighty man of valour."
                AppLanguage.YORUBA -> "OLÚWA wà pẹ̀lú rẹ, ìwọ akọni alágbára."
                AppLanguage.HAUSA -> "Ubangiji yana tare da kai, ya kai jarumi mai ƙarfi."
                AppLanguage.IGBO -> "Onyenwe anyị nọnyeere gị, gị dike n'agha."
            }
            refLower.contains("4:13") -> when (lang) {
                AppLanguage.ENGLISH -> "I can do all things through Christ which strengtheneth me."
                AppLanguage.YORUBA -> "Èmi le ṣe ohun gbogbo nínú Kristi ẹni tí ó ń fi agbára fún mi."
                AppLanguage.HAUSA -> "Zan iya yin komai ta wurin Almasihu wanda yake ƙarfafa ni."
                AppLanguage.IGBO -> "Enwere m ike ime ihe niile site n'aka Kraịst onye na-enye m ike."
            }
            refLower.contains("4:12") -> when (lang) {
                AppLanguage.ENGLISH -> "Let no man despise thy youth; but be thou an example of the believers, in word, in conversation, in charity, in spirit, in faith, in purity."
                AppLanguage.YORUBA -> "Má ṣe jẹ́ kí ẹnikẹ́ni gan ìgbà èwe rẹ; ṣùgbọ́n jẹ́ àpẹẹrẹ fún àwọn onígbàgbọ́."
                AppLanguage.HAUSA -> "Kada kowa ya rena ƙuruciyarka, amma ka zama abin koyi ga masu ba da gaskiya."
                AppLanguage.IGBO -> "Ekwekwala ka onye ọ bụla leda okorobịa gị anya; kama bụrụ ihe nlereanya nye ndị kwere ekwe."
            }
            refLower.contains("1:9") -> when (lang) {
                AppLanguage.ENGLISH -> "Have not I commanded thee? Be strong and of a good courage; be not afraid, neither be thou dismayed: for the LORD thy God is with thee."
                AppLanguage.YORUBA -> "Èmi kò ha ti pa á láṣẹ fún ọ? Jẹ́ alágbára kí o sì ní ìgboyà; má bẹ̀rù, nítorí OLÚWA Ọlọ́run rẹ wà pẹ̀lú rẹ."
                AppLanguage.HAUSA -> "Ban umarce ka ba? Ka yi ƙarfi, ka yi ƙarfin hali; kada ka ji tsoro, gama Ubangiji Allahna yana tare da kai."
                AppLanguage.IGBO -> "Ọ bụghị mụ nyere gị iwu? Dị ike, nweekwa obi ike; atụla egwu, n'ihi na Onyenwe anyị Chineke gị nọnyeere gị."
            }
            refLower.contains("40:31") -> when (lang) {
                AppLanguage.ENGLISH -> "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint."
                AppLanguage.YORUBA -> "Ṣùgbọ́n àwọn tí ó bá dúró de OLÚWA yóò sọ agbára wọn dọtun; wọn yóòaa fi ìyẹ́ fò sókè bí idì; wọn yóò sáré, kì yóò sì rẹ̀ wọ́n."
                AppLanguage.HAUSA -> "Amma waɗanda suke jira Ubangiji za su sabunta ƙarfinsu; za su tashi sama da fukafukai kamar gaggafa."
                AppLanguage.IGBO -> "Ma ndị na-echere Onyenwe anyị ga-agbanwe ike ha ọhụrụ; ha ga-efe elu dị ka ugo; ha ga-agba ọsọ, ike agwụghị ha."
            }
            fallbackText.isNotBlank() -> fallbackText
            else -> when (lang) {
                AppLanguage.ENGLISH -> "...but the people that do know their God shall be strong, and do exploits."
                AppLanguage.YORUBA -> "...ṣùgbọ́n àwọn ènìyàn tí ó mọ Ọlọ́run wọn yóò lágbára, wọn yóò sì ṣe àwọn iṣẹ́ agbára."
                AppLanguage.HAUSA -> "...amma mutanen da suka san Allahnsu za su kasance da ƙarfi, su kuma yi manyan ayyuka."
                AppLanguage.IGBO -> "...ma ndị maara Chineke ha ga-esi ike, meekwa ihe dị ukwuu."
            }
        }
    }

    fun getWelcomeBannerText(): String {
        return when (currentLanguage.value) {
            AppLanguage.ENGLISH -> "The Gideonites - The Youth Alive Fellowship"
            AppLanguage.YORUBA -> "The Gideonites - The Youth Alive Fellowship (Àwọn Òkìkí)"
            AppLanguage.HAUSA -> "The Gideonites - The Youth Alive Fellowship (Manyan Masarautar)"
            AppLanguage.IGBO -> "The Gideonites - The Youth Alive Fellowship (Ndị Dike Alaeze)"
        }
    }
}
