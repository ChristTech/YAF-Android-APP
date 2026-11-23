package com.example.data.repository

import com.example.data.local.dao.AnnouncementDao
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.model.AnnouncementAudience
import com.example.data.model.AnnouncementPriority
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

interface AnnouncementRepository {
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>
    fun getAnnouncementsByPriority(priority: AnnouncementPriority): Flow<List<AnnouncementEntity>>
    fun getAnnouncementsForAudience(role: UserRole): Flow<List<AnnouncementEntity>>
    fun getAnnouncementById(id: String): Flow<AnnouncementEntity?>
    suspend fun publishAnnouncement(announcement: AnnouncementEntity, actorRole: UserRole): Boolean
    suspend fun setPinned(id: String, isPinned: Boolean, actorId: String, actorRole: UserRole)
    suspend fun deleteAnnouncement(id: String, actorId: String, actorRole: UserRole)
    suspend fun pushLocalAnnouncementsToCloud()
}

class AnnouncementRepositoryImpl(
    private val announcementDao: AnnouncementDao,
    private val auditLogDao: AuditLogDao,
    private val cloudRepository: GideonitesCloudRepository? = null,
    private val notificationService: com.example.core.notification.FellowshipNotificationService? = null
) : AnnouncementRepository {

    override fun getAllAnnouncements(): Flow<List<AnnouncementEntity>> =
        announcementDao.getAllAnnouncements()

    override fun getAnnouncementsByPriority(priority: AnnouncementPriority): Flow<List<AnnouncementEntity>> =
        announcementDao.getAnnouncementsByPriority(priority)

    override fun getAnnouncementsForAudience(role: UserRole): Flow<List<AnnouncementEntity>> {
        val audiences = when (role) {
            UserRole.ADMINISTRATOR, UserRole.YOUTH_EXECUTIVE ->
                listOf(AnnouncementAudience.ALL, AnnouncementAudience.YOUTH_ONLY, AnnouncementAudience.EXECUTIVES_ONLY, AnnouncementAudience.DEPARTMENT_SPECIFIC)
            else ->
                listOf(AnnouncementAudience.ALL, AnnouncementAudience.YOUTH_ONLY)
        }
        return announcementDao.getAnnouncementsForAudiences(audiences)
    }

    override fun getAnnouncementById(id: String): Flow<AnnouncementEntity?> =
        announcementDao.getAnnouncementById(id)

    private suspend fun syncAnnouncementToCloud(announcement: AnnouncementEntity) {
        if (cloudRepository != null) {
            try {
                cloudRepository.publishCommunityPost(
                    postId = "ann_${announcement.id}",
                    authorId = announcement.authorId,
                    authorName = announcement.authorName,
                    authorRole = announcement.authorRole,
                    category = if (announcement.priority == AnnouncementPriority.URGENT) "ANNOUNCEMENT_URGENT" else "ANNOUNCEMENT",
                    title = announcement.title,
                    content = announcement.content,
                    isPinned = announcement.isPinned
                )
            } catch (e: Exception) {
                // Local Room announcement and notification remain active
            }
        }
    }

    override suspend fun pushLocalAnnouncementsToCloud() {
        val cloudRepo = cloudRepository ?: return
        try {
            val allLocal = announcementDao.getAllAnnouncements().firstOrNull().orEmpty()
            for (ann in allLocal) {
                syncAnnouncementToCloud(ann)
            }
        } catch (e: Exception) {
            // Ignore when offline
        }
    }

    override suspend fun publishAnnouncement(announcement: AnnouncementEntity, actorRole: UserRole): Boolean {
        announcementDao.insertAnnouncement(announcement)
        notificationService?.postNotification(
            id = "notif_ann_${announcement.id}",
            title = if (announcement.priority == AnnouncementPriority.URGENT) "🚨 URGENT: ${announcement.title}" else "📢 ${announcement.title}",
            message = announcement.content,
            type = com.example.data.local.entity.NotificationType.ANNOUNCEMENT,
            authorName = announcement.authorName,
            authorRole = announcement.authorRole,
            isUrgent = announcement.priority == AnnouncementPriority.URGENT
        )
        syncAnnouncementToCloud(announcement)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = announcement.authorId,
                userRole = actorRole,
                actionType = "ANNOUNCEMENT_PUBLISH",
                targetEntity = "Announcement",
                targetId = announcement.id,
                details = "Published announcement: ${announcement.title} (Priority: ${announcement.priority.name})"
            )
        )
        return true
    }

    override suspend fun setPinned(id: String, isPinned: Boolean, actorId: String, actorRole: UserRole) {
        announcementDao.setPinned(id, isPinned)
        try {
            cloudRepository?.updateCommunityPostEngagement(
                postId = "ann_$id",
                isPinned = isPinned
            )
        } catch (e: Exception) {
            // Local update already succeeded
        }
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = actorId,
                userRole = actorRole,
                actionType = "ANNOUNCEMENT_PIN",
                targetEntity = "Announcement",
                targetId = id,
                details = "Pinned status set to $isPinned"
            )
        )
    }

    override suspend fun deleteAnnouncement(id: String, actorId: String, actorRole: UserRole) {
        announcementDao.deleteAnnouncement(id)
        try {
            cloudRepository?.deleteCommunityPost("ann_$id")
        } catch (e: Exception) {
            // Local delete already succeeded
        }
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = actorId,
                userRole = actorRole,
                actionType = "ANNOUNCEMENT_DELETE",
                targetEntity = "Announcement",
                targetId = id,
                details = "Deleted announcement id: $id"
            )
        )
    }
}
