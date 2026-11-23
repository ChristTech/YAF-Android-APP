package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM fellowship_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM fellowship_notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationByIdSync(id: String): NotificationEntity?

    @Query("SELECT * FROM fellowship_notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    suspend fun getUnreadNotificationsSync(): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM fellowship_notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE fellowship_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE fellowship_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM fellowship_notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM fellowship_notifications")
    suspend fun clearAll()
}
