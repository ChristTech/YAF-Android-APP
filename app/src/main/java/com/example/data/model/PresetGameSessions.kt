package com.example.data.model

import com.example.core.localization.AppLanguage
import com.example.core.localization.LanguageManager
import com.example.ui.engagement.QuizQuestion

object PresetGameSessions {

    fun getTriviaChallengeQuestions(lang: AppLanguage = LanguageManager.currentLanguage.value): List<MultiplayerQuestion> {
        val base = listOf(
            MultiplayerQuestion(
                id = 1,
                gameType = GameType.TRIVIA_CHALLENGE,
                prompt = "In which book of the Bible is the prophecy '...the people that do know their God shall be strong, and do exploits' found?",
                options = listOf("Daniel", "Isaiah", "Ezekiel", "Zechariah"),
                correctAnswer = "Daniel",
                scriptureReference = "Daniel 11:32",
                explanation = "Daniel 11:32b declares the covenant promise for The Gideonites (Youth Alive) who know their God."
            ),
            MultiplayerQuestion(
                id = 2,
                gameType = GameType.TRIVIA_CHALLENGE,
                prompt = "How many days did Esther and the Jews fast before she appeared before King Ahasuerus?",
                options = listOf("7 days", "3 days", "40 days", "1 day"),
                correctAnswer = "3 days",
                scriptureReference = "Esther 4:16",
                explanation = "Esther instructed Mordecai: 'Fast ye for me, and neither eat nor drink three days, night or day.'"
            ),
            MultiplayerQuestion(
                id = 3,
                gameType = GameType.TRIVIA_CHALLENGE,
                prompt = "How many days did it take Nehemiah and the children of Israel to rebuild the broken wall of Jerusalem?",
                options = listOf("120 days", "52 days", "70 days", "30 days"),
                correctAnswer = "52 days",
                scriptureReference = "Nehemiah 6:15",
                explanation = "The wall was finished in fifty and two days, demonstrating divine speed."
            ),
            MultiplayerQuestion(
                id = 4,
                gameType = GameType.TRIVIA_CHALLENGE,
                prompt = "Which Old Testament king was appointed and began to reign over Judah at just seven years of age?",
                options = listOf("Josiah", "Joash", "Hezekiah", "David"),
                correctAnswer = "Joash",
                scriptureReference = "2 Kings 11:21",
                explanation = "Seven years old was Jehoash when he began to reign."
            )
        )
        return base.map { localizeMultiplayerQuestion(it, lang) }
    }

    fun getWhoAmIQuestions(lang: AppLanguage = LanguageManager.currentLanguage.value): List<MultiplayerQuestion> {
        val base = listOf(
            MultiplayerQuestion(
                id = 1,
                gameType = GameType.WHO_AM_I,
                prompt = "Mystery Character #1: Identify this Bible figure from the clues:",
                clues = listOf(
                    "Clue 1: I was the youngest son of Jesse and tended sheep in Bethlehem.",
                    "Clue 2: With only a sling and five smooth stones, I defeated a giant named Goliath.",
                    "Clue 3: I became king of Israel, wrote many Psalms, and was called a man after God's own heart."
                ),
                options = listOf("David", "Solomon", "Samuel", "Saul"),
                correctAnswer = "David",
                scriptureReference = "1 Samuel 17, Acts 13:22",
                explanation = "King David slew Goliath, united Israel, and received an everlasting covenant."
            ),
            MultiplayerQuestion(
                id = 2,
                gameType = GameType.WHO_AM_I,
                prompt = "Mystery Character #2: Identify this Bible figure from the clues:",
                clues = listOf(
                    "Clue 1: I was an orphan raised by my cousin Mordecai in Shushan.",
                    "Clue 2: I became queen of Persia and risked my life to approach the golden sceptre.",
                    "Clue 3: I courageously proclaimed 'If I perish, I perish' to deliver my people from Haman's plot."
                ),
                options = listOf("Ruth", "Esther", "Deborah", "Sarah"),
                correctAnswer = "Esther",
                scriptureReference = "Esther 4:16",
                explanation = "Queen Esther delivered the Jewish nation for such a time as this."
            ),
            MultiplayerQuestion(
                id = 3,
                gameType = GameType.WHO_AM_I,
                prompt = "Mystery Character #3: Identify this Bible figure from the clues:",
                clues = listOf(
                    "Clue 1: I was a prophet of fire who confronted 450 prophets of Baal on Mount Carmel.",
                    "Clue 2: I prayed and fire fell from heaven to consume the sacrifice and the water.",
                    "Clue 3: I did not see death, but was taken up into heaven in a chariot of fire and horses of fire."
                ),
                options = listOf("Elisha", "Elijah", "Isaiah", "Jeremiah"),
                correctAnswer = "Elijah",
                scriptureReference = "1 Kings 18, 2 Kings 2:11",
                explanation = "The Prophet Elijah was taken by a whirlwind into heaven."
            ),
            MultiplayerQuestion(
                id = 4,
                gameType = GameType.WHO_AM_I,
                prompt = "Mystery Character #4: Identify this Bible figure from the clues:",
                clues = listOf(
                    "Clue 1: I was once a strict Pharisee of Tarsus named Saul who persecuted believers.",
                    "Clue 2: A blinding light on the road to Damascus transformed my life forever.",
                    "Clue 3: I became the apostle to the Gentiles and authored much of the New Testament epistles."
                ),
                options = listOf("Peter", "Paul", "Stephen", "Barnabas"),
                correctAnswer = "Paul",
                scriptureReference = "Acts 9, 2 Timothy 4:7",
                explanation = "The Apostle Paul fought the good fight, finished his course, and kept the faith."
            )
        )
        return base.map { localizeMultiplayerQuestion(it, lang) }
    }

    fun getScriptureScrambleQuestions(lang: AppLanguage = LanguageManager.currentLanguage.value): List<MultiplayerQuestion> {
        val base = listOf(
            MultiplayerQuestion(
                id = 1,
                gameType = GameType.SCRIPTURE_SCRAMBLE,
                prompt = "Reconstruct the verse from Philippians 4:13:",
                scrambledWords = listOf("strengthens", "all", "do", "Christ", "can", "through", "me", "things", "I", "who"),
                correctAnswer = "I can do all things through Christ who strengthens me",
                options = listOf(
                    "I can do all things through Christ who strengthens me",
                    "Through Christ who strengthens me I can do things all",
                    "All things I can do through who strengthens Christ me",
                    "I do all things can through Christ who strengthens me"
                ),
                scriptureReference = "Philippians 4:13",
                explanation = "A foundational scripture of empowerment for every Gideonite."
            ),
            MultiplayerQuestion(
                id = 2,
                gameType = GameType.SCRIPTURE_SCRAMBLE,
                prompt = "Reconstruct the covenant exploit verse from Daniel 11:32b:",
                scrambledWords = listOf("strong", "and", "shall", "know", "God", "be", "do", "exploits", "the", "people", "that", "their"),
                correctAnswer = "the people that do know their God shall be strong and do exploits",
                options = listOf(
                    "the people that do know their God shall be strong and do exploits",
                    "their God that know the people shall be strong and do exploits",
                    "shall be strong and do exploits the people that do know their God",
                    "the people shall be strong and do exploits that do know their God"
                ),
                scriptureReference = "Daniel 11:32b",
                explanation = "The Gideonites - Youth Alive covenant verse: knowledge of God produces strength and supernatural exploits."
            ),
            MultiplayerQuestion(
                id = 3,
                gameType = GameType.SCRIPTURE_SCRAMBLE,
                prompt = "Reconstruct the prophetic promise in Jeremiah 29:11:",
                scrambledWords = listOf("peace", "I", "thoughts", "of", "the", "know", "for", "you", "thoughts", "of"),
                correctAnswer = "for I know the thoughts that I think toward you thoughts of peace",
                options = listOf(
                    "for I know the thoughts that I think toward you thoughts of peace",
                    "thoughts of peace for I know that I think toward you",
                    "toward you thoughts of peace for I know the thoughts",
                    "I know for the thoughts of peace toward you that I think"
                ),
                scriptureReference = "Jeremiah 29:11",
                explanation = "God's plans for our lives are of peace and not of evil, to give an expected end."
            )
        )
        return base.map { localizeMultiplayerQuestion(it, lang) }
    }

    /**
     * Dynamically localizes a solo QuizQuestion to the active language (EN, YO, HA, IG)
     * so toggling the TopAppBar language button immediately updates the question on screen.
     */
    fun localizeQuizQuestion(question: QuizQuestion, lang: AppLanguage): QuizQuestion {
        if (lang == AppLanguage.ENGLISH) return question
        val ref = question.scriptureRef.lowercase()
        val qLower = question.question.lowercase()

        return when {
            ref.contains("11:32") || qLower.contains("exploits") -> when (lang) {
                AppLanguage.YORUBA -> question.copy(
                    question = "Nínú ìwé Bíbélì wo ni '...àwọn ènìyàn tí ó mọ Ọlọ́run wọn yóò lágbára, wọn yóò sì ṣe iṣẹ́ agbára' wà?",
                    scriptureRef = "Danieli 11:32 (Bibeli Mimo)"
                )
                AppLanguage.HAUSA -> question.copy(
                    question = "A wane littafi na Littafi Mai Tsarki aka rubuta '...mutanen da suka san Allahnsu za su kasance da ƙarfi, su kuma yi manyan ayyuka'?",
                    scriptureRef = "Daniyel 11:32 (Littafi Mai Tsarki)"
                )
                AppLanguage.IGBO -> question.copy(
                    question = "N'ime akwụkwọ Baịbụl ole ka e dere '...ndị maara Chineke ha ga-esi ike, meekwa ihe dị ukwuu'?",
                    scriptureRef = "Daniel 11:32 (Baịbụl Nsọ)"
                )
                else -> question
            }
            ref.contains("4:16") || qLower.contains("esther") -> when (lang) {
                AppLanguage.YORUBA -> question.copy(
                    question = "Ọjọ́ mélòó ni Ẹ́sítà àti àwọn Júù fi gbààwẹ̀ kí ó tó lọ síwájú Ọba?",
                    scriptureRef = "Ẹ́sítà 4:16 (Bibeli Mimo)"
                )
                AppLanguage.HAUSA -> question.copy(
                    question = "Kwana nawa ne Esta da Yahudawa suka yi azumi kafin ta shiga gaban Sarki?",
                    scriptureRef = "Esta 4:16 (Littafi Mai Tsarki)"
                )
                AppLanguage.IGBO -> question.copy(
                    question = "Ụbọchị ole ka Esta na ndị Juu buuru ọnụ tupu ọ baa n'ihu Eze?",
                    scriptureRef = "Esta 4:16 (Baịbụl Nsọ)"
                )
                else -> question
            }
            ref.contains("6:15") || qLower.contains("nehemiah") -> when (lang) {
                AppLanguage.YORUBA -> question.copy(
                    question = "Ọjọ́ mélòó ni ó gba Nehemáyà láti tún odi Jerúsálẹ́mù tí ó wó kọ́?",
                    scriptureRef = "Nehemáyà 6:15 (Bibeli Mimo)"
                )
                AppLanguage.HAUSA -> question.copy(
                    question = "Kwana nawa ne Nehemiya ya ɗauka don sake gina ganuwar Urushalima?",
                    scriptureRef = "Nehemiya 6:15 (Littafi Mai Tsarki)"
                )
                AppLanguage.IGBO -> question.copy(
                    question = "Ụbọchị ole ka o were Nehemaya iwuighachi mgbidi Jerusalem dara ada?",
                    scriptureRef = "Nehemaya 6:15 (Baịbụl Nsọ)"
                )
                else -> question
            }
            ref.contains("11:21") || qLower.contains("seven years") -> when (lang) {
                AppLanguage.YORUBA -> question.copy(
                    question = "Ọba Májẹ́mú Láéláé wo ni ó bẹ̀rẹ̀ sí í jọba lórí Júdà ní ọmọ ọdún méje péré?",
                    scriptureRef = "2 Àwọn Ọba 11:21 (Bibeli Mimo)"
                )
                AppLanguage.HAUSA -> question.copy(
                    question = "Wane sarki a Tsohon Alkawari ya fara mulki a kan Yahuza yana ɗan shekara bakwai kawai?",
                    scriptureRef = "2 Sarakuna 11:21 (Littafi Mai Tsarki)"
                )
                AppLanguage.IGBO -> question.copy(
                    question = "Olee eze Agba Ochie malitere ịchị Juda mgbe ọ dị naanị afọ asaa?",
                    scriptureRef = "2 Ndị Eze 11:21 (Baịbụl Nsọ)"
                )
                else -> question
            }
            else -> question
        }
    }

    /**
     * Dynamically localizes a MultiplayerQuestion to the active language (EN, YO, HA, IG).
     */
    fun localizeMultiplayerQuestion(question: MultiplayerQuestion, lang: AppLanguage): MultiplayerQuestion {
        if (lang == AppLanguage.ENGLISH) return question
        val ref = question.scriptureReference.lowercase()

        return when (question.gameType) {
            GameType.TRIVIA_CHALLENGE -> {
                val localized = localizeQuizQuestion(
                    QuizQuestion(
                        id = question.id,
                        question = question.prompt,
                        options = question.options,
                        correctAnswerIndex = 0,
                        scriptureRef = question.scriptureReference
                    ),
                    lang
                )
                question.copy(
                    prompt = localized.question,
                    scriptureReference = localized.scriptureRef
                )
            }
            GameType.WHO_AM_I -> when {
                question.correctAnswer.equals("David", ignoreCase = true) -> when (lang) {
                    AppLanguage.YORUBA -> question.copy(
                        prompt = "Àdììtú #1: Ta ni ẹni Bíbélì yìí látara àwọn àmì wọ̀nyí?",
                        clues = listOf(
                            "Àmì 1: Èmi ni ọmọkùnrin àbíkẹ́yìn Jésè tí ó ńsin àgùntàn ní Bẹ́tílẹ́hẹ́mù.",
                            "Àmì 2: Pẹ̀lú kànnàkànnà àti òkúta márùn-ún, mo ṣẹ́gun Gòláyátì.",
                            "Àmì 3: Mo di ọba Ísírẹ́lì, mo sì kọ ọ̀pọ̀lọpọ̀ Sáàmù."
                        )
                    )
                    AppLanguage.HAUSA -> question.copy(
                        prompt = "Tambaya #1: Wane mutum ne a Littafi Mai Tsarki daga waɗannan alamu?",
                        clues = listOf(
                            "Alama 1: Ni ne ƙaramin ɗan Yesse mai kiwon tumaki a Baitalami.",
                            "Alama 2: Da majajjawa da duwatsu biyar kawai na kayar da Goliya.",
                            "Alama 3: Na zama sarkin Isra'ila kuma na rubuta Zabura da yawa."
                        )
                    )
                    AppLanguage.IGBO -> question.copy(
                        prompt = "Ajụjụ #1: Onye bụ onye a n'ime Baịbụl site n'ihe ngosi ndị a?",
                        clues = listOf(
                            "Ihe Ngosi 1: Abụ m nwa ikpeazụ Jesi na-azụ atụrụ na Betlehem.",
                            "Ihe Ngosi 2: Site na égbè nkume na nkume ise, emeri m Golaịat.",
                            "Ihe Ngosi 3: Aghọrọ m eze Izrel ma dee ọtụtụ Abụ Ọma."
                        )
                    )
                    else -> question
                }
                question.correctAnswer.equals("Esther", ignoreCase = true) -> when (lang) {
                    AppLanguage.YORUBA -> question.copy(
                        prompt = "Àdììtú #2: Ta ni ẹni Bíbélì yìí látara àwọn àmì wọ̀nyí?",
                        clues = listOf(
                            "Àmì 1: Mọ́dékáì ni ó tọ́ mi dàgbà ní Ṣúṣánì.",
                            "Àmì 2: Mo di ayaba Pásíà, mo sì fi ẹ̀mí mi wewu láti tọ ọba lọ.",
                            "Àmì 3: Mo wí pé 'Bí mo bá ṣègbé, mo ṣègbé' láti gba àwọn ènìyàn mi là."
                        )
                    )
                    AppLanguage.HAUSA -> question.copy(
                        prompt = "Tambaya #2: Wace mace ce a Littafi Mai Tsarki daga waɗannan alamu?",
                        clues = listOf(
                            "Alama 1: Mordekai ne ya rene ni a birnin Shushan.",
                            "Alama 2: Na zama sarauniyar Farisa kuma na sadaukar da raina.",
                            "Alama 3: Na ce 'Idan na halaka, na halaka' don ceto jama'ata."
                        )
                    )
                    AppLanguage.IGBO -> question.copy(
                        prompt = "Ajụjụ #2: Onye bụ nwanyị a n'ime Baịbụl site n'ihe ngosi ndị a?",
                        clues = listOf(
                            "Ihe Ngosi 1: Mọdekaị zụlitere m n'obodo Shushan.",
                            "Ihe Ngosi 2: Aghọrọ m eze nwanyị Peshia ma tinye ndụ m n'ihe ize ndụ.",
                            "Ihe Ngosi 3: Ekwuru m sị 'Ọ bụrụ na m laa n'iyi, ka m laa n'iyi' iji zọpụta ndị m."
                        )
                    )
                    else -> question
                }
                else -> question
            }
            GameType.SCRIPTURE_SCRAMBLE -> when {
                ref.contains("4:13") -> when (lang) {
                    AppLanguage.YORUBA -> question.copy(
                        prompt = "Tún ẹsẹ Bíbélì Filipi 4:13 tò (Bibeli Mimo: 'Èmi le ṣe ohun gbogbo nínú Kristi'):"
                    )
                    AppLanguage.HAUSA -> question.copy(
                        prompt = "Sake tsara ayar Filibiyawa 4:13 (Littafi Mai Tsarki: 'Zan iya yin komai ta wurin Almasihu'):"
                    )
                    AppLanguage.IGBO -> question.copy(
                        prompt = "Hazigharịa amaokwu Ndị Filipaị 4:13 (Baịbụl Nsọ: 'Enwere m ike ime ihe niile site n'aka Kraịst'):"
                    )
                    else -> question
                }
                ref.contains("11:32") -> when (lang) {
                    AppLanguage.YORUBA -> question.copy(
                        prompt = "Tún ẹsẹ Bíbélì Danieli 11:32b tò (Bibeli Mimo: 'Àwọn ènìyàn tí ó mọ Ọlọ́run wọn yóò lágbára'):"
                    )
                    AppLanguage.HAUSA -> question.copy(
                        prompt = "Sake tsara ayar Daniyel 11:32b (Littafi Mai Tsarki: 'Mutanen da suka san Allahnsu za su kasance da ƙarfi'):"
                    )
                    AppLanguage.IGBO -> question.copy(
                        prompt = "Hazigharịa amaokwu Daniel 11:32b (Baịbụl Nsọ: 'Ndị maara Chineke ha ga-esi ike'):"
                    )
                    else -> question
                }
                else -> question
            }
        }
    }
}
