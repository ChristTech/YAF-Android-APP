package com.example.data.model

/**
 * 4 Primary User Personas: Admin, Youth Executive, Member, and Guest
 */
enum class UserRole(val displayName: String) {
    ADMINISTRATOR("Admin"),
    YOUTH_EXECUTIVE("Youth Executive"),
    YOUTH_MEMBER("Member"),
    GUEST("Guest");

    fun isLeadership(): Boolean {
        return this == ADMINISTRATOR || this == YOUTH_EXECUTIVE
    }

    fun canManageUsers(): Boolean {
        return this == ADMINISTRATOR
    }

    fun canPublishAnnouncements(): Boolean {
        return this == ADMINISTRATOR || this == YOUTH_EXECUTIVE
    }

    fun canManageEvents(): Boolean {
        return this == ADMINISTRATOR || this == YOUTH_EXECUTIVE
    }

    fun canReviewReports(): Boolean {
        return this == ADMINISTRATOR || this == YOUTH_EXECUTIVE
    }

    fun canViewAllFinances(): Boolean {
        return this == ADMINISTRATOR || this == YOUTH_EXECUTIVE
    }

    fun canSubmitReports(): Boolean {
        return this == ADMINISTRATOR || this == YOUTH_EXECUTIVE
    }

    fun canRecordAttendance(): Boolean {
        return this == ADMINISTRATOR || this == YOUTH_EXECUTIVE
    }

    fun isGuest(): Boolean {
        return this == GUEST
    }

    fun getRoleBadgeSubtitle(): String {
        return when (this) {
            ADMINISTRATOR -> "Admin • Full Platform Oversight"
            YOUTH_EXECUTIVE -> "Executive Leader • Ministry Coordination"
            YOUTH_MEMBER -> "Kingdom Giant • Active Youth Member"
            GUEST -> "Welcome to The Gideonites - The Youth Alive Fellowship!"
        }
    }
}

enum class AccountStatus {
    PENDING_APPROVAL,
    APPROVED,
    SUSPENDED
}

enum class Gender {
    MALE,
    FEMALE,
    PREFER_NOT_TO_SAY
}

enum class DepartmentType(val displayName: String) {
    MEDIA("Media & Technical"),
    CHOIR("Choir / Music Ministry"),
    DRAMA("Drama & Creative Arts"),
    USHERING("Ushering & Protocol"),
    PRAYER("Prayer & Intercession"),
    SPORTS("Sports & Recreation"),
    HOSPITALITY("Hospitality & Welfare"),
    GENERAL("General Youth Fellowship")
}

enum class EventCategory(val displayName: String) {
    YOUTH_SERVICE("Youth Service"),
    WORKSHOP("Skills & Workshop"),
    RETREAT("Spiritual Retreat"),
    SOCIAL("Social & Hangout"),
    OUTREACH("Evangelism & Outreach"),
    BIBLE_STUDY("Bible Study"),
    PRAYER_MEETING("Prayer Meeting")
}

enum class EventStatus {
    UPCOMING,
    ONGOING,
    COMPLETED,
    CANCELLED
}

enum class AnnouncementPriority {
    NORMAL,
    IMPORTANT,
    URGENT
}

enum class AnnouncementAudience {
    ALL,
    YOUTH_ONLY,
    EXECUTIVES_ONLY,
    DEPARTMENT_SPECIFIC,
    CELL_SPECIFIC
}

enum class FinancialCategory(val displayName: String) {
    TITHE("Tithe"),
    OFFERING("General Offering"),
    PLEDGE("Faith Pledge"),
    SPECIAL_PROJECT("Youth Project / Building"),
    DUES("Youth Monthly Dues"),
    DONATION("Special Donation")
}

enum class PaymentStatus {
    PLEDGED,
    COMPLETED,
    PENDING_VERIFICATION
}

enum class PaymentMethod(val displayName: String) {
    BANK_TRANSFER("Direct Bank Transfer"),
    DEBIT_CARD("Debit / Credit Card"),
    CASH("Cash to Financial Secretary"),
    USSD("USSD Mobile Banking")
}

enum class QuizCategory(val displayName: String) {
    BIBLE_TRIVIA("Bible Trivia"),
    SCRIPTURE_MEMORY("Scripture Memory"),
    CHURCH_DOCTRINE("Christian Living & Doctrine"),
    YOUTH_HISTORY("Church & Youth History")
}

enum class ReportStatus {
    DRAFT,
    SUBMITTED,
    REVIEWED,
    ARCHIVED
}
