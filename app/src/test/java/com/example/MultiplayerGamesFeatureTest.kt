package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChurchYouthDatabase
import com.example.data.local.entity.MultiplayerGameSessionEntity
import com.example.data.model.GameType
import com.example.data.model.MultiplayerQuestion
import com.example.data.model.PresetGameSessions
import com.example.data.model.SessionStatus
import com.example.data.model.SessionSubstate
import com.example.data.model.SubmissionResult
import com.example.data.model.UserRole
import com.example.data.model.scrambleVerse
import com.example.data.repository.MultiplayerGameRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

import com.example.data.repository.BibleRepositoryImpl
import com.example.data.local.entity.BibleVerseEntity

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiplayerGamesFeatureTest {

    private lateinit var database: ChurchYouthDatabase
    private lateinit var multiplayerRepo: MultiplayerGameRepositoryImpl
    private lateinit var bibleRepo: BibleRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChurchYouthDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        multiplayerRepo = MultiplayerGameRepositoryImpl(
            multiplayerGameDao = database.multiplayerGameDao(),
            auditLogDao = database.auditLogDao()
        )
        bibleRepo = BibleRepositoryImpl(
            bibleDao = database.bibleDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun testTriviaChallengeGameTypeScoringAndLeaderboard() = runTest {
        // 1. Admin creates Trivia Challenge with 4 options, sets correct answer, timer, and points
        val questions = listOf(
            MultiplayerQuestion(
                id = 1,
                gameType = GameType.TRIVIA_CHALLENGE,
                prompt = "In which book is 'the people that do know their God shall be strong' found?",
                options = listOf("Daniel", "Isaiah", "Ezekiel", "Zechariah"),
                correctAnswer = "Daniel",
                scriptureReference = "Daniel 11:32",
                timerSeconds = 20,
                points = 100
            ),
            MultiplayerQuestion(
                id = 2,
                gameType = GameType.TRIVIA_CHALLENGE,
                prompt = "How many days did Esther fast before going to the king?",
                options = listOf("7 days", "3 days", "40 days", "1 day"),
                correctAnswer = "3 days",
                scriptureReference = "Esther 4:16",
                timerSeconds = 20,
                points = 100
            )
        )

        val now = 1_000_000L
        val sessionId = multiplayerRepo.createAndScheduleSession(
            title = "Youth Alive Trivia Challenge",
            description = "Timed multiple choice competition",
            gameType = GameType.TRIVIA_CHALLENGE,
            scheduledStartTime = now,
            timeLimitPerQuestionSeconds = 20,
            leaderboardDurationSeconds = 6,
            baseScorePerQuestion = 100,
            speedBonusEnabled = true,
            maxSpeedBonus = 50,
            questions = questions,
            actorId = "admin-1",
            actorName = "Elder David",
            actorRole = UserRole.ADMINISTRATOR
        )

        // 2. Synchronized Start (no host code needed)
        val startedSession = multiplayerRepo.syncSessionState(sessionId, now)
        assertNotNull(startedSession)
        assertEquals(SessionStatus.ACTIVE, startedSession?.status)
        assertEquals(SessionSubstate.QUESTION_OPEN, startedSession?.currentSubstate)
        assertEquals(0, startedSession?.currentQuestionIndex)

        // 3. Participants join without PIN or room codes
        multiplayerRepo.joinSession(sessionId, "user-alice", "Alice Gideon", UserRole.YOUTH_MEMBER)
        multiplayerRepo.joinSession(sessionId, "user-bob", "Bob Gideon", UserRole.YOUTH_MEMBER)

        // 4. Server-Authoritative answer validation:
        // Alice answers correctly early (gets base points + speed bonus)
        val aliceResult = multiplayerRepo.submitAnswer(
            sessionId = sessionId,
            questionIndex = 0,
            userId = "user-alice",
            userName = "Alice Gideon",
            submittedAnswer = "Daniel",
            submissionTime = now + 2000L // 2 seconds in, 18 seconds remaining
        )
        assertTrue(aliceResult is SubmissionResult.Success)
        val aliceSuccess = aliceResult as SubmissionResult.Success
        assertTrue(aliceSuccess.isCorrect)
        assertTrue("Alice should receive base points + speed bonus", aliceSuccess.pointsAwarded >= 100)

        // Bob answers incorrectly
        val bobResult = multiplayerRepo.submitAnswer(
            sessionId = sessionId,
            questionIndex = 0,
            userId = "user-bob",
            userName = "Bob Gideon",
            submittedAnswer = "Isaiah",
            submissionTime = now + 4000L
        )
        assertTrue(bobResult is SubmissionResult.Success)
        val bobSuccess = bobResult as SubmissionResult.Success
        assertFalse(bobSuccess.isCorrect)
        assertEquals(0, bobSuccess.pointsAwarded)

        // 5. Server prevents duplicate submissions
        val duplicateResult = multiplayerRepo.submitAnswer(
            sessionId = sessionId,
            questionIndex = 0,
            userId = "user-alice",
            userName = "Alice Gideon",
            submittedAnswer = "Daniel",
            submissionTime = now + 5000L
        )
        assertTrue(duplicateResult is SubmissionResult.DuplicateRejected)

        // 6. Leaderboard updates with correct ranking
        val leaderboard = multiplayerRepo.getLeaderboardSync(sessionId, "user-alice")
        assertEquals(2, leaderboard.size)
        assertEquals("user-alice", leaderboard[0].userId)
        assertEquals(1, leaderboard[0].rank)
        assertTrue(leaderboard[0].score >= 100)
        assertEquals("user-bob", leaderboard[1].userId)
        assertEquals(2, leaderboard[1].rank)
        assertEquals(0, leaderboard[1].score)
    }

    @Test
    fun testWhoAmIProgressiveCluesAndEarlierAnswerBonus() = runTest {
        // Who Am I?: Admin enters correct Bible character, provides progressive clues arranged from hardest to easiest
        val questions = listOf(
            MultiplayerQuestion(
                id = 1,
                gameType = GameType.WHO_AM_I,
                prompt = "Identify this Bible figure from progressive clues:",
                clues = listOf(
                    "Clue 1 (Hardest): I was the youngest son of Jesse who tended sheep in Bethlehem.",
                    "Clue 2 (Medium): With a sling and five smooth stones, I defeated Goliath.",
                    "Clue 3 (Easiest): I became king of Israel and was a man after God's own heart."
                ),
                options = listOf("David", "Solomon", "Samuel", "Saul"),
                correctAnswer = "David",
                scriptureReference = "1 Samuel 17",
                timerSeconds = 30,
                points = 100
            )
        )

        val startTime = 2_000_000L
        val sessionId = multiplayerRepo.createAndScheduleSession(
            title = "Youth Alive Who Am I? Showdown",
            description = "Progressive Bible character identification",
            gameType = GameType.WHO_AM_I,
            scheduledStartTime = startTime,
            timeLimitPerQuestionSeconds = 30,
            baseScorePerQuestion = 100,
            speedBonusEnabled = false, // isolate clue bonus
            questions = questions,
            actorId = "admin-1",
            actorName = "Elder David",
            actorRole = UserRole.ADMINISTRATOR
        )

        multiplayerRepo.syncSessionState(sessionId, startTime)

        // User 1 answers during Clue 1 (earliest interval -> hardest clue):
        multiplayerRepo.joinSession(sessionId, "user-champion", "Champion", UserRole.YOUTH_MEMBER)
        val earlyResult = multiplayerRepo.submitAnswer(
            sessionId = sessionId,
            questionIndex = 0,
            userId = "user-champion",
            userName = "Champion",
            submittedAnswer = "David",
            submissionTime = startTime + 2000L // 2s in (Clue 1 active, 0-10s window)
        )
        assertTrue(earlyResult is SubmissionResult.Success)
        val earlySuccess = earlyResult as SubmissionResult.Success
        assertTrue("Earlier correct answer during hardest clue earns 1.5x bonus", earlySuccess.pointsAwarded == 150)

        // User 2 answers later during Clue 3 (easiest clue window):
        multiplayerRepo.joinSession(sessionId, "user-late-solver", "Late Solver", UserRole.YOUTH_MEMBER)
        val lateResult = multiplayerRepo.submitAnswer(
            sessionId = sessionId,
            questionIndex = 0,
            userId = "user-late-solver",
            userName = "Late Solver",
            submittedAnswer = "David",
            submissionTime = startTime + 25000L // 25s in (Clue 3 active, final window)
        )
        assertTrue(lateResult is SubmissionResult.Success)
        val lateSuccess = lateResult as SubmissionResult.Success
        assertTrue("Later answer during easiest clue earns base points", lateSuccess.pointsAwarded == 100)

        assertTrue("Earlier solver earned strictly more points than late solver", earlySuccess.pointsAwarded > lateSuccess.pointsAwarded)
    }

    @Test
    fun testScriptureScrambleAutoScrambleAndReconstruction() = runTest {
        // Scripture Scramble: Admin enters a Bible verse, its reference, and translation.
        val verse = "I can do all things through Christ who strengthens me"
        val ref = "Philippians 4:13"
        val translation = "KJV"

        // 1. Test auto-scramble utility
        val scrambled = scrambleVerse(verse)
        assertEquals("Scrambled words count should match original verse words count", 10, scrambled.size)
        assertTrue("Scrambled words should contain 'Christ'", scrambled.contains("Christ"))
        assertTrue("Scrambled words should contain 'strengthens'", scrambled.contains("strengthens"))

        val questions = listOf(
            MultiplayerQuestion(
                id = 1,
                gameType = GameType.SCRIPTURE_SCRAMBLE,
                prompt = "Reconstruct the verse from Philippians 4:13:",
                scrambledWords = scrambled,
                correctAnswer = verse,
                scriptureReference = ref,
                translation = translation,
                timerSeconds = 30,
                points = 100
            )
        )

        val startTime = 3_000_000L
        val sessionId = multiplayerRepo.createAndScheduleSession(
            title = "Youth Alive Scripture Scramble",
            description = "Verse reconstruction competition",
            gameType = GameType.SCRIPTURE_SCRAMBLE,
            scheduledStartTime = startTime,
            timeLimitPerQuestionSeconds = 30,
            baseScorePerQuestion = 100,
            questions = questions,
            actorId = "admin-1",
            actorName = "Elder David",
            actorRole = UserRole.ADMINISTRATOR
        )

        multiplayerRepo.syncSessionState(sessionId, startTime)
        multiplayerRepo.joinSession(sessionId, "user-scholar", "Scholar John", UserRole.YOUTH_MEMBER)

        // Participant rearranges words and submits correctly
        val correctSubmission = multiplayerRepo.submitAnswer(
            sessionId = sessionId,
            questionIndex = 0,
            userId = "user-scholar",
            userName = "Scholar John",
            submittedAnswer = "I can do all things through Christ who strengthens me",
            submissionTime = startTime + 5000L
        )
        assertTrue(correctSubmission is SubmissionResult.Success)
        assertTrue((correctSubmission as SubmissionResult.Success).isCorrect)
        assertTrue(correctSubmission.pointsAwarded >= 100)

        // Incorrect reconstruction
        multiplayerRepo.joinSession(sessionId, "user-mixed", "Mixed John", UserRole.YOUTH_MEMBER)
        val wrongSubmission = multiplayerRepo.submitAnswer(
            sessionId = sessionId,
            questionIndex = 0,
            userId = "user-mixed",
            userName = "Mixed John",
            submittedAnswer = "All things I can do through Christ",
            submissionTime = startTime + 6000L
        )
        assertTrue(wrongSubmission is SubmissionResult.Success)
        assertFalse((wrongSubmission as SubmissionResult.Success).isCorrect)
    }

    @Test
    fun testAdminQuestionManagementAddEditReorderDelete() {
        val q1 = MultiplayerQuestion(id = 1, gameType = GameType.TRIVIA_CHALLENGE, prompt = "Q1", options = listOf("A", "B"), correctAnswer = "A")
        val q2 = MultiplayerQuestion(id = 2, gameType = GameType.WHO_AM_I, prompt = "Q2", clues = listOf("Clue A", "Clue B"), correctAnswer = "Moses")
        val q3 = MultiplayerQuestion(id = 3, gameType = GameType.SCRIPTURE_SCRAMBLE, prompt = "Q3", correctAnswer = "In the beginning God created", scriptureReference = "Genesis 1:1", translation = "KJV")

        val list = mutableListOf(q1, q2, q3)

        // Reorder: Move Q2 up
        val itemToMove = list.removeAt(1)
        list.add(0, itemToMove)
        assertEquals("Moses", list[0].correctAnswer)
        assertEquals("Q1", list[1].prompt)

        // Edit Q1: Update prompt and options
        list[1] = list[1].copy(prompt = "Updated Q1 Prompt", points = 150)
        assertEquals("Updated Q1 Prompt", list[1].prompt)
        assertEquals(150, list[1].points)

        // Delete Q3:
        list.removeAt(2)
        assertEquals(2, list.size)

        // Serialization and parsing round-trip
        val json = multiplayerRepo.serializeQuestions(list)
        val parsed = multiplayerRepo.parseQuestions(json)
        assertEquals(2, parsed.size)
        assertEquals("Moses", parsed[0].correctAnswer)
        assertEquals(GameType.WHO_AM_I, parsed[0].gameType)
        assertEquals("Updated Q1 Prompt", parsed[1].prompt)
        assertEquals(150, parsed[1].points)
    }

    @Test
    fun testSynchronizedAutoStartAndLateParticipantsJoining() = runTest {
        val questions = PresetGameSessions.getTriviaChallengeQuestions()
        val now = 5_000_000L
        val futureStart = now + 10_000L // 10 seconds in future

        val sessionId = multiplayerRepo.createAndScheduleSession(
            title = "Scheduled Gideonites Tournament",
            description = "Automatic start tournament",
            gameType = GameType.TRIVIA_CHALLENGE,
            scheduledStartTime = futureStart,
            timeLimitPerQuestionSeconds = 15,
            leaderboardDurationSeconds = 5,
            questions = questions,
            actorId = "admin-1",
            actorName = "Elder David",
            actorRole = UserRole.ADMINISTRATOR
        )

        // 1. Before scheduled time: session remains SCHEDULED
        val beforeSync = multiplayerRepo.syncSessionState(sessionId, now + 2000L)
        assertEquals(SessionStatus.SCHEDULED, beforeSync?.status)

        // Participant 1 joins before game starts (Question 0)
        val p1 = multiplayerRepo.joinSession(sessionId, "user-early", "Early Bird", UserRole.YOUTH_MEMBER)
        assertEquals(0, p1.joinedAtQuestionIndex)

        // 2. Scheduled time reached: Automatic start!
        val activeSync = multiplayerRepo.syncSessionState(sessionId, futureStart + 100L)
        assertNotNull(activeSync)
        assertEquals(SessionStatus.ACTIVE, activeSync?.status)
        assertEquals(SessionSubstate.QUESTION_OPEN, activeSync?.currentSubstate)
        assertEquals(0, activeSync?.currentQuestionIndex)

        // P1 submits for Question 0
        val p1Sub = multiplayerRepo.submitAnswer(sessionId, 0, "user-early", "Early Bird", "Daniel", futureStart + 500L)
        assertTrue(p1Sub is SubmissionResult.Success)

        // 3. Question 0 deadline passes -> transitions to LEADERBOARD
        val deadlineQ0 = futureStart + 100L + (15 * 1000L)
        val lbSubstate = multiplayerRepo.syncSessionState(sessionId, deadlineQ0 + 50L)
        assertEquals(SessionSubstate.LEADERBOARD, lbSubstate?.currentSubstate)

        // 4. Leaderboard interval elapses -> transitions to Question 1
        val nextQTime = deadlineQ0 + (5 * 1000L) + 50L
        val nextQSubstate = multiplayerRepo.syncSessionState(sessionId, nextQTime)
        assertEquals(SessionSubstate.QUESTION_OPEN, nextQSubstate?.currentSubstate)
        assertEquals(1, nextQSubstate?.currentQuestionIndex)

        // 5. Late Participant Joins during Question 1:
        // Rule: Late participants start at ongoing question, missed questions receive 0 pts
        val pLate = multiplayerRepo.joinSession(sessionId, "user-late", "Late Comer", UserRole.YOUTH_MEMBER)
        assertEquals("Late participant starting index should be 1", 1, pLate.joinedAtQuestionIndex)
        assertEquals("Late participant score starts at 0", 0, pLate.currentScore)

        // Attempting to answer missed Question 0 is rejected by server
        val missedResult = multiplayerRepo.submitAnswer(sessionId, 0, "user-late", "Late Comer", "Daniel", nextQTime + 500L)
        assertTrue("Submitting for missed question should return Error or MissedQuestion",
            missedResult is SubmissionResult.MissedQuestion || missedResult is SubmissionResult.Error)

        // Answering ongoing Question 1 succeeds
        val q1Result = multiplayerRepo.submitAnswer(sessionId, 1, "user-late", "Late Comer", "3 days", nextQTime + 1000L)
        assertTrue("Submitting for current ongoing question succeeds", q1Result is SubmissionResult.Success)
        assertTrue((q1Result as SubmissionResult.Success).isCorrect)
    }

    @Test
    fun testBibleDatabaseSeedingAndSearch() = runTest {
        // Clear to test seeding from pristine state
        bibleRepo.clearDatabase()
        assertEquals(0, bibleRepo.getVerseCount())

        // Ensure seeded with foundational Gideonites / Youth Alive scriptures
        bibleRepo.ensureSeeded()
        val count = bibleRepo.getVerseCount()
        assertTrue("Bible database should contain seeded scriptures", count >= 20)

        // Search by keyword
        val exploitsResults = bibleRepo.searchVerses("exploits")
        assertTrue("Should find verse containing 'exploits'", exploitsResults.isNotEmpty())
        assertTrue("Should include Daniel 11:32", exploitsResults.any { it.reference == "Daniel 11:32" })

        // Search by reference
        val philResults = bibleRepo.searchVerses("Philippians 4:13")
        assertTrue("Should find Philippians 4:13", philResults.isNotEmpty())
        assertTrue(philResults[0].verseText.contains("I can do all things"))

        // Lookup exact reference
        val joshuaVerse = bibleRepo.getVerseByReference("Joshua 1:9")
        assertNotNull("Should find Joshua 1:9", joshuaVerse)
        assertTrue(joshuaVerse!!.verseText.contains("Be strong and of a good courage"))
    }

    @Test
    fun testBibleJsonAndTextImport() = runTest {
        bibleRepo.ensureSeeded()
        val initialCount = bibleRepo.getVerseCount()

        // 1. Delimited text import (Reference | Verse Text | Translation | Theme)
        val sampleText = """
            Romans 10:17 | So then faith cometh by hearing, and hearing by the word of God. | KJV | Faith & Word
            Psalm 100:4 | Enter into his gates with thanksgiving, and into his courts with praise. | KJV | Praise & Thanksgiving
        """.trimIndent()

        val textImportResult = bibleRepo.importFromDelimitedText(sampleText, "KJV")
        assertTrue(textImportResult.isSuccess)
        assertEquals(2, textImportResult.getOrNull())
        assertEquals(initialCount + 2, bibleRepo.getVerseCount())

        val romans10 = bibleRepo.getVerseByReference("Romans 10:17")
        assertNotNull(romans10)
        assertEquals("Faith & Word", romans10?.theme)

        // 2. JSON import
        val sampleJson = """
            [
                {
                    "book": "Isaiah",
                    "chapter": 60,
                    "verse": 1,
                    "reference": "Isaiah 60:1",
                    "verseText": "Arise, shine; for thy light is come, and the glory of the LORD is risen upon thee.",
                    "translation": "KJV",
                    "theme": "Exploits & Glory"
                }
            ]
        """.trimIndent()

        val jsonImportResult = bibleRepo.importFromJson(sampleJson)
        assertTrue(jsonImportResult.isSuccess)
        assertEquals(1, jsonImportResult.getOrNull())
        assertEquals(initialCount + 3, bibleRepo.getVerseCount())

        val isaiah60 = bibleRepo.getVerseByReference("Isaiah 60:1")
        assertNotNull(isaiah60)
        assertTrue(isaiah60!!.verseText.startsWith("Arise, shine"))
    }

    @Test
    fun testBibleScriptureToScriptureScrambleGameIntegration() = runTest {
        bibleRepo.ensureSeeded()

        // Admin finds verse in Bible database: Daniel 11:32
        val verse = bibleRepo.getVerseByReference("Daniel 11:32")
        assertNotNull(verse)

        // Convert Bible verse to Scripture Scramble question
        val scrambleQuestion = MultiplayerQuestion(
            id = 1,
            gameType = GameType.SCRIPTURE_SCRAMBLE,
            prompt = "Reconstruct the scripture from ${verse!!.reference} (${verse.translation}):",
            correctAnswer = verse.verseText,
            scrambledWords = scrambleVerse(verse.verseText),
            scriptureReference = verse.reference,
            translation = verse.translation,
            explanation = "Theme: ${verse.theme}",
            timerSeconds = 30,
            points = 150
        )

        // Verify scrambled words exist and are shuffled
        assertTrue(scrambleQuestion.scrambledWords.isNotEmpty())

        // Create multiplayer session with this scripture scramble question
        val now = 2_000_000L
        val sessionId = multiplayerRepo.createAndScheduleSession(
            title = "Youth Scripture Scramble Showdown",
            description = "Verse reconstruction competition",
            gameType = GameType.SCRIPTURE_SCRAMBLE,
            scheduledStartTime = now,
            timeLimitPerQuestionSeconds = 30,
            leaderboardDurationSeconds = 6,
            baseScorePerQuestion = 150,
            speedBonusEnabled = false,
            maxSpeedBonus = 0,
            questions = listOf(scrambleQuestion),
            actorId = "admin-1",
            actorName = "Elder David",
            actorRole = UserRole.ADMINISTRATOR
        )

        // Join participant
        multiplayerRepo.joinSession(sessionId, "user-scramble-champion", "David O.", UserRole.YOUTH_MEMBER)

        // Start game session
        multiplayerRepo.syncSessionState(sessionId, now + 100L)

        // Participant submits correct verse sequence
        val result = multiplayerRepo.submitAnswer(
            sessionId = sessionId,
            questionIndex = 0,
            userId = "user-scramble-champion",
            userName = "David O.",
            submittedAnswer = verse.verseText,
            submissionTime = now + 10_000L
        )

        assertTrue("Answer submission should be successful", result is SubmissionResult.Success)
        val success = result as SubmissionResult.Success
        assertTrue("Answer should be evaluated as correct", success.isCorrect)
        assertEquals(150, success.pointsAwarded)

        // Advance past question deadline and leaderboard interval -> session completes!
        val afterQuestionDeadline = now + 100L + (30 * 1000L) + 100L
        multiplayerRepo.syncSessionState(sessionId, afterQuestionDeadline)
        val afterLbDeadline = afterQuestionDeadline + (6 * 1000L) + 100L
        val completedSession = multiplayerRepo.syncSessionState(sessionId, afterLbDeadline)
        assertNotNull(completedSession)
        assertEquals(SessionStatus.COMPLETED, completedSession?.status)

        val completedList = multiplayerRepo.getCompletedSessions().first()
        assertTrue(completedList.any { it.id == sessionId })
    }

    @Test
    fun testAdminCancelScheduledMultiplayerSession() = runTest {
        val now = 9_000_000L
        val sessionId = multiplayerRepo.createAndScheduleSession(
            title = "Scheduled Who Am I Showdown",
            description = "To be cancelled",
            gameType = GameType.WHO_AM_I,
            scheduledStartTime = now + 60_000L,
            timeLimitPerQuestionSeconds = 30,
            questions = PresetGameSessions.getWhoAmIQuestions(),
            actorId = "admin-1",
            actorName = "Elder David",
            actorRole = UserRole.ADMINISTRATOR
        )

        val beforeCancel = multiplayerRepo.getSessionByIdSync(sessionId)
        assertEquals(SessionStatus.SCHEDULED, beforeCancel?.status)

        multiplayerRepo.cancelSession(sessionId, "admin-1", UserRole.ADMINISTRATOR)

        val afterCancel = multiplayerRepo.getSessionByIdSync(sessionId)
        assertEquals(SessionStatus.CANCELLED, afterCancel?.status)
    }

    @Test
    fun testZeroDeadlineInitializationDoesNotPrematurelyExpire() = runTest {
        val now = 4_000_000L
        val sessionId = "quiz_zero_deadline_test"
        database.multiplayerGameDao().insertSession(
            MultiplayerGameSessionEntity(
                id = sessionId,
                title = "Synced Session",
                description = "From cloud with unset deadline",
                gameType = GameType.TRIVIA_CHALLENGE,
                scheduledStartTime = now - 5_000L,
                status = SessionStatus.ACTIVE,
                currentQuestionIndex = 0,
                currentSubstate = SessionSubstate.QUESTION_OPEN,
                currentQuestionDeadline = 0L,
                leaderboardDeadline = 0L,
                timeLimitPerQuestionSeconds = 25,
                leaderboardDurationSeconds = 8,
                questionsJson = multiplayerRepo.serializeQuestions(PresetGameSessions.getTriviaChallengeQuestions()),
                totalQuestions = 3,
                createdById = "admin-1",
                createdByName = "Elder David",
                createdAt = now - 10_000L,
                roomPin = "GIDEON"
            )
        )

        val afterQuestionSync = multiplayerRepo.syncSessionState(sessionId, now)
        assertNotNull(afterQuestionSync)
        assertEquals(SessionSubstate.QUESTION_OPEN, afterQuestionSync?.currentSubstate)
        assertEquals(now + 25_000L, afterQuestionSync?.currentQuestionDeadline)

        database.multiplayerGameDao().updateSession(
            afterQuestionSync!!.copy(
                currentSubstate = SessionSubstate.LEADERBOARD,
                leaderboardDeadline = 0L
            )
        )
        val afterLbSync = multiplayerRepo.syncSessionState(sessionId, now + 1000L)
        assertNotNull(afterLbSync)
        assertEquals(SessionSubstate.LEADERBOARD, afterLbSync?.currentSubstate)
        assertEquals(now + 1000L + 8_000L, afterLbSync?.leaderboardDeadline)
    }

    @Test
    fun testCloudSyncConvergesSubstateDeadlinesScoringAndCancellation() = runTest {
        val sessionId = "quiz_cloud_converge_01"
        val cloudActive = com.example.data.remote.CloudQuizSession(
            id = sessionId,
            hostId = "host-1",
            hostName = "Host Exco",
            roomPin = "PIN300",
            titleEn = "Synced Showdown",
            gameType = GameType.WHO_AM_I.name,
            status = SessionStatus.ACTIVE.name,
            currentQuestionIndex = 1L,
            currentSubstate = SessionSubstate.LEADERBOARD.name,
            currentQuestionDeadline = 123_000L,
            leaderboardDeadline = 131_000L,
            timeLimitPerQuestionSeconds = 30L,
            leaderboardDurationSeconds = 8L,
            baseScorePerQuestion = 200L,
            speedBonusEnabled = false,
            maxSpeedBonus = 0L,
            penaltyForIncorrect = true,
            totalQuestions = 4L
        )
        multiplayerRepo.syncCloudQuizSessionsToLocalDb(listOf(cloudActive))

        val synced = multiplayerRepo.getSessionByIdSync(sessionId)
        assertNotNull(synced)
        assertEquals("PIN300", synced?.roomPin)
        assertEquals(SessionStatus.ACTIVE, synced?.status)
        assertEquals(1, synced?.currentQuestionIndex)
        assertEquals(SessionSubstate.LEADERBOARD, synced?.currentSubstate)
        assertEquals(123_000L, synced?.currentQuestionDeadline)
        assertEquals(131_000L, synced?.leaderboardDeadline)
        assertEquals(200, synced?.baseScorePerQuestion)
        assertEquals(false, synced?.speedBonusEnabled)
        assertEquals(true, synced?.penaltyForIncorrect)

        multiplayerRepo.syncCloudQuizSessionsToLocalDb(
            listOf(cloudActive.copy(status = SessionStatus.CANCELLED.name))
        )
        val cancelled = multiplayerRepo.getSessionByIdSync(sessionId)
        assertEquals(SessionStatus.CANCELLED, cancelled?.status)
    }

    @Test
    fun testSaveEditLaunchAndDeleteDraftSession() = runTest {
        val initialQuestions = PresetGameSessions.getTriviaChallengeQuestions()
        val draftId = multiplayerRepo.saveDraftSession(
            title = "Sunday Evening Draft",
            description = "Prepared ahead of fellowship",
            gameType = GameType.TRIVIA_CHALLENGE,
            timeLimitPerQuestionSeconds = 25,
            baseScorePerQuestion = 100,
            speedBonusEnabled = true,
            questions = initialQuestions,
            actorId = "admin-1",
            actorName = "Elder David",
            actorRole = UserRole.ADMINISTRATOR
        )

        val savedDraft = multiplayerRepo.getSessionByIdSync(draftId)
        assertNotNull(savedDraft)
        assertEquals(SessionStatus.DRAFT, savedDraft?.status)
        assertEquals(0L, savedDraft?.scheduledStartTime)
        assertEquals(initialQuestions.size, savedDraft?.totalQuestions)

        // Draft appears in getDraftSessions(), NOT in upcoming/active sessions
        val drafts = multiplayerRepo.getDraftSessions().first()
        assertTrue(drafts.any { it.id == draftId })
        val activeOrScheduled = multiplayerRepo.getUpcomingAndActiveSessions().first()
        assertFalse(activeOrScheduled.any { it.id == draftId })

        // syncSessionState must NOT start a DRAFT session prematurely
        val syncedNoOp = multiplayerRepo.syncSessionState(draftId, System.currentTimeMillis())
        assertEquals(SessionStatus.DRAFT, syncedNoOp?.status)

        // Edit existing draft with fewer questions (e.g., 1 question) and updated title
        val editedQuestions = listOf(initialQuestions.first())
        multiplayerRepo.saveDraftSession(
            title = "Edited Fellowship Draft",
            description = "Updated description",
            gameType = GameType.WHO_AM_I,
            timeLimitPerQuestionSeconds = 30,
            baseScorePerQuestion = 200,
            speedBonusEnabled = false,
            questions = editedQuestions,
            actorId = "admin-1",
            actorName = "Elder David",
            actorRole = UserRole.ADMINISTRATOR,
            existingSessionId = draftId
        )

        val editedDraft = multiplayerRepo.getSessionByIdSync(draftId)
        assertEquals("Edited Fellowship Draft", editedDraft?.title)
        assertEquals(GameType.WHO_AM_I, editedDraft?.gameType)
        assertEquals(1, editedDraft?.totalQuestions)
        assertEquals(savedDraft?.roomPin, editedDraft?.roomPin)

        // Cloud sync for a DRAFT keeps scheduledStartTime=0 and updated question count
        multiplayerRepo.syncCloudQuizSessionsToLocalDb(
            listOf(
                com.example.data.remote.CloudQuizSession(
                    id = draftId,
                    hostId = "admin-1",
                    hostName = "Elder David",
                    roomPin = savedDraft!!.roomPin,
                    titleEn = "Edited Fellowship Draft",
                    gameType = GameType.WHO_AM_I.name,
                    status = SessionStatus.DRAFT.name,
                    scheduledStartTime = 0L,
                    totalQuestions = 1L,
                    questionsJson = multiplayerRepo.serializeQuestions(editedQuestions)
                )
            )
        )
        val cloudSyncedDraft = multiplayerRepo.getSessionByIdSync(draftId)
        assertEquals(SessionStatus.DRAFT, cloudSyncedDraft?.status)
        assertEquals(0L, cloudSyncedDraft?.scheduledStartTime)
        assertEquals(1, cloudSyncedDraft?.totalQuestions)

        // Launch draft immediately -> moves to ACTIVE and QUESTION_OPEN
        val launchTime = 7_000_000L
        val launched = multiplayerRepo.launchDraftSession(draftId, delayMinutes = 0, currentTime = launchTime)
        assertNotNull(launched)
        assertEquals(SessionStatus.ACTIVE, launched?.status)
        assertEquals(SessionSubstate.QUESTION_OPEN, launched?.currentSubstate)
        assertEquals(launchTime + 30_000L, launched?.currentQuestionDeadline)
        assertFalse(multiplayerRepo.getDraftSessions().first().any { it.id == draftId })
        assertTrue(multiplayerRepo.getUpcomingAndActiveSessions().first().any { it.id == draftId })

        // Create and delete another draft
        val secondDraftId = multiplayerRepo.saveDraftSession(
            title = "Disposable Draft",
            description = "To delete",
            gameType = GameType.TRIVIA_CHALLENGE,
            questions = initialQuestions,
            actorId = "admin-1",
            actorName = "Elder David",
            actorRole = UserRole.ADMINISTRATOR
        )
        assertTrue(multiplayerRepo.getDraftSessions().first().any { it.id == secondDraftId })
        multiplayerRepo.deleteDraftSession(secondDraftId, "admin-1", UserRole.ADMINISTRATOR)
        assertFalse(multiplayerRepo.getDraftSessions().first().any { it.id == secondDraftId })
    }
}
