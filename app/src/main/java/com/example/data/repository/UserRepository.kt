package com.example.data.repository

import com.example.core.localization.AppLanguage
import com.example.core.localization.LanguageManager
import com.example.core.security.PasswordHasher
import com.example.data.local.ChurchYouthDatabase
import com.example.data.local.DatabasePrepopulator
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.model.DepartmentType
import com.example.data.model.Gender
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

sealed class AuthResult {
    data class Success(val user: UserEntity, val token: String) : AuthResult()
    data class PendingApproval(val message: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface UserRepository {
    fun getActiveSession(): Flow<SessionEntity?>
    suspend fun getActiveSessionSync(): SessionEntity?
    fun getUserById(userId: String): Flow<UserEntity?>
    suspend fun getUserByIdSync(userId: String): UserEntity?
    fun getAllUsers(): Flow<List<UserEntity>>
    fun getUsersByStatus(status: AccountStatus): Flow<List<UserEntity>>
    suspend fun registerUser(
        fullName: String,
        email: String,
        phone: String,
        plainPassword: String,
        role: UserRole = UserRole.YOUTH_MEMBER,
        department: DepartmentType = DepartmentType.GENERAL,
        fellowshipGroup: String = "",
        emergencyContactName: String = "",
        emergencyContactPhone: String = "",
        emergencyRelationship: String = "",
        preferredLanguage: AppLanguage = AppLanguage.ENGLISH,
        profilePhotoUri: String? = null
    ): AuthResult
    suspend fun login(emailOrPhone: String, plainPassword: String): AuthResult
    suspend fun loginWithGoogle(
        uid: String,
        fullName: String,
        email: String,
        preferredLanguage: AppLanguage = AppLanguage.ENGLISH
    ): AuthResult
    suspend fun logout()
    suspend fun updateUserProfile(user: UserEntity)
    suspend fun updateUserStatus(userId: String, status: AccountStatus, adminUserId: String)
    suspend fun updateUserRole(userId: String, role: UserRole, adminUserId: String)
    suspend fun refreshUsersFromCloud()
    suspend fun syncCloudAccountsToLocalDb(cloudProfiles: List<com.example.data.remote.CloudUserProfile>)
    suspend fun pushLocalUsersToCloud()
    suspend fun clearAllDatabasesPreserveAdmin(): Result<String>
}

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao,
    private val database: ChurchYouthDatabase? = null,
    private val cloudRepository: GideonitesCloudRepository? = null
) : UserRepository {

    override fun getActiveSession(): Flow<SessionEntity?> = userDao.getActiveSession()

    override suspend fun getActiveSessionSync(): SessionEntity? = userDao.getActiveSessionSync()

    override fun getUserById(userId: String): Flow<UserEntity?> = userDao.getUserById(userId)

    override suspend fun getUserByIdSync(userId: String): UserEntity? = userDao.getUserByIdSync(userId)

    override fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    override fun getUsersByStatus(status: AccountStatus): Flow<List<UserEntity>> = userDao.getUsersByStatus(status)

    private suspend fun syncUserToCloud(user: UserEntity) {
        try {
            val syncablePhoto = com.example.ui.components.ensureSyncableImageUri(
                uriStr = user.profilePhotoUri,
                maxDimensionPx = 280,
                maxBase64Length = 90_000
            )
            cloudRepository?.upsertFellowshipAccount(
                userId = user.id,
                fullName = user.fullName,
                email = user.email,
                phone = user.phone,
                passwordHash = user.passwordHash,
                salt = user.salt,
                status = user.status.name,
                profilePhotoUri = syncablePhoto,
                role = user.role.name,
                department = user.department.name,
                address = user.address,
                fellowshipGroup = user.fellowshipGroup,
                emergencyContactName = user.emergencyContactName,
                emergencyContactPhone = user.emergencyContactPhone,
                emergencyContactRelationship = user.emergencyContactRelationship,
                preferredLanguage = user.preferredLanguage.code,
                englishOverride = LanguageManager.isEnglishOverride.value
            )
        } catch (e: Exception) {
            // Local database write already succeeded
        }
    }

    override suspend fun refreshUsersFromCloud() {
        val cloudRepo = cloudRepository ?: return
        try {
            val cloudAccounts = cloudRepo.fetchFellowshipAccounts().getOrNull().orEmpty()
            if (cloudAccounts.isNotEmpty()) {
                syncCloudAccountsToLocalDb(cloudAccounts)
            }
        } catch (e: Exception) {
            // Offline fallback to local Room DB
        }
    }

    private suspend fun purgeRetiredDemoAccountsLocally() {
        for (demoId in DatabasePrepopulator.RETIRED_DEMO_USER_IDS) {
            try {
                userDao.deleteUser(demoId)
            } catch (_: Exception) {
            }
        }
        for (demoEmail in DatabasePrepopulator.RETIRED_DEMO_EMAILS) {
            try {
                userDao.getUserByEmail(demoEmail)?.let { userDao.deleteUser(it.id) }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun ensureAdminAccountExistsLocally(): UserEntity {
        val existingAdmin = userDao.getUserByEmail(DatabasePrepopulator.OFFICIAL_ADMIN_EMAIL)
            ?: userDao.getUserByIdSync(DatabasePrepopulator.OFFICIAL_ADMIN_ID)
        if (existingAdmin != null) {
            val hasLegacyName = DatabasePrepopulator.isLegacyAdminAlias(existingAdmin.fullName)
            val hasLegacyAddress = existingAdmin.address == "12 Cathedral Way, Lagos" ||
                existingAdmin.address.isBlank() ||
                existingAdmin.address.contains("ECWA", ignoreCase = true)
            val needsInstitutionalCleanup =
                hasLegacyName ||
                hasLegacyAddress ||
                existingAdmin.role != UserRole.ADMINISTRATOR ||
                existingAdmin.status != AccountStatus.APPROVED ||
                existingAdmin.emergencyContactName.isNotBlank()

            val finalAdmin = if (needsInstitutionalCleanup) {
                val fixed = existingAdmin.copy(
                    fullName = if (hasLegacyName) DatabasePrepopulator.OFFICIAL_ADMIN_NAME else existingAdmin.fullName,
                    role = UserRole.ADMINISTRATOR,
                    status = AccountStatus.APPROVED,
                    dateOfBirth = "",
                    gender = Gender.PREFER_NOT_TO_SAY,
                    address = if (hasLegacyAddress) {
                        DatabasePrepopulator.OFFICIAL_ADMIN_ADDRESS
                    } else {
                        existingAdmin.address
                    },
                    fellowshipGroup = "Central Administration",
                    emergencyContactName = "",
                    emergencyContactPhone = "",
                    emergencyContactRelationship = ""
                )
                userDao.updateUser(fixed)
                fixed
            } else {
                existingAdmin
            }

            val activeSession = userDao.getActiveSessionSync()
            if (activeSession != null &&
                (activeSession.userId == finalAdmin.id || activeSession.userRole == UserRole.ADMINISTRATOR)
            ) {
                val targetName = if (activeSession.userId == finalAdmin.id) {
                    finalAdmin.fullName.takeIf { !DatabasePrepopulator.isLegacyAdminAlias(it) }
                        ?: DatabasePrepopulator.OFFICIAL_ADMIN_NAME
                } else {
                    activeSession.userName.takeIf { !DatabasePrepopulator.isLegacyAdminAlias(it) }
                        ?: DatabasePrepopulator.OFFICIAL_ADMIN_NAME
                }
                if (activeSession.userName != targetName) {
                    userDao.saveSession(activeSession.copy(userName = targetName))
                }
            }
            return finalAdmin
        }
        val adminSalt = PasswordHasher.generateSalt()
        val admin = UserEntity(
            id = DatabasePrepopulator.OFFICIAL_ADMIN_ID,
            fullName = DatabasePrepopulator.OFFICIAL_ADMIN_NAME,
            email = DatabasePrepopulator.OFFICIAL_ADMIN_EMAIL,
            phone = "+2348011223344",
            passwordHash = PasswordHasher.hashPassword("AdminPass#2026", adminSalt),
            salt = adminSalt,
            role = UserRole.ADMINISTRATOR,
            status = AccountStatus.APPROVED,
            dateOfBirth = "",
            gender = Gender.PREFER_NOT_TO_SAY,
            address = DatabasePrepopulator.OFFICIAL_ADMIN_ADDRESS,
            department = DepartmentType.GENERAL,
            fellowshipGroup = "Central Administration",
            emergencyContactName = "",
            emergencyContactPhone = "",
            emergencyContactRelationship = ""
        )
        userDao.insertUser(admin)
        return admin
    }

    override suspend fun syncCloudAccountsToLocalDb(
        cloudProfiles: List<com.example.data.remote.CloudUserProfile>
    ) {
        purgeRetiredDemoAccountsLocally()
        ensureAdminAccountExistsLocally()

        for (profile in cloudProfiles) {
            val targetId = profile.userId.ifBlank { profile.id }
            val normalizedEmail = profile.email.trim().lowercase()
            if (targetId.isBlank() || normalizedEmail.isBlank()) continue
            if (targetId in DatabasePrepopulator.RETIRED_DEMO_USER_IDS ||
                normalizedEmail in DatabasePrepopulator.RETIRED_DEMO_EMAILS ||
                profile.status.equals("PURGED", ignoreCase = true)
            ) {
                try {
                    userDao.deleteUser(targetId)
                } catch (_: Exception) {
                }
                continue
            }

            val existingById = userDao.getUserByIdSync(targetId)
            val existingByEmail = userDao.getUserByEmail(normalizedEmail)
            val existing = existingById ?: existingByEmail
            val finalId = existing?.id ?: targetId

            val resolvedRole = try {
                UserRole.valueOf(profile.role)
            } catch (e: Exception) {
                existing?.role ?: UserRole.YOUTH_MEMBER
            }
            val resolvedStatus = try {
                profile.status?.let { AccountStatus.valueOf(it) } ?: existing?.status ?: AccountStatus.APPROVED
            } catch (e: Exception) {
                existing?.status ?: AccountStatus.APPROVED
            }
            val resolvedDept = try {
                DepartmentType.valueOf(profile.department)
            } catch (e: Exception) {
                existing?.department ?: DepartmentType.GENERAL
            }
            val resolvedLang = AppLanguage.fromCodeOrName(profile.preferredLanguage)

            val fallbackSalt = existing?.salt ?: PasswordHasher.generateSalt()
            val fallbackHash = existing?.passwordHash ?: PasswordHasher.hashPassword(finalId, fallbackSalt)

            val isOfficialAdminProfile =
                normalizedEmail == DatabasePrepopulator.OFFICIAL_ADMIN_EMAIL ||
                finalId == DatabasePrepopulator.OFFICIAL_ADMIN_ID

            val cloudUpdatedAt = profile.updatedAt?.toDate()?.time ?: 0L
            val localIsNewer = existing != null &&
                cloudUpdatedAt > 0L &&
                existing.updatedAt > cloudUpdatedAt + 2_000L

            fun sanitizeAddress(raw: String?): String? = raw
                ?.trim()
                ?.takeIf { it.isNotBlank() && it != "12 Cathedral Way, Lagos" }
                ?.replace("ECWA", "Living Faith Church", ignoreCase = true)

            val resolvedFullName = if (localIsNewer) {
                existing!!.fullName
            } else if (isOfficialAdminProfile) {
                profile.fullName.trim().takeIf { !DatabasePrepopulator.isLegacyAdminAlias(it) }
                    ?: existing?.fullName?.trim()?.takeIf { !DatabasePrepopulator.isLegacyAdminAlias(it) }
                    ?: DatabasePrepopulator.OFFICIAL_ADMIN_NAME
            } else {
                profile.fullName.ifBlank { existing?.fullName ?: "Gideonite Member" }
            }

            val resolvedPhone = when {
                localIsNewer -> existing!!.phone
                existing != null && cloudUpdatedAt > existing.updatedAt -> profile.phone?.trim() ?: ""
                else -> profile.phone?.takeIf { it.isNotBlank() } ?: existing?.phone ?: ""
            }

            val resolvedPhotoUri = when {
                localIsNewer -> existing!!.profilePhotoUri
                existing != null && cloudUpdatedAt > existing.updatedAt -> profile.profilePhotoUri?.takeIf { it.isNotBlank() }
                else -> profile.profilePhotoUri?.takeIf { it.isNotBlank() } ?: existing?.profilePhotoUri
            }

            val resolvedAddress = if (localIsNewer) {
                sanitizeAddress(existing!!.address)
                    ?: if (isOfficialAdminProfile) DatabasePrepopulator.OFFICIAL_ADMIN_ADDRESS else ""
            } else {
                sanitizeAddress(profile.address)
                    ?: sanitizeAddress(existing?.address)
                    ?: if (isOfficialAdminProfile) DatabasePrepopulator.OFFICIAL_ADMIN_ADDRESS else ""
            }

            val syncedUser = UserEntity(
                id = finalId,
                fullName = resolvedFullName,
                email = normalizedEmail,
                phone = resolvedPhone,
                profilePhotoUri = resolvedPhotoUri,
                passwordHash = profile.passwordHash?.takeIf { it.isNotBlank() } ?: fallbackHash,
                salt = profile.salt?.takeIf { it.isNotBlank() } ?: fallbackSalt,
                role = if (isOfficialAdminProfile) UserRole.ADMINISTRATOR else resolvedRole,
                status = if (isOfficialAdminProfile) AccountStatus.APPROVED else resolvedStatus,
                address = resolvedAddress,
                department = if (localIsNewer) existing!!.department else resolvedDept,
                fellowshipGroup = if (isOfficialAdminProfile) "Central Administration" else (profile.fellowshipGroup ?: existing?.fellowshipGroup ?: ""),
                emergencyContactName = if (isOfficialAdminProfile) "" else (profile.emergencyContactName ?: existing?.emergencyContactName ?: ""),
                emergencyContactPhone = if (isOfficialAdminProfile) "" else (profile.emergencyContactPhone ?: existing?.emergencyContactPhone ?: ""),
                emergencyContactRelationship = if (isOfficialAdminProfile) "" else (profile.emergencyContactRelationship ?: existing?.emergencyContactRelationship ?: ""),
                preferredLanguage = if (localIsNewer) existing!!.preferredLanguage else resolvedLang,
                createdAt = existing?.createdAt ?: (profile.createdAt?.toDate()?.time ?: System.currentTimeMillis()),
                updatedAt = if (localIsNewer) existing!!.updatedAt else (cloudUpdatedAt.takeIf { it > 0L } ?: System.currentTimeMillis())
            )
            userDao.insertUser(syncedUser)

            val activeSession = userDao.getActiveSessionSync()
            if (activeSession != null && activeSession.userId == syncedUser.id && activeSession.userName != syncedUser.fullName) {
                userDao.saveSession(activeSession.copy(userName = syncedUser.fullName))
            }

            if (isOfficialAdminProfile && DatabasePrepopulator.isLegacyAdminAlias(profile.fullName)) {
                syncUserToCloud(syncedUser)
            }
        }
    }

    override suspend fun pushLocalUsersToCloud() {
        val cloudRepo = cloudRepository ?: return
        try {
            purgeRetiredDemoAccountsLocally()
            val admin = ensureAdminAccountExistsLocally()
            syncUserToCloud(admin)

            val allLocalUsers = userDao.getAllUsers().firstOrNull().orEmpty()
            for (u in allLocalUsers) {
                if (u.id in DatabasePrepopulator.RETIRED_DEMO_USER_IDS ||
                    u.email.lowercase() in DatabasePrepopulator.RETIRED_DEMO_EMAILS
                ) {
                    continue
                }
                syncUserToCloud(u)
            }
        } catch (e: Exception) {
            // Ignore when offline
        }
    }

    override suspend fun clearAllDatabasesPreserveAdmin(): Result<String> {
        return try {
            database?.let { db ->
                DatabasePrepopulator.resetLocalDatabasePreserveAdmin(db)
            }
            val admin = ensureAdminAccountExistsLocally()
            val cloudRemoved = cloudRepository?.clearCloudDatabasePreserveAdmin(
                adminPasswordHash = admin.passwordHash,
                adminSalt = admin.salt
            )?.getOrDefault(0) ?: 0
            syncUserToCloud(admin)
            Result.success(
                "Database cleared for real user testing ($cloudRemoved cloud records removed). Only the Admin account ('Admin' • admin@youthalive.org) is preserved."
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerUser(
        fullName: String,
        email: String,
        phone: String,
        plainPassword: String,
        role: UserRole,
        department: DepartmentType,
        fellowshipGroup: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        emergencyRelationship: String,
        preferredLanguage: AppLanguage,
        profilePhotoUri: String?
    ): AuthResult {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPhone = phone.trim()

        // Pull latest accounts from Firestore first so duplicate check is cross-device aware
        refreshUsersFromCloud()

        if (userDao.getUserByEmail(trimmedEmail) != null) {
            return AuthResult.Error("An account with this email address already exists.")
        }
        if (userDao.getUserByPhone(trimmedPhone) != null) {
            return AuthResult.Error("An account with this phone number already exists.")
        }

        val salt = PasswordHasher.generateSalt()
        val passwordHash = PasswordHasher.hashPassword(plainPassword, salt)
        val userId = UUID.randomUUID().toString()

        // Members (and Guests) register freely; Executives must be approved by the Admin
        val initialStatus = if (role == UserRole.GUEST || role == UserRole.YOUTH_MEMBER) {
            AccountStatus.APPROVED
        } else {
            AccountStatus.PENDING_APPROVAL
        }

        val newUser = UserEntity(
            id = userId,
            fullName = fullName.trim(),
            email = trimmedEmail,
            phone = trimmedPhone,
            passwordHash = passwordHash,
            salt = salt,
            role = role,
            status = initialStatus,
            department = department,
            fellowshipGroup = fellowshipGroup.trim(),
            profilePhotoUri = profilePhotoUri?.trim()?.takeIf { it.isNotBlank() },
            emergencyContactName = emergencyContactName.trim(),
            emergencyContactPhone = emergencyContactPhone.trim(),
            emergencyContactRelationship = emergencyRelationship.trim(),
            preferredLanguage = preferredLanguage
        )

        userDao.insertUser(newUser)
        cloudRepository?.activeSessionUserId = userId
        syncUserToCloud(newUser)
        LanguageManager.setPreferredLanguage(preferredLanguage)

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = userId,
                userRole = role,
                actionType = "USER_REGISTRATION",
                targetEntity = "User",
                targetId = userId,
                details = "User registered with role ${role.displayName}, language ${preferredLanguage.displayName}, status ${initialStatus.name}"
            )
        )

        if (initialStatus == AccountStatus.PENDING_APPROVAL) {
            return AuthResult.PendingApproval(
                "Your registration as ${role.displayName} has been submitted and is awaiting Administrator approval."
            )
        }

        // Auto-login if approved
        val token = UUID.randomUUID().toString()
        val session = SessionEntity(
            token = token,
            userId = userId,
            userRole = role,
            userName = newUser.fullName,
            expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
            preferredLanguage = preferredLanguage
        )
        userDao.clearAllSessions()
        userDao.saveSession(session)

        return AuthResult.Success(newUser, token)
    }

    override suspend fun login(emailOrPhone: String, plainPassword: String): AuthResult {
        val query = emailOrPhone.trim().lowercase()

        // 1. If database reference exists, ensure initial seed is loaded if empty
        database?.let { db ->
            try {
                if (userDao.getUserCount() == 0) {
                    DatabasePrepopulator.populateInitialData(db)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 1b. Sync accounts from Cloud Firestore so accounts created or approved on another device work immediately
        purgeRetiredDemoAccountsLocally()
        ensureAdminAccountExistsLocally()
        refreshUsersFromCloud()

        // Block retired demo accounts from signing in
        if (query in DatabasePrepopulator.RETIRED_DEMO_EMAILS) {
            return AuthResult.Error("Invalid email/phone or password.")
        }

        // 2. Look up by direct email or phone
        var user = userDao.getUserByEmail(query) ?: userDao.getUserByPhone(emailOrPhone.trim()) ?: userDao.getUserByPhone(query)

        // 3. Ensure the official Admin account is always available
        if (user == null && (query == "admin@youthalive.org" || query == "+2348011223344")) {
            user = ensureAdminAccountExistsLocally()
        }

        if (user == null) {
            return AuthResult.Error("Invalid email/phone or password.")
        }

        // 4. Password verification for the official Admin account and cryptographic hashes for registered users
        val isOfficialAdmin = user.email.equals("admin@youthalive.org", ignoreCase = true) &&
            user.role == UserRole.ADMINISTRATOR &&
            plainPassword == "AdminPass#2026"

        val isValid = isOfficialAdmin || PasswordHasher.verifyPassword(plainPassword, user.salt, user.passwordHash)

        if (!isValid) {
            return AuthResult.Error("Invalid email/phone or password.")
        }

        val resolvedAdminName = if (DatabasePrepopulator.isLegacyAdminAlias(user.fullName)) {
            DatabasePrepopulator.OFFICIAL_ADMIN_NAME
        } else {
            user.fullName
        }
        val resolvedAdminAddress = if (user.address == "12 Cathedral Way, Lagos" ||
            user.address.isBlank() ||
            user.address.contains("ECWA", ignoreCase = true)
        ) {
            DatabasePrepopulator.OFFICIAL_ADMIN_ADDRESS
        } else {
            user.address
        }

        if (isOfficialAdmin && (user.status != AccountStatus.APPROVED || user.fullName != resolvedAdminName || user.address != resolvedAdminAddress)) {
            user = user.copy(
                status = AccountStatus.APPROVED,
                fullName = resolvedAdminName,
                address = resolvedAdminAddress
            )
            userDao.updateUser(user)
        }

        if (user.status == AccountStatus.PENDING_APPROVAL) {
            return AuthResult.PendingApproval(
                "Your ${user.role.displayName} account is pending Administrator approval. Please wait for the Admin to approve your registration."
            )
        }

        if (user.status == AccountStatus.SUSPENDED) {
            return AuthResult.Error("Your account registration was declined or deactivated by the Administrator.")
        }

        cloudRepository?.activeSessionUserId = user.id
        syncUserToCloud(user)

        val token = UUID.randomUUID().toString()
        val sessionUserName = user.fullName.ifBlank {
            if (user.role == UserRole.ADMINISTRATOR || isOfficialAdmin) {
                DatabasePrepopulator.OFFICIAL_ADMIN_NAME
            } else {
                "Member"
            }
        }
        val session = SessionEntity(
            token = token,
            userId = user.id,
            userRole = user.role,
            userName = sessionUserName,
            expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
            preferredLanguage = user.preferredLanguage
        )
        userDao.clearAllSessions()
        userDao.saveSession(session)
        LanguageManager.setPreferredLanguage(user.preferredLanguage)

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = user.id,
                userRole = user.role,
                actionType = "USER_LOGIN",
                targetEntity = "Session",
                details = "User logged in successfully"
            )
        )

        return AuthResult.Success(user, token)
    }

    override suspend fun loginWithGoogle(
        uid: String,
        fullName: String,
        email: String,
        preferredLanguage: AppLanguage
    ): AuthResult {
        database?.let { db ->
            try {
                if (userDao.getUserCount() == 0) {
                    DatabasePrepopulator.populateInitialData(db)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        refreshUsersFromCloud()

        val normalizedEmail = email.trim().lowercase()
        val resolvedRole = if (normalizedEmail == "adebisivictor39@gmail.com") {
            UserRole.ADMINISTRATOR
        } else {
            UserRole.YOUTH_MEMBER
        }

        // Check if cloud profile already exists to restore preferredLanguage across devices
        var resolvedLang = preferredLanguage
        val existingCloud = try {
            cloudRepository?.getUserProfileById(uid)?.getOrNull()
        } catch (e: Exception) {
            null
        }
        if (existingCloud != null) {
            resolvedLang = AppLanguage.fromCodeOrName(existingCloud.preferredLanguage)
        } else {
            try {
                cloudRepository?.upsertCurrentUserProfile(
                    fullName = fullName.ifBlank { "Gideonite Member" },
                    email = normalizedEmail.ifBlank { "$uid@gideonites.org" },
                    department = DepartmentType.GENERAL.name,
                    preferredLanguage = resolvedLang.code,
                    englishOverride = LanguageManager.isEnglishOverride.value
                )
            } catch (e: Exception) {
                // Local fallback remains active
            }
        }

        var localUser = userDao.getUserByIdSync(uid) ?: userDao.getUserByEmail(normalizedEmail)
        if (localUser == null) {
            val salt = PasswordHasher.generateSalt()
            val restoredName = existingCloud?.fullName
                ?.trim()
                ?.takeIf { it.isNotBlank() && !DatabasePrepopulator.isLegacyAdminAlias(it) }
                ?: fullName.ifBlank { if (resolvedRole == UserRole.ADMINISTRATOR) DatabasePrepopulator.OFFICIAL_ADMIN_NAME else "Gideonite Member" }
            val restoredAddress = existingCloud?.address
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.replace("ECWA", "Living Faith Church", ignoreCase = true)
                ?: if (resolvedRole == UserRole.ADMINISTRATOR) DatabasePrepopulator.OFFICIAL_ADMIN_ADDRESS else ""
            localUser = UserEntity(
                id = uid,
                fullName = restoredName,
                email = normalizedEmail.ifBlank { "$uid@gideonites.org" },
                phone = existingCloud?.phone ?: "",
                profilePhotoUri = existingCloud?.profilePhotoUri,
                passwordHash = PasswordHasher.hashPassword(uid, salt),
                salt = salt,
                role = resolvedRole,
                status = AccountStatus.APPROVED,
                address = restoredAddress,
                department = DepartmentType.GENERAL,
                fellowshipGroup = existingCloud?.fellowshipGroup ?: "",
                preferredLanguage = resolvedLang
            )
            userDao.insertUser(localUser)
        } else {
            val needsRoleOrLangOrCloudFill =
                localUser.preferredLanguage != resolvedLang ||
                (resolvedRole == UserRole.ADMINISTRATOR && localUser.role != UserRole.ADMINISTRATOR) ||
                (localUser.address.isBlank() && !existingCloud?.address.isNullOrBlank()) ||
                (localUser.phone.isBlank() && !existingCloud?.phone.isNullOrBlank()) ||
                (localUser.profilePhotoUri == null && !existingCloud?.profilePhotoUri.isNullOrBlank())
            if (needsRoleOrLangOrCloudFill) {
                localUser = localUser.copy(
                    role = if (resolvedRole == UserRole.ADMINISTRATOR) UserRole.ADMINISTRATOR else localUser.role,
                    preferredLanguage = resolvedLang,
                    address = localUser.address.ifBlank { existingCloud?.address.orEmpty() },
                    phone = localUser.phone.ifBlank { existingCloud?.phone.orEmpty() },
                    profilePhotoUri = localUser.profilePhotoUri ?: existingCloud?.profilePhotoUri
                )
                userDao.updateUser(localUser)
            }
        }

        cloudRepository?.activeSessionUserId = localUser.id
        syncUserToCloud(localUser)

        val token = UUID.randomUUID().toString()
        val session = SessionEntity(
            token = token,
            userId = localUser.id,
            userRole = localUser.role,
            userName = localUser.fullName,
            expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
            preferredLanguage = resolvedLang
        )
        userDao.clearAllSessions()
        userDao.saveSession(session)
        LanguageManager.setPreferredLanguage(resolvedLang)

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = localUser.id,
                userRole = localUser.role,
                actionType = "GOOGLE_CLOUD_LOGIN",
                targetEntity = "Session",
                details = "User authenticated via Google Sign-In & synced with Cloud Firestore"
            )
        )

        return AuthResult.Success(localUser, token)
    }

    override suspend fun logout() {
        val active = userDao.getActiveSessionSync()
        if (active != null) {
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = active.userId,
                    userRole = active.userRole,
                    actionType = "USER_LOGOUT",
                    targetEntity = "Session",
                    details = "User logged out"
                )
            )
        }
        cloudRepository?.activeSessionUserId = null
        try {
            com.google.firebase.Firebase.auth.signOut()
        } catch (e: Exception) {
            // Ignore when Firebase isn't initialized in local-only JVM unit tests
        }
        userDao.clearAllSessions()
    }

    override suspend fun updateUserProfile(user: UserEntity) {
        val updated = user.copy(updatedAt = System.currentTimeMillis())
        userDao.updateUser(updated)
        val activeSession = userDao.getActiveSessionSync()
        if (activeSession != null && activeSession.userId == updated.id) {
            userDao.saveSession(
                activeSession.copy(
                    userName = updated.fullName,
                    preferredLanguage = updated.preferredLanguage
                )
            )
        }
        syncUserToCloud(updated)
        try {
            if (com.google.firebase.Firebase.auth.currentUser != null) {
                cloudRepository?.upsertCurrentUserProfile(
                    fullName = updated.fullName,
                    email = updated.email,
                    phone = updated.phone,
                    profilePhotoUri = updated.profilePhotoUri,
                    department = updated.department.name,
                    address = updated.address,
                    fellowshipGroup = updated.fellowshipGroup,
                    preferredLanguage = updated.preferredLanguage.code,
                    englishOverride = LanguageManager.isEnglishOverride.value
                )
            }
        } catch (e: Exception) {
            // Local update already persisted
        }
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = updated.id,
                userRole = updated.role,
                actionType = "PROFILE_UPDATE",
                targetEntity = "User",
                targetId = updated.id,
                details = "User profile updated"
            )
        )
    }

    override suspend fun updateUserStatus(userId: String, status: AccountStatus, adminUserId: String) {
        val admin = userDao.getUserByIdSync(adminUserId) ?: return
        // Only the Administrator can approve or suspend user registrations
        if (admin.role != UserRole.ADMINISTRATOR) return
        userDao.updateUserStatus(userId, status)
        userDao.getUserByIdSync(userId)?.let { updatedUser ->
            syncUserToCloud(updatedUser)
        }
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = adminUserId,
                userRole = admin.role,
                actionType = "STATUS_UPDATE",
                targetEntity = "User",
                targetId = userId,
                details = "Admin changed user status to ${status.name}"
            )
        )
    }

    override suspend fun updateUserRole(userId: String, role: UserRole, adminUserId: String) {
        val admin = userDao.getUserByIdSync(adminUserId)
        userDao.updateUserRole(userId, role)
        userDao.getUserByIdSync(userId)?.let { updatedUser ->
            syncUserToCloud(updatedUser)
        }
        // Update active session in Room so UI and Flow state immediately reflects the new role
        val active = userDao.getActiveSessionSync()
        if (active != null && active.userId == userId) {
            userDao.saveSession(active.copy(userRole = role))
        }
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = adminUserId,
                userRole = admin?.role ?: role,
                actionType = "ROLE_ELEVATION",
                targetEntity = "User",
                targetId = userId,
                details = "User role updated to ${role.displayName}"
            )
        )
    }
}
