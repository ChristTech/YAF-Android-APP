package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationType(val displayName: String) {
    ANNOUNCEMENT("Announcement"),
    EVENT("Programme & Event"),
    COMMUNITY_POST("Community Post"),
    GAME_TIME("Game Time Alert"),
    REPORT_REVIEW("Weekly Report Review"),
    GENERAL("Fellowship Update")
}

@Entity(tableName = "fellowship_notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.ANNOUNCEMENT,
    val authorName: String,
    val authorRole: String, // "System Administrator" or "Youth Executive"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionDeepLink: String = "",
    val isUrgent: Boolean = false
)
