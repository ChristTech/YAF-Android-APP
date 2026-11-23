package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChurchYouthDatabase
import com.example.data.local.entity.SessionEntity
import com.example.data.model.DepartmentType
import com.example.data.model.EventCategory
import com.example.data.model.FinancialCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.UserRole
import com.example.data.repository.EventRepositoryImpl
import com.example.data.repository.FinanceRepositoryImpl
import com.example.data.repository.GamesRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WeeklyReportRepositoryImpl
import com.example.ui.auth.AuthViewModel
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.engagement.EngagementViewModel
import com.example.ui.finance.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ViewModelFlowsTest {

    private lateinit var database: ChurchYouthDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var eventRepository: EventRepositoryImpl
    private lateinit var financeRepository: FinanceRepositoryImpl
    private lateinit var gamesRepository: GamesRepositoryImpl
    private lateinit var weeklyReportRepository: WeeklyReportRepositoryImpl

    private val testDispatcher = StandardTestDispatcher()

    private val testSession = SessionEntity(
        token = "token-test-123",
        userId = "test-user-id",
        userRole = UserRole.YOUTH_EXECUTIVE,
        userName = "Brother Peter",
        expiresAt = System.currentTimeMillis() + 86400000L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val executor = testDispatcher.asExecutor()
        database = Room.inMemoryDatabaseBuilder(context, ChurchYouthDatabase::class.java)
            .setQueryExecutor(executor)
            .setTransactionExecutor(executor)
            .allowMainThreadQueries()
            .build()

        userRepository = UserRepositoryImpl(database.userDao(), database.auditLogDao())
        eventRepository = EventRepositoryImpl(database.eventDao(), database.auditLogDao())
        financeRepository = FinanceRepositoryImpl(database.financialRecordDao(), database.auditLogDao())
        gamesRepository = GamesRepositoryImpl(database.gameDao(), database.auditLogDao())
        weeklyReportRepository = WeeklyReportRepositoryImpl(database.weeklyReportDao(), database.auditLogDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `auth viewmodel validates and toggles mode`() = runTest {
        val authVm = AuthViewModel(userRepository)

        assertFalse(authVm.uiState.value.isRegisterMode)
        authVm.toggleAuthMode()
        assertTrue(authVm.uiState.value.isRegisterMode)

        // Empty login validation
        authVm.login("", "")
        assertEquals("Please enter both credentials.", authVm.uiState.value.errorMessage)
    }

    @Test
    fun `dashboard viewmodel creates event and handles registration`() = runTest {
        val dashboardVm = DashboardViewModel(eventRepository)

        dashboardVm.createEvent(
            title = "Youth Revival Night",
            description = "Special worship and word session",
            daysInFuture = 3,
            venue = "Youth Chapel",
            speaker = "Pastor James",
            category = EventCategory.YOUTH_SERVICE,
            limit = 100,
            session = testSession
        )
        testScheduler.advanceUntilIdle()

        val events = eventRepository.getAllEvents().first()
        assertEquals(1, events.size)
        assertEquals("Youth Revival Night", events[0].title)

        // Register for event
        dashboardVm.clearMessage()
        dashboardVm.registerForEvent(events[0], testSession)
        testScheduler.advanceUntilIdle()
        assertTrue(dashboardVm.uiState.value.message?.startsWith("You have successfully registered for Youth Revival Night!") == true)
    }

    @Test
    fun `finance viewmodel records giving and calculates summary`() = runTest {
        val financeVm = FinanceViewModel(financeRepository, testSession)

        financeVm.recordGiving(
            amount = 25000.0,
            category = FinancialCategory.SPECIAL_PROJECT,
            method = PaymentMethod.BANK_TRANSFER,
            isPledge = false,
            notes = "Youth Sound Gear Donation"
        )
        testScheduler.advanceUntilIdle()

        val records = financeRepository.getRecordsForUser(testSession.userId).first()
        assertEquals(1, records.size)
        assertEquals(25000.0, records[0].amount, 0.01)

        val myTotal = financeRepository.getTotalCompletedAmountForUser(testSession.userId).first()
        assertEquals(25000.0, myTotal ?: 0.0, 0.01)
    }

    @Test
    fun `engagement viewmodel handles quiz flow and score submission`() = runTest {
        val engagementVm = EngagementViewModel(gamesRepository, weeklyReportRepository, testSession)

        // Verify initial state
        assertEquals(0, engagementVm.uiState.value.activeTab)
        engagementVm.setActiveTab(1)
        assertEquals(1, engagementVm.uiState.value.activeTab)

        // Submit weekly report
        engagementVm.submitWeeklyReport(
            title = "Choir Department Rehearsal Report",
            dept = DepartmentType.CHOIR,
            fellowship = "Zion",
            attendance = 22,
            males = 10,
            females = 12,
            firstTimers = 3,
            activities = "Harmonized vocals for conference songs",
            achievements = "Mastered 4 anthems",
            challenges = "Microphone stand loose",
            finSummary = "None",
            nextWeekGoals = "Stage rehearsal on Saturday"
        )
        testScheduler.advanceUntilIdle()

        val reports = weeklyReportRepository.getAllReports().first()
        assertEquals(1, reports.size)
        assertEquals("Choir Department Rehearsal Report", reports[0].title)
    }

    @Test
    fun `game session hides answer correctness until timer expires and ranks top 5 with individual standing`() = runTest {
        val engagementVm = EngagementViewModel(gamesRepository, weeklyReportRepository, testSession)
        val quiz = com.example.data.local.entity.GameQuizEntity(
            id = "quiz-test-1",
            title = "Gideonites Showdown",
            description = "Delayed reveal test",
            category = com.example.data.model.QuizCategory.entries.first(),
            difficulty = "Medium",
            questionsJson = """
                [
                  {
                    "id": 1,
                    "question": "Who led 300 men with trumpets and pitchers?",
                    "options": ["Gideon", "Samson", "Saul", "David"],
                    "correctAnswerIndex": 0,
                    "scriptureRef": "Judges 7:7"
                  },
                  {
                    "id": 2,
                    "question": "Who succeeded Moses?",
                    "options": ["Caleb", "Joshua", "Aaron", "Hur"],
                    "correctAnswerIndex": 1,
                    "scriptureRef": "Joshua 1:1"
                  }
                ]
            """.trimIndent(),
            totalQuestions = 2,
            timeLimitSeconds = 30
        )

        engagementVm.startQuiz(quiz, isLiveGame = true)
        testScheduler.runCurrent()

        // 7 total competitors seeded (so Top 5 public display vs Individual Standing #1-#7 works)
        assertEquals(7, engagementVm.uiState.value.activeQuiz.competitors.size)

        // User selects option 0 while timer still has 15s left:
        // Answer is locked in, but correctness/points/leaderboard are NOT revealed until timer hits 0!
        engagementVm.selectOption(0)
        val lockedState = engagementVm.uiState.value.activeQuiz
        assertEquals(0, lockedState.selectedOptionIndex)
        assertFalse("Correctness must remain hidden while timer is running", lockedState.isAnswerChecked)
        assertFalse("Leaderboard must not open until timer reaches 0", lockedState.isIntermissionLeaderboard)
        assertEquals(0, lockedState.pointsAwardedLastQuestion)

        // Advance timer by 15 seconds so question timer expires -> now answer & leaderboard reveal!
        testScheduler.advanceTimeBy(15_100L)
        testScheduler.runCurrent()

        val revealedState = engagementVm.uiState.value.activeQuiz
        assertTrue("Answer should be checked once timer reaches 0", revealedState.isAnswerChecked)
        assertTrue("Intermission leaderboard should open once timer reaches 0", revealedState.isIntermissionLeaderboard)
        assertTrue("Points should now be awarded for correct locked-in answer", revealedState.pointsAwardedLastQuestion > 0)

        // Verify leaderboard is sorted by highest score descending
        val scores = revealedState.competitors.map { it.score }
        assertEquals(scores.sortedDescending(), scores)

        // Clean up active quiz timers
        engagementVm.exitQuiz()
    }

    @Test
    fun `createQuiz preserves scriptureRef and startQuiz honors custom question timer`() = runTest {
        val engagementVm = EngagementViewModel(gamesRepository, weeklyReportRepository, testSession)
        val questions = listOf(
            com.example.ui.engagement.QuizQuestion(
                id = 1,
                question = "Where did Jesus turn water into wine?",
                options = listOf("Cana", "Nazareth", "Bethlehem", "Jericho"),
                correctAnswerIndex = 0,
                scriptureRef = "John 2:1-11"
            ),
            com.example.ui.engagement.QuizQuestion(
                id = 2,
                question = "Who climbed a sycamore tree to see Jesus?",
                options = listOf("Bartimaeus", "Zacchaeus", "Nicodemus", "Lazarus"),
                correctAnswerIndex = 1,
                scriptureRef = "Luke 19:4"
            )
        )

        engagementVm.createQuiz(
            title = "Gospel Highlights",
            description = "Scripture reference check",
            category = com.example.data.model.QuizCategory.BIBLE_TRIVIA,
            difficulty = "Easy",
            timeLimitSeconds = 40,
            questions = questions
        )
        testScheduler.advanceUntilIdle()

        val savedQuiz = gamesRepository.getAllQuizzes().first().firstOrNull { it.title == "Gospel Highlights" }
        assertTrue(savedQuiz != null)
        assertTrue(savedQuiz!!.questionsJson.contains("John 2:1-11"))
        assertTrue(savedQuiz.questionsJson.contains("Luke 19:4"))

        engagementVm.startQuiz(savedQuiz, isLiveGame = false)
        testScheduler.runCurrent()

        val active = engagementVm.uiState.value.activeQuiz
        assertEquals(20, active.maxQuestionTimeSeconds)
        assertEquals("John 2:1-11", active.questions[0].scriptureRef)
        assertEquals("Luke 19:4", active.questions[1].scriptureRef)
        engagementVm.exitQuiz()
    }
}
