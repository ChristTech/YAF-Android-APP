package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AnnouncementAudience
import com.example.data.model.AnnouncementPriority

@Entity(
    tableName = "announcements",
    indices = [
        Index(value = ["publishedAt"]),
        Index(value = ["priority"]),
        Index(value = ["targetAudience"])
    ]
)
data class AnnouncementEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val content: String,
    val priority: AnnouncementPriority = AnnouncementPriority.NORMAL,
    val targetAudience: AnnouncementAudience = AnnouncementAudience.ALL,
    val targetDepartmentOrGroupId: String? = null,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val publishedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isPinned: Boolean = false,
    val isReadByUser: Boolean = false
)
