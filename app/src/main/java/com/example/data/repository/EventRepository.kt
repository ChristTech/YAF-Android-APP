package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.EventDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.EventRegistrationEntity
import com.example.data.model.EventCategory
import com.example.data.model.EventStatus
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

sealed class RegistrationResult {
    object Success : RegistrationResult()
    data class Error(val message: String) : RegistrationResult()
}

interface EventRepository {
    fun getAllEvents(): Flow<List<EventEntity>>
    fun getEventsByStatus(status: EventStatus): Flow<List<EventEntity>>
    fun getEventsByCategory(category: EventCategory): Flow<List<EventEntity>>
    fun getEventById(eventId: String): Flow<EventEntity?>
    suspend fun getEventByIdSync(eventId: String): EventEntity?
    suspend fun createEvent(event: EventEntity, actorRole: UserRole): Boolean
    suspend fun updateEvent(event: EventEntity, actorRole: UserRole): Boolean
    suspend fun deleteEvent(eventId: String, actorId: String, actorRole: UserRole)
    fun getAllRegistrations(): Flow<List<EventRegistrationEntity>>
    fun getRegistrationsForEvent(eventId: String): Flow<List<EventRegistrationEntity>>
    fun getRegistrationsForUser(userId: String): Flow<List<EventRegistrationEntity>>
    suspend fun isUserRegistered(eventId: String, userId: String): Boolean
    suspend fun registerForEvent(
        eventId: String,
        userId: String,
        userName: String,
        userEmail: String,
        userPhone: String,
        userRole: UserRole
    ): RegistrationResult
    suspend fun cancelRegistration(eventId: String, userId: String, userRole: UserRole)
    suspend fun checkAndMarkDueEmailReminders(
        userId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Triple<EventEntity, EventRegistrationEntity, Int>>
    suspend fun purgeExpiredEvents(nowMillis: Long = System.currentTimeMillis()): Int = 0
    suspend fun syncCloudProgrammesToLocalDb(cloudProgrammes: List<com.example.data.remote.CloudYouthProgramme>)
    suspend fun pushLocalEventsToCloud()
}

class EventRepositoryImpl(
    private val eventDao: EventDao,
    private val auditLogDao: AuditLogDao,
    private val cloudRepository: GideonitesCloudRepository? = null,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null,
    private val appContext: Context? = null
) : EventRepository {

    companion object {
        const val FIVE_DAYS_MILLIS = 5L * 24 * 60 * 60 * 1000L
        const val ONE_DAY_MILLIS = 1L * 24 * 60 * 60 * 1000L

        fun startOfDayMillis(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun endOfDayMillis(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

        /**
         * Returns true once the event's scheduled calendar date has completely passed
         * (i.e., after 23:59:59.999 of the event's date).
         */
        fun isEventDatePassed(
            eventDateTimeMillis: Long,
            nowMillis: Long = System.currentTimeMillis()
        ): Boolean {
            return eventDateTimeMillis < startOfDayMillis(nowMillis)
        }
    }

    override fun getAllEvents(): Flow<List<EventEntity>> =
        eventDao.getAllEvents().map { list ->
            val now = System.currentTimeMillis()
            list.filter { !isEventDatePassed(it.dateTimeMillis, now) }
        }

    override fun getEventsByStatus(status: EventStatus): Flow<List<EventEntity>> =
        eventDao.getEventsByStatus(status).map { list ->
            val now = System.currentTimeMillis()
            list.filter { !isEventDatePassed(it.dateTimeMillis, now) }
        }

    override fun getEventsByCategory(category: EventCategory): Flow<List<EventEntity>> =
        eventDao.getEventsByCategory(category).map { list ->
            val now = System.currentTimeMillis()
            list.filter { !isEventDatePassed(it.dateTimeMillis, now) }
        }

    override fun getEventById(eventId: String): Flow<EventEntity?> = eventDao.getEventById(eventId)

    override suspend fun getEventByIdSync(eventId: String): EventEntity? = eventDao.getEventByIdSync(eventId)

    private fun serializeRegistrations(regs: List<EventRegistrationEntity>): String? {
        if (regs.isEmpty()) return null
        val arr = org.json.JSONArray()
        for (reg in regs) {
            val obj = org.json.JSONObject().apply {
                put("id", reg.id)
                put("eventId", reg.eventId)
                put("userId", reg.userId)
                put("userName", reg.userName)
                put("userEmail", reg.userEmail)
                put("userPhone", reg.userPhone)
                put("registeredAt", reg.registeredAt)
                put("reminder5DaysScheduledAt", reg.reminder5DaysScheduledAt)
                put("reminder1DayScheduledAt", reg.reminder1DayScheduledAt)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeRegistrations(
        json: String?,
        targetEventId: String,
        eventDateMillis: Long
    ): List<EventRegistrationEntity> {
        if (json.isNullOrBlank()) return emptyList()
        val result = mutableListOf<EventRegistrationEntity>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val userId = obj.optString("userId").trim()
                if (userId.isBlank()) continue
                val regId = obj.optString("id").trim().ifBlank { "${targetEventId}_$userId" }
                val userName = obj.optString("userName", "Fellowship Member")
                val userEmail = obj.optString("userEmail", "")
                val userPhone = obj.optString("userPhone", "")
                val registeredAt = obj.optLong("registeredAt", System.currentTimeMillis())
                val rem5 = obj.optLong("reminder5DaysScheduledAt", eventDateMillis - FIVE_DAYS_MILLIS)
                val rem1 = obj.optLong("reminder1DayScheduledAt", eventDateMillis - ONE_DAY_MILLIS)
                result.add(
                    EventRegistrationEntity(
                        id = regId,
                        eventId = targetEventId,
                        userId = userId,
                        userName = userName,
                        userEmail = userEmail,
                        userPhone = userPhone,
                        registeredAt = registeredAt,
                        reminder5DaysScheduledAt = rem5,
                        reminder1DayScheduledAt = rem1,
                        reminder5DaysSent = false,
                        reminder1DaySent = false,
                        confirmationEmailSent = true
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    private suspend fun syncEventToCloud(event: EventEntity) {
        try {
            val localRegs = eventDao.getRegistrationsForEventSync(event.id)
            val regsJson = serializeRegistrations(localRegs)
            val effectiveCount = maxOf(event.registeredCount, localRegs.size).toLong()
            cloudRepository?.publishYouthProgramme(
                programmeId = event.id,
                createdById = event.createdById,
                titleEn = event.title,
                descriptionEn = event.description,
                category = event.category.name,
                venue = event.venue,
                speaker = event.speaker,
                contactPerson = event.contactPerson,
                participantLimit = event.participantLimit.toLong(),
                isRegistrationEnabled = event.isRegistrationEnabled,
                startDateMillis = event.dateTimeMillis,
                endDateMillis = maxOf(event.dateTimeMillis, endOfDayMillis(event.dateTimeMillis)),
                status = event.status.name,
                registeredCount = effectiveCount,
                registrationsJson = regsJson
            )
        } catch (e: Exception) {
            // Local Room write already succeeded
        }
    }

    override suspend fun purgeExpiredEvents(nowMillis: Long): Int {
        val cutoffMillis = startOfDayMillis(nowMillis)
        val expiredEvents = try {
            eventDao.getExpiredEventsSync(cutoffMillis)
        } catch (_: Exception) {
            emptyList()
        }
        var deletedCount = 0
        for (expired in expiredEvents) {
            try {
                eventDao.deleteEventWithRegistrations(expired.id)
                deletedCount++
            } catch (_: Exception) {
            }
            appContext?.let {
                com.example.core.notification.FellowshipBackgroundSyncWorker.cancelProgrammeReminders(it, expired.id)
            }
            try {
                cloudRepository?.deleteYouthProgramme(expired.id)
            } catch (_: Exception) {
                // Ignore cloud permission/offline errors; local cleanup succeeded
            }
        }
        return deletedCount
    }

    override suspend fun pushLocalEventsToCloud() {
        val cloudRepo = cloudRepository ?: return
        try {
            purgeExpiredEvents()
            val allLocalEvents = eventDao.getAllEvents().firstOrNull().orEmpty()
            for (ev in allLocalEvents) {
                if (!isEventDatePassed(ev.dateTimeMillis)) {
                    syncEventToCloud(ev)
                }
            }
        } catch (e: Exception) {
            // Ignore when offline
        }
    }

    override suspend fun createEvent(event: EventEntity, actorRole: UserRole): Boolean {
        eventDao.insertEvent(event)
        syncEventToCloud(event)
        notificationService?.postNotification(
            id = "notif_event_${event.id}",
            title = "📅 Upcoming Youth Programme: ${event.title}",
            message = "${event.venue} • Minister: ${event.speaker.ifBlank { "TBA" }}. ${event.description}",
            type = com.example.data.local.entity.NotificationType.EVENT,
            authorName = event.contactPerson.ifBlank { "Youth Leadership" },
            authorRole = actorRole.displayName
        )
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = event.createdById,
                userRole = actorRole,
                actionType = "EVENT_CREATE",
                targetEntity = "Event",
                targetId = event.id,
                details = "Created programme: ${event.title}"
            )
        )
        return true
    }

    override suspend fun syncCloudProgrammesToLocalDb(
        cloudProgrammes: List<com.example.data.remote.CloudYouthProgramme>
    ) {
        val nowMillis = System.currentTimeMillis()
        purgeExpiredEvents(nowMillis)
        val activeUserId = cloudRepository?.activeSessionUserId
        for (prog in cloudProgrammes) {
            if (prog.id.isBlank()) continue
            val rawId = prog.id.removePrefix("prog_")
            val existingByRaw = eventDao.getEventByIdSync(rawId)
            val existingByCloud = eventDao.getEventByIdSync(prog.id)
            val targetId = existingByRaw?.id ?: existingByCloud?.id ?: rawId.ifBlank { prog.id }
            val existing = existingByRaw ?: existingByCloud

            val resolvedDateMillis = if (prog.startDateMillis > 0L) {
                prog.startDateMillis
            } else {
                existing?.dateTimeMillis ?: nowMillis
            }

            if (isEventDatePassed(resolvedDateMillis, nowMillis)) {
                if (existing != null) {
                    eventDao.deleteEventWithRegistrations(existing.id)
                }
                appContext?.let {
                    com.example.core.notification.FellowshipBackgroundSyncWorker.cancelProgrammeReminders(it, targetId)
                }
                try {
                    cloudRepository?.deleteYouthProgramme(prog.id)
                } catch (_: Exception) {
                }
                continue
            }

            // Sync registrations from cloud JSON into local Room table
            val existingRegs = eventDao.getRegistrationsForEventSync(targetId)
            val existingByUserId = existingRegs.associateBy { it.userId }
            val cloudRegs = deserializeRegistrations(
                json = prog.registrationsJson,
                targetEventId = targetId,
                eventDateMillis = resolvedDateMillis
            )
            for (cReg in cloudRegs) {
                val localExisting = existingByUserId[cReg.userId]
                if (localExisting == null) {
                    eventDao.upsertRegistration(cReg.copy(eventId = targetId))
                    notificationService?.syncRemoteNotificationIfNew(
                        id = "notif_reg_${targetId.removePrefix("prog_")}_${cReg.userId}",
                        title = "🎟️ New Programme Registration: ${prog.titleEn}",
                        message = "${cReg.userName.ifBlank { "A fellowship member" }} registered for ${prog.titleEn}.",
                        type = com.example.data.local.entity.NotificationType.EVENT,
                        authorName = cReg.userName.ifBlank { "Fellowship Member" },
                        authorRole = "Programme Registration",
                        timestamp = cReg.registeredAt,
                        activeUserId = activeUserId
                    )
                } else {
                    // Keep local reminder flags intact
                    val mergedReg = localExisting.copy(
                        userName = cReg.userName.ifBlank { localExisting.userName },
                        userEmail = cReg.userEmail.ifBlank { localExisting.userEmail },
                        userPhone = cReg.userPhone.ifBlank { localExisting.userPhone }
                    )
                    eventDao.upsertRegistration(mergedReg)
                }
            }

            val allMergedRegs = eventDao.getRegistrationsForEventSync(targetId)

            val resolvedCategory = try {
                EventCategory.valueOf(prog.category)
            } catch (e: Exception) {
                EventCategory.entries.first()
            }
            val resolvedStatus = try {
                EventStatus.valueOf(prog.status)
            } catch (e: Exception) {
                EventStatus.UPCOMING
            }

            val syncedEvent = EventEntity(
                id = targetId,
                title = prog.titleEn.ifBlank { existing?.title ?: "Youth Programme" },
                description = prog.descriptionEn.ifBlank { existing?.description ?: "" },
                dateTimeMillis = resolvedDateMillis,
                venue = prog.venue.ifBlank { existing?.venue ?: "Main Sanctuary" },
                speaker = prog.speaker?.takeIf { it.isNotBlank() } ?: existing?.speaker?.takeIf { it.isNotBlank() } ?: "Youth Alive Ministry",
                contactPerson = prog.contactPerson?.takeIf { it.isNotBlank() } ?: existing?.contactPerson?.takeIf { it.isNotBlank() } ?: "Youth Leadership",
                category = resolvedCategory,
                isRegistrationEnabled = prog.isRegistrationEnabled ?: existing?.isRegistrationEnabled ?: true,
                participantLimit = prog.participantLimit?.toInt() ?: existing?.participantLimit ?: 150,
                registeredCount = maxOf(
                    existing?.registeredCount ?: 0,
                    prog.registeredCount.toInt(),
                    allMergedRegs.size
                ),
                status = resolvedStatus,
                createdById = prog.createdById.ifBlank { existing?.createdById ?: "admin-1" },
                createdAt = existing?.createdAt ?: (prog.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            )
            eventDao.insertEvent(syncedEvent)
        }
    }

    override suspend fun updateEvent(event: EventEntity, actorRole: UserRole): Boolean {
        eventDao.updateEvent(event)
        syncEventToCloud(event)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = event.createdById,
                userRole = actorRole,
                actionType = "EVENT_UPDATE",
                targetEntity = "Event",
                targetId = event.id,
                details = "Updated programme: ${event.title}"
            )
        )
        return true
    }

    override suspend fun deleteEvent(eventId: String, actorId: String, actorRole: UserRole) {
        eventDao.deleteEventWithRegistrations(eventId)
        appContext?.let {
            com.example.core.notification.FellowshipBackgroundSyncWorker.cancelProgrammeReminders(it, eventId)
        }
        try {
            cloudRepository?.deleteYouthProgramme(eventId)
        } catch (e: Exception) {
            // Local delete already completed
        }
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = actorId,
                userRole = actorRole,
                actionType = "EVENT_DELETE",
                targetEntity = "Event",
                targetId = eventId,
                details = "Deleted programme id: $eventId"
            )
        )
    }

    override fun getAllRegistrations(): Flow<List<EventRegistrationEntity>> =
        eventDao.getAllRegistrations()

    override fun getRegistrationsForEvent(eventId: String): Flow<List<EventRegistrationEntity>> =
        eventDao.getRegistrationsForEvent(eventId)

    override fun getRegistrationsForUser(userId: String): Flow<List<EventRegistrationEntity>> =
        eventDao.getRegistrationsForUser(userId)

    override suspend fun isUserRegistered(eventId: String, userId: String): Boolean {
        return eventDao.getRegistration(eventId, userId) != null
    }

    override suspend fun registerForEvent(
        eventId: String,
        userId: String,
        userName: String,
        userEmail: String,
        userPhone: String,
        userRole: UserRole
    ): RegistrationResult {
        val event = eventDao.getEventByIdSync(eventId) ?: return RegistrationResult.Error("Programme not found.")
        val now = System.currentTimeMillis()
        if (isEventDatePassed(event.dateTimeMillis, now)) {
            return RegistrationResult.Error("This programme's date has already passed.")
        }
        if (!event.isRegistrationEnabled) {
            return RegistrationResult.Error("Registration is currently disabled for this programme.")
        }
        if (event.participantLimit > 0 && event.registeredCount >= event.participantLimit) {
            return RegistrationResult.Error("Registration is closed: participant limit reached (${event.participantLimit}).")
        }
        if (eventDao.getRegistration(eventId, userId) != null) {
            return RegistrationResult.Error("You are already registered for this programme.")
        }

        val fiveDaysBeforeMillis = event.dateTimeMillis - FIVE_DAYS_MILLIS
        val oneDayBeforeMillis = event.dateTimeMillis - ONE_DAY_MILLIS

        val reg = EventRegistrationEntity(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            userPhone = userPhone,
            registeredAt = now,
            reminder5DaysScheduledAt = fiveDaysBeforeMillis,
            reminder1DayScheduledAt = oneDayBeforeMillis,
            reminder5DaysSent = false,
            reminder1DaySent = false,
            confirmationEmailSent = true
        )
        eventDao.registerUserForEvent(reg)

        appContext?.let {
            com.example.core.notification.FellowshipBackgroundSyncWorker.scheduleProgrammeReminders(
                context = it,
                eventId = event.id,
                eventTitle = event.title,
                eventDateMillis = event.dateTimeMillis,
                userId = userId
            )
        }

        val allRegs = eventDao.getRegistrationsForEventSync(eventId)
        val newCount = maxOf(event.registeredCount + 1, allRegs.size)
        val updatedEvent = event.copy(registeredCount = newCount, updatedAt = now)
        eventDao.updateEvent(updatedEvent)

        val regsJson = serializeRegistrations(allRegs) ?: "[]"
        try {
            val cloudUpdate = cloudRepository?.updateYouthProgrammeRegistration(
                programmeId = eventId,
                registeredCount = newCount.toLong(),
                registrationsJson = regsJson
            )
            if (cloudUpdate == null || cloudUpdate.isFailure) {
                syncEventToCloud(updatedEvent)
            }
        } catch (_: Exception) {
            syncEventToCloud(updatedEvent)
        }

        notificationService?.broadcastCrossDeviceOnly(
            id = "notif_reg_${eventId.removePrefix("prog_")}_${userId}",
            title = "🎟️ New Programme Registration: ${event.title}",
            message = "${userName.ifBlank { "A fellowship member" }} registered for ${event.title} ($newCount registered).",
            type = com.example.data.local.entity.NotificationType.EVENT,
            authorName = userName.ifBlank { "Fellowship Member" },
            authorRole = userRole.displayName
        )

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = userId,
                userRole = userRole,
                actionType = "EVENT_REGISTRATION",
                targetEntity = "EventRegistration",
                targetId = reg.id,
                details = "Registered for event: ${event.title} (5-day & 1-day reminders scheduled for ${userEmail.ifBlank { "member email" }})"
            )
        )
        return RegistrationResult.Success
    }

    override suspend fun cancelRegistration(eventId: String, userId: String, userRole: UserRole) {
        // Programme registrations are permanent and cannot be cancelled by users.
    }

    override suspend fun checkAndMarkDueEmailReminders(
        userId: String,
        nowMillis: Long
    ): List<Triple<EventEntity, EventRegistrationEntity, Int>> {
        val dispatched = mutableListOf<Triple<EventEntity, EventRegistrationEntity, Int>>()
        val registrations = eventDao.getRegistrationsForUserSync(userId)
        for (reg in registrations) {
            val event = eventDao.getEventByIdSync(reg.eventId) ?: continue
            if (nowMillis >= event.dateTimeMillis) continue

            // Check 5-day email reminder window (due when within 5 days of the programme)
            val fiveDayTarget = if (reg.reminder5DaysScheduledAt > 0L) reg.reminder5DaysScheduledAt else (event.dateTimeMillis - FIVE_DAYS_MILLIS)
            val oneDayTarget = if (reg.reminder1DayScheduledAt > 0L) reg.reminder1DayScheduledAt else (event.dateTimeMillis - ONE_DAY_MILLIS)

            if (!reg.reminder5DaysSent && nowMillis >= fiveDayTarget && nowMillis < oneDayTarget) {
                eventDao.mark5DayReminderSent(reg.id)
                val updatedReg = reg.copy(reminder5DaysSent = true)
                dispatched.add(Triple(event, updatedReg, 5))
                auditLogDao.insertLog(
                    AuditLogEntity(
                        userId = userId,
                        userRole = UserRole.YOUTH_MEMBER,
                        actionType = "EMAIL_REMINDER_5_DAYS",
                        targetEntity = "EventRegistration",
                        targetId = reg.id,
                        details = "Sent 5-day email reminder to ${reg.userEmail} for ${event.title}"
                    )
                )
            }

            // Check 1-day email reminder window (due when within 1 day of the programme)
            if (!reg.reminder1DaySent && nowMillis >= oneDayTarget) {
                eventDao.mark1DayReminderSent(reg.id)
                val updatedReg = reg.copy(reminder1DaySent = true)
                dispatched.add(Triple(event, updatedReg, 1))
                auditLogDao.insertLog(
                    AuditLogEntity(
                        userId = userId,
                        userRole = UserRole.YOUTH_MEMBER,
                        actionType = "EMAIL_REMINDER_1_DAY",
                        targetEntity = "EventRegistration",
                        targetId = reg.id,
                        details = "Sent 1-day email reminder to ${reg.userEmail} for ${event.title}"
                    )
                )
            }
        }
        return dispatched
    }
}
