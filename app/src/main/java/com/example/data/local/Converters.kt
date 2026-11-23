package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AccountStatus
import com.example.data.model.AnnouncementAudience
import com.example.data.model.AnnouncementPriority
import com.example.data.model.DepartmentType
import com.example.data.model.EventCategory
import com.example.data.model.EventStatus
import com.example.data.model.FinancialCategory
import com.example.data.model.GameType
import com.example.data.model.Gender
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.QuizCategory
import com.example.data.model.ReportStatus
import com.example.data.model.SessionStatus
import com.example.data.model.SessionSubstate
import com.example.data.model.UserRole

class Converters {

    @TypeConverter
    fun fromUserRole(role: UserRole?): String? = role?.name

    @TypeConverter
    fun toUserRole(value: String?): UserRole? = value?.let {
        runCatching { UserRole.valueOf(it) }.getOrDefault(UserRole.YOUTH_MEMBER)
    }

    @TypeConverter
    fun fromAccountStatus(status: AccountStatus?): String? = status?.name

    @TypeConverter
    fun toAccountStatus(value: String?): AccountStatus? = value?.let {
        runCatching { AccountStatus.valueOf(it) }.getOrDefault(AccountStatus.PENDING_APPROVAL)
    }

    @TypeConverter
    fun fromGender(gender: Gender?): String? = gender?.name

    @TypeConverter
    fun toGender(value: String?): Gender? = value?.let {
        runCatching { Gender.valueOf(it) }.getOrDefault(Gender.PREFER_NOT_TO_SAY)
    }

    @TypeConverter
    fun fromDepartmentType(dept: DepartmentType?): String? = dept?.name

    @TypeConverter
    fun toDepartmentType(value: String?): DepartmentType? = value?.let {
        runCatching { DepartmentType.valueOf(it) }.getOrDefault(DepartmentType.GENERAL)
    }

    @TypeConverter
    fun fromEventCategory(cat: EventCategory?): String? = cat?.name

    @TypeConverter
    fun toEventCategory(value: String?): EventCategory? = value?.let {
        runCatching { EventCategory.valueOf(it) }.getOrDefault(EventCategory.YOUTH_SERVICE)
    }

    @TypeConverter
    fun fromEventStatus(status: EventStatus?): String? = status?.name

    @TypeConverter
    fun toEventStatus(value: String?): EventStatus? = value?.let {
        runCatching { EventStatus.valueOf(it) }.getOrDefault(EventStatus.UPCOMING)
    }

    @TypeConverter
    fun fromAnnouncementPriority(priority: AnnouncementPriority?): String? = priority?.name

    @TypeConverter
    fun toAnnouncementPriority(value: String?): AnnouncementPriority? = value?.let {
        runCatching { AnnouncementPriority.valueOf(it) }.getOrDefault(AnnouncementPriority.NORMAL)
    }

    @TypeConverter
    fun fromAnnouncementAudience(audience: AnnouncementAudience?): String? = audience?.name

    @TypeConverter
    fun toAnnouncementAudience(value: String?): AnnouncementAudience? = value?.let {
        runCatching { AnnouncementAudience.valueOf(it) }.getOrDefault(AnnouncementAudience.ALL)
    }

    @TypeConverter
    fun fromFinancialCategory(category: FinancialCategory?): String? = category?.name

    @TypeConverter
    fun toFinancialCategory(value: String?): FinancialCategory? = value?.let {
        runCatching { FinancialCategory.valueOf(it) }.getOrDefault(FinancialCategory.OFFERING)
    }

    @TypeConverter
    fun fromPaymentStatus(status: PaymentStatus?): String? = status?.name

    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus? = value?.let {
        runCatching { PaymentStatus.valueOf(it) }.getOrDefault(PaymentStatus.COMPLETED)
    }

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod?): String? = method?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let {
        runCatching { PaymentMethod.valueOf(it) }.getOrDefault(PaymentMethod.BANK_TRANSFER)
    }

    @TypeConverter
    fun fromQuizCategory(category: QuizCategory?): String? = category?.name

    @TypeConverter
    fun toQuizCategory(value: String?): QuizCategory? = value?.let {
        runCatching { QuizCategory.valueOf(it) }.getOrDefault(QuizCategory.BIBLE_TRIVIA)
    }

    @TypeConverter
    fun fromReportStatus(status: ReportStatus?): String? = status?.name

    @TypeConverter
    fun toReportStatus(value: String?): ReportStatus? = value?.let {
        runCatching { ReportStatus.valueOf(it) }.getOrDefault(ReportStatus.SUBMITTED)
    }

    @TypeConverter
    fun fromGameType(type: GameType?): String? = type?.name

    @TypeConverter
    fun toGameType(value: String?): GameType? = value?.let {
        runCatching { GameType.valueOf(it) }.getOrDefault(GameType.TRIVIA_CHALLENGE)
    }

    @TypeConverter
    fun fromSessionStatus(status: SessionStatus?): String? = status?.name

    @TypeConverter
    fun toSessionStatus(value: String?): SessionStatus? = value?.let {
        runCatching { SessionStatus.valueOf(it) }.getOrDefault(SessionStatus.SCHEDULED)
    }

    @TypeConverter
    fun fromSessionSubstate(substate: SessionSubstate?): String? = substate?.name

    @TypeConverter
    fun toSessionSubstate(value: String?): SessionSubstate? = value?.let {
        runCatching { SessionSubstate.valueOf(it) }.getOrDefault(SessionSubstate.WAITING_TO_START)
    }

    @TypeConverter
    fun fromAppLanguage(language: com.example.core.localization.AppLanguage?): String? = language?.name

    @TypeConverter
    fun toAppLanguage(value: String?): com.example.core.localization.AppLanguage? = value?.let {
        com.example.core.localization.AppLanguage.fromCodeOrName(it)
    }
}
