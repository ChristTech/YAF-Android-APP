package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AccountStatus
import com.example.data.model.DepartmentType
import com.example.data.model.Gender
import com.example.data.model.UserRole

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["phone"], unique = true),
        Index(value = ["role"]),
        Index(value = ["status"])
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String, // UUID
    val fullName: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val salt: String,
    val role: UserRole,
    val status: AccountStatus,
    val dateOfBirth: String = "",
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    val address: String = "",
    val department: DepartmentType = DepartmentType.GENERAL,
    val fellowshipGroup: String = "",
    val profilePhotoUri: String? = null,
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelationship: String = "",
    val preferredLanguage: com.example.core.localization.AppLanguage = com.example.core.localization.AppLanguage.ENGLISH,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["token"], unique = true),
        Index(value = ["userId"])
    ]
)
data class SessionEntity(
    @PrimaryKey
    val token: String,
    val userId: String,
    val userRole: UserRole,
    val userName: String,
    val expiresAt: Long,
    val preferredLanguage: com.example.core.localization.AppLanguage = com.example.core.localization.AppLanguage.ENGLISH,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["timestamp"]),
        Index(value = ["actionType"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String,
    val userRole: UserRole,
    val actionType: String, // e.g. "USER_REGISTRATION", "FINANCE_GIVING", "EVENT_PUBLISH", "ROLE_ELEVATION"
    val targetEntity: String, // e.g. "FinancialRecord", "User", "Event"
    val targetId: String? = null,
    val details: String,
    val ipOrDeviceId: String = "local_device"
)
