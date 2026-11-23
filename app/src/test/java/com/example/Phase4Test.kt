package com.example

import com.example.core.integration.ChmsSyncResult
import com.example.core.integration.DataExporter
import com.example.core.integration.MockChmsSyncAdapter
import com.example.core.integration.MockSmsGatewayAdapter
import com.example.core.localization.AppLanguage
import com.example.core.localization.LanguageManager
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.FinancialRecordEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.model.DepartmentType
import com.example.data.model.FinancialCategory
import com.example.data.model.Gender
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase4Test {

    @Test
    fun testLanguageManagerLocalization() {
        LanguageManager.setLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, LanguageManager.currentLanguage.value)
        assertTrue(LanguageManager.getMemoryVerse("Daniel 11:32").contains("strong, and do exploits"))
        assertEquals("The Gideonites - The Youth Alive Fellowship", LanguageManager.getWelcomeBannerText())

        // Switch to Yoruba
        LanguageManager.setLanguage(AppLanguage.YORUBA)
        assertEquals(AppLanguage.YORUBA, LanguageManager.currentLanguage.value)
        assertTrue(LanguageManager.getMemoryVerse("Daniel 11:32").contains("lágbára"))
        assertTrue(LanguageManager.getMemoryVerse("Judges 6:12").contains("akọni alágbára"))
        assertEquals("The Gideonites - The Youth Alive Fellowship (Àwọn Òkìkí)", LanguageManager.getWelcomeBannerText())

        // Verify Quiz Arena questions auto-localize to Yoruba
        val yorubaTrivia = com.example.data.model.PresetGameSessions.getTriviaChallengeQuestions(AppLanguage.YORUBA)
        assertTrue(yorubaTrivia.first().prompt.contains("Bíbélì"))
        assertTrue(yorubaTrivia.first().scriptureReference.contains("Bibeli Mimo"))

        // Switch to Hausa
        LanguageManager.setLanguage(AppLanguage.HAUSA)
        assertEquals(AppLanguage.HAUSA, LanguageManager.currentLanguage.value)
        assertTrue(LanguageManager.getMemoryVerse("Daniel 11:32").contains("ƙarfi"))
        assertTrue(LanguageManager.getMemoryVerse("Judges 6:12").contains("jarumi mai ƙarfi"))

        // Switch to Igbo
        LanguageManager.setLanguage(AppLanguage.IGBO)
        assertEquals(AppLanguage.IGBO, LanguageManager.currentLanguage.value)
        assertTrue(LanguageManager.getMemoryVerse("Daniel 11:32").contains("Chineke"))
        assertTrue(LanguageManager.getMemoryVerse("Judges 6:12").contains("dike n'agha"))

        // Test Signup Preferred Language & 1-tap English Override Toggle
        LanguageManager.setPreferredLanguage(AppLanguage.YORUBA)
        assertEquals(AppLanguage.YORUBA, LanguageManager.preferredLanguage.value)
        assertEquals(AppLanguage.YORUBA, LanguageManager.currentLanguage.value)
        assertFalse(LanguageManager.isEnglishOverride.value)

        // Toggle to English
        LanguageManager.toggleEnglishOverride()
        assertTrue(LanguageManager.isEnglishOverride.value)
        assertEquals(AppLanguage.ENGLISH, LanguageManager.currentLanguage.value)
        assertEquals(AppLanguage.YORUBA, LanguageManager.preferredLanguage.value)

        // Toggle back to normal signup language (Yoruba)
        LanguageManager.toggleEnglishOverride()
        assertFalse(LanguageManager.isEnglishOverride.value)
        assertEquals(AppLanguage.YORUBA, LanguageManager.currentLanguage.value)

        // Reset to English
        LanguageManager.setPreferredLanguage(AppLanguage.ENGLISH)
    }

    @Test
    fun testFinancialDataCsvExport() {
        val records = listOf(
            FinancialRecordEntity(
                id = "fin-001",
                userId = "user-01",
                userName = "David Adeleke",
                category = FinancialCategory.TITHE,
                amount = 25000.0,
                status = PaymentStatus.COMPLETED,
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                referenceCode = "TITHE-2026-001",
                purposeOrNotes = "October First Fruit Tithe",
                recordedById = "user-01"
            )
        )
        val csv = DataExporter.exportFinancesToCsv(records)
        assertTrue(csv.contains("Reference,User,Category,Amount,PaymentMethod,Status,Date,Notes"))
        assertTrue(csv.contains("TITHE-2026-001"))
        assertTrue(csv.contains("25000.0"))
        assertTrue(csv.contains("October First Fruit Tithe"))
    }

    @Test
    fun testAttendanceDataCsvExport() {
        val records = listOf(
            AttendanceRecordEntity(
                id = "att-001",
                meetingTitle = "Sunday Youth Service",
                meetingType = "SUNDAY_SERVICE",
                departmentOrGroup = "General",
                dateMillis = System.currentTimeMillis(),
                totalAttendees = 85,
                maleCount = 38,
                femaleCount = 42,
                firstTimersCount = 5,
                checkInMethod = "QR_CODE",
                recordedById = "exco-01",
                recordedByName = "Youth President"
            )
        )
        val csv = DataExporter.exportAttendanceToCsv(records)
        assertTrue(csv.contains("ID,MeetingTitle,MeetingType,Department,Date,Total,Males,Females,FirstTimers,Method,Recorder"))
        assertTrue(csv.contains("Sunday Youth Service"))
        assertTrue(csv.contains("85"))
        assertTrue(csv.contains("Youth President"))
    }

    @Test
    fun testMemberRosterCsvExport() {
        val members = listOf(
            UserEntity(
                id = "user-01",
                fullName = "Grace Adeleke",
                email = "grace@churchyouth.org",
                phone = "+2348011223344",
                passwordHash = "hash",
                salt = "salt",
                role = UserRole.YOUTH_MEMBER,
                status = AccountStatus.APPROVED,
                gender = Gender.FEMALE,
                department = DepartmentType.CHOIR,
                fellowshipGroup = "Bethel Youth Fellowship"
            )
        )
        val csv = DataExporter.exportMembersToCsv(members)
        assertTrue(csv.contains("ID,FullName,Email,Phone,Role,Status,Department,Fellowship,RegisteredAt"))
        assertTrue(csv.contains("Grace Adeleke"))
        assertTrue(csv.contains("grace@churchyouth.org"))
        assertTrue(csv.contains("Choir / Music Ministry"))
    }

    @Test
    fun testExternalSystemAdapters() = runBlocking {
        // Test ChMS Sync
        val chmsAdapter = MockChmsSyncAdapter()
        val syncResult = chmsAdapter.synchronizeWithCentralChurchSystem()
        assertTrue(syncResult.isSuccess)
        assertTrue(syncResult.syncedMembersCount > 0)
        assertTrue(syncResult.syncedEventsCount > 0)

        // Test SMS Gateway
        val smsAdapter = MockSmsGatewayAdapter()
        val emptyResult = smsAdapter.sendSmsBroadcast(emptyList(), "Test message")
        assertFalse(emptyResult.isSuccess)

        val successResult = smsAdapter.sendSmsBroadcast(listOf("+2348011223344", "+2348022334455"), "Reminder: Youth Prayer Vigil tonight at 10 PM!")
        assertTrue(successResult.isSuccess)
        assertEquals(2, successResult.recipientCount)
        assertTrue(successResult.messageId.startsWith("SMS-"))
    }
}
