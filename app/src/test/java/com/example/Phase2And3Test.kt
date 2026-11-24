package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChurchYouthDatabase
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.CommunityCommentEntity
import com.example.data.local.entity.CommunityPostEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.SpiritualResourceEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.model.DepartmentType
import com.example.data.model.Gender
import com.example.data.model.UserRole
import com.example.data.repository.AttendanceRepositoryImpl
import com.example.data.repository.CommunityRepositoryImpl
import com.example.data.repository.DepartmentRepositoryImpl
import com.example.data.repository.SpiritualResourceRepositoryImpl
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase2And3Test {

    private lateinit var database: ChurchYouthDatabase
    private lateinit var departmentRepository: DepartmentRepositoryImpl
    private lateinit var attendanceRepository: AttendanceRepositoryImpl
    private lateinit var communityRepository: CommunityRepositoryImpl
    private lateinit var spiritualRepository: SpiritualResourceRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChurchYouthDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        departmentRepository = DepartmentRepositoryImpl(
            departmentDao = database.departmentDao(),
            userDao = database.userDao(),
            auditLogDao = database.auditLogDao()
        )
        attendanceRepository = AttendanceRepositoryImpl(
            attendanceDao = database.attendanceDao(),
            auditLogDao = database.auditLogDao()
        )
        communityRepository = CommunityRepositoryImpl(
            communityDao = database.communityDao(),
            auditLogDao = database.auditLogDao()
        )
        spiritualRepository = SpiritualResourceRepositoryImpl(
            spiritualResourceDao = database.spiritualResourceDao(),
            appContext = context
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testThemeDefaultsToLightModeAndIsSwitchable() {
        ThemeManager.setDark(false)
        assertFalse("Theme must default to Light Mode", ThemeManager.isDarkMode.value)

        ThemeManager.toggle()
        assertTrue("Theme must toggle to Dark Mode", ThemeManager.isDarkMode.value)

        ThemeManager.toggle()
        assertFalse("Theme must toggle back to Light Mode", ThemeManager.isDarkMode.value)
    }

    @Test
    fun testDepartmentAndRosterManagement() = runBlocking {
        val dept = DepartmentEntity(
            id = "dept-media",
            type = DepartmentType.MEDIA,
            name = "Media & Technical Unit",
            description = "Handles audio, visuals, and live streaming.",
            leaderId = "leader-01",
            leaderName = "Brother Paul",
            meetingSchedule = "Saturdays 4:00 PM",
            meetingVenue = "Main Sanctuary Media Booth",
            memberCount = 1
        )
        database.departmentDao().insertDepartments(listOf(dept))

        val retrievedDept = departmentRepository.getDepartmentByType(DepartmentType.MEDIA).first()
        assertNotNull(retrievedDept)
        assertEquals("Media & Technical Unit", retrievedDept?.name)

        // Insert a user and assign to Media
        val user = UserEntity(
            id = "user-test-01",
            fullName = "Joshua Adeniyi",
            email = "joshua@churchyouth.org",
            phone = "+2348012345678",
            passwordHash = "hash123",
            salt = "salt123",
            role = UserRole.YOUTH_MEMBER,
            status = AccountStatus.APPROVED,
            gender = Gender.MALE,
            department = DepartmentType.GENERAL
        )
        database.userDao().insertUser(user)

        // Assign to Media department
        val assignResult = departmentRepository.assignMemberToDepartment(
            userId = user.id,
            department = DepartmentType.MEDIA,
            adminUserId = "admin-01"
        )
        assertTrue(assignResult.isSuccess)

        val mediaMembers = departmentRepository.getDepartmentMembers(DepartmentType.MEDIA).first()
        assertEquals(1, mediaMembers.size)
        assertEquals("Joshua Adeniyi", mediaMembers.first().fullName)
    }

    @Test
    fun testMultiMethodAttendanceTracking() = runBlocking {
        val record = AttendanceRecordEntity(
            id = UUID.randomUUID().toString(),
            meetingTitle = "Midweek Youth Bible Study",
            meetingType = "BIBLE_STUDY",
            departmentOrGroup = "General",
            dateMillis = System.currentTimeMillis(),
            totalAttendees = 45,
            maleCount = 20,
            femaleCount = 22,
            firstTimersCount = 3,
            checkInMethod = "MANUAL",
            recordedById = "exco-01",
            recordedByName = "Youth President",
            notes = "Inspiring study on the Holy Spirit"
        )
        val insertResult = attendanceRepository.recordAttendance(record)
        assertTrue(insertResult.isSuccess)

        val allRecords = attendanceRepository.getAllAttendance().first()
        assertEquals(1, allRecords.size)
        assertEquals(45, allRecords.first().totalAttendees)

        // Test check-in code generation and verification
        val code = attendanceRepository.generateCheckInCode()
        assertTrue(code.startsWith("CYP-"))

        val checkInResult = attendanceRepository.verifyAndCheckIn(
            verificationCode = code,
            userId = "member-01",
            userName = "Sister Mary",
            serviceTitle = "Sunday Youth Service"
        )
        assertTrue(checkInResult.isSuccess)
        assertEquals("QR_SELF_CODE", checkInResult.getOrNull()?.checkInMethod)
    }

    @Test
    fun testCommunityForumAndModeration() = runBlocking {
        val createResult = communityRepository.createPost(
            authorId = "user-01",
            authorName = "Timothy Bamidele",
            authorRole = "Youth Member",
            category = "Faith & Career",
            title = "Overcoming Interview Anxiety with Scripture",
            content = "Philippians 4:6-7 has been my anchor this week during job assessments!"
        )
        assertTrue(createResult.isSuccess)

        val activePosts = communityRepository.getActivePosts().first()
        assertEquals(1, activePosts.size)
        val post = activePosts.first()
        assertEquals("Overcoming Interview Anxiety with Scripture", post.title)

        // Like post
        communityRepository.likePost(post.id)
        val updatedPost = communityRepository.getPostById(post.id).first()
        assertEquals(1, updatedPost?.likesCount)

        // Add Comment
        val commentResult = communityRepository.addComment(
            postId = post.id,
            authorId = "user-02",
            authorName = "Sister Grace",
            authorRole = "Youth Executive",
            content = "Amen! Standing with you in prayers."
        )
        assertTrue(commentResult.isSuccess)

        val comments = communityRepository.getCommentsForPost(post.id).first()
        assertEquals(1, comments.size)
        assertEquals("Sister Grace", comments.first().authorName)

        // Flag and Hide post (Moderation)
        communityRepository.flagPost(post.id, true)
        communityRepository.hidePost(post.id, true, "admin-01")

        val remainingActive = communityRepository.getActivePosts().first()
        assertEquals(0, remainingActive.size)
    }

    @Test
    fun testSpiritualResourceLibraryAndBookmarks() = runBlocking {
        val resource = SpiritualResourceEntity(
            id = "res-001",
            title = "Walking in Divine Purpose",
            category = "SERMON",
            speakerOrAuthor = "Pastor David",
            scriptureReference = "Jeremiah 29:11",
            summary = "Understanding God's sovereign blueprint for your youth years.",
            contentBody = "For I know the thoughts that I think toward you, says the Lord...",
            audioDurationMinutes = 35,
            isBookmarked = false
        )
        val addResult = spiritualRepository.addResource(resource)
        assertTrue(addResult.isSuccess)

        val allResources = spiritualRepository.getAllResources().first()
        assertEquals(1, allResources.size)

        // Toggle bookmark
        spiritualRepository.toggleBookmark(resource.id, false)
        val bookmarked = spiritualRepository.getBookmarkedResources().first()
        assertEquals(1, bookmarked.size)
        assertTrue(bookmarked.first().isBookmarked)
    }

    @Test
    fun testAudioSermonReplacesPreviousAudioSermon() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioDir = java.io.File(context.filesDir, "sermon_audio").apply { mkdirs() }
        val firstFile = java.io.File(audioDir, "sa_old123.mp3").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }

        val textDevotional = SpiritualResourceEntity(
            id = "devo-1",
            title = "Morning Devotional",
            category = "DEVOTIONAL",
            speakerOrAuthor = "Youth Exco",
            scriptureReference = "Psalm 23:1",
            summary = "The Lord is my shepherd",
            contentBody = "Daily reflection",
            audioDurationMinutes = 0,
            isBookmarked = false
        )
        spiritualRepository.addResource(textDevotional)

        val firstAudioSermon = SpiritualResourceEntity(
            id = "sermon-audio-1",
            title = "Old Audio Sermon",
            category = "SERMON",
            speakerOrAuthor = "Pastor John",
            scriptureReference = "Judges 6:12",
            summary = "First audio message",
            contentBody = "First message outline",
            audioDurationMinutes = 12,
            isBookmarked = false,
            audioUri = firstFile.absolutePath,
            audioCloudId = "sa_old123",
            audioFileName = "old_sermon.mp3",
            audioMimeType = "audio/mpeg",
            audioSizeBytes = 4L
        )
        spiritualRepository.addResource(firstAudioSermon)
        assertTrue(firstFile.exists())

        val secondFile = java.io.File(audioDir, "sa_new456.mp3").apply { writeBytes(byteArrayOf(5, 6, 7, 8)) }
        val secondAudioSermon = SpiritualResourceEntity(
            id = "sermon-audio-2",
            title = "New Audio Sermon",
            category = "SERMON",
            speakerOrAuthor = "Pastor John",
            scriptureReference = "Daniel 11:32",
            summary = "Replacement audio message",
            contentBody = "Replacement message outline",
            audioDurationMinutes = 25,
            isBookmarked = false,
            audioUri = secondFile.absolutePath,
            audioCloudId = "sa_new456",
            audioFileName = "new_sermon.mp3",
            audioMimeType = "audio/mpeg",
            audioSizeBytes = 4L
        )
        val replaceResult = spiritualRepository.addResource(secondAudioSermon)
        assertTrue(replaceResult.isSuccess)

        val allResources = spiritualRepository.getAllResources().first()
        assertFalse("Old audio sermon should be deleted from Room", allResources.any { it.id == "sermon-audio-1" })
        assertTrue("New audio sermon should remain in Room", allResources.any { it.id == "sermon-audio-2" })
        assertTrue("Text devotional should remain untouched", allResources.any { it.id == "devo-1" })
        assertFalse("Old cached audio file should be deleted", firstFile.exists())
        assertTrue("New cached audio file should remain", secondFile.exists())
    }

    @Test
    fun testExpiredEventsArePurgedAndFilteredAutomatically() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val eventRepo = com.example.data.repository.EventRepositoryImpl(
            eventDao = database.eventDao(),
            auditLogDao = database.auditLogDao(),
            appContext = context
        )
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (2L * 24 * 60 * 60 * 1000L)
        val twoDaysFuture = now + (2L * 24 * 60 * 60 * 1000L)

        val pastEvent = com.example.data.local.entity.EventEntity(
            id = "ev-past-01",
            title = "Yesterday Prayer Vigil",
            description = "Already happened",
            dateTimeMillis = twoDaysAgo,
            venue = "Upper Room",
            createdById = "admin-1"
        )
        val todayEvent = com.example.data.local.entity.EventEntity(
            id = "ev-today-01",
            title = "Today Worship Night",
            description = "Happening today",
            dateTimeMillis = now,
            venue = "Main Sanctuary",
            createdById = "admin-1"
        )
        val futureEvent = com.example.data.local.entity.EventEntity(
            id = "ev-future-01",
            title = "Upcoming Youth Summit",
            description = "In two days",
            dateTimeMillis = twoDaysFuture,
            venue = "Youth Hall",
            createdById = "admin-1"
        )

        database.eventDao().insertEvent(pastEvent)
        database.eventDao().insertEvent(todayEvent)
        database.eventDao().insertEvent(futureEvent)
        database.eventDao().upsertRegistration(
            com.example.data.local.entity.EventRegistrationEntity(
                id = "reg-past-1",
                eventId = pastEvent.id,
                userId = "user-1",
                userName = "Member One",
                userEmail = "member1@youthalive.org",
                userPhone = "+2348000000000"
            )
        )

        // Flow filter immediately hides passed-date event
        val visibleBeforePurge = eventRepo.getAllEvents().first()
        assertFalse(visibleBeforePurge.any { it.id == "ev-past-01" })
        assertTrue(visibleBeforePurge.any { it.id == "ev-today-01" })
        assertTrue(visibleBeforePurge.any { it.id == "ev-future-01" })

        // Explicit purge removes expired event and its registrations from Room
        val deletedCount = eventRepo.purgeExpiredEvents(now)
        assertEquals(1, deletedCount)
        assertEquals(null, database.eventDao().getEventByIdSync("ev-past-01"))
        assertTrue(database.eventDao().getRegistrationsForEventSync("ev-past-01").isEmpty())
        assertNotNull(database.eventDao().getEventByIdSync("ev-today-01"))
        assertNotNull(database.eventDao().getEventByIdSync("ev-future-01"))

        // Cloud sync skips expired cloud programmes and does not re-insert them
        eventRepo.syncCloudProgrammesToLocalDb(
            listOf(
                com.example.data.remote.CloudYouthProgramme(
                    id = "prog_cloud_past",
                    titleEn = "Expired Cloud Event",
                    descriptionEn = "Old",
                    category = "FELLOWSHIP",
                    venue = "Chapel",
                    startDateMillis = twoDaysAgo,
                    endDateMillis = twoDaysAgo + 3600_000L
                ),
                com.example.data.remote.CloudYouthProgramme(
                    id = "prog_cloud_future",
                    titleEn = "Valid Cloud Summit",
                    descriptionEn = "Upcoming",
                    category = "FELLOWSHIP",
                    venue = "Sanctuary",
                    startDateMillis = twoDaysFuture,
                    endDateMillis = twoDaysFuture + 3600_000L
                )
            )
        )
        assertEquals(null, database.eventDao().getEventByIdSync("cloud_past"))
        assertNotNull(database.eventDao().getEventByIdSync("cloud_future"))
    }

    @Test
    fun testNotificationServicePersistsDeliveryAndFiltersDuplicates() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("fellowship_notif_delivery_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()

        val notifDao = database.notificationDao()
        val service1 = com.example.core.notification.FellowshipNotificationService(
            context = context,
            notificationDao = notifDao
        )

        // Post local notification; verify persisted origin prevents remote echo even across new service instances
        service1.postNotification(
            id = "notif_local_123",
            title = "Local Programme",
            message = "Created on this device",
            type = com.example.data.local.entity.NotificationType.EVENT,
            authorName = "Admin",
            authorRole = "Admin",
            publishToCloud = false
        )
        kotlinx.coroutines.delay(150L)

        // Simulate app restart with fresh service instance
        val service2 = com.example.core.notification.FellowshipNotificationService(
            context = context,
            notificationDao = notifDao
        )

        // Remote notification from another device addressed to another user should be ignored
        service2.syncRemoteNotificationIfNew(
            id = "notif_private_other",
            title = "Private Reply",
            message = "For Bob",
            type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
            authorName = "Exco",
            authorRole = "Exco",
            senderDeviceId = "other_device_xyz",
            recipientUserId = "user-bob",
            activeUserId = "user-alice"
        )
        assertEquals(null, notifDao.getNotificationByIdSync("notif_private_other"))

        // Remote notification from another device for Alice is stored and live banner emitted
        service2.syncRemoteNotificationIfNew(
            id = "notif_remote_alice",
            title = "New Fellowship Announcement",
            message = "Friday Vigil at 10 PM",
            type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
            authorName = "Youth President",
            authorRole = "Youth Executive",
            senderDeviceId = "other_device_xyz",
            recipientUserId = null,
            activeUserId = "user-alice"
        )
        assertNotNull(notifDao.getNotificationByIdSync("notif_remote_alice"))
    }

    @Test
    fun testAdminOrganizationMigratesFromLegacyToLivingFaithChurch() = runBlocking {
        val legacyAdmin = UserEntity(
            id = com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_ID,
            fullName = com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_NAME,
            email = com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_EMAIL,
            phone = "+2348011223344",
            passwordHash = "hash",
            salt = "salt",
            role = UserRole.ADMINISTRATOR,
            status = AccountStatus.APPROVED,
            gender = Gender.PREFER_NOT_TO_SAY,
            address = "ECWA Youth Alive Fellowship Secretariat"
        )
        database.userDao().insertUser(legacyAdmin)

        com.example.data.local.DatabasePrepopulator.populateInitialData(database)

        val updatedAdmin = database.userDao().getUserByIdSync(com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_ID)
        assertNotNull(updatedAdmin)
        assertEquals("Living Faith Church Youth Alive Fellowship Secretariat", updatedAdmin!!.address)
        assertFalse(updatedAdmin.address.contains("ECWA", ignoreCase = true))
    }

    @Test
    fun testAudioSermonFileCandidateValidationSupportsAudioAndRejectsDocs() {
        assertTrue(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("audio/mpeg", "sunday_sermon.mp3"))
        assertTrue(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("application/octet-stream", "voice_note.m4a"))
        assertTrue(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("application/ogg", "whatsapp_audio.opus"))
        assertTrue(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("video/mp4", "recording.m4a"))
        assertTrue(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("video/mp4", "sunday_video_sermon.mp4"))
        assertTrue(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("video/quicktime", "youth_conference.mov"))
        assertTrue(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("video/x-matroska", "revival_night.mkv"))
        assertTrue(SpiritualResourceRepositoryImpl.isVideoContainerCandidate("video/mp4", "sunday_video_sermon.mp4"))
        assertFalse(SpiritualResourceRepositoryImpl.isVideoContainerCandidate("audio/mpeg", "sunday_sermon.mp3"))
        assertFalse(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("application/pdf", "sermon_notes.pdf"))
        assertFalse(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("application/epub+zip", "bible.epub"))
        assertFalse(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("text/plain", "notes.txt"))
        assertFalse(SpiritualResourceRepositoryImpl.isSupportedAudioCandidate("image/jpeg", "flyer.jpg"))
    }

    @Test
    fun testGitHubUpdateCheckerVersionComparisonAndJsonParsing() {
        assertTrue(com.example.core.update.AppUpdateChecker.isNewerVersion("1.0", "v1.0.1"))
        assertTrue(com.example.core.update.AppUpdateChecker.isNewerVersion("1.0", "v1.1"))
        assertTrue(com.example.core.update.AppUpdateChecker.isNewerVersion("1.0.9", "v1.1.0"))
        assertFalse(com.example.core.update.AppUpdateChecker.isNewerVersion("1.0", "v1.0.0"))
        assertFalse(com.example.core.update.AppUpdateChecker.isNewerVersion("1.2.0", "v1.1.9"))

        val sampleJson = """
            {
              "tag_name": "v1.2.0",
              "name": "Gideonites v1.2.0 Release",
              "body": "- Removed ECWA branding\n- Fixed audio sermon upload",
              "html_url": "https://github.com/ChristTech/YAF-Android-APP/releases/tag/v1.2.0",
              "draft": false,
              "published_at": "2026-11-23T10:00:00Z",
              "assets": [
                {
                  "name": "app-release.apk",
                  "size": 19500000,
                  "browser_download_url": "https://github.com/ChristTech/YAF-Android-APP/releases/download/v1.2.0/app-release.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = com.example.core.update.AppUpdateChecker.parseGitHubReleaseJson(sampleJson)
        assertNotNull(parsed)
        assertEquals("1.2.0", parsed!!.latestVersion)
        assertEquals("v1.2.0", parsed.tagName)
        assertEquals("https://github.com/ChristTech/YAF-Android-APP/releases/download/v1.2.0/app-release.apk", parsed.apkDownloadUrl)
        assertEquals("app-release.apk", parsed.apkFileName)
    }

    @Test
    fun testEditedAdminInfoPersistsAcrossLogoutPrepopulationCloudSyncAndReLogin() = runBlocking {
        val userRepo = com.example.data.repository.UserRepositoryImpl(
            userDao = database.userDao(),
            auditLogDao = database.auditLogDao(),
            database = database
        )

        // 1. Admin logs in initially with default credentials
        val initialLogin = userRepo.login("admin@youthalive.org", "AdminPass#2026")
        assertTrue(initialLogin is com.example.data.repository.AuthResult.Success)
        val initialAdmin = (initialLogin as com.example.data.repository.AuthResult.Success).user

        // 2. Admin edits name, phone, office address, and emblem URI
        val editedAdmin = initialAdmin.copy(
            fullName = "Pastor Victor Adebisi",
            phone = "+2349012345678",
            address = "Winners Chapel International Youth Secretariat, Ota",
            profilePhotoUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg=="
        )
        userRepo.updateUserProfile(editedAdmin)

        // Active session must reflect updated name immediately
        val activeSessionAfterEdit = userRepo.getActiveSessionSync()
        assertEquals("Pastor Victor Adebisi", activeSessionAfterEdit?.userName)

        // 3. Log out, simulate prepopulator & cloud sync (both stale cloud snapshot and updated cloud snapshot)
        userRepo.logout()
        assertEquals(null, userRepo.getActiveSessionSync())

        com.example.data.local.DatabasePrepopulator.populateInitialData(database)

        // Simulate stale cloud snapshot arriving right after logout
        val staleTime = java.util.Date(System.currentTimeMillis() - 60_000L)
        userRepo.syncCloudAccountsToLocalDb(
            listOf(
                com.example.data.remote.CloudUserProfile(
                    id = com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_ID,
                    userId = com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_ID,
                    fullName = "Admin",
                    email = "admin@youthalive.org",
                    phone = "+2348011223344",
                    address = "Living Faith Church Youth Alive Fellowship Secretariat",
                    role = "ADMINISTRATOR",
                    status = "APPROVED",
                    updatedAt = com.google.firebase.Timestamp(staleTime)
                )
            )
        )

        // 4. Re-login and verify edited admin info did NOT revert to default state
        val reLogin = userRepo.login("admin@youthalive.org", "AdminPass#2026")
        assertTrue(reLogin is com.example.data.repository.AuthResult.Success)
        val restoredAdmin = (reLogin as com.example.data.repository.AuthResult.Success).user
        assertEquals("Pastor Victor Adebisi", restoredAdmin.fullName)
        assertEquals("+2349012345678", restoredAdmin.phone)
        assertEquals("Winners Chapel International Youth Secretariat, Ota", restoredAdmin.address)
        assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", restoredAdmin.profilePhotoUri)
        assertEquals("Pastor Victor Adebisi", userRepo.getActiveSessionSync()?.userName)

        // 5. Verify newer cloud update (e.g., removing emblem from another device) propagates cleanly
        val newerTime = java.util.Date(System.currentTimeMillis() + 60_000L)
        userRepo.syncCloudAccountsToLocalDb(
            listOf(
                com.example.data.remote.CloudUserProfile(
                    id = com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_ID,
                    userId = com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_ID,
                    fullName = "Pastor Victor Adebisi",
                    email = "admin@youthalive.org",
                    phone = "+2349012345678",
                    address = "Canaanland Youth Office",
                    profilePhotoUri = null,
                    role = "ADMINISTRATOR",
                    status = "APPROVED",
                    updatedAt = com.google.firebase.Timestamp(newerTime)
                )
            )
        )
        val afterCloudClear = database.userDao().getUserByIdSync(com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_ID)
        assertEquals("Canaanland Youth Office", afterCloudClear?.address)
        assertEquals(null, afterCloudClear?.profilePhotoUri)
    }

    @Test
    fun testAudioSermonMetadataJsonNeverTruncatesAudioFieldsOrWipesLocalAudio() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioDir = java.io.File(context.filesDir, "sermon_audio").apply { mkdirs() }
        val audioFile = java.io.File(audioDir, "sa_longmeta99.m4a").apply { writeBytes(byteArrayOf(9, 8, 7, 6)) }

        val longTitle = "Dominion Through The Holy Ghost & Fire For Living Faith Church Youths Across Every Campus And City Fellowship " + "A".repeat(120)
        val longSummary = "Detailed sermon description with quotes \"Jesus is Lord\" and\nnewlines explaining every point of the video message so it would have overflowed 980 chars previously. " + "B".repeat(260)

        val audioSermon = SpiritualResourceEntity(
            id = "sermon-long-meta-1",
            title = longTitle,
            category = "SERMON",
            speakerOrAuthor = "Pastor David",
            scriptureReference = "Acts 1:8",
            summary = longSummary,
            contentBody = longSummary,
            audioDurationMinutes = 42,
            publishedAt = 2_000_000_000L,
            audioUri = audioFile.absolutePath,
            audioCloudId = "sa_longmeta99",
            audioFileName = "very_long_sunday_service_sermon_video_extracted_audio_track_recording_final_mix.m4a",
            audioMimeType = "audio/mp4",
            audioSizeBytes = 5_432_100L,
            audioChunkCount = 12
        )

        val json = (spiritualRepository as SpiritualResourceRepositoryImpl).buildResourceMetadataJson(audioSermon)
        assertTrue("Metadata JSON must not exceed 980 chars (was ${json.length})", json.length <= 980)
        assertTrue("Metadata JSON must remain valid JSON ending with '}'", json.startsWith("{") && json.endsWith("}"))
        val parsed = org.json.JSONObject(json)
        assertEquals("sa_longmeta99", parsed.getString("audioCloudId"))
        assertEquals(12, parsed.getInt("audioChunkCount"))
        assertEquals(42, parsed.getInt("audioDurationMinutes"))

        // Save locally and simulate cloud sync returning this metadata AND an older stale cloud audio sermon
        spiritualRepository.addResource(audioSermon)
        spiritualRepository.syncCloudScripturesToLocalDb(
            listOf(
                com.example.data.remote.CloudDailyScripture(
                    id = "scripture_sermon-long-meta-1",
                    authorName = "Pastor David",
                    referenceEn = "Acts 1:8",
                    verseTextEn = longSummary,
                    reflectionEn = longSummary,
                    prayerPromptEn = json
                ),
                com.example.data.remote.CloudDailyScripture(
                    id = "scripture_older-audio",
                    authorName = "Pastor Old",
                    referenceEn = "John 3:16",
                    verseTextEn = "Older audio",
                    reflectionEn = "Older body",
                    prayerPromptEn = """{"category":"SERMON","audioDurationMinutes":10,"publishedAt":1000000000,"audioCloudId":"sa_old999","audioFileName":"old.mp3","audioMimeType":"audio/mpeg","audioSizeBytes":1000,"audioChunkCount":1,"title":"Older Audio"}"""
                )
            )
        )

        val synced = spiritualRepository.getResourceById("sermon-long-meta-1").let {
            database.spiritualResourceDao().getResourceByIdSync("sermon-long-meta-1")
        }
        assertNotNull("Newer audio sermon must NOT be deleted or wiped by sync", synced)
        assertTrue("hasAudio must remain true after cloud sync", synced!!.hasAudio)
        assertEquals("sa_longmeta99", synced.audioCloudId)
        assertEquals(audioFile.absolutePath, synced.audioUri)
        assertEquals(12, synced.audioChunkCount)
        assertTrue("Local audio file must still exist", audioFile.exists())
    }

    @Test
    fun testTextOnlySermonHasZeroAudioDurationAndNoFakeNarrationDuration() = runBlocking {
        val vm = com.example.ui.spiritual.SpiritualViewModel(spiritualRepository)
        vm.publishResource(
            title = "Written Sermon Notes Only",
            category = "SERMON",
            author = "Pastor Sam",
            scripture = "Romans 8:28",
            summary = "Summary of text sermon without uploaded media",
            content = "Full outline of text sermon",
            duration = 30,
            audioDraft = null
        )
        kotlinx.coroutines.delay(150L)
        val all = spiritualRepository.getAllResources().first()
        val textSermon = all.firstOrNull { it.title == "Written Sermon Notes Only" }
        assertNotNull(textSermon)
        assertFalse(textSermon!!.hasAudio)
        assertEquals(0, textSermon.audioDurationMinutes)
    }
}
