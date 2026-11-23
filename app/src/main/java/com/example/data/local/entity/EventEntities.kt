package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.EventCategory
import com.example.data.model.EventStatus

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["dateTimeMillis"]),
        Index(value = ["status"]),
        Index(value = ["category"])
    ]
)
data class EventEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val description: String,
    val dateTimeMillis: Long,
    val venue: String,
    val speaker: String = "",
    val posterUri: String? = null,
    val contactPerson: String = "",
    val contactPhone: String = "",
    val isRegistrationEnabled: Boolean = true,
    val participantLimit: Int = 0, // 0 = unlimited
    val registeredCount: Int = 0,
    val category: EventCategory = EventCategory.YOUTH_SERVICE,
    val status: EventStatus = EventStatus.UPCOMING,
    val createdById: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "event_registrations",
    indices = [
        Index(value = ["eventId", "userId"], unique = true),
        Index(value = ["userId"]),
        Index(value = ["eventId"])
    ]
)
data class EventRegistrationEntity(
    @PrimaryKey
    val id: String, // UUID
    val eventId: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userPhone: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val attended: Boolean = false,
    val reminder5DaysScheduledAt: Long = 0L,
    val reminder1DayScheduledAt: Long = 0L,
    val reminder5DaysSent: Boolean = false,
    val reminder1DaySent: Boolean = false,
    val confirmationEmailSent: Boolean = true
)
