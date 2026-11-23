package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.CommunityCommentEntity
import com.example.data.local.entity.CommunityPostEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.ExecutiveInquiryEntity
import com.example.data.local.entity.SpiritualResourceEntity
import com.example.data.model.DepartmentType
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {

    @Query("SELECT * FROM departments ORDER BY name ASC")
    fun getAllDepartments(): Flow<List<DepartmentEntity>>

    @Query("SELECT * FROM departments WHERE type = :type LIMIT 1")
    fun getDepartmentByType(type: DepartmentType): Flow<DepartmentEntity?>

    @Query("SELECT * FROM departments WHERE id = :id LIMIT 1")
    suspend fun getDepartmentById(id: String): DepartmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepartments(departments: List<DepartmentEntity>)

    @Update
    suspend fun updateDepartment(department: DepartmentEntity)

    @Query("UPDATE departments SET memberCount = memberCount + 1 WHERE type = :type")
    suspend fun incrementMemberCount(type: DepartmentType)
}

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance_records ORDER BY dateMillis DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE departmentOrGroup = :dept ORDER BY dateMillis DESC")
    fun getAttendanceByDepartment(dept: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT SUM(totalAttendees) FROM attendance_records")
    fun getTotalAttendeesAcrossServices(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecordEntity)
}

@Dao
interface CommunityDao {

    @Query("SELECT * FROM community_posts WHERE isHidden = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getActivePosts(): Flow<List<CommunityPostEntity>>

    @Query("SELECT * FROM community_posts WHERE isHidden = 0 AND createdAt > :minCreatedAt ORDER BY isPinned DESC, createdAt DESC")
    fun getActivePostsAfter(minCreatedAt: Long): Flow<List<CommunityPostEntity>>

    @Query("SELECT * FROM community_posts WHERE isFlagged = 1 AND isHidden = 0 ORDER BY createdAt DESC")
    fun getFlaggedPosts(): Flow<List<CommunityPostEntity>>

    @Query("SELECT * FROM community_posts WHERE id = :postId LIMIT 1")
    fun getPostById(postId: String): Flow<CommunityPostEntity?>

    @Query("SELECT * FROM community_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostByIdSnapshot(postId: String): CommunityPostEntity?

    @Query("""
        SELECT * FROM community_posts
        WHERE createdAt <= :sevenDayCutoffMillis
           OR (LOWER(departmentOrCategory) IN ('work/skills', 'work & skills', 'work_skills') AND createdAt <= :threeDayCutoffMillis)
    """)
    suspend fun getExpiredPostsSync(
        sevenDayCutoffMillis: Long,
        threeDayCutoffMillis: Long = sevenDayCutoffMillis
    ): List<CommunityPostEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPostIgnore(post: CommunityPostEntity): Long

    @Update
    suspend fun updatePostEntity(post: CommunityPostEntity)

    @androidx.room.Transaction
    suspend fun insertPost(post: CommunityPostEntity) {
        if (insertPostIgnore(post) == -1L) {
            updatePostEntity(post)
        }
    }

    @Query("""
        UPDATE community_posts
        SET departmentOrCategory = :category,
            title = :title,
            content = :content,
            attachmentPhotoUri = :attachmentPhotoUri,
            whatsappContact = :whatsappContact
        WHERE id = :postId
    """)
    suspend fun updatePostContent(
        postId: String,
        category: String,
        title: String,
        content: String,
        attachmentPhotoUri: String?,
        whatsappContact: String?
    )

    @Query("UPDATE community_posts SET likesCount = likesCount + 1 WHERE id = :postId")
    suspend fun likePost(postId: String)

    @Query("UPDATE community_posts SET likesCount = :likesCount, likedByUserIds = :likedByUserIds WHERE id = :postId")
    suspend fun updatePostLikes(postId: String, likesCount: Int, likedByUserIds: String)

    @Query("UPDATE community_posts SET commentsCount = :commentsCount WHERE id = :postId")
    suspend fun updatePostCommentsCount(postId: String, commentsCount: Int)

    @Query("UPDATE community_posts SET isPinned = :pinned WHERE id = :postId")
    suspend fun pinPost(postId: String, pinned: Boolean)

    @Query("UPDATE community_posts SET commentsCount = commentsCount + 1 WHERE id = :postId")
    suspend fun incrementCommentsCount(postId: String)

    @Query("UPDATE community_posts SET isFlagged = :flagged WHERE id = :postId")
    suspend fun flagPost(postId: String, flagged: Boolean)

    @Query("UPDATE community_posts SET isHidden = :hidden WHERE id = :postId")
    suspend fun hidePost(postId: String, hidden: Boolean)

    // Likes Table (community_post_likes)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostLike(like: com.example.data.local.entity.CommunityPostLikeEntity)

    @Query("DELETE FROM community_post_likes WHERE postId = :postId AND userId = :userId")
    suspend fun deletePostLike(postId: String, userId: String)

    @Query("SELECT * FROM community_post_likes WHERE postId = :postId")
    suspend fun getLikesForPostSync(postId: String): List<com.example.data.local.entity.CommunityPostLikeEntity>

    @Query("SELECT COUNT(*) FROM community_post_likes WHERE postId = :postId")
    suspend fun getLikesCountForPostSync(postId: String): Int

    // Comments / Replies (community_comments)
    @Query("SELECT * FROM community_comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun getCommentsForPost(postId: String): Flow<List<CommunityCommentEntity>>

    @Query("SELECT * FROM community_comments WHERE postId = :postId ORDER BY createdAt ASC")
    suspend fun getCommentsForPostSync(postId: String): List<CommunityCommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommunityCommentEntity)

    // 7-Day Auto-Deletion of Forum Posts & 3-Day Auto-Deletion of Work/Skills Posts Alongside All Replies & Likes
    @Query("""
        DELETE FROM community_post_likes
        WHERE postId IN (
            SELECT id FROM community_posts
            WHERE createdAt <= :sevenDayCutoffMillis
               OR (LOWER(departmentOrCategory) IN ('work/skills', 'work & skills', 'work_skills') AND createdAt <= :threeDayCutoffMillis)
        )
    """)
    suspend fun deleteLikesForExpiredPosts(sevenDayCutoffMillis: Long, threeDayCutoffMillis: Long): Int

    @Query("""
        DELETE FROM community_comments
        WHERE postId IN (
            SELECT id FROM community_posts
            WHERE createdAt <= :sevenDayCutoffMillis
               OR (LOWER(departmentOrCategory) IN ('work/skills', 'work & skills', 'work_skills') AND createdAt <= :threeDayCutoffMillis)
        )
    """)
    suspend fun deleteCommentsForExpiredPosts(sevenDayCutoffMillis: Long, threeDayCutoffMillis: Long): Int

    @Query("""
        DELETE FROM community_posts
        WHERE createdAt <= :sevenDayCutoffMillis
           OR (LOWER(departmentOrCategory) IN ('work/skills', 'work & skills', 'work_skills') AND createdAt <= :threeDayCutoffMillis)
    """)
    suspend fun deleteExpiredPosts(sevenDayCutoffMillis: Long, threeDayCutoffMillis: Long): Int

    @Query("DELETE FROM community_post_likes WHERE postId = :postId")
    suspend fun deleteLikesForPost(postId: String): Int

    @Query("DELETE FROM community_comments WHERE postId = :postId")
    suspend fun deleteCommentsForPost(postId: String): Int

    @Query("DELETE FROM community_posts WHERE id = :postId")
    suspend fun deletePostById(postId: String): Int

    @androidx.room.Transaction
    suspend fun deletePostWithCommentsAndLikes(postId: String): Int {
        deleteLikesForPost(postId)
        deleteCommentsForPost(postId)
        return deletePostById(postId)
    }

    @androidx.room.Transaction
    suspend fun purgeExpiredPostsWithCommentsAndLikes(
        sevenDayCutoffMillis: Long,
        threeDayCutoffMillis: Long = sevenDayCutoffMillis
    ): Int {
        deleteLikesForExpiredPosts(sevenDayCutoffMillis, threeDayCutoffMillis)
        deleteCommentsForExpiredPosts(sevenDayCutoffMillis, threeDayCutoffMillis)
        return deleteExpiredPosts(sevenDayCutoffMillis, threeDayCutoffMillis)
    }

    // Executive Contact Inquiries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInquiry(inquiry: ExecutiveInquiryEntity)

    @Query("SELECT * FROM executive_inquiries ORDER BY createdAt DESC")
    fun getAllInquiries(): Flow<List<ExecutiveInquiryEntity>>

    @Query("SELECT * FROM executive_inquiries WHERE senderId = :senderId ORDER BY createdAt DESC")
    fun getInquiriesBySender(senderId: String): Flow<List<ExecutiveInquiryEntity>>

    @Query("SELECT * FROM executive_inquiries WHERE id = :inquiryId LIMIT 1")
    suspend fun getInquiryByIdSnapshot(inquiryId: String): ExecutiveInquiryEntity?

    @Query("""
        UPDATE executive_inquiries
        SET status = :status,
            responseMessage = :responseMessage,
            respondedByName = :respondedByName,
            respondedByRole = :respondedByRole,
            respondedAt = :respondedAt
        WHERE id = :inquiryId
    """)
    suspend fun respondToInquiry(
        inquiryId: String,
        status: String,
        responseMessage: String,
        respondedByName: String,
        respondedByRole: String,
        respondedAt: Long
    )
}

@Dao
interface SpiritualResourceDao {

    @Query("SELECT * FROM spiritual_resources ORDER BY publishedAt DESC")
    fun getAllResources(): Flow<List<SpiritualResourceEntity>>

    @Query("SELECT * FROM spiritual_resources WHERE category = :category ORDER BY publishedAt DESC")
    fun getResourcesByCategory(category: String): Flow<List<SpiritualResourceEntity>>

    @Query("SELECT * FROM spiritual_resources WHERE isBookmarked = 1 ORDER BY publishedAt DESC")
    fun getBookmarkedResources(): Flow<List<SpiritualResourceEntity>>

    @Query("SELECT * FROM spiritual_resources WHERE id = :id LIMIT 1")
    fun getResourceById(id: String): Flow<SpiritualResourceEntity?>

    @Query("SELECT * FROM spiritual_resources WHERE id = :id LIMIT 1")
    suspend fun getResourceByIdSync(id: String): SpiritualResourceEntity?

    @Query("SELECT * FROM spiritual_resources ORDER BY publishedAt DESC")
    suspend fun getAllResourcesSync(): List<SpiritualResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResources(resources: List<SpiritualResourceEntity>)

    @Query("UPDATE spiritual_resources SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: String, bookmarked: Boolean)

    @Query("DELETE FROM spiritual_resources WHERE id = :id")
    suspend fun deleteResourceById(id: String): Int
}
