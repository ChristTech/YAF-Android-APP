package com.example.data.remote

import androidx.annotation.Keep
import com.example.core.localization.AppLanguage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String? = null,
    val passwordHash: String? = null,
    val salt: String? = null,
    val status: String? = null,
    val profilePhotoUri: String? = null,
    val role: String = "YOUTH_MEMBER",
    val department: String = "GENERAL",
    val address: String? = null,
    val fellowshipGroup: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactRelationship: String? = null,
    val preferredLanguage: String = "en",
    val englishOverride: Boolean = false,
    val xpPoints: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "userId" to userId,
        "fullName" to fullName,
        "email" to email,
        "phone" to phone?.takeIf { it.isNotBlank() },
        "passwordHash" to passwordHash?.takeIf { it.isNotBlank() },
        "salt" to salt?.takeIf { it.isNotBlank() },
        "status" to status?.takeIf { it.isNotBlank() },
        "profilePhotoUri" to profilePhotoUri?.takeIf { it.isNotBlank() },
        "role" to role,
        "department" to department,
        "address" to address?.takeIf { it.isNotBlank() }?.take(250),
        "fellowshipGroup" to fellowshipGroup?.takeIf { it.isNotBlank() },
        "emergencyContactName" to emergencyContactName?.takeIf { it.isNotBlank() },
        "emergencyContactPhone" to emergencyContactPhone?.takeIf { it.isNotBlank() },
        "emergencyContactRelationship" to emergencyContactRelationship?.takeIf { it.isNotBlank() },
        "preferredLanguage" to preferredLanguage,
        "englishOverride" to englishOverride,
        "xpPoints" to xpPoints,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class CloudDailyScripture(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val fellowshipId: String = "GIDEONITES",
    val dateKey: String = "",
    val referenceEn: String = "",
    val referenceYo: String? = null,
    val referenceHa: String? = null,
    val referenceIg: String? = null,
    val verseTextEn: String = "",
    val verseTextYo: String? = null,
    val verseTextHa: String? = null,
    val verseTextIg: String? = null,
    val reflectionEn: String = "",
    val reflectionYo: String? = null,
    val reflectionHa: String? = null,
    val reflectionIg: String? = null,
    val prayerPromptEn: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedReference(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> referenceEn
        AppLanguage.YORUBA -> referenceYo?.takeIf { it.isNotBlank() } ?: referenceEn
        AppLanguage.HAUSA -> referenceHa?.takeIf { it.isNotBlank() } ?: referenceEn
        AppLanguage.IGBO -> referenceIg?.takeIf { it.isNotBlank() } ?: referenceEn
    }

    fun localizedVerseText(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> verseTextEn
        AppLanguage.YORUBA -> verseTextYo?.takeIf { it.isNotBlank() } ?: verseTextEn
        AppLanguage.HAUSA -> verseTextHa?.takeIf { it.isNotBlank() } ?: verseTextEn
        AppLanguage.IGBO -> verseTextIg?.takeIf { it.isNotBlank() } ?: verseTextEn
    }

    fun localizedReflection(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> reflectionEn
        AppLanguage.YORUBA -> reflectionYo?.takeIf { it.isNotBlank() } ?: reflectionEn
        AppLanguage.HAUSA -> reflectionHa?.takeIf { it.isNotBlank() } ?: reflectionEn
        AppLanguage.IGBO -> reflectionIg?.takeIf { it.isNotBlank() } ?: reflectionEn
    }

    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "authorId" to authorId,
        "authorName" to authorName,
        "fellowshipId" to fellowshipId,
        "dateKey" to dateKey,
        "referenceEn" to referenceEn,
        "referenceYo" to referenceYo?.takeIf { it.isNotBlank() },
        "referenceHa" to referenceHa?.takeIf { it.isNotBlank() },
        "referenceIg" to referenceIg?.takeIf { it.isNotBlank() },
        "verseTextEn" to verseTextEn,
        "verseTextYo" to verseTextYo?.takeIf { it.isNotBlank() },
        "verseTextHa" to verseTextHa?.takeIf { it.isNotBlank() },
        "verseTextIg" to verseTextIg?.takeIf { it.isNotBlank() },
        "reflectionEn" to reflectionEn,
        "reflectionYo" to reflectionYo?.takeIf { it.isNotBlank() },
        "reflectionHa" to reflectionHa?.takeIf { it.isNotBlank() },
        "reflectionIg" to reflectionIg?.takeIf { it.isNotBlank() },
        "prayerPromptEn" to prayerPromptEn,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class CloudYouthProgramme(
    @DocumentId val id: String = "",
    val createdById: String = "",
    val fellowshipId: String = "GIDEONITES",
    val titleEn: String = "",
    val titleYo: String? = null,
    val titleHa: String? = null,
    val titleIg: String? = null,
    val descriptionEn: String = "",
    val descriptionYo: String? = null,
    val descriptionHa: String? = null,
    val descriptionIg: String? = null,
    val category: String = "FELLOWSHIP",
    val venue: String = "Youth Chapel",
    val speaker: String? = null,
    val contactPerson: String? = null,
    val participantLimit: Long? = null,
    val isRegistrationEnabled: Boolean? = null,
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    val status: String = "UPCOMING",
    val registeredCount: Long = 0L,
    val registrationsJson: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedTitle(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> titleEn
        AppLanguage.YORUBA -> titleYo?.takeIf { it.isNotBlank() } ?: titleEn
        AppLanguage.HAUSA -> titleHa?.takeIf { it.isNotBlank() } ?: titleEn
        AppLanguage.IGBO -> titleIg?.takeIf { it.isNotBlank() } ?: titleEn
    }

    fun localizedDescription(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> descriptionEn
        AppLanguage.YORUBA -> descriptionYo?.takeIf { it.isNotBlank() } ?: descriptionEn
        AppLanguage.HAUSA -> descriptionHa?.takeIf { it.isNotBlank() } ?: descriptionEn
        AppLanguage.IGBO -> descriptionIg?.takeIf { it.isNotBlank() } ?: descriptionEn
    }

    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "createdById" to createdById,
        "fellowshipId" to fellowshipId,
        "titleEn" to titleEn,
        "titleYo" to titleYo?.takeIf { it.isNotBlank() },
        "titleHa" to titleHa?.takeIf { it.isNotBlank() },
        "titleIg" to titleIg?.takeIf { it.isNotBlank() },
        "descriptionEn" to descriptionEn,
        "descriptionYo" to descriptionYo?.takeIf { it.isNotBlank() },
        "descriptionHa" to descriptionHa?.takeIf { it.isNotBlank() },
        "descriptionIg" to descriptionIg?.takeIf { it.isNotBlank() },
        "category" to category,
        "venue" to venue,
        "speaker" to speaker?.takeIf { it.isNotBlank() },
        "contactPerson" to contactPerson?.takeIf { it.isNotBlank() },
        "participantLimit" to participantLimit,
        "isRegistrationEnabled" to isRegistrationEnabled,
        "startDateMillis" to startDateMillis,
        "endDateMillis" to endDateMillis,
        "status" to status,
        "registeredCount" to registeredCount,
        "registrationsJson" to registrationsJson?.takeIf { it.isNotBlank() },
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class CloudQuizSession(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val fellowshipId: String = "GIDEONITES",
    val roomPin: String = "",
    val titleEn: String = "",
    val titleYo: String? = null,
    val titleHa: String? = null,
    val titleIg: String? = null,
    val description: String? = null,
    val gameType: String = "BIBLE_QUIZ",
    val status: String = "SCHEDULED",
    val scheduledStartTime: Long? = null,
    val timeLimitPerQuestionSeconds: Long? = null,
    val leaderboardDurationSeconds: Long? = null,
    val currentQuestionIndex: Long = 0L,
    val currentSubstate: String? = null,
    val currentQuestionDeadline: Long? = null,
    val leaderboardDeadline: Long? = null,
    val baseScorePerQuestion: Long? = null,
    val speedBonusEnabled: Boolean? = null,
    val maxSpeedBonus: Long? = null,
    val penaltyForIncorrect: Boolean? = null,
    val totalQuestions: Long = 5L,
    val participantCount: Long = 1L,
    val questionsJson: String? = null,
    val participantsJson: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedTitle(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> titleEn
        AppLanguage.YORUBA -> titleYo?.takeIf { it.isNotBlank() } ?: titleEn
        AppLanguage.HAUSA -> titleHa?.takeIf { it.isNotBlank() } ?: titleEn
        AppLanguage.IGBO -> titleIg?.takeIf { it.isNotBlank() } ?: titleEn
    }

    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "hostId" to hostId,
        "hostName" to hostName,
        "fellowshipId" to fellowshipId,
        "roomPin" to roomPin,
        "titleEn" to titleEn,
        "titleYo" to titleYo?.takeIf { it.isNotBlank() },
        "titleHa" to titleHa?.takeIf { it.isNotBlank() },
        "titleIg" to titleIg?.takeIf { it.isNotBlank() },
        "description" to description?.takeIf { it.isNotBlank() },
        "gameType" to gameType,
        "status" to status,
        "scheduledStartTime" to scheduledStartTime,
        "timeLimitPerQuestionSeconds" to timeLimitPerQuestionSeconds,
        "leaderboardDurationSeconds" to leaderboardDurationSeconds,
        "currentQuestionIndex" to currentQuestionIndex,
        "currentSubstate" to currentSubstate?.takeIf { it.isNotBlank() },
        "currentQuestionDeadline" to currentQuestionDeadline,
        "leaderboardDeadline" to leaderboardDeadline,
        "baseScorePerQuestion" to baseScorePerQuestion,
        "speedBonusEnabled" to speedBonusEnabled,
        "maxSpeedBonus" to maxSpeedBonus,
        "penaltyForIncorrect" to penaltyForIncorrect,
        "totalQuestions" to totalQuestions,
        "participantCount" to participantCount,
        "questionsJson" to questionsJson?.takeIf { it.isNotBlank() },
        "participantsJson" to participantsJson?.takeIf { it.isNotBlank() },
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class CloudGameQuiz(
    @DocumentId val id: String = "",
    val createdById: String = "",
    val fellowshipId: String = "GIDEONITES",
    val title: String = "",
    val description: String? = null,
    val category: String = "BIBLE_TRIVIA",
    val questionsJson: String = "[]",
    val totalQuestions: Long = 1L,
    val timeLimitSeconds: Long = 120L,
    val difficulty: String = "Medium",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "createdById" to createdById,
        "fellowshipId" to fellowshipId,
        "title" to title.trim().ifBlank { "Scripture Quiz" }.take(150),
        "description" to description?.takeIf { it.isNotBlank() }?.take(1000),
        "category" to category.ifBlank { "BIBLE_TRIVIA" }.take(50),
        "questionsJson" to questionsJson.ifBlank { "[]" }.take(100000),
        "totalQuestions" to totalQuestions.coerceIn(1L, 500L),
        "timeLimitSeconds" to timeLimitSeconds.coerceIn(10L, 7200L),
        "difficulty" to difficulty.ifBlank { "Medium" }.take(40),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class CloudGameScore(
    @DocumentId val id: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val userId: String = "",
    val userName: String = "",
    val fellowshipId: String = "GIDEONITES",
    val score: Long = 0L,
    val totalQuestions: Long = 100L,
    val timeTakenSeconds: Long = 0L,
    val completedAt: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "quizId" to quizId.ifBlank { "quiz" }.take(128),
        "quizTitle" to quizTitle.trim().ifBlank { "Scripture Challenge" }.take(150),
        "userId" to userId.ifBlank { "member" }.take(128),
        "userName" to userName.trim().ifBlank { "Youth Member" }.take(100),
        "fellowshipId" to fellowshipId,
        "score" to score.coerceIn(0L, 1_000_000L),
        "totalQuestions" to totalQuestions.coerceIn(1L, 1_000_000L),
        "timeTakenSeconds" to timeTakenSeconds.coerceIn(0L, 86_400L),
        "completedAt" to completedAt.coerceAtLeast(0L),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class CloudCommunityPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: String = "Youth Member",
    val authorAvatarUri: String? = null,
    val fellowshipId: String = "GIDEONITES",
    val category: String = "Discussion",
    val title: String = "",
    val content: String = "",
    val attachmentPhotoUri: String? = null,
    val whatsappContact: String? = null,
    val commentsJson: String? = null,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val isPinned: Boolean = false,
    val isFlagged: Boolean = false,
    val isHidden: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "authorId" to authorId,
        "authorName" to authorName,
        "authorRole" to authorRole,
        "authorAvatarUri" to authorAvatarUri?.takeIf { it.isNotBlank() },
        "fellowshipId" to fellowshipId,
        "category" to category,
        "title" to title,
        "content" to content,
        "attachmentPhotoUri" to attachmentPhotoUri?.takeIf { it.isNotBlank() },
        "whatsappContact" to whatsappContact?.takeIf { it.isNotBlank() },
        "commentsJson" to commentsJson?.takeIf { it.isNotBlank() },
        "likesCount" to likesCount,
        "commentsCount" to commentsCount,
        "isPinned" to isPinned,
        "isFlagged" to isFlagged,
        "isHidden" to isHidden,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class CloudExecutiveInquiry(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "Youth Member",
    val senderAvatarUri: String? = null,
    val fellowshipId: String = "GIDEONITES",
    val isAnonymous: Boolean = false,
    val topic: String = "",
    val message: String = "",
    val status: String = "PENDING",
    val responseMessage: String? = null,
    val respondedByName: String? = null,
    val respondedByRole: String? = null,
    val respondedAt: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "senderId" to senderId,
        "senderName" to senderName,
        "senderRole" to senderRole,
        "senderAvatarUri" to senderAvatarUri?.takeIf { it.isNotBlank() },
        "fellowshipId" to fellowshipId,
        "isAnonymous" to isAnonymous,
        "topic" to topic,
        "message" to message,
        "status" to status,
        "responseMessage" to responseMessage?.takeIf { it.isNotBlank() },
        "respondedByName" to respondedByName?.takeIf { it.isNotBlank() },
        "respondedByRole" to respondedByRole?.takeIf { it.isNotBlank() },
        "respondedAt" to respondedAt,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class CloudFellowshipNotification(
    @DocumentId val id: String = "",
    val fellowshipId: String = "GIDEONITES",
    val title: String = "",
    val message: String = "",
    val type: String = "ANNOUNCEMENT",
    val authorName: String = "",
    val authorRole: String = "",
    val senderDeviceId: String? = null,
    val recipientUserId: String? = null,
    val actionDeepLink: String? = null,
    val isUrgent: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "fellowshipId" to fellowshipId,
        "title" to title.take(300),
        "message" to message.take(4000),
        "type" to type,
        "authorName" to authorName.take(150),
        "authorRole" to authorRole.take(100),
        "senderDeviceId" to senderDeviceId?.takeIf { it.isNotBlank() }?.take(128),
        "recipientUserId" to recipientUserId?.takeIf { it.isNotBlank() }?.take(128),
        "actionDeepLink" to actionDeepLink?.takeIf { it.isNotBlank() }?.take(300),
        "isUrgent" to isUrgent,
        "timestampMillis" to timestampMillis.coerceAtLeast(0L),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@Keep
@IgnoreExtraProperties
data class CloudSermonAudioChunk(
    @DocumentId val id: String = "",
    val audioCloudId: String = "",
    val fellowshipId: String = "GIDEONITES",
    val uploadedById: String = "",
    val chunkIndex: Int = 0,
    val totalChunks: Int = 1,
    val dataBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf<String, Any?>(
        "audioCloudId" to audioCloudId.take(128),
        "fellowshipId" to fellowshipId,
        "uploadedById" to uploadedById.take(128),
        "chunkIndex" to chunkIndex.coerceIn(0, 500),
        "totalChunks" to totalChunks.coerceIn(1, 500),
        "dataBase64" to dataBase64.take(700_000),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}
