package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.DepartmentType

@Entity(
    tableName = "departments",
    indices = [
        Index(value = ["type"], unique = true)
    ]
)
data class DepartmentEntity(
    @PrimaryKey
    val id: String, // UUID
    val type: DepartmentType,
    val name: String,
    val description: String,
    val leaderId: String,
    val leaderName: String,
    val meetingSchedule: String,
    val meetingVenue: String,
    val memberCount: Int = 0
)

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["dateMillis"]),
        Index(value = ["meetingType"]),
        Index(value = ["departmentOrGroup"])
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey
    val id: String, // UUID
    val meetingTitle: String,
    val meetingType: String, // "SUNDAY_SERVICE", "YOUTH_MEETING", "BIBLE_STUDY", "PRAYER_VIGIL", "DEPARTMENT_REHEARSAL"
    val departmentOrGroup: String = "General",
    val dateMillis: Long = System.currentTimeMillis(),
    val totalAttendees: Int,
    val maleCount: Int,
    val femaleCount: Int,
    val firstTimersCount: Int,
    val checkInMethod: String = "MANUAL", // "MANUAL", "QR_CODE", "SELF_CODE"
    val verificationCode: String = "",
    val recordedById: String,
    val recordedByName: String,
    val notes: String = ""
)

@Entity(
    tableName = "community_posts",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isFlagged"]),
        Index(value = ["isPinned"]),
        Index(value = ["departmentOrCategory"])
    ]
)
data class CommunityPostEntity(
    @PrimaryKey
    val id: String, // UUID
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val authorAvatarUri: String? = null,
    val departmentOrCategory: String = "Discussion",
    val title: String,
    val content: String,
    val attachmentPhotoUri: String? = null,
    val whatsappContact: String? = null,
    val likesCount: Int = 0,
    val likedByUserIds: String = "", // Comma-separated user IDs for per-user like toggle
    val commentsCount: Int = 0,
    val isPinned: Boolean = false,
    val isFlagged: Boolean = false,
    val isHidden: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000L
        const val THREE_DAYS_MILLIS = 3L * 24 * 60 * 60 * 1000L
        const val CATEGORY_DISCUSSION = "Discussion"
        const val CATEGORY_WORK_SKILLS = "Work/Skills"

        fun isWorkSkillsCategory(category: String?): Boolean {
            if (category.isNullOrBlank()) return false
            val norm = category.trim().lowercase()
            return norm == "work/skills" || norm == "work & skills" || norm == "work_skills"
        }

        fun retentionDurationForCategory(category: String?): Long {
            return if (isWorkSkillsCategory(category)) THREE_DAYS_MILLIS else SEVEN_DAYS_MILLIS
        }

        fun isTuesdayToday(nowMillis: Long = System.currentTimeMillis()): Boolean {
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMillis
            }
            return cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.TUESDAY
        }
    }

    val isWorkSkillsPost: Boolean
        get() = isWorkSkillsCategory(departmentOrCategory)

    val retentionDurationMillis: Long
        get() = retentionDurationForCategory(departmentOrCategory)

    val expiresAt: Long
        get() = createdAt + retentionDurationMillis

    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return nowMillis - createdAt >= retentionDurationMillis
    }

    fun remainingRetentionText(nowMillis: Long = System.currentTimeMillis()): String {
        val remainingMillis = (expiresAt - nowMillis).coerceAtLeast(0L)
        val totalHours = remainingMillis / (60 * 60 * 1000L)
        val days = totalHours / 24
        val hours = totalHours % 24
        val prefix = if (isWorkSkillsPost) "Clears in" else "Auto-deletes in"
        return when {
            days > 0 -> "$prefix ${days}d ${hours}h"
            hours > 0 -> "$prefix ${hours}h"
            else -> "Expiring soon"
        }
    }

    fun isLikedBy(userId: String): Boolean {
        if (userId.isBlank() || likedByUserIds.isBlank()) return false
        return likedByUserIds.split(",").any { it.trim() == userId }
    }
}

@Entity(
    tableName = "community_comments",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = CommunityPostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["createdAt"])
    ]
)
data class CommunityCommentEntity(
    @PrimaryKey
    val id: String, // UUID
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val authorAvatarUri: String? = null,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "community_post_likes",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = CommunityPostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["postId"]),
        Index(value = ["userId"]),
        Index(value = ["postId", "userId"], unique = true)
    ]
)
data class CommunityPostLikeEntity(
    @PrimaryKey
    val id: String, // "${postId}_${userId}"
    val postId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "executive_inquiries",
    indices = [
        Index(value = ["senderId"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class ExecutiveInquiryEntity(
    @PrimaryKey
    val id: String, // UUID
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val senderAvatarUri: String? = null,
    val isAnonymous: Boolean = false,
    val topic: String,
    val message: String,
    val status: String = "PENDING", // "PENDING", "IN_PROGRESS", "RESOLVED"
    val responseMessage: String? = null,
    val respondedByName: String? = null,
    val respondedByRole: String? = null,
    val respondedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "spiritual_resources",
    indices = [
        Index(value = ["category"]),
        Index(value = ["isBookmarked"]),
        Index(value = ["publishedAt"])
    ]
)
data class SpiritualResourceEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val category: String, // "SERMON", "DEVOTIONAL", "BIBLE_STUDY", "PRAYER_POINTS", "WORSHIP_MUSIC"
    val speakerOrAuthor: String,
    val scriptureReference: String = "",
    val summary: String,
    val contentBody: String,
    val audioDurationMinutes: Int = 0,
    val isBookmarked: Boolean = false,
    val publishedAt: Long = System.currentTimeMillis(),
    val audioUri: String? = null,
    val audioCloudId: String? = null,
    val audioFileName: String? = null,
    val audioMimeType: String? = null,
    val audioSizeBytes: Long = 0L,
    val audioChunkCount: Int = 0
) {
    val hasAudio: Boolean
        get() = !audioUri.isNullOrBlank() || !audioCloudId.isNullOrBlank()
}

