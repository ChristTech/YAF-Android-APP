package com.example.core.security

import com.example.data.model.UserRole

enum class Permission {
    VIEW_PROGRAMMES,
    REGISTER_EVENTS,
    MANAGE_EVENTS,
    PUBLISH_ANNOUNCEMENTS,
    VIEW_ALL_ANNOUNCEMENTS,
    RECORD_FINANCIAL_GIVING,
    VIEW_PERSONAL_FINANCES,
    VIEW_ALL_FINANCES,
    APPROVE_FINANCIAL_RECORDS,
    PLAY_GAMES,
    SUBMIT_WEEKLY_REPORTS,
    REVIEW_WEEKLY_REPORTS,
    MANAGE_DEPARTMENTS,
    TAKE_ATTENDANCE,
    MANAGE_USERS,
    APPROVE_REGISTRATIONS,
    VIEW_AUDIT_LOGS,
    SYSTEM_CONFIGURATION
}

object RoleEvaluator {

    fun getPermissions(role: UserRole): Set<Permission> {
        return when (role) {
            UserRole.ADMINISTRATOR -> Permission.values().toSet()

            UserRole.YOUTH_EXECUTIVE -> setOf(
                Permission.VIEW_PROGRAMMES,
                Permission.REGISTER_EVENTS,
                Permission.MANAGE_EVENTS,
                Permission.PUBLISH_ANNOUNCEMENTS,
                Permission.VIEW_ALL_ANNOUNCEMENTS,
                Permission.RECORD_FINANCIAL_GIVING,
                Permission.VIEW_PERSONAL_FINANCES,
                Permission.VIEW_ALL_FINANCES,
                Permission.APPROVE_FINANCIAL_RECORDS,
                Permission.PLAY_GAMES,
                Permission.SUBMIT_WEEKLY_REPORTS,
                Permission.REVIEW_WEEKLY_REPORTS,
                Permission.MANAGE_DEPARTMENTS,
                Permission.TAKE_ATTENDANCE,
                Permission.VIEW_AUDIT_LOGS
            )

            UserRole.YOUTH_MEMBER -> setOf(
                Permission.VIEW_PROGRAMMES,
                Permission.REGISTER_EVENTS,
                Permission.VIEW_ALL_ANNOUNCEMENTS,
                Permission.RECORD_FINANCIAL_GIVING,
                Permission.VIEW_PERSONAL_FINANCES,
                Permission.PLAY_GAMES
            )

            UserRole.GUEST -> setOf(
                Permission.VIEW_PROGRAMMES,
                Permission.REGISTER_EVENTS,
                Permission.PLAY_GAMES
            )
        }
    }

    fun hasPermission(role: UserRole, permission: Permission): Boolean {
        return getPermissions(role).contains(permission)
    }
}
