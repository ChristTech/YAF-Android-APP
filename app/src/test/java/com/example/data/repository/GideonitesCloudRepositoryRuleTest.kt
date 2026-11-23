package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GideonitesCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertUserProfile_authenticatedOwner_persistsAndUpdatesLanguage() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = GideonitesCloudRepository(firestore)

        val upsertResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertCurrentUserProfile(
                fullName = "Alice Gideon",
                email = ALICE_EMAIL,
                phone = "+2348011112222",
                department = "CHOIR",
                fellowshipGroup = "Bethel Cell",
                preferredLanguage = "yo",
                englishOverride = false,
                xpPoints = 120L
            )
        }
        assertTrue("Expected upsert to succeed: ${upsertResult.exceptionOrNull()}", upsertResult.isSuccess)
        val profile = upsertResult.getOrThrow()
        assertEquals(aliceUid, profile.userId)
        assertEquals("yo", profile.preferredLanguage)

        val updateLangResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateLanguagePreference(preferredLanguage = "ig", englishOverride = true)
        }
        assertTrue(updateLangResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfileById(aliceUid).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals("ig", fetched?.preferredLanguage)
        assertEquals(true, fetched?.englishOverride)
    }

    @Test
    fun getUserProfileById_crossUserPiiAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = GideonitesCloudRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertCurrentUserProfile(
                fullName = "Alice Gideon",
                email = ALICE_EMAIL,
                preferredLanguage = "yo"
            ).getOrThrow()
        }

        // Switch to Bob and attempt to read Alice's PII profile
        signInTestUser(BOB_EMAIL)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfileById(aliceUid)
        }
        assertTrue("Expected cross-user profile read to fail", crossReadResult.isFailure)
        val firestoreException = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun publishAndObserveDailyScripture_multilingualFields_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GideonitesCloudRepository(firestore)

        val docIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.publishDailyScripture(
                authorName = "Elder David Adeleke",
                dateKey = "2026-10-25",
                referenceEn = "Judges 6:12",
                verseTextEn = "The Lord is with thee, thou mighty man of valour.",
                reflectionEn = "Gideonites arise in strength and faith.",
                prayerPromptEn = "Lord, ignite valour in our youth fellowship.",
                referenceYo = "Awon Onidajo 6:12",
                verseTextYo = "Oluwa pelu re, iwo alagbara ogun.",
                reflectionYo = "E dide gege bi omo ogun Oluwa."
            )
        }
        assertTrue("Expected publishDailyScripture to succeed", docIdResult.isSuccess)
        val docId = docIdResult.getOrThrow()

        val emittedScriptures = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDailyScriptures().first { list -> list.any { it.id == docId } }
        }
        val created = emittedScriptures.first { it.id == docId }
        assertEquals("Judges 6:12", created.referenceEn)
        assertEquals("Awon Onidajo 6:12", created.referenceYo)
    }

    @Test
    fun publishProgrammeAndQuizSession_authenticatedUser_emitsInFlow() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GideonitesCloudRepository(firestore)
        val now = System.currentTimeMillis()

        val progResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.publishYouthProgramme(
                titleEn = "Gideonites Fire Night",
                descriptionEn = "All-night youth revival and Bible quiz.",
                category = "SPIRITUAL",
                venue = "Youth Chapel",
                startDateMillis = now + 3600_000L,
                endDateMillis = now + 10800_000L,
                titleYo = "Ale Ina Awon Odo Gideonites",
                descriptionYo = "Isoji ale ati idije Bibeli."
            )
        }
        assertTrue(progResult.isSuccess)
        val progId = progResult.getOrThrow()

        val programmes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeYouthProgrammes().first { list -> list.any { it.id == progId } }
        }
        assertTrue(programmes.any { it.id == progId && it.titleYo == "Ale Ina Awon Odo Gideonites" })

        val quizResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCloudQuizSession(
                hostName = "Alice Host",
                roomPin = "GID888",
                titleEn = "Gideonites Champions Showdown",
                titleYo = "Idije Awon Odo Gideonites"
            )
        }
        assertTrue(quizResult.isSuccess)
        val quizId = quizResult.getOrThrow()

        val sessions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCloudQuizSessions().first { list -> list.any { it.id == quizId } }
        }
        assertTrue(sessions.any { it.id == quizId && it.roomPin == "GID888" })
    }

    @Test
    fun observeDailyScriptures_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = GideonitesCloudRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeDailyScriptures().first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull("Expected exception for unauthenticated flow collection", caughtException)
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    @Test
    fun publishCommunityPostAndExecutiveInquiry_authenticatedUser_emitsAndUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GideonitesCloudRepository(firestore)

        val postResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.publishCommunityPost(
                authorName = "Alice Gideon",
                authorRole = "Youth Member",
                category = "Testimonies",
                title = "Gideonites Fellowship Testimony",
                content = "Amazing youth service today!",
                attachmentPhotoUri = "file:///data/user/0/com.example/files/profile_photos/post_1.jpg"
            )
        }
        assertTrue("Expected publishCommunityPost to succeed: ${postResult.exceptionOrNull()}", postResult.isSuccess)
        val postId = postResult.getOrThrow()

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCommunityPosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId && it.title == "Gideonites Fellowship Testimony" })

        val inquiryResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitExecutiveInquiry(
                senderName = "Alice Gideon",
                senderRole = "Youth Member",
                isAnonymous = true,
                topic = "Confidential Prayer Request",
                message = "Please keep my upcoming exams in prayer."
            )
        }
        assertTrue("Expected submitExecutiveInquiry to succeed: ${inquiryResult.exceptionOrNull()}", inquiryResult.isSuccess)
        val inquiryId = inquiryResult.getOrThrow()

        val respondResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.respondToExecutiveInquiry(
                inquiryId = inquiryId,
                status = "RESOLVED",
                responseMessage = "Praying for excellence and divine wisdom!",
                respondedByName = "Emmanuel Okon",
                respondedByRole = "Youth Executive"
            )
        }
        assertTrue("Expected respondToExecutiveInquiry to succeed: ${respondResult.exceptionOrNull()}", respondResult.isSuccess)

        val inquiries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeExecutiveInquiries().first { list -> list.any { it.id == inquiryId && it.status == "RESOLVED" } }
        }
        val updatedInquiry = inquiries.first { it.id == inquiryId }
        assertEquals("Anonymous Youth Member", updatedInquiry.senderName)
        assertEquals("Praying for excellence and divine wisdom!", updatedInquiry.responseMessage)
    }

    private companion object {
        const val ALICE_EMAIL = "alice.gideon@test.com"
        const val BOB_EMAIL = "bob.gideon@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
