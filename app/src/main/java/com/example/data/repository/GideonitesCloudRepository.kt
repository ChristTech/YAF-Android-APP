package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.remote.CloudCommunityPost
import com.example.data.remote.CloudDailyScripture
import com.example.data.remote.CloudExecutiveInquiry
import com.example.data.remote.CloudFellowshipNotification
import com.example.data.remote.CloudGameQuiz
import com.example.data.remote.CloudGameScore
import com.example.data.remote.CloudQuizSession
import com.example.data.remote.CloudSermonAudioChunk
import com.example.data.remote.CloudUserProfile
import com.example.data.remote.CloudYouthProgramme
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GideonitesCloudRepository(
    private val db: FirebaseFirestore
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    @Volatile
    var activeSessionUserId: String? = null

    private fun requireUserId(fallbackUserId: String? = null): String {
        val firebaseUid = try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
        return firebaseUid
            ?: fallbackUserId?.takeIf { it.isNotBlank() }
            ?: activeSessionUserId?.takeIf { it.isNotBlank() }
            ?: "gideonite_member"
    }

    private suspend fun fetchFromServerOrCache(query: Query): QuerySnapshot {
        return try {
            query.get(Source.SERVER).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            query.get(Source.DEFAULT).await()
        }
    }

    suspend fun fetchFellowshipNotificationsOnce(): List<CloudFellowshipNotification> {
        return try {
            val snap = fetchFromServerOrCache(
                db.collection(COLLECTION_FELLOWSHIP_NOTIFICATIONS)
                    .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            )
            snap.documents.mapNotNull { doc ->
                runCatching {
                    doc.toObject(
                        CloudFellowshipNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.getOrNull()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchYouthProgrammesOnce(): List<CloudYouthProgramme> {
        return try {
            val snap = fetchFromServerOrCache(
                db.collection(COLLECTION_PROGRAMMES)
                    .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            )
            snap.documents.mapNotNull { doc ->
                runCatching {
                    doc.toObject(
                        CloudYouthProgramme::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.getOrNull()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchCommunityPostsOnce(): List<CloudCommunityPost> {
        return try {
            val snap = fetchFromServerOrCache(
                db.collection(COLLECTION_COMMUNITY_POSTS)
                    .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            )
            snap.documents.mapNotNull { doc ->
                runCatching {
                    doc.toObject(
                        CloudCommunityPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.getOrNull()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchCloudQuizSessionsOnce(): List<CloudQuizSession> {
        return try {
            val snap = fetchFromServerOrCache(
                db.collection(COLLECTION_QUIZ_SESSIONS)
                    .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            )
            snap.documents.mapNotNull { doc ->
                runCatching {
                    doc.toObject(
                        CloudQuizSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.getOrNull()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchDailyScripturesOnce(): List<CloudDailyScripture> {
        return try {
            val snap = fetchFromServerOrCache(
                db.collection(COLLECTION_DAILY_SCRIPTURES)
                    .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            )
            snap.documents.mapNotNull { doc ->
                runCatching {
                    doc.toObject(
                        CloudDailyScripture::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }.getOrNull()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ==========================================
    // 1. User Profiles (/users/{userId}) & Fellowship Accounts (/fellowship_accounts/{accountId})
    // ==========================================

    suspend fun upsertFellowshipAccount(
        userId: String,
        fullName: String,
        email: String,
        phone: String? = null,
        passwordHash: String? = null,
        salt: String? = null,
        status: String = "APPROVED",
        profilePhotoUri: String? = null,
        role: String = "YOUTH_MEMBER",
        department: String = "GENERAL",
        address: String? = null,
        fellowshipGroup: String? = null,
        emergencyContactName: String? = null,
        emergencyContactPhone: String? = null,
        emergencyContactRelationship: String? = null,
        preferredLanguage: String = "en",
        englishOverride: Boolean = false,
        xpPoints: Long = 0L
    ): Result<CloudUserProfile> {
        val cleanId = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(120).ifBlank {
            "usr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val docRef = db.collection(COLLECTION_FELLOWSHIP_ACCOUNTS).document(cleanId)
        val profile = CloudUserProfile(
            id = cleanId,
            userId = cleanId,
            fullName = fullName.ifBlank { "Gideonite Member" },
            email = email.ifBlank { "$cleanId@gideonites.org" },
            phone = phone?.takeIf { it.isNotBlank() },
            passwordHash = passwordHash?.takeIf { it.isNotBlank() },
            salt = salt?.takeIf { it.isNotBlank() },
            status = status.ifBlank { "APPROVED" },
            profilePhotoUri = profilePhotoUri?.takeIf { it.isNotBlank() },
            role = role.ifBlank { "YOUTH_MEMBER" },
            department = department.ifBlank { "GENERAL" },
            address = address?.takeIf { it.isNotBlank() }?.take(250),
            fellowshipGroup = fellowshipGroup?.takeIf { it.isNotBlank() },
            emergencyContactName = emergencyContactName?.takeIf { it.isNotBlank() },
            emergencyContactPhone = emergencyContactPhone?.takeIf { it.isNotBlank() },
            emergencyContactRelationship = emergencyContactRelationship?.takeIf { it.isNotBlank() },
            preferredLanguage = preferredLanguage.ifBlank { "en" },
            englishOverride = englishOverride,
            xpPoints = xpPoints
        )
        return try {
            val payload = profile.toCreateMap().toMutableMap()
            payload["fellowshipId"] = FELLOWSHIP_ID
            docRef.set(payload).await()
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchFellowshipAccounts(): Result<List<CloudUserProfile>> {
        return try {
            val snapshot = db.collection(COLLECTION_FELLOWSHIP_ACCOUNTS)
                .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
                .get()
                .await()
            val accounts = snapshot.toObjects(
                CloudUserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).filter { !it.status.equals("PURGED", ignoreCase = true) }
            Result.success(accounts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_FELLOWSHIP_ACCOUNTS)
            Result.failure(e)
        }
    }

    fun observeFellowshipAccounts(): Flow<List<CloudUserProfile>> =
        db.collection(COLLECTION_FELLOWSHIP_ACCOUNTS)
            .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudUserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }.filter { !it.status.equals("PURGED", ignoreCase = true) }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_FELLOWSHIP_ACCOUNTS)
                }
                throw error
            }

    suspend fun upsertCurrentUserProfile(
        fullName: String,
        email: String,
        phone: String? = null,
        profilePhotoUri: String? = null,
        department: String = "GENERAL",
        address: String? = null,
        fellowshipGroup: String? = null,
        preferredLanguage: String = "en",
        englishOverride: Boolean = false,
        xpPoints: Long = 0L
    ): Result<CloudUserProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users")
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = mutableMapOf<String, Any>(
                    "fullName" to fullName,
                    "department" to department,
                    "preferredLanguage" to preferredLanguage,
                    "englishOverride" to englishOverride,
                    "xpPoints" to xpPoints,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val cleanPhone = phone?.trim()?.takeIf { it.isNotBlank() }?.take(30)
                if (cleanPhone != null) {
                    updatePayload["phone"] = cleanPhone
                } else if (existingSnap.contains("phone")) {
                    updatePayload["phone"] = FieldValue.delete()
                }
                val cleanPhoto = profilePhotoUri?.trim()?.takeIf { it.isNotBlank() }
                if (cleanPhoto != null) {
                    updatePayload["profilePhotoUri"] = cleanPhoto
                } else if (existingSnap.contains("profilePhotoUri")) {
                    updatePayload["profilePhotoUri"] = FieldValue.delete()
                }
                val cleanAddress = address?.trim()?.takeIf { it.isNotBlank() }?.take(250)
                if (cleanAddress != null) {
                    updatePayload["address"] = cleanAddress
                } else if (existingSnap.contains("address")) {
                    updatePayload["address"] = FieldValue.delete()
                }
                val cleanGroup = fellowshipGroup?.trim()?.takeIf { it.isNotBlank() }?.take(100)
                if (cleanGroup != null) {
                    updatePayload["fellowshipGroup"] = cleanGroup
                } else if (existingSnap.contains("fellowshipGroup")) {
                    updatePayload["fellowshipGroup"] = FieldValue.delete()
                }
                docRef.update(updatePayload).await()
            } else {
                val profile = CloudUserProfile(
                    id = uid,
                    userId = uid,
                    fullName = fullName,
                    email = email,
                    phone = phone?.takeIf { it.isNotBlank() },
                    profilePhotoUri = profilePhotoUri?.takeIf { it.isNotBlank() },
                    role = "YOUTH_MEMBER",
                    department = department,
                    address = address?.takeIf { it.isNotBlank() }?.take(250),
                    fellowshipGroup = fellowshipGroup?.takeIf { it.isNotBlank() },
                    preferredLanguage = preferredLanguage,
                    englishOverride = englishOverride,
                    xpPoints = xpPoints
                )
                docRef.set(profile.toCreateMap()).await()
            }
            val updated = docRef.get().await().toObject(
                CloudUserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: CloudUserProfile(
                id = uid,
                userId = uid,
                fullName = fullName,
                email = email,
                phone = phone,
                profilePhotoUri = profilePhotoUri,
                address = address,
                preferredLanguage = preferredLanguage,
                englishOverride = englishOverride
            )
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfileById(targetUserId: String): Result<CloudUserProfile?> {
        val docRef = db.collection(COLLECTION_USERS).document(targetUserId)
        return try {
            val snap = docRef.get().await()
            val profile = snap.toObject(
                CloudUserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCurrentUserProfile(): Flow<CloudUserProfile?> = flow {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(
                        CloudUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun updateLanguagePreference(
        preferredLanguage: String,
        englishOverride: Boolean
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users")
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            docRef.update(
                mapOf(
                    "preferredLanguage" to preferredLanguage,
                    "englishOverride" to englishOverride,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // ==========================================
    // 2. Daily Scriptures (/daily_scriptures/{scriptureId})
    // ==========================================

    suspend fun publishDailyScripture(
        scriptureId: String? = null,
        authorName: String,
        dateKey: String,
        referenceEn: String,
        verseTextEn: String,
        reflectionEn: String,
        prayerPromptEn: String,
        referenceYo: String? = null,
        verseTextYo: String? = null,
        reflectionYo: String? = null,
        referenceHa: String? = null,
        verseTextHa: String? = null,
        reflectionHa: String? = null,
        referenceIg: String? = null,
        verseTextIg: String? = null,
        reflectionIg: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_DAILY_SCRIPTURES)
            return Result.failure(e)
        }
        val cleanId = scriptureId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "")?.takeIf { it.isNotBlank() }
        val docId = if (cleanId != null) {
            if (cleanId.startsWith("scripture_")) cleanId.take(64) else "scripture_${cleanId.take(50)}"
        } else {
            "scripture_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val docRef = db.collection(COLLECTION_DAILY_SCRIPTURES).document(docId)
        val scripture = CloudDailyScripture(
            id = docId,
            authorId = uid.take(128),
            authorName = authorName.trim().ifBlank { "Youth Leadership" }.take(100),
            fellowshipId = FELLOWSHIP_ID,
            dateKey = dateKey.trim().ifBlank { "2026-10-25" }.take(40),
            referenceEn = referenceEn.trim().ifBlank { "Judges 6:12" }.take(120),
            referenceYo = referenceYo?.take(120),
            referenceHa = referenceHa?.take(120),
            referenceIg = referenceIg?.take(120),
            verseTextEn = verseTextEn.trim().ifBlank { "The Lord is with thee, thou mighty man of valour." }.take(2000),
            verseTextYo = verseTextYo?.take(2000),
            verseTextHa = verseTextHa?.take(2000),
            verseTextIg = verseTextIg?.take(2000),
            reflectionEn = reflectionEn.trim().ifBlank { "Daily charge for The Gideonites." }.take(3000),
            reflectionYo = reflectionYo?.take(3000),
            reflectionHa = reflectionHa?.take(3000),
            reflectionIg = reflectionIg?.take(3000),
            prayerPromptEn = prayerPromptEn.trim().ifBlank { "Lord, strengthen The Gideonites in word and deed." }.take(1000)
        )
        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                docRef.set(scripture.toCreateMap()).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "referenceEn" to scripture.referenceEn,
                    "verseTextEn" to scripture.verseTextEn,
                    "reflectionEn" to scripture.reflectionEn,
                    "prayerPromptEn" to scripture.prayerPromptEn,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                scripture.referenceYo?.let { updates["referenceYo"] = it }
                scripture.referenceHa?.let { updates["referenceHa"] = it }
                scripture.referenceIg?.let { updates["referenceIg"] = it }
                scripture.verseTextYo?.let { updates["verseTextYo"] = it }
                scripture.verseTextHa?.let { updates["verseTextHa"] = it }
                scripture.verseTextIg?.let { updates["verseTextIg"] = it }
                scripture.reflectionYo?.let { updates["reflectionYo"] = it }
                scripture.reflectionHa?.let { updates["reflectionHa"] = it }
                scripture.reflectionIg?.let { updates["reflectionIg"] = it }
                docRef.update(updates).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDailyScripture(scriptureId: String): Result<Unit> {
        val cleanId = scriptureId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid scriptureId"))
        val docId = if (cleanId.startsWith("scripture_")) cleanId.take(64) else "scripture_${cleanId.take(50)}"
        val docRef = db.collection(COLLECTION_DAILY_SCRIPTURES).document(docId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeDailyScriptures(): Flow<List<CloudDailyScripture>> =
        db.collection(COLLECTION_DAILY_SCRIPTURES)
            .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudDailyScripture::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_DAILY_SCRIPTURES)
                }
                throw error
            }

    // ==========================================
    // 3. Youth Programmes (/programmes/{programmeId})
    // ==========================================

    private fun mergeRegistrationsJson(existingJson: String?, incomingJson: String?): Pair<String?, Int> {
        val byUserOrId = LinkedHashMap<String, org.json.JSONObject>()
        fun parseInto(raw: String?) {
            if (raw.isNullOrBlank()) return
            try {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val userId = obj.optString("userId").trim()
                    val regId = obj.optString("id").trim()
                    val key = userId.ifBlank { regId }
                    if (key.isNotBlank()) {
                        byUserOrId[key] = obj
                    }
                }
            } catch (_: Exception) {
            }
        }
        parseInto(existingJson)
        parseInto(incomingJson)
        if (byUserOrId.isEmpty()) return null to 0
        val out = org.json.JSONArray()
        for (obj in byUserOrId.values) {
            out.put(obj)
        }
        return out.toString() to byUserOrId.size
    }

    suspend fun publishYouthProgramme(
        programmeId: String? = null,
        createdById: String? = null,
        titleEn: String,
        descriptionEn: String,
        category: String,
        venue: String,
        speaker: String? = null,
        contactPerson: String? = null,
        participantLimit: Long? = null,
        isRegistrationEnabled: Boolean? = null,
        startDateMillis: Long,
        endDateMillis: Long,
        status: String = "UPCOMING",
        registeredCount: Long = 0L,
        registrationsJson: String? = null,
        titleYo: String? = null,
        descriptionYo: String? = null,
        titleHa: String? = null,
        descriptionHa: String? = null,
        titleIg: String? = null,
        descriptionIg: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId(createdById)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_PROGRAMMES)
            return Result.failure(e)
        }
        val cleanId = programmeId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "")?.takeIf { it.isNotBlank() }
        val docId = if (cleanId != null) {
            if (cleanId.startsWith("prog_")) cleanId.take(64) else "prog_${cleanId.take(56)}"
        } else {
            "prog_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val docRef = db.collection(COLLECTION_PROGRAMMES).document(docId)
        val safeEndMillis = if (endDateMillis >= startDateMillis) endDateMillis else startDateMillis + 7_200_000L
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingProg = existingSnap.toObject(
                    CloudYouthProgramme::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val (mergedJson, mergedSize) = mergeRegistrationsJson(existingProg?.registrationsJson, registrationsJson)
                val mergedCount = maxOf(
                    existingProg?.registeredCount ?: 0L,
                    registeredCount,
                    mergedSize.toLong()
                )
                if (existingProg != null && existingProg.createdById != uid && auth.currentUser != null) {
                    // Only update registration fields if current Firebase Auth user is not the creator
                    val regUpdates = mutableMapOf<String, Any>(
                        "registeredCount" to mergedCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!mergedJson.isNullOrBlank()) {
                        regUpdates["registrationsJson"] = mergedJson
                    }
                    docRef.update(regUpdates).await()
                } else {
                    val updates = mutableMapOf<String, Any>(
                        "titleEn" to titleEn.ifBlank { existingProg?.titleEn ?: "Youth Programme" },
                        "descriptionEn" to descriptionEn.ifBlank { existingProg?.descriptionEn ?: "Youth Alive Fellowship Programme" },
                        "category" to category.ifBlank { existingProg?.category ?: "FELLOWSHIP" },
                        "venue" to venue.ifBlank { existingProg?.venue ?: "Main Sanctuary" },
                        "startDateMillis" to startDateMillis.coerceAtLeast(1000L),
                        "endDateMillis" to safeEndMillis.coerceAtLeast(1000L),
                        "status" to status,
                        "registeredCount" to mergedCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!speaker.isNullOrBlank()) updates["speaker"] = speaker
                    if (!contactPerson.isNullOrBlank()) updates["contactPerson"] = contactPerson
                    if (participantLimit != null) updates["participantLimit"] = participantLimit
                    if (isRegistrationEnabled != null) updates["isRegistrationEnabled"] = isRegistrationEnabled
                    if (!mergedJson.isNullOrBlank()) updates["registrationsJson"] = mergedJson
                    docRef.update(updates).await()
                }
            } else {
                val (mergedJson, mergedSize) = mergeRegistrationsJson(null, registrationsJson)
                val programme = CloudYouthProgramme(
                    id = docId,
                    createdById = uid,
                    fellowshipId = FELLOWSHIP_ID,
                    titleEn = titleEn.ifBlank { "Youth Programme" },
                    titleYo = titleYo,
                    titleHa = titleHa,
                    titleIg = titleIg,
                    descriptionEn = descriptionEn.ifBlank { "Youth Alive Fellowship Programme" },
                    descriptionYo = descriptionYo,
                    descriptionHa = descriptionHa,
                    descriptionIg = descriptionIg,
                    category = category.ifBlank { "FELLOWSHIP" },
                    venue = venue.ifBlank { "Main Sanctuary" },
                    speaker = speaker,
                    contactPerson = contactPerson,
                    participantLimit = participantLimit,
                    isRegistrationEnabled = isRegistrationEnabled,
                    startDateMillis = startDateMillis.coerceAtLeast(1000L),
                    endDateMillis = safeEndMillis.coerceAtLeast(1000L),
                    status = status,
                    registeredCount = maxOf(registeredCount, mergedSize.toLong()),
                    registrationsJson = mergedJson
                )
                docRef.set(programme.toCreateMap()).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateYouthProgrammeRegistration(
        programmeId: String,
        registeredCount: Long,
        registrationsJson: String
    ): Result<Unit> {
        val cleanId = programmeId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid programmeId"))
        val docId = if (cleanId.startsWith("prog_")) cleanId.take(64) else "prog_${cleanId.take(56)}"
        val docRef = db.collection(COLLECTION_PROGRAMMES).document(docId)
        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                return Result.failure(IllegalStateException("Programme document does not exist in Firestore yet"))
            }
            val existingProg = existingSnap.toObject(
                CloudYouthProgramme::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val (mergedJson, mergedSize) = mergeRegistrationsJson(existingProg?.registrationsJson, registrationsJson)
            val finalCount = maxOf(
                (existingProg?.registeredCount ?: 0L) + 1L,
                registeredCount,
                mergedSize.toLong()
            )
            val updates = mutableMapOf<String, Any>(
                "registeredCount" to finalCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!mergedJson.isNullOrBlank()) {
                updates["registrationsJson"] = mergedJson
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteYouthProgramme(programmeId: String): Result<Unit> {
        val cleanId = programmeId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid programmeId"))
        val docId = if (cleanId.startsWith("prog_")) cleanId.take(64) else "prog_${cleanId.take(56)}"
        val docRef = db.collection(COLLECTION_PROGRAMMES).document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeYouthProgrammes(): Flow<List<CloudYouthProgramme>> =
        db.collection(COLLECTION_PROGRAMMES)
            .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudYouthProgramme::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_PROGRAMMES)
                }
                throw error
            }

    // ==========================================
    // 4. Live Quiz Sessions (/quiz_sessions/{sessionId})
    // ==========================================

    suspend fun createCloudQuizSession(
        sessionId: String? = null,
        hostId: String? = null,
        hostName: String,
        roomPin: String,
        titleEn: String,
        titleYo: String? = null,
        titleHa: String? = null,
        titleIg: String? = null,
        description: String? = null,
        gameType: String = "BIBLE_QUIZ",
        status: String = "ACTIVE",
        scheduledStartTime: Long? = null,
        timeLimitPerQuestionSeconds: Long? = null,
        leaderboardDurationSeconds: Long? = null,
        currentQuestionIndex: Long = 0L,
        currentSubstate: String? = null,
        currentQuestionDeadline: Long? = null,
        leaderboardDeadline: Long? = null,
        baseScorePerQuestion: Long? = null,
        speedBonusEnabled: Boolean? = null,
        maxSpeedBonus: Long? = null,
        penaltyForIncorrect: Boolean? = null,
        totalQuestions: Int = 5,
        questionsJson: String? = null,
        participantsJson: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId(hostId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_QUIZ_SESSIONS)
            return Result.failure(e)
        }
        val cleanId = sessionId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "")?.takeIf { it.isNotBlank() }
        val docId = if (cleanId != null) {
            if (cleanId.startsWith("quiz_")) cleanId.take(64) else "quiz_${cleanId.take(56)}"
        } else {
            "quiz_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        val docRef = db.collection(COLLECTION_QUIZ_SESSIONS).document(docId)
        val safeTitleEn = titleEn.trim().ifBlank { "Fellowship Quiz" }.take(150)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingSession = existingSnap.toObject(
                    CloudQuizSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val existingStatus = existingSession?.status
                val isTerminal = existingStatus == "COMPLETED" || existingStatus == "CANCELLED"
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!isTerminal && (existingSession == null || existingSession.hostId == uid || auth.currentUser == null)) {
                    updates["titleEn"] = safeTitleEn
                    description?.let { updates["description"] = it.take(1000) }
                    updates["gameType"] = gameType.ifBlank { "BIBLE_QUIZ" }.take(50)
                    updates["status"] = status
                    scheduledStartTime?.let { updates["scheduledStartTime"] = it.coerceAtLeast(0L) }
                    timeLimitPerQuestionSeconds?.let { updates["timeLimitPerQuestionSeconds"] = it.coerceIn(1L, 600L) }
                    leaderboardDurationSeconds?.let { updates["leaderboardDurationSeconds"] = it.coerceIn(1L, 600L) }
                    updates["currentQuestionIndex"] = maxOf(existingSession?.currentQuestionIndex ?: 0L, currentQuestionIndex)
                    currentSubstate?.let { updates["currentSubstate"] = it }
                    currentQuestionDeadline?.let { updates["currentQuestionDeadline"] = it }
                    leaderboardDeadline?.let { updates["leaderboardDeadline"] = it }
                    baseScorePerQuestion?.let { updates["baseScorePerQuestion"] = it }
                    speedBonusEnabled?.let { updates["speedBonusEnabled"] = it }
                    maxSpeedBonus?.let { updates["maxSpeedBonus"] = it }
                    penaltyForIncorrect?.let { updates["penaltyForIncorrect"] = it }
                    updates["totalQuestions"] = totalQuestions.coerceIn(1, 500).toLong()
                    if (questionsJson != null) {
                        updates["questionsJson"] = questionsJson.take(100000)
                    }
                }
                if (participantsJson != null) {
                    val (mergedJson, mergedCount) = mergeParticipantsJson(
                        existingSession?.participantsJson,
                        participantsJson
                    )
                    if (mergedJson != null) {
                        updates["participantsJson"] = mergedJson.take(100000)
                        updates["participantCount"] = mergedCount.coerceAtLeast(1).toLong()
                    }
                }
                if (updates.size > 1) {
                    docRef.update(updates).await()
                }
            } else {
                val (cleanedJson, cleanedCount) = mergeParticipantsJson(null, participantsJson)
                val session = CloudQuizSession(
                    id = docId,
                    hostId = uid.take(128),
                    hostName = hostName.trim().ifBlank { "Youth Leadership" }.take(100),
                    fellowshipId = FELLOWSHIP_ID,
                    roomPin = roomPin.trim().ifBlank { "GID777" }.take(12).padEnd(4, '0'),
                    titleEn = safeTitleEn,
                    titleYo = titleYo?.take(150),
                    titleHa = titleHa?.take(150),
                    titleIg = titleIg?.take(150),
                    description = description?.take(1000),
                    gameType = gameType.ifBlank { "BIBLE_QUIZ" }.take(50),
                    status = status,
                    scheduledStartTime = scheduledStartTime,
                    timeLimitPerQuestionSeconds = timeLimitPerQuestionSeconds?.coerceIn(1L, 600L),
                    leaderboardDurationSeconds = leaderboardDurationSeconds?.coerceIn(1L, 600L),
                    currentQuestionIndex = currentQuestionIndex.coerceAtLeast(0L),
                    currentSubstate = currentSubstate,
                    currentQuestionDeadline = currentQuestionDeadline,
                    leaderboardDeadline = leaderboardDeadline,
                    baseScorePerQuestion = baseScorePerQuestion?.coerceIn(1L, 10000L),
                    speedBonusEnabled = speedBonusEnabled,
                    maxSpeedBonus = maxSpeedBonus?.coerceIn(0L, 5000L),
                    penaltyForIncorrect = penaltyForIncorrect,
                    totalQuestions = totalQuestions.coerceIn(1, 500).toLong(),
                    participantCount = cleanedCount.coerceAtLeast(1).toLong(),
                    questionsJson = questionsJson?.take(100000),
                    participantsJson = cleanedJson?.take(100000)
                )
                docRef.set(session.toCreateMap()).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun mergeParticipantsJson(
        existingJson: String?,
        incomingJson: String?
    ): Pair<String?, Int> {
        val byUserId = linkedMapOf<String, org.json.JSONObject>()

        fun ingest(rawJson: String?) {
            if (rawJson.isNullOrBlank()) return
            runCatching {
                val arr = org.json.JSONArray(rawJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val userId = obj.optString("userId", "").trim()
                    if (userId.isBlank() || userId.startsWith("peer-")) continue
                    val prev = byUserId[userId]
                    if (prev == null) {
                        byUserId[userId] = obj
                    } else {
                        val prevScore = prev.optInt("currentScore", 0)
                        val newScore = obj.optInt("currentScore", 0)
                        val prevAnswered = prev.optInt("answeredQuestionsCount", 0)
                        val newAnswered = obj.optInt("answeredQuestionsCount", 0)
                        val prevLastPts = prev.optInt("lastQuestionPoints", 0)
                        val newLastPts = obj.optInt("lastQuestionPoints", 0)
                        val prevTs = prev.optLong("lastAnswerTimestamp", 0L)
                        val newTs = obj.optLong("lastAnswerTimestamp", 0L)
                        val prevJoinedAt = prev.optLong("joinedAt", System.currentTimeMillis())
                        val newJoinedAt = obj.optLong("joinedAt", prevJoinedAt)

                        val merged = org.json.JSONObject()
                        merged.put("id", obj.optString("id", prev.optString("id", "part_$userId")))
                        merged.put("userId", userId)
                        merged.put("userName", obj.optString("userName", prev.optString("userName", "Member")))
                        merged.put("userRole", obj.optString("userRole", prev.optString("userRole", "Youth Member")))
                        merged.put("joinedAt", minOf(prevJoinedAt, newJoinedAt))
                        merged.put(
                            "joinedAtQuestionIndex",
                            minOf(prev.optInt("joinedAtQuestionIndex", 0), obj.optInt("joinedAtQuestionIndex", 0))
                        )
                        merged.put("currentScore", maxOf(prevScore, newScore))
                        merged.put(
                            "lastQuestionPoints",
                            if (newScore > prevScore || newAnswered > prevAnswered) newLastPts else prevLastPts
                        )
                        merged.put(
                            "lastAnswerTimestamp",
                            if (newScore > prevScore || newAnswered > prevAnswered) maxOf(prevTs, newTs) else prevTs.takeIf { it > 0L } ?: newTs
                        )
                        merged.put("answeredQuestionsCount", maxOf(prevAnswered, newAnswered))
                        byUserId[userId] = merged
                    }
                }
            }
        }

        ingest(existingJson)
        ingest(incomingJson)

        if (byUserId.isEmpty()) return Pair(null, 0)
        val out = org.json.JSONArray()
        byUserId.values.forEach { out.put(it) }
        return Pair(out.toString(), byUserId.size)
    }

    suspend fun updateCloudQuizSessionState(
        sessionId: String,
        status: String? = null,
        currentQuestionIndex: Long? = null,
        currentSubstate: String? = null,
        currentQuestionDeadline: Long? = null,
        leaderboardDeadline: Long? = null,
        participantCount: Long? = null,
        participantsJson: String? = null,
        scheduledStartTime: Long? = null
    ): Result<Unit> {
        val cleanId = sessionId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid sessionId"))
        val docId = if (cleanId.startsWith("quiz_")) cleanId.take(64) else "quiz_${cleanId.take(56)}"
        val docRef = db.collection(COLLECTION_QUIZ_SESSIONS).document(docId)
        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) return@runTransaction
                val existingParticipantsJson = snapshot.getString("participantsJson")
                val existingStatus = snapshot.getString("status")
                val existingQuestionIndex = snapshot.getLong("currentQuestionIndex") ?: 0L
                val isTerminal = existingStatus == "COMPLETED" || existingStatus == "CANCELLED"

                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!isTerminal) {
                    if (status != null) {
                        val shouldUpdateStatus = when (existingStatus) {
                            "COMPLETED", "CANCELLED" -> false
                            "ACTIVE" -> status == "ACTIVE" || status == "COMPLETED" || status == "CANCELLED"
                            else -> true
                        }
                        if (shouldUpdateStatus) {
                            updates["status"] = status
                        }
                    }
                    if (currentQuestionIndex != null) {
                        updates["currentQuestionIndex"] = maxOf(existingQuestionIndex, currentQuestionIndex)
                    }
                    if (currentSubstate != null) {
                        updates["currentSubstate"] = currentSubstate
                    }
                    if (currentQuestionDeadline != null) {
                        updates["currentQuestionDeadline"] = currentQuestionDeadline.coerceAtLeast(0L)
                    }
                    if (leaderboardDeadline != null) {
                        updates["leaderboardDeadline"] = leaderboardDeadline.coerceAtLeast(0L)
                    }
                    if (scheduledStartTime != null) {
                        updates["scheduledStartTime"] = scheduledStartTime.coerceAtLeast(0L)
                    }
                }
                if (participantsJson != null && existingStatus != "CANCELLED") {
                    val (mergedJson, mergedCount) = mergeParticipantsJson(existingParticipantsJson, participantsJson)
                    if (mergedJson != null) {
                        updates["participantsJson"] = mergedJson.take(100000)
                        updates["participantCount"] = mergedCount.coerceAtLeast(1).toLong()
                    }
                } else if (participantCount != null && existingStatus != "CANCELLED") {
                    val existingCount = snapshot.getLong("participantCount") ?: 1L
                    updates["participantCount"] = maxOf(existingCount, participantCount)
                }
                if (updates.size > 1) {
                    transaction.update(docRef, updates)
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudQuizSession(sessionId: String): Result<Unit> {
        val cleanId = sessionId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid sessionId"))
        val docId = if (cleanId.startsWith("quiz_")) cleanId.take(64) else "quiz_${cleanId.take(56)}"
        val docRef = db.collection(COLLECTION_QUIZ_SESSIONS).document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCloudQuizSessions(): Flow<List<CloudQuizSession>> =
        db.collection(COLLECTION_QUIZ_SESSIONS)
            .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudQuizSession::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_QUIZ_SESSIONS)
                }
                throw error
            }

    // ==========================================
    // 4b. Solo Game Quizzes (/game_quizzes/{quizId}) & Scores (/game_scores/{scoreId})
    // ==========================================

    suspend fun upsertGameQuiz(
        quizId: String,
        createdById: String? = null,
        title: String,
        description: String?,
        category: String,
        questionsJson: String,
        totalQuestions: Int,
        timeLimitSeconds: Int,
        difficulty: String
    ): Result<String> {
        val uid = try {
            requireUserId(createdById)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_GAME_QUIZZES)
            return Result.failure(e)
        }
        val cleanId = quizId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(120)
            .ifBlank { "gquiz_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection(COLLECTION_GAME_QUIZZES).document(cleanId)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "title" to title.trim().ifBlank { "Scripture Quiz" }.take(150),
                    "category" to category.ifBlank { "BIBLE_TRIVIA" }.take(50),
                    "questionsJson" to questionsJson.ifBlank { "[]" }.take(100000),
                    "totalQuestions" to totalQuestions.coerceIn(1, 500).toLong(),
                    "timeLimitSeconds" to timeLimitSeconds.coerceIn(10, 7200).toLong(),
                    "difficulty" to difficulty.ifBlank { "Medium" }.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                description?.takeIf { it.isNotBlank() }?.let { updates["description"] = it.take(1000) }
                docRef.update(updates).await()
            } else {
                val quiz = CloudGameQuiz(
                    id = cleanId,
                    createdById = uid.take(128),
                    fellowshipId = FELLOWSHIP_ID,
                    title = title,
                    description = description,
                    category = category,
                    questionsJson = questionsJson,
                    totalQuestions = totalQuestions.toLong(),
                    timeLimitSeconds = timeLimitSeconds.toLong(),
                    difficulty = difficulty
                )
                docRef.set(quiz.toCreateMap()).await()
            }
            Result.success(cleanId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteGameQuiz(quizId: String): Result<Unit> {
        val cleanId = quizId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid quizId"))
        val docRef = db.collection(COLLECTION_GAME_QUIZZES).document(cleanId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeGameQuizzes(): Flow<List<CloudGameQuiz>> =
        db.collection(COLLECTION_GAME_QUIZZES)
            .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudGameQuiz::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_GAME_QUIZZES)
                }
                throw error
            }

    suspend fun upsertGameScore(
        scoreId: String,
        quizId: String,
        quizTitle: String,
        userId: String,
        userName: String,
        score: Int,
        totalQuestions: Int,
        timeTakenSeconds: Int,
        completedAt: Long
    ): Result<String> {
        val uid = try {
            requireUserId(userId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_GAME_SCORES)
            return Result.failure(e)
        }
        val cleanId = scoreId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(120)
            .ifBlank { "score_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection(COLLECTION_GAME_SCORES).document(cleanId)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mapOf<String, Any>(
                    "quizTitle" to quizTitle.trim().ifBlank { "Scripture Challenge" }.take(150),
                    "userName" to userName.trim().ifBlank { "Youth Member" }.take(100),
                    "score" to score.coerceIn(0, 1_000_000).toLong(),
                    "totalQuestions" to totalQuestions.coerceIn(1, 1_000_000).toLong(),
                    "timeTakenSeconds" to timeTakenSeconds.coerceIn(0, 86_400).toLong(),
                    "completedAt" to completedAt.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val cloudScore = CloudGameScore(
                    id = cleanId,
                    quizId = quizId,
                    quizTitle = quizTitle,
                    userId = uid.take(128),
                    userName = userName,
                    fellowshipId = FELLOWSHIP_ID,
                    score = score.toLong(),
                    totalQuestions = totalQuestions.toLong(),
                    timeTakenSeconds = timeTakenSeconds.toLong(),
                    completedAt = completedAt
                )
                docRef.set(cloudScore.toCreateMap()).await()
            }
            Result.success(cleanId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteGameScore(scoreId: String): Result<Unit> {
        val cleanId = scoreId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid scoreId"))
        val docRef = db.collection(COLLECTION_GAME_SCORES).document(cleanId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeGameScores(): Flow<List<CloudGameScore>> =
        db.collection(COLLECTION_GAME_SCORES)
            .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudGameScore::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_GAME_SCORES)
                }
                throw error
            }

    // ==========================================
    // 5. Community Forum Posts (/community_posts/{postId})
    // ==========================================

    private fun mergeCommentsJson(existingJson: String?, incomingJson: String?): Pair<String?, Int> {
        val byId = LinkedHashMap<String, org.json.JSONObject>()
        fun parseInto(raw: String?) {
            if (raw.isNullOrBlank()) return
            try {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").trim()
                    if (id.isNotBlank()) {
                        byId[id] = obj
                    }
                }
            } catch (_: Exception) {
            }
        }
        parseInto(existingJson)
        parseInto(incomingJson)
        if (byId.isEmpty()) return null to 0
        val sortedValues = byId.values.sortedBy { it.optLong("createdAt", 0L) }
        val out = org.json.JSONArray()
        for (obj in sortedValues) {
            out.put(obj)
        }
        val serialized = out.toString()
        if (serialized.length <= 340_000) {
            return serialized to byId.size
        }
        val compactOut = org.json.JSONArray()
        for (obj in sortedValues) {
            val copy = org.json.JSONObject(obj.toString())
            copy.remove("authorAvatarUri")
            compactOut.put(copy)
        }
        val compactSerialized = compactOut.toString()
        return if (compactSerialized.length <= 340_000) compactSerialized to byId.size else existingJson to byId.size
    }

    suspend fun publishCommunityPost(
        postId: String? = null,
        authorId: String? = null,
        authorName: String,
        authorRole: String,
        authorAvatarUri: String? = null,
        category: String,
        title: String,
        content: String,
        attachmentPhotoUri: String? = null,
        whatsappContact: String? = null,
        commentsJson: String? = null,
        isPinned: Boolean = false
    ): Result<String> {
        val uid = try {
            requireUserId(authorId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_COMMUNITY_POSTS)
            return Result.failure(e)
        }
        val cleanId = postId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "")?.takeIf { it.isNotBlank() }?.take(120)
            ?: "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_COMMUNITY_POSTS).document(cleanId)
        val safeAuthorName = authorName.trim().ifBlank { "Youth Member" }.take(100)
        val safeAuthorRole = authorRole.trim().ifBlank { "Youth Member" }.take(60)
        val safeCategory = category.trim().ifBlank { "Discussion" }.take(60)
        val safeTitle = title.trim().ifBlank { "Fellowship Post" }.take(200)
        val safeContent = content.trim().ifBlank { "Shared with The Gideonites." }.take(4000)
        val safeAvatarUri = com.example.ui.components.ensureSyncableImageUri(
            uriStr = authorAvatarUri,
            maxDimensionPx = 280,
            maxBase64Length = 90_000
        )?.takeIf { it.isNotBlank() && it.length <= 340_000 }
        val safeAttachmentUri = com.example.ui.components.ensureSyncableImageUri(
            uriStr = attachmentPhotoUri,
            maxDimensionPx = 760,
            maxBase64Length = 260_000
        )?.takeIf { it.isNotBlank() && it.length <= 340_000 }
        val safeWhatsapp = whatsappContact?.trim()?.takeIf { it.isNotBlank() }?.take(300)

        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingPost = existingSnap.toObject(
                    CloudCommunityPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val (mergedComments, mergedCommentsSize) = mergeCommentsJson(existingPost?.commentsJson, commentsJson)
                // If the document already exists in Firestore, never overwrite immutable createdAt or authorId
                if (existingPost != null && (existingPost.authorId == uid || auth.currentUser == null)) {
                    val updates = mutableMapOf<String, Any?>(
                        "category" to safeCategory,
                        "title" to safeTitle,
                        "content" to safeContent,
                        "isPinned" to isPinned,
                        "attachmentPhotoUri" to safeAttachmentUri,
                        "whatsappContact" to safeWhatsapp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (safeAvatarUri != null) {
                        updates["authorAvatarUri"] = safeAvatarUri
                    }
                    if (!mergedComments.isNullOrBlank()) {
                        updates["commentsJson"] = mergedComments
                        updates["commentsCount"] = maxOf(existingPost.commentsCount, mergedCommentsSize.toLong())
                    }
                    docRef.update(updates).await()
                } else if (existingPost != null) {
                    val guestUpdates = mutableMapOf<String, Any>(
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (existingPost.isPinned != isPinned) {
                        guestUpdates["isPinned"] = isPinned
                    }
                    if (!mergedComments.isNullOrBlank() && mergedComments != existingPost.commentsJson) {
                        guestUpdates["commentsJson"] = mergedComments
                        guestUpdates["commentsCount"] = maxOf(existingPost.commentsCount, mergedCommentsSize.toLong())
                    }
                    if (guestUpdates.size > 1) {
                        docRef.update(guestUpdates).await()
                    }
                }
            } else {
                val (mergedComments, mergedCommentsSize) = mergeCommentsJson(null, commentsJson)
                val post = CloudCommunityPost(
                    id = cleanId,
                    authorId = uid.take(128),
                    authorName = safeAuthorName,
                    authorRole = safeAuthorRole,
                    authorAvatarUri = safeAvatarUri,
                    fellowshipId = FELLOWSHIP_ID,
                    category = safeCategory,
                    title = safeTitle,
                    content = safeContent,
                    attachmentPhotoUri = safeAttachmentUri,
                    whatsappContact = safeWhatsapp,
                    commentsJson = mergedComments,
                    likesCount = 0L,
                    commentsCount = mergedCommentsSize.toLong(),
                    isPinned = isPinned,
                    isFlagged = false,
                    isHidden = false
                )
                docRef.set(post.toCreateMap()).await()
            }
            Result.success(cleanId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCommunityPostContent(
        postId: String,
        category: String,
        title: String,
        content: String,
        attachmentPhotoUri: String?,
        whatsappContact: String?,
        authorAvatarUri: String? = null
    ): Result<Unit> {
        val cleanId = postId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid postId"))
        val docRef = db.collection(COLLECTION_COMMUNITY_POSTS).document(cleanId)
        val safeCategory = category.trim().ifBlank { "Discussion" }.take(60)
        val safeTitle = title.trim().ifBlank { "Fellowship Post" }.take(200)
        val safeContent = content.trim().ifBlank { "Shared with The Gideonites." }.take(4000)
        val safeAttachmentUri = com.example.ui.components.ensureSyncableImageUri(
            uriStr = attachmentPhotoUri,
            maxDimensionPx = 760,
            maxBase64Length = 260_000
        )?.takeIf { it.isNotBlank() && it.length <= 340_000 }
        val safeAvatarUri = com.example.ui.components.ensureSyncableImageUri(
            uriStr = authorAvatarUri,
            maxDimensionPx = 280,
            maxBase64Length = 90_000
        )?.takeIf { it.isNotBlank() && it.length <= 340_000 }
        val safeWhatsapp = whatsappContact?.trim()?.takeIf { it.isNotBlank() }?.take(300)

        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any?>(
                "category" to safeCategory,
                "title" to safeTitle,
                "content" to safeContent,
                "attachmentPhotoUri" to safeAttachmentUri,
                "whatsappContact" to safeWhatsapp,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safeAvatarUri != null) {
                updates["authorAvatarUri"] = safeAvatarUri
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCommunityPostEngagement(
        postId: String,
        likesCount: Long? = null,
        commentsCount: Long? = null,
        commentsJson: String? = null,
        isPinned: Boolean? = null,
        isFlagged: Boolean? = null,
        isHidden: Boolean? = null
    ): Result<Unit> {
        val cleanId = postId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid postId"))
        val docRef = db.collection(COLLECTION_COMMUNITY_POSTS).document(cleanId)
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (likesCount != null) updates["likesCount"] = likesCount
            if (commentsJson != null) {
                val existingSnap = try { docRef.get().await() } catch (_: Exception) { null }
                val existingPost = existingSnap?.toObject(
                    CloudCommunityPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val (mergedComments, mergedSize) = mergeCommentsJson(existingPost?.commentsJson, commentsJson)
                if (!mergedComments.isNullOrBlank()) {
                    updates["commentsJson"] = mergedComments
                }
                updates["commentsCount"] = maxOf(commentsCount ?: 0L, mergedSize.toLong(), existingPost?.commentsCount ?: 0L)
            } else if (commentsCount != null) {
                updates["commentsCount"] = commentsCount
            }
            if (isPinned != null) updates["isPinned"] = isPinned
            if (isFlagged != null) updates["isFlagged"] = isFlagged
            if (isHidden != null) updates["isHidden"] = isHidden
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCommunityPost(postId: String): Result<Unit> {
        val cleanId = postId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Invalid postId"))
        val docRef = db.collection(COLLECTION_COMMUNITY_POSTS).document(cleanId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCommunityPosts(): Flow<List<CloudCommunityPost>> =
        db.collection(COLLECTION_COMMUNITY_POSTS)
            .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudCommunityPost::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_COMMUNITY_POSTS)
                }
                throw error
            }

    // ==========================================
    // 6. Executive Contact Inquiries (/executive_inquiries/{inquiryId})
    // ==========================================

    suspend fun submitExecutiveInquiry(
        inquiryId: String? = null,
        senderId: String? = null,
        senderName: String,
        senderRole: String,
        senderAvatarUri: String? = null,
        isAnonymous: Boolean = false,
        topic: String,
        message: String
    ): Result<String> {
        val uid = try {
            requireUserId(senderId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_EXECUTIVE_INQUIRIES)
            return Result.failure(e)
        }
        val cleanId = inquiryId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "")?.takeIf { it.isNotBlank() }?.take(120)
            ?: "inq_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_EXECUTIVE_INQUIRIES).document(cleanId)
        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val inquiry = CloudExecutiveInquiry(
                    id = cleanId,
                    senderId = uid.take(128),
                    senderName = (if (isAnonymous) "Anonymous Youth Member" else senderName.trim().ifBlank { "Youth Member" }).take(100),
                    senderRole = senderRole.trim().ifBlank { "Youth Member" }.take(60),
                    senderAvatarUri = if (isAnonymous) null else senderAvatarUri?.trim()?.takeIf { it.isNotBlank() && it.length <= 340_000 },
                    fellowshipId = FELLOWSHIP_ID,
                    isAnonymous = isAnonymous,
                    topic = topic.trim().ifBlank { "General Inquiry" }.take(120),
                    message = message.trim().ifBlank { "Submitted to Youth Executive Council." }.take(4000),
                    status = "PENDING"
                )
                docRef.set(inquiry.toCreateMap()).await()
            }
            Result.success(cleanId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun respondToExecutiveInquiry(
        inquiryId: String,
        status: String,
        responseMessage: String,
        respondedByName: String,
        respondedByRole: String
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_EXECUTIVE_INQUIRIES).document(inquiryId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "status" to status,
                    "responseMessage" to responseMessage,
                    "respondedByName" to respondedByName,
                    "respondedByRole" to respondedByRole,
                    "respondedAt" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeExecutiveInquiries(): Flow<List<CloudExecutiveInquiry>> =
        db.collection(COLLECTION_EXECUTIVE_INQUIRIES)
            .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudExecutiveInquiry::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_EXECUTIVE_INQUIRIES)
                }
                throw error
            }

    // ==========================================
    // 7. Cross-Device Fellowship Notifications (/fellowship_notifications/{notificationId})
    // ==========================================

    suspend fun publishFellowshipNotification(
        notificationId: String? = null,
        title: String,
        message: String,
        type: String = "ANNOUNCEMENT",
        authorName: String,
        authorRole: String,
        senderDeviceId: String? = null,
        recipientUserId: String? = null,
        actionDeepLink: String? = null,
        isUrgent: Boolean = false,
        timestampMillis: Long = System.currentTimeMillis()
    ): Result<String> {
        val cleanId = notificationId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "")?.takeIf { it.isNotBlank() }?.take(120)
            ?: "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection(COLLECTION_FELLOWSHIP_NOTIFICATIONS).document(cleanId)
        val validType = if (type in setOf("ANNOUNCEMENT", "EVENT", "COMMUNITY_POST", "GAME_TIME", "REPORT_REVIEW", "GENERAL")) {
            type
        } else {
            "GENERAL"
        }
        val cloudNotif = CloudFellowshipNotification(
            id = cleanId,
            fellowshipId = FELLOWSHIP_ID,
            title = title.trim().ifBlank { "Fellowship Update" }.take(300),
            message = message.trim().ifBlank { "New update in The Gideonites Fellowship." }.take(4000),
            type = validType,
            authorName = authorName.trim().ifBlank { "Youth Leadership" }.take(150),
            authorRole = authorRole.trim().ifBlank { "Executive" }.take(100),
            senderDeviceId = senderDeviceId?.trim()?.takeIf { it.isNotBlank() }?.take(128),
            recipientUserId = recipientUserId?.trim()?.takeIf { it.isNotBlank() }?.take(128),
            actionDeepLink = actionDeepLink?.trim()?.takeIf { it.isNotBlank() }?.take(300),
            isUrgent = isUrgent,
            timestampMillis = timestampMillis.coerceAtLeast(0L)
        )
        return try {
            docRef.set(cloudNotif.toCreateMap()).await()
            Result.success(cleanId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeFellowshipNotifications(): Flow<List<CloudFellowshipNotification>> =
        db.collection(COLLECTION_FELLOWSHIP_NOTIFICATIONS)
            .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudFellowshipNotification::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_FELLOWSHIP_NOTIFICATIONS)
                }
                throw error
            }

    /**
     * Clears all test/demo documents across Cloud Firestore collections while preserving
     * ONLY the official Admin account ("Admin", admin@youthalive.org).
     */
    suspend fun clearCloudDatabasePreserveAdmin(
        adminPasswordHash: String? = null,
        adminSalt: String? = null
    ): Result<Int> {
        var removedCount = 0
        val collectionsToDelete = listOf(
            COLLECTION_DAILY_SCRIPTURES,
            COLLECTION_SERMON_AUDIO_CHUNKS,
            COLLECTION_PROGRAMMES,
            COLLECTION_QUIZ_SESSIONS,
            COLLECTION_GAME_QUIZZES,
            COLLECTION_GAME_SCORES,
            COLLECTION_COMMUNITY_POSTS,
            COLLECTION_EXECUTIVE_INQUIRIES,
            COLLECTION_FELLOWSHIP_NOTIFICATIONS
        )

        for (collectionName in collectionsToDelete) {
            try {
                val snap = db.collection(collectionName)
                    .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
                    .get()
                    .await()
                for (doc in snap.documents) {
                    try {
                        doc.reference.delete().await()
                        removedCount++
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Purge all non-admin accounts in fellowship_accounts & preserve only Admin
        try {
            val accountSnap = db.collection(COLLECTION_FELLOWSHIP_ACCOUNTS)
                .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
                .get()
                .await()
            for (doc in accountSnap.documents) {
                val docEmail = (doc.getString("email") ?: "").trim().lowercase()
                val isOfficialAdmin = doc.id == "user-admin-01" || docEmail == "admin@youthalive.org"
                if (!isOfficialAdmin) {
                    try {
                        val existingProfile = doc.toObject(
                            CloudUserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (existingProfile != null && !existingProfile.status.equals("PURGED", ignoreCase = true)) {
                            val purgedPayload = existingProfile.copy(status = "PURGED").toCreateMap().toMutableMap()
                            purgedPayload["fellowshipId"] = FELLOWSHIP_ID
                            doc.reference.set(purgedPayload).await()
                            removedCount++
                        }
                    } catch (_: Exception) {
                    }
                    try {
                        doc.reference.delete().await()
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Ensure the official Admin account ("Admin", no aliases) is cleanly stored in Cloud Firestore
        try {
            upsertFellowshipAccount(
                userId = "user-admin-01",
                fullName = "Admin",
                email = "admin@youthalive.org",
                phone = "+2348011223344",
                passwordHash = adminPasswordHash,
                salt = adminSalt,
                status = "APPROVED",
                role = "ADMINISTRATOR",
                department = "GENERAL",
                address = com.example.data.local.DatabasePrepopulator.OFFICIAL_ADMIN_ADDRESS,
                fellowshipGroup = "Central Administration",
                preferredLanguage = "en",
                englishOverride = false,
                xpPoints = 0L
            )
        } catch (_: Exception) {
        }

        return Result.success(removedCount)
    }

    // ==========================================
    // 9. Sermon Audio Chunks (/sermon_audio_chunks/{chunkId})
    // ==========================================

    suspend fun uploadSermonAudioFile(
        audioCloudId: String,
        sourceFile: java.io.File,
        onProgress: ((uploadedChunks: Int, totalChunks: Int) -> Unit)? = null
    ): Result<Int> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_SERMON_AUDIO_CHUNKS)
            return Result.failure(e)
        }
        val cleanAudioId = audioCloudId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(100)
        if (cleanAudioId.isBlank() || !sourceFile.exists() || sourceFile.length() <= 0L) {
            return Result.failure(IllegalArgumentException("Invalid sermon audio file"))
        }

        val rawChunkSize = 450_000 // ~600,000 base64 chars per Firestore doc
        val totalSize = sourceFile.length()
        val totalChunks = ((totalSize + rawChunkSize - 1) / rawChunkSize).toInt().coerceAtLeast(1)
        if (totalChunks > 500) {
            return Result.failure(IllegalArgumentException("Audio file exceeds cloud chunk limit"))
        }

        return try {
            sourceFile.inputStream().buffered().use { input ->
                val buffer = ByteArray(rawChunkSize)
                var chunkIndex = 0
                while (chunkIndex < totalChunks) {
                    var bytesReadTotal = 0
                    while (bytesReadTotal < rawChunkSize) {
                        val read = input.read(buffer, bytesReadTotal, rawChunkSize - bytesReadTotal)
                        if (read == -1) break
                        bytesReadTotal += read
                    }
                    if (bytesReadTotal <= 0) break

                    val slice = if (bytesReadTotal == buffer.size) buffer else buffer.copyOf(bytesReadTotal)
                    val encoded = android.util.Base64.encodeToString(slice, android.util.Base64.NO_WRAP)
                    val chunkDocId = "${cleanAudioId}_c${chunkIndex}".take(128)
                    val chunkDoc = CloudSermonAudioChunk(
                        id = chunkDocId,
                        audioCloudId = cleanAudioId,
                        fellowshipId = FELLOWSHIP_ID,
                        uploadedById = uid.take(128),
                        chunkIndex = chunkIndex,
                        totalChunks = totalChunks,
                        dataBase64 = encoded
                    )
                    db.collection(COLLECTION_SERMON_AUDIO_CHUNKS)
                        .document(chunkDocId)
                        .set(chunkDoc.toCreateMap())
                        .await()
                    chunkIndex++
                    onProgress?.invoke(chunkIndex, totalChunks)
                }
            }
            Result.success(totalChunks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_SERMON_AUDIO_CHUNKS)
            Result.failure(e)
        }
    }

    suspend fun downloadSermonAudioFile(
        audioCloudId: String,
        expectedChunkCount: Int,
        destFile: java.io.File
    ): Result<java.io.File> {
        val cleanAudioId = audioCloudId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(100)
        if (cleanAudioId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid audioCloudId"))
        }
        return try {
            val snap = db.collection(COLLECTION_SERMON_AUDIO_CHUNKS)
                .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
                .get()
                .await()
            val matchingChunks = snap.documents
                .mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            CloudSermonAudioChunk::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.getOrNull()
                }
                .filter { it.audioCloudId == cleanAudioId && it.dataBase64.isNotBlank() }
                .sortedBy { it.chunkIndex }

            if (matchingChunks.isEmpty()) {
                return Result.failure(IllegalStateException("Sermon audio chunks not found in cloud yet"))
            }
            val declaredTotal = matchingChunks.first().totalChunks.coerceAtLeast(expectedChunkCount)
            if (matchingChunks.size < declaredTotal) {
                return Result.failure(IllegalStateException("Sermon audio still syncing (${matchingChunks.size}/$declaredTotal parts)"))
            }

            destFile.parentFile?.mkdirs()
            val tempFile = java.io.File(destFile.parentFile, "${destFile.name}.tmp")
            tempFile.outputStream().buffered().use { out ->
                for (chunk in matchingChunks) {
                    val decoded = android.util.Base64.decode(chunk.dataBase64, android.util.Base64.DEFAULT)
                    out.write(decoded)
                }
                out.flush()
            }
            if (destFile.exists()) destFile.delete()
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }
            Result.success(destFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_SERMON_AUDIO_CHUNKS)
            Result.failure(e)
        }
    }

    suspend fun deleteSermonAudioChunksForId(audioCloudId: String): Result<Int> {
        val cleanAudioId = audioCloudId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(100)
        if (cleanAudioId.isBlank()) return Result.success(0)
        return deleteSermonAudioChunksWhere { chunkId -> chunkId == cleanAudioId }
    }

    suspend fun deleteOtherSermonAudioChunks(keepAudioCloudId: String?): Result<Int> {
        val cleanKeep = keepAudioCloudId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "")?.take(100)
        return deleteSermonAudioChunksWhere { chunkAudioId ->
            cleanKeep.isNullOrBlank() || chunkAudioId != cleanKeep
        }
    }

    private suspend fun deleteSermonAudioChunksWhere(predicate: (String) -> Boolean): Result<Int> {
        var deletedCount = 0
        return try {
            requireUserId()
            val snap = db.collection(COLLECTION_SERMON_AUDIO_CHUNKS)
                .whereEqualTo("fellowshipId", FELLOWSHIP_ID)
                .get()
                .await()
            for (doc in snap.documents) {
                val chunkAudioId = doc.getString("audioCloudId") ?: ""
                if (predicate(chunkAudioId)) {
                    try {
                        doc.reference.delete().await()
                        deletedCount++
                    } catch (_: Exception) {
                    }
                }
            }
            Result.success(deletedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, COLLECTION_SERMON_AUDIO_CHUNKS)
            Result.failure(e)
        }
    }

    companion object {
        const val FELLOWSHIP_ID = "GIDEONITES"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_FELLOWSHIP_ACCOUNTS = "fellowship_accounts"
        const val COLLECTION_DAILY_SCRIPTURES = "daily_scriptures"
        const val COLLECTION_PROGRAMMES = "programmes"
        const val COLLECTION_QUIZ_SESSIONS = "quiz_sessions"
        const val COLLECTION_GAME_QUIZZES = "game_quizzes"
        const val COLLECTION_GAME_SCORES = "game_scores"
        const val COLLECTION_COMMUNITY_POSTS = "community_posts"
        const val COLLECTION_EXECUTIVE_INQUIRIES = "executive_inquiries"
        const val COLLECTION_FELLOWSHIP_NOTIFICATIONS = "fellowship_notifications"
        const val COLLECTION_SERMON_AUDIO_CHUNKS = "sermon_audio_chunks"
    }
}

