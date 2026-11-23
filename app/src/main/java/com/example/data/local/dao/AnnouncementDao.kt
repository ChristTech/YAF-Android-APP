package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.model.AnnouncementAudience
import com.example.data.model.AnnouncementPriority
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnouncementDao {

    @Query("SELECT * FROM announcements ORDER BY isPinned DESC, publishedAt DESC")
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE priority = :priority ORDER BY publishedAt DESC")
    fun getAnnouncementsByPriority(priority: AnnouncementPriority): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE targetAudience IN (:audiences) ORDER BY isPinned DESC, publishedAt DESC")
    fun getAnnouncementsForAudiences(audiences: List<AnnouncementAudience>): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE id = :id LIMIT 1")
    fun getAnnouncementById(id: String): Flow<AnnouncementEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Update
    suspend fun updateAnnouncement(announcement: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)

    @Query("UPDATE announcements SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean)
}
