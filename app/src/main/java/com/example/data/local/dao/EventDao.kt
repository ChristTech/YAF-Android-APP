package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.EventRegistrationEntity
import com.example.data.model.EventCategory
import com.example.data.model.EventStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY dateTimeMillis ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE status = :status ORDER BY dateTimeMillis ASC")
    fun getEventsByStatus(status: EventStatus): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE category = :category ORDER BY dateTimeMillis ASC")
    fun getEventsByCategory(category: EventCategory): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    fun getEventById(eventId: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    suspend fun getEventByIdSync(eventId: String): EventEntity?

    @Query("SELECT * FROM events WHERE dateTimeMillis < :cutoffMillis")
    suspend fun getExpiredEventsSync(cutoffMillis: Long): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: String)

    @Query("DELETE FROM event_registrations WHERE eventId = :eventId")
    suspend fun deleteRegistrationsForEvent(eventId: String)

    @Transaction
    suspend fun deleteEventWithRegistrations(eventId: String) {
        deleteRegistrationsForEvent(eventId)
        deleteEvent(eventId)
    }

    @Query("UPDATE events SET registeredCount = registeredCount + 1 WHERE id = :eventId")
    suspend fun incrementRegisteredCount(eventId: String)

    @Query("UPDATE events SET registeredCount = CASE WHEN registeredCount > 0 THEN registeredCount - 1 ELSE 0 END WHERE id = :eventId")
    suspend fun decrementRegisteredCount(eventId: String)

    // Registrations
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRegistration(registration: EventRegistrationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRegistration(registration: EventRegistrationEntity)

    @Query("SELECT * FROM event_registrations ORDER BY registeredAt DESC")
    fun getAllRegistrations(): Flow<List<EventRegistrationEntity>>

    @Query("SELECT * FROM event_registrations WHERE eventId = :eventId ORDER BY registeredAt ASC")
    fun getRegistrationsForEvent(eventId: String): Flow<List<EventRegistrationEntity>>

    @Query("SELECT * FROM event_registrations WHERE eventId = :eventId ORDER BY registeredAt ASC")
    suspend fun getRegistrationsForEventSync(eventId: String): List<EventRegistrationEntity>

    @Query("SELECT * FROM event_registrations WHERE userId = :userId ORDER BY registeredAt DESC")
    fun getRegistrationsForUser(userId: String): Flow<List<EventRegistrationEntity>>

    @Query("SELECT * FROM event_registrations WHERE userId = :userId ORDER BY registeredAt DESC")
    suspend fun getRegistrationsForUserSync(userId: String): List<EventRegistrationEntity>

    @Query("SELECT * FROM event_registrations WHERE eventId = :eventId AND userId = :userId LIMIT 1")
    suspend fun getRegistration(eventId: String, userId: String): EventRegistrationEntity?

    @Query("UPDATE event_registrations SET reminder5DaysSent = 1 WHERE id = :registrationId")
    suspend fun mark5DayReminderSent(registrationId: String)

    @Query("UPDATE event_registrations SET reminder1DaySent = 1 WHERE id = :registrationId")
    suspend fun mark1DayReminderSent(registrationId: String)

    @Query("DELETE FROM event_registrations WHERE eventId = :eventId AND userId = :userId")
    suspend fun deleteRegistration(eventId: String, userId: String)

    @Transaction
    suspend fun registerUserForEvent(registration: EventRegistrationEntity) {
        insertRegistration(registration)
        incrementRegisteredCount(registration.eventId)
    }

    @Transaction
    suspend fun cancelUserRegistration(eventId: String, userId: String) {
        deleteRegistration(eventId, userId)
        decrementRegisteredCount(eventId)
    }
}
