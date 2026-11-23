package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChurchYouthDatabase
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.FinancialRecordEntity
import com.example.data.model.AnnouncementAudience
import com.example.data.model.AnnouncementPriority
import com.example.data.model.DepartmentType
import com.example.data.model.EventCategory
import com.example.data.model.FinancialCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import com.example.data.repository.AnnouncementRepositoryImpl
import com.example.data.repository.AuthResult
import com.example.data.repository.EventRepositoryImpl
import com.example.data.repository.FinanceRepositoryImpl
import com.example.data.repository.RegistrationResult
import com.example.data.repository.UserRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseAndRepositoryTest {

    private lateinit var database: ChurchYouthDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var eventRepository: EventRepositoryImpl
    private lateinit var announcementRepository: AnnouncementRepositoryImpl
    private lateinit var financeRepository: FinanceRepositoryImpl

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChurchYouthDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        userRepository = UserRepositoryImpl(
            userDao = database.userDao(),
            auditLogDao = database.auditLogDao()
        )
        eventRepository = EventRepositoryImpl(
            eventDao = database.eventDao(),
            auditLogDao = database.auditLogDao()
        )
        announcementRepository = AnnouncementRepositoryImpl(
            announcementDao = database.announcementDao(),
            auditLogDao = database.auditLogDao()
        )
        financeRepository = FinanceRepositoryImpl(
            financialRecordDao = database.financialRecordDao(),
            auditLogDao = database.auditLogDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun `user registration and login succeeds with valid credentials`() = runTest {
        val regResult = userRepository.registerUser(
            fullName = "Tolu Joseph",
            email = "tolu.joseph@example.com",
            phone = "+2348055667788",
            plainPassword = "SecurePassword#2026",
            role = UserRole.YOUTH_MEMBER,
            department = DepartmentType.MEDIA,
            fellowshipGroup = "Living Spring"
        )

        assertTrue("Registration must succeed", regResult is AuthResult.Success)
        val registeredUser = (regResult as AuthResult.Success).user
        assertEquals("Tolu Joseph", registeredUser.fullName)

        // Login with valid credentials
        val loginResult = userRepository.login("tolu.joseph@example.com", "SecurePassword#2026")
        assertTrue("Login must succeed with correct password", loginResult is AuthResult.Success)

        // Login with invalid password
        val failedLogin = userRepository.login("tolu.joseph@example.com", "WrongPassword")
        assertTrue("Login must fail with wrong password", failedLogin is AuthResult.Error)
    }

    @Test
    fun `event creation and participant registration updates count correctly`() = runTest {
        val event = EventEntity(
            id = "test-event-1",
            title = "Youth Leadership Retreat",
            description = "Annual leadership training camp",
            dateTimeMillis = System.currentTimeMillis() + 86400000L,
            venue = "Conference Hall A",
            isRegistrationEnabled = true,
            participantLimit = 50,
            registeredCount = 0,
            category = EventCategory.RETREAT,
            createdById = "admin-1"
        )
        eventRepository.createEvent(event, UserRole.ADMINISTRATOR)

        val retrievedEvent = eventRepository.getEventByIdSync("test-event-1")
        assertNotNull(retrievedEvent)
        assertEquals(0, retrievedEvent?.registeredCount)

        // Register member
        val regRes = eventRepository.registerForEvent(
            eventId = "test-event-1",
            userId = "user-100",
            userName = "Grace Okon",
            userEmail = "grace@example.com",
            userPhone = "+2348000000000",
            userRole = UserRole.YOUTH_MEMBER
        )
        assertTrue("Registration should succeed", regRes is RegistrationResult.Success)

        val updatedEvent = eventRepository.getEventByIdSync("test-event-1")
        assertEquals(1, updatedEvent?.registeredCount)
    }

    @Test
    fun `financial record entry persists and calculates total balance`() = runTest {
        val record = FinancialRecordEntity(
            id = "fin-rec-1",
            userId = "user-1",
            userName = "David Mark",
            category = FinancialCategory.TITHE,
            amount = 10000.0,
            status = PaymentStatus.COMPLETED,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            referenceCode = "TRF-9921",
            recordedById = "user-1"
        )
        financeRepository.recordFinancialEntry(record, UserRole.YOUTH_MEMBER)

        val total = financeRepository.getTotalCompletedAmount().first()
        assertEquals(10000.0, total ?: 0.0, 0.01)

        val allRecords = financeRepository.getAllFinancialRecords().first()
        assertEquals(1, allRecords.size)
        assertEquals("TRF-9921", allRecords[0].referenceCode)
    }

    @Test
    fun `announcements filtering returns audience specific broadcasts`() = runTest {
        val announcementGeneral = AnnouncementEntity(
            id = "ann-gen",
            title = "Sunday Service Reminder",
            content = "Youth service starts by 8am.",
            priority = AnnouncementPriority.NORMAL,
            targetAudience = AnnouncementAudience.ALL,
            authorId = "lead-1",
            authorName = "Youth Leader",
            authorRole = "President"
        )
        val announcementExco = AnnouncementEntity(
            id = "ann-exco",
            title = "Exco Emergency Briefing",
            content = "Meeting in board room.",
            priority = AnnouncementPriority.URGENT,
            targetAudience = AnnouncementAudience.EXECUTIVES_ONLY,
            authorId = "lead-1",
            authorName = "Youth Leader",
            authorRole = "President"
        )
        announcementRepository.publishAnnouncement(announcementGeneral, UserRole.YOUTH_EXECUTIVE)
        announcementRepository.publishAnnouncement(announcementExco, UserRole.YOUTH_EXECUTIVE)

        // Youth member audience feed
        val memberFeed = announcementRepository.getAnnouncementsForAudience(UserRole.YOUTH_MEMBER).first()
        assertEquals(1, memberFeed.size)
        assertEquals("Sunday Service Reminder", memberFeed[0].title)

        // Executive audience feed
        val excoFeed = announcementRepository.getAnnouncementsForAudience(UserRole.YOUTH_EXECUTIVE).first()
        assertEquals(2, excoFeed.size)
    }

    @Test
    fun `audit log tracks actions chronologically`() = runTest {
        val logsBefore = database.auditLogDao().getRecentLogs(10).first()
        val initialCount = logsBefore.size

        userRepository.registerUser(
            fullName = "Audit Test User",
            email = "audit@test.com",
            phone = "+2348099112233",
            plainPassword = "Pass#12345678",
            role = UserRole.YOUTH_MEMBER
        )

        val logsAfter = database.auditLogDao().getRecentLogs(10).first()
        assertTrue("Audit log count must increase", logsAfter.size > initialCount)
        assertEquals("USER_REGISTRATION", logsAfter.first().actionType)
    }

    @Test
    fun `demo accounts for admin executive and member log in successfully`() = runTest {
        // 1. Admin login
        val adminLogin = userRepository.login("admin@youthalive.org", "AdminPass#2026")
        assertTrue("Admin login must succeed", adminLogin is AuthResult.Success)
        assertEquals(UserRole.ADMINISTRATOR, (adminLogin as AuthResult.Success).user.role)

        // 2. Executive login
        val excoLogin = userRepository.login("president@youthalive.org", "ExcoPass#2026")
        assertTrue("Executive login must succeed", excoLogin is AuthResult.Success)
        assertEquals(UserRole.YOUTH_EXECUTIVE, (excoLogin as AuthResult.Success).user.role)

        // 3. Member login
        val memberLogin = userRepository.login("sarah.eze@youthalive.org", "MemberPass#2026")
        assertTrue("Member login must succeed", memberLogin is AuthResult.Success)
        assertEquals(UserRole.YOUTH_MEMBER, (memberLogin as AuthResult.Success).user.role)

        // 4. Guest login
        val guestLogin = userRepository.login("guest@youthalive.org", "GuestPass#2026")
        assertTrue("Guest login must succeed", guestLogin is AuthResult.Success)
        assertEquals(UserRole.GUEST, (guestLogin as AuthResult.Success).user.role)

        // 5. Role alias / shortcuts
        val aliasExco = userRepository.login("president", "ExcoPass#2026")
        assertTrue("Executive alias login must succeed", aliasExco is AuthResult.Success)

        val aliasMember = userRepository.login("member", "MemberPass#2026")
        assertTrue("Member alias login must succeed", aliasMember is AuthResult.Success)

        val aliasGuest = userRepository.login("guest", "GuestPass#2026")
        assertTrue("Guest alias login must succeed", aliasGuest is AuthResult.Success)
    }

    @Test
    fun `executive registration requires admin approval while member registers freely with optional photo`() = runTest {
        // 1. Member registers freely with optional profile photo
        val memberReg = userRepository.registerUser(
            fullName = "Chinedu Okafor",
            email = "chinedu@youthalive.org",
            phone = "+2348066778899",
            plainPassword = "MemberPass#2026",
            role = UserRole.YOUTH_MEMBER,
            department = DepartmentType.CHOIR,
            profilePhotoUri = "file:///data/user/0/com.example/files/profile_photos/avatar_1.jpg"
        )
        assertTrue("Member registration must succeed immediately", memberReg is AuthResult.Success)
        val memberUser = (memberReg as AuthResult.Success).user
        assertEquals("file:///data/user/0/com.example/files/profile_photos/avatar_1.jpg", memberUser.profilePhotoUri)

        // 2. Executive registration enters PENDING_APPROVAL
        val excoReg = userRepository.registerUser(
            fullName = "Sister Blessing Adeyemi",
            email = "blessing.exco@youthalive.org",
            phone = "+2348077889900",
            plainPassword = "ExcoSecret#2026",
            role = UserRole.YOUTH_EXECUTIVE,
            department = DepartmentType.PRAYER
        )
        assertTrue("Executive registration must require Admin approval", excoReg is AuthResult.PendingApproval)

        // 3. Executive cannot log in before Admin approval
        val blockedLogin = userRepository.login("blessing.exco@youthalive.org", "ExcoSecret#2026")
        assertTrue("Unapproved executive login must be blocked", blockedLogin is AuthResult.PendingApproval)

        // 4. Non-admin cannot approve the executive
        val pendingExco = database.userDao().getUserByEmail("blessing.exco@youthalive.org")!!
        userRepository.updateUserStatus(pendingExco.id, com.example.data.model.AccountStatus.APPROVED, memberUser.id)
        val stillBlocked = userRepository.login("blessing.exco@youthalive.org", "ExcoSecret#2026")
        assertTrue("Member cannot approve an executive", stillBlocked is AuthResult.PendingApproval)

        // 5. Admin approves the executive -> login now succeeds
        val adminRes = userRepository.login("admin@youthalive.org", "AdminPass#2026") as AuthResult.Success
        userRepository.updateUserStatus(pendingExco.id, com.example.data.model.AccountStatus.APPROVED, adminRes.user.id)

        val approvedLogin = userRepository.login("blessing.exco@youthalive.org", "ExcoSecret#2026")
        assertTrue("Approved executive login must succeed", approvedLogin is AuthResult.Success)
        assertEquals(UserRole.YOUTH_EXECUTIVE, (approvedLogin as AuthResult.Success).user.role)
    }

    @Test
    fun `community forum supports attachments per-user likes pinning and executive inquiries`() = runTest {
        val communityRepository = com.example.data.repository.CommunityRepositoryImpl(
            communityDao = database.communityDao(),
            auditLogDao = database.auditLogDao(),
            userDao = database.userDao()
        )

        // 1. Create two posts (one with attachment)
        communityRepository.createPost(
            authorId = "user-member-01",
            authorName = "Sarah Chioma Eze",
            authorRole = "Youth Member",
            category = "Testimonies",
            title = "First Post",
            content = "God came through in my final exams!",
            attachmentPhotoUri = "file:///data/user/0/com.example/files/profile_photos/testimony.jpg"
        )
        communityRepository.createPost(
            authorId = "user-exco-01",
            authorName = "Emmanuel Okon",
            authorRole = "Youth Executive",
            category = "Spiritual Growth",
            title = "Gideonites Vigil Charge",
            content = "Prepare your hearts for Friday prayer vigil."
        )

        val posts = communityRepository.getActivePosts().first()
        assertEquals(2, posts.size)
        val testimonyPost = posts.first { it.title == "First Post" }
        val vigilPost = posts.first { it.title == "Gideonites Vigil Charge" }
        assertEquals("file:///data/user/0/com.example/files/profile_photos/testimony.jpg", testimonyPost.attachmentPhotoUri)

        // 2. Per-user like toggle prevents infinite spam and toggles cleanly
        val firstToggle = communityRepository.toggleLikePost(testimonyPost.id, "user-member-01").getOrThrow()
        assertTrue("First tap must like the post", firstToggle)
        val afterLike = communityRepository.getActivePosts().first().first { it.id == testimonyPost.id }
        assertEquals(1, afterLike.likesCount)
        assertTrue(afterLike.isLikedBy("user-member-01"))

        val secondToggle = communityRepository.toggleLikePost(testimonyPost.id, "user-member-01").getOrThrow()
        assertEquals(false, secondToggle)
        val afterUnlike = communityRepository.getActivePosts().first().first { it.id == testimonyPost.id }
        assertEquals(0, afterUnlike.likesCount)

        // 3. Pinning floats post to the top
        communityRepository.pinPost(testimonyPost.id, true, "user-exco-01")
        val orderedAfterPin = communityRepository.getActivePosts().first()
        assertEquals(testimonyPost.id, orderedAfterPin.first().id)
        assertTrue(orderedAfterPin.first().isPinned)

        // 4. Flagging & unflagging
        communityRepository.flagPost(vigilPost.id, true)
        assertEquals(1, communityRepository.getFlaggedPosts().first().size)
        communityRepository.flagPost(vigilPost.id, false)
        assertEquals(0, communityRepository.getFlaggedPosts().first().size)

        // 5. Executive Contact Inquiry & Pastoral Response
        val inquiry = communityRepository.submitExecutiveInquiry(
            senderId = "user-member-01",
            senderName = "Sarah Chioma Eze",
            senderRole = "Youth Member",
            isAnonymous = true,
            topic = "Confidential Prayer Request",
            message = "Please pray for my family's health this week."
        ).getOrThrow()
        assertEquals("Anonymous Youth Member", inquiry.senderName)
        assertEquals("PENDING", inquiry.status)

        communityRepository.respondToExecutiveInquiry(
            inquiryId = inquiry.id,
            status = "RESOLVED",
            responseMessage = "We are standing in prayer with your family, Sister!",
            responderId = "user-exco-01",
            responderName = "Emmanuel Okon",
            responderRole = "Youth Executive"
        )

        val memberInquiries = communityRepository.getInquiriesForUser("user-member-01").first()
        assertEquals(1, memberInquiries.size)
        assertEquals("RESOLVED", memberInquiries.first().status)
        assertEquals("We are standing in prayer with your family, Sister!", memberInquiries.first().responseMessage)
    }

    @Test
    fun `programme registration schedules and dispatches 5-day and 1-day email reminders`() = runTest {
        val now = System.currentTimeMillis()
        val tenDaysLater = now + (10L * 24 * 60 * 60 * 1000L)
        val event = EventEntity(
            id = "event-reminders-test",
            title = "Gideonites Youth Summit",
            description = "Annual youth conference",
            dateTimeMillis = tenDaysLater,
            venue = "Main Youth Auditorium",
            isRegistrationEnabled = true,
            participantLimit = 100,
            registeredCount = 0,
            category = EventCategory.RETREAT,
            createdById = "admin-1"
        )
        eventRepository.createEvent(event, UserRole.ADMINISTRATOR)

        val regRes = eventRepository.registerForEvent(
            eventId = "event-reminders-test",
            userId = "user-member-01",
            userName = "Sarah Chioma Eze",
            userEmail = "sarah.eze@youthalive.org",
            userPhone = "+2348033445566",
            userRole = UserRole.YOUTH_MEMBER
        )
        assertTrue(regRes is RegistrationResult.Success)

        val registrations = eventRepository.getRegistrationsForUser("user-member-01").first()
        assertEquals(1, registrations.size)
        val reg = registrations.first()
        assertEquals("sarah.eze@youthalive.org", reg.userEmail)
        assertEquals(tenDaysLater - EventRepositoryImpl.FIVE_DAYS_MILLIS, reg.reminder5DaysScheduledAt)
        assertEquals(tenDaysLater - EventRepositoryImpl.ONE_DAY_MILLIS, reg.reminder1DayScheduledAt)
        assertEquals(false, reg.reminder5DaysSent)
        assertEquals(false, reg.reminder1DaySent)

        // 1. At 6 days before: no reminder due yet
        val dueAt6Days = eventRepository.checkAndMarkDueEmailReminders(
            userId = "user-member-01",
            nowMillis = tenDaysLater - (6L * 24 * 60 * 60 * 1000L)
        )
        assertEquals(0, dueAt6Days.size)

        // 2. At 5 days before: 5-day email reminder fires once
        val dueAt5Days = eventRepository.checkAndMarkDueEmailReminders(
            userId = "user-member-01",
            nowMillis = tenDaysLater - (5L * 24 * 60 * 60 * 1000L) + 1000L
        )
        assertEquals(1, dueAt5Days.size)
        assertEquals(5, dueAt5Days.first().third)

        // 3. At 1 day before: 1-day email reminder fires once
        val dueAt1Day = eventRepository.checkAndMarkDueEmailReminders(
            userId = "user-member-01",
            nowMillis = tenDaysLater - (1L * 24 * 60 * 60 * 1000L) + 1000L
        )
        assertEquals(1, dueAt1Day.size)
        assertEquals(1, dueAt1Day.first().third)

        val updatedReg = eventRepository.getRegistrationsForUser("user-member-01").first().first()
        assertTrue(updatedReg.reminder5DaysSent)
        assertTrue(updatedReg.reminder1DaySent)
    }

    @Test
    fun `creating a post, announcement, or programme dispatches device notifications`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notifService = com.example.core.notification.FellowshipNotificationService(
            context = context,
            notificationDao = database.notificationDao()
        )
        val eventRepoWithNotif = EventRepositoryImpl(
            eventDao = database.eventDao(),
            auditLogDao = database.auditLogDao(),
            notificationService = notifService
        )
        val annRepoWithNotif = AnnouncementRepositoryImpl(
            announcementDao = database.announcementDao(),
            auditLogDao = database.auditLogDao(),
            notificationService = notifService
        )
        val commRepoWithNotif = com.example.data.repository.CommunityRepositoryImpl(
            communityDao = database.communityDao(),
            auditLogDao = database.auditLogDao(),
            notificationService = notifService
        )

        // 1. Schedule a Programme
        val event = EventEntity(
            id = "prog-101",
            title = "Youth Alive Fire Night",
            description = "All-night prayer and worship",
            dateTimeMillis = System.currentTimeMillis() + 86_400_000L,
            venue = "Main Sanctuary",
            speaker = "Pastor David",
            category = EventCategory.PRAYER_MEETING,
            createdById = "admin-1"
        )
        eventRepoWithNotif.createEvent(event, UserRole.ADMINISTRATOR)

        // 2. Publish an Announcement
        val announcement = AnnouncementEntity(
            id = "ann-101",
            title = "Choir Rehearsal Time Change",
            content = "Choir rehearsal starts at 4:00 PM this Saturday.",
            priority = AnnouncementPriority.URGENT,
            targetAudience = AnnouncementAudience.ALL,
            authorId = "admin-1",
            authorName = "Pastor David",
            authorRole = "Administrator"
        )
        annRepoWithNotif.publishAnnouncement(announcement, UserRole.ADMINISTRATOR)

        // 3. Create a Community Forum Post
        val postRes = commRepoWithNotif.createPost(
            authorId = "member-1",
            authorName = "Grace Okafor",
            authorRole = "Youth Member",
            category = "Testimony",
            title = "Miracle Job Offer!",
            content = "God blessed me with a new software engineering role!"
        )
        assertTrue(postRes.isSuccess)

        // Verify all 3 notifications are persisted in NotificationDao for all users
        val eventNotif = database.notificationDao().getNotificationByIdSync("notif_event_prog-101")
        val annNotif = database.notificationDao().getNotificationByIdSync("notif_ann_ann-101")
        val unreadList = database.notificationDao().getUnreadNotificationsSync()
        val postNotif = unreadList.firstOrNull { it.type == com.example.data.local.entity.NotificationType.COMMUNITY_POST }

        assertNotNull("Programme notification must be created", eventNotif)
        assertNotNull("Announcement notification must be created", annNotif)
        assertNotNull("Community Post notification must be created", postNotif)

        assertEquals(com.example.data.local.entity.NotificationType.EVENT, eventNotif?.type)
        assertEquals(com.example.data.local.entity.NotificationType.ANNOUNCEMENT, annNotif?.type)
        assertEquals(com.example.data.local.entity.NotificationType.COMMUNITY_POST, postNotif?.type)

        // Deliver unread notifications to a second user's device session
        notifService.deliverUnreadNotificationsToDeviceForUser("member-2")
        assertEquals(3, unreadList.size)
    }

    @Test
    fun `forum posts older than 7 days are automatically deleted alongside all replies and likes while programmes and games persist across all users`() = runTest {
        val commRepo = com.example.data.repository.CommunityRepositoryImpl(
            communityDao = database.communityDao(),
            auditLogDao = database.auditLogDao()
        )
        val multiplayerRepo = com.example.data.repository.MultiplayerGameRepositoryImpl(
            multiplayerGameDao = database.multiplayerGameDao(),
            auditLogDao = database.auditLogDao()
        )

        val now = System.currentTimeMillis()
        val eightDaysAgo = now - (8L * 24 * 60 * 60 * 1000L)
        val twoDaysAgo = now - (2L * 24 * 60 * 60 * 1000L)

        // 1. Insert an expired post (8 days old) and an active post (2 days old)
        val expiredPost = com.example.data.local.entity.CommunityPostEntity(
            id = "post_expired_8d",
            authorId = "user-member-01",
            authorName = "Sarah Chioma Eze",
            authorRole = "Youth Member",
            departmentOrCategory = "Prayer Requests",
            title = "Last Week Prayer Focus",
            content = "Praying for last week's outreach.",
            createdAt = eightDaysAgo
        )
        val activePost = com.example.data.local.entity.CommunityPostEntity(
            id = "post_active_2d",
            authorId = "user-exco-01",
            authorName = "Emmanuel Okon",
            authorRole = "Youth Executive",
            departmentOrCategory = "Faith & Career",
            title = "Tech & Ministry Workshop",
            content = "Join us as we discuss faith in the workplace.",
            createdAt = twoDaysAgo
        )
        database.communityDao().insertPost(expiredPost)
        database.communityDao().insertPost(activePost)

        // 2. Add replies (comments) and likes to BOTH posts
        commRepo.addComment(
            postId = "post_expired_8d",
            authorId = "user-admin-01",
            authorName = "Elder David",
            authorRole = "Administrator",
            content = "Amen, praying along!"
        )
        commRepo.toggleLikePost("post_expired_8d", "user-admin-01")
        commRepo.toggleLikePost("post_expired_8d", "user-exco-01")

        commRepo.addComment(
            postId = "post_active_2d",
            authorId = "user-member-01",
            authorName = "Sarah Chioma Eze",
            authorRole = "Youth Member",
            content = "I will be attending!"
        )
        commRepo.toggleLikePost("post_active_2d", "user-member-01")

        // Verify replies and likes exist before purge
        assertEquals(1, database.communityDao().getCommentsForPostSync("post_expired_8d").size)
        assertEquals(2, database.communityDao().getLikesForPostSync("post_expired_8d").size)
        assertEquals(1, database.communityDao().getCommentsForPostSync("post_active_2d").size)
        assertEquals(1, database.communityDao().getLikesForPostSync("post_active_2d").size)

        // 3. Trigger automatic 7-day purge
        val purgedCount = commRepo.purgeExpiredPosts(nowMillis = now)
        assertEquals(1, purgedCount)

        // Verify expired post AND all its replies and likes are completely deleted from the database
        assertEquals(null, database.communityDao().getPostByIdSnapshot("post_expired_8d"))
        assertEquals(0, database.communityDao().getCommentsForPostSync("post_expired_8d").size)
        assertEquals(0, database.communityDao().getLikesForPostSync("post_expired_8d").size)

        // Verify active post (< 7 days old) and its replies and likes remain intact
        assertNotNull(database.communityDao().getPostByIdSnapshot("post_active_2d"))
        assertEquals(1, database.communityDao().getCommentsForPostSync("post_active_2d").size)
        assertEquals(1, database.communityDao().getLikesForPostSync("post_active_2d").size)

        // 4. Verify cross-user sync for Programmes and Multiplayer Games into the local database
        eventRepository.syncCloudProgrammesToLocalDb(
            listOf(
                com.example.data.remote.CloudYouthProgramme(
                    id = "prog_cloud_summit_01",
                    createdById = "admin-1",
                    titleEn = "Gideonites National Youth Camp",
                    descriptionEn = "3 days of power and spiritual renewal",
                    category = "RETREAT",
                    venue = "Camp Ground Auditorium",
                    startDateMillis = now + 86_400_000L,
                    endDateMillis = now + 172_800_000L,
                    status = "UPCOMING",
                    registeredCount = 12L
                )
            )
        )
        val syncedEvent = eventRepository.getEventByIdSync("prog_cloud_summit_01")
        assertNotNull("Cloud programme must be saved into local database for all users", syncedEvent)
        assertEquals("Gideonites National Youth Camp", syncedEvent?.title)

        multiplayerRepo.syncCloudQuizSessionsToLocalDb(
            listOf(
                com.example.data.remote.CloudQuizSession(
                    id = "quiz_cloud_arena_01",
                    hostId = "admin-1",
                    hostName = "Elder David",
                    roomPin = "GID300",
                    titleEn = "Judges & Gideon Scripture Showdown",
                    gameType = "BIBLE_QUIZ",
                    status = "SCHEDULED",
                    totalQuestions = 5L
                )
            )
        )
        val syncedGame = multiplayerRepo.getSessionByIdSync("quiz_cloud_arena_01")
        assertNotNull("Cloud game session must be saved into local database for all users", syncedGame)
        assertEquals("Judges & Gideon Scripture Showdown", syncedGame?.title)
    }
}
