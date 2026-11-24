package com.example.data.local

import com.example.core.security.PasswordHasher
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.model.DepartmentType
import com.example.data.model.Gender
import com.example.data.model.UserRole

/**
 * Prepopulates the official Admin account ("Admin", no aliases) and baseline department structures
 * for Youth Alive Fellowship, and provides a full database reset that preserves only the Admin account.
 */
object DatabasePrepopulator {

    const val OFFICIAL_ADMIN_ID = "user-admin-01"
    const val OFFICIAL_ADMIN_EMAIL = "admin@youthalive.org"
    const val OFFICIAL_ADMIN_NAME = "Admin"
    const val OFFICIAL_ADMIN_ADDRESS = "Living Faith Church Youth Alive Fellowship Secretariat"

    val RETIRED_DEMO_USER_IDS = setOf(
        "user-exco-01",
        "user-member-01",
        "user-guest-01",
        "user-exco-legacy",
        "user-member-legacy",
        "user-admin-legacy"
    )

    val RETIRED_DEMO_EMAILS = setOf(
        "president@youthalive.org",
        "sarah.eze@youthalive.org",
        "guest@youthalive.org",
        "president@churchyouth.org",
        "sarah.eze@churchyouth.org",
        "admin@churchyouth.org"
    )

    fun isLegacyAdminAlias(name: String?): Boolean {
        if (name.isNullOrBlank()) return true
        val trimmed = name.trim()
        return trimmed.equals("Elder David Adeleke", ignoreCase = true) ||
            trimmed.equals("System Administrator", ignoreCase = true) ||
            trimmed.contains("Adeleke", ignoreCase = true) ||
            trimmed.startsWith("Elder ", ignoreCase = true) ||
            trimmed.startsWith("System Admin", ignoreCase = true)
    }

    suspend fun populateInitialData(database: ChurchYouthDatabase) {
        val userDao = database.userDao()
        val departmentDao = database.departmentDao()
        val auditLogDao = database.auditLogDao()

        // Remove any previously seeded non-admin demo accounts
        for (demoId in RETIRED_DEMO_USER_IDS) {
            try {
                userDao.deleteUser(demoId)
            } catch (_: Exception) {
            }
        }
        for (demoEmail in RETIRED_DEMO_EMAILS) {
            try {
                userDao.getUserByEmail(demoEmail)?.let { userDao.deleteUser(it.id) }
            } catch (_: Exception) {
            }
        }

        // 1. Official Admin Account ("Admin" only — no aliases)
        val adminSalt = PasswordHasher.generateSalt()
        val adminUser = UserEntity(
            id = OFFICIAL_ADMIN_ID,
            fullName = OFFICIAL_ADMIN_NAME,
            email = OFFICIAL_ADMIN_EMAIL,
            phone = "+2348011223344",
            passwordHash = PasswordHasher.hashPassword("AdminPass#2026", adminSalt),
            salt = adminSalt,
            role = UserRole.ADMINISTRATOR,
            status = AccountStatus.APPROVED,
            dateOfBirth = "",
            gender = Gender.PREFER_NOT_TO_SAY,
            address = OFFICIAL_ADMIN_ADDRESS,
            department = DepartmentType.GENERAL,
            fellowshipGroup = "Central Administration",
            emergencyContactName = "",
            emergencyContactPhone = "",
            emergencyContactRelationship = ""
        )

        try {
            val existingAdmin = userDao.getUserByEmail(adminUser.email) ?: userDao.getUserByIdSync(adminUser.id)
            if (existingAdmin == null) {
                userDao.insertUser(adminUser)
            } else {
                val hasLegacyName = isLegacyAdminAlias(existingAdmin.fullName)
                val hasLegacyAddress = existingAdmin.address == "12 Cathedral Way, Lagos" ||
                    existingAdmin.address.isBlank() ||
                    existingAdmin.address.contains("ECWA", ignoreCase = true)

                if (hasLegacyName ||
                    hasLegacyAddress ||
                    existingAdmin.role != UserRole.ADMINISTRATOR ||
                    existingAdmin.status != AccountStatus.APPROVED ||
                    existingAdmin.emergencyContactName.isNotBlank()
                ) {
                    userDao.updateUser(
                        existingAdmin.copy(
                            fullName = if (hasLegacyName) OFFICIAL_ADMIN_NAME else existingAdmin.fullName,
                            role = UserRole.ADMINISTRATOR,
                            status = AccountStatus.APPROVED,
                            dateOfBirth = "",
                            gender = Gender.PREFER_NOT_TO_SAY,
                            address = if (hasLegacyAddress) OFFICIAL_ADMIN_ADDRESS else existingAdmin.address,
                            fellowshipGroup = "Central Administration",
                            emergencyContactName = "",
                            emergencyContactPhone = "",
                            emergencyContactRelationship = ""
                        )
                    )
                }
            }

            // Ensure any active Admin session in Room reflects the current Admin display name without legacy aliases
            val resolvedAdmin = userDao.getUserByEmail(adminUser.email) ?: userDao.getUserByIdSync(adminUser.id)
            val activeSession = userDao.getActiveSessionSync()
            if (activeSession != null &&
                (activeSession.userId == OFFICIAL_ADMIN_ID || activeSession.userRole == UserRole.ADMINISTRATOR)
            ) {
                val targetName = if (activeSession.userId == OFFICIAL_ADMIN_ID) {
                    resolvedAdmin?.fullName?.takeIf { !isLegacyAdminAlias(it) } ?: OFFICIAL_ADMIN_NAME
                } else {
                    activeSession.userName.takeIf { !isLegacyAdminAlias(it) } ?: OFFICIAL_ADMIN_NAME
                }
                if (activeSession.userName != targetName) {
                    userDao.saveSession(activeSession.copy(userName = targetName))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Ministry Departments Structure (No personal demo aliases)
        val departments = listOf(
            DepartmentEntity(
                id = "dept-media",
                type = DepartmentType.MEDIA,
                name = "Media & Technical Team",
                description = "Audio engineering, livestream broadcasting, videography, photography, and presentation graphics.",
                leaderId = adminUser.id,
                leaderName = "Media Unit Coordinator",
                meetingSchedule = "Saturdays 4:00 PM",
                meetingVenue = "Control Booth & Studio",
                memberCount = 0
            ),
            DepartmentEntity(
                id = "dept-choir",
                type = DepartmentType.CHOIR,
                name = "Choir & Music Ministry",
                description = "Worship leading, choral harmonies, contemporary praise, instrumentalists, and song writing.",
                leaderId = adminUser.id,
                leaderName = "Choir Coordinator",
                meetingSchedule = "Saturdays 5:00 PM & Sundays 7:30 AM",
                meetingVenue = "Main Choir Gallery",
                memberCount = 0
            ),
            DepartmentEntity(
                id = "dept-drama",
                type = DepartmentType.DRAMA,
                name = "Drama & Creative Arts",
                description = "Stage plays, spoken word, prophetic choreography, skits, and gospel film productions.",
                leaderId = adminUser.id,
                leaderName = "Drama Unit Coordinator",
                meetingSchedule = "Fridays 6:00 PM",
                meetingVenue = "Youth Amphitheatre",
                memberCount = 0
            ),
            DepartmentEntity(
                id = "dept-ushering",
                type = DepartmentType.USHERING,
                name = "Ushering & Protocol",
                description = "Orderliness, crowd management, VIP protocol, sanctuary beautification, and hospitality logistics.",
                leaderId = adminUser.id,
                leaderName = "Ushering Unit Coordinator",
                meetingSchedule = "Sundays 7:00 AM",
                meetingVenue = "Sanctuary Foyer",
                memberCount = 0
            ),
            DepartmentEntity(
                id = "dept-prayer",
                type = DepartmentType.PRAYER,
                name = "Prayer & Intercession Unit",
                description = "Spiritual watchmen, vigil coordinators, pre-service prayer cover, and prophetic intercession.",
                leaderId = adminUser.id,
                leaderName = "Prayer Unit Coordinator",
                meetingSchedule = "Thursdays 6:00 PM",
                meetingVenue = "Upper Room Chapel",
                memberCount = 0
            ),
            DepartmentEntity(
                id = "dept-sports",
                type = DepartmentType.SPORTS,
                name = "Sports & Recreation Unit",
                description = "Youth athletic tournaments, football team, fitness club, table tennis, and health seminars.",
                leaderId = adminUser.id,
                leaderName = "Sports Unit Coordinator",
                meetingSchedule = "Saturdays 7:00 AM",
                meetingVenue = "Community Sports Field",
                memberCount = 0
            )
        )
        try {
            departmentDao.insertDepartments(departments)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Prepopulate Bible Scriptures Database for Game Questions & Scramble
        try {
            val bibleDao = database.bibleDao()
            if (bibleDao.getVerseCount() == 0) {
                bibleDao.insertVerses(com.example.data.repository.BibleRepositoryImpl.getBaselineSeedVerses())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Wipes all user-generated and test data from the local Room database while preserving ONLY
     * the official Admin account ("Admin", admin@youthalive.org / AdminPass#2026), baseline
     * department definitions, and Bible verses.
     */
    suspend fun resetLocalDatabasePreserveAdmin(database: ChurchYouthDatabase) {
        try {
            val writableDb = database.openHelper.writableDatabase
            writableDb.beginTransaction()
            try {
                writableDb.execSQL("DELETE FROM community_post_likes")
                writableDb.execSQL("DELETE FROM community_comments")
                writableDb.execSQL("DELETE FROM community_posts")
                writableDb.execSQL("DELETE FROM executive_inquiries")
                writableDb.execSQL("DELETE FROM event_registrations")
                writableDb.execSQL("DELETE FROM events")
                writableDb.execSQL("DELETE FROM announcements")
                writableDb.execSQL("DELETE FROM financial_records")
                writableDb.execSQL("DELETE FROM game_scores")
                writableDb.execSQL("DELETE FROM game_quizzes")
                writableDb.execSQL("DELETE FROM multiplayer_submissions")
                writableDb.execSQL("DELETE FROM multiplayer_participants")
                writableDb.execSQL("DELETE FROM multiplayer_game_sessions")
                writableDb.execSQL("DELETE FROM weekly_reports")
                writableDb.execSQL("DELETE FROM attendance_records")
                writableDb.execSQL("DELETE FROM spiritual_resources")
                writableDb.execSQL("DELETE FROM fellowship_notifications")
                writableDb.execSQL("DELETE FROM audit_logs")
                writableDb.execSQL("DELETE FROM departments")
                writableDb.execSQL(
                    "DELETE FROM users WHERE id != ? AND LOWER(email) != ?",
                    arrayOf(OFFICIAL_ADMIN_ID, OFFICIAL_ADMIN_EMAIL)
                )
                writableDb.execSQL(
                    "DELETE FROM sessions WHERE userId != ? AND userRole != 'ADMINISTRATOR'",
                    arrayOf(OFFICIAL_ADMIN_ID)
                )
                writableDb.setTransactionSuccessful()
            } finally {
                writableDb.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        populateInitialData(database)

        try {
            database.auditLogDao().insertLog(
                AuditLogEntity(
                    userId = OFFICIAL_ADMIN_ID,
                    userRole = UserRole.ADMINISTRATOR,
                    actionType = "DATABASE_RESET",
                    targetEntity = "Database",
                    details = "Database cleared for real user testing; preserved Admin account only."
                )
            )
        } catch (_: Exception) {
        }
    }
}
