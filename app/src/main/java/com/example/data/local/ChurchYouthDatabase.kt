package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AnnouncementDao
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.CommunityDao
import com.example.data.local.dao.DepartmentDao
import com.example.data.local.dao.EventDao
import com.example.data.local.dao.FinancialRecordDao
import com.example.data.local.dao.GameDao
import com.example.data.local.dao.SpiritualResourceDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.WeeklyReportDao
import com.example.data.local.entity.AnnouncementEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CommunityCommentEntity
import com.example.data.local.entity.CommunityPostEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.EventRegistrationEntity
import com.example.data.local.entity.ExecutiveInquiryEntity
import com.example.data.local.entity.FinancialRecordEntity
import com.example.data.local.entity.GameQuizEntity
import com.example.data.local.dao.MultiplayerGameDao
import com.example.data.local.entity.GameScoreEntity
import com.example.data.local.entity.MultiplayerGameSessionEntity
import com.example.data.local.entity.MultiplayerParticipantEntity
import com.example.data.local.entity.MultiplayerSubmissionEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.SpiritualResourceEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WeeklyReportEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
        AuditLogEntity::class,
        EventEntity::class,
        EventRegistrationEntity::class,
        AnnouncementEntity::class,
        FinancialRecordEntity::class,
        GameQuizEntity::class,
        GameScoreEntity::class,
        WeeklyReportEntity::class,
        DepartmentEntity::class,
        AttendanceRecordEntity::class,
        CommunityPostEntity::class,
        CommunityCommentEntity::class,
        com.example.data.local.entity.CommunityPostLikeEntity::class,
        ExecutiveInquiryEntity::class,
        SpiritualResourceEntity::class,
        MultiplayerGameSessionEntity::class,
        MultiplayerParticipantEntity::class,
        MultiplayerSubmissionEntity::class,
        com.example.data.local.entity.NotificationEntity::class,
        com.example.data.local.entity.BibleVerseEntity::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChurchYouthDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun eventDao(): EventDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun financialRecordDao(): FinancialRecordDao
    abstract fun gameDao(): GameDao
    abstract fun multiplayerGameDao(): MultiplayerGameDao
    abstract fun weeklyReportDao(): WeeklyReportDao
    abstract fun departmentDao(): DepartmentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun communityDao(): CommunityDao
    abstract fun spiritualResourceDao(): SpiritualResourceDao
    abstract fun notificationDao(): com.example.data.local.dao.NotificationDao
    abstract fun bibleDao(): com.example.data.local.dao.BibleDao

    companion object {
        @Volatile
        private var INSTANCE: ChurchYouthDatabase? = null

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spiritual_resources ADD COLUMN audioUri TEXT")
                db.execSQL("ALTER TABLE spiritual_resources ADD COLUMN audioCloudId TEXT")
                db.execSQL("ALTER TABLE spiritual_resources ADD COLUMN audioFileName TEXT")
                db.execSQL("ALTER TABLE spiritual_resources ADD COLUMN audioMimeType TEXT")
                db.execSQL("ALTER TABLE spiritual_resources ADD COLUMN audioSizeBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE spiritual_resources ADD COLUMN audioChunkCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): ChurchYouthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChurchYouthDatabase::class.java,
                    "church_youth_platform.db"
                )
                    .addMigrations(MIGRATION_12_13)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                // Ensure initial seed data is populated and expired posts are purged
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        DatabasePrepopulator.populateInitialData(instance)
                        val now = System.currentTimeMillis()
                        val sevenDayCutoff = now - CommunityPostEntity.SEVEN_DAYS_MILLIS
                        val threeDayCutoff = now - CommunityPostEntity.THREE_DAYS_MILLIS
                        instance.communityDao().purgeExpiredPostsWithCommentsAndLikes(sevenDayCutoff, threeDayCutoff)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                instance
            }
        }
    }
}
