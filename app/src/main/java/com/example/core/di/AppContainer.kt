package com.example.core.di

import android.content.Context
import com.example.data.local.ChurchYouthDatabase
import com.example.data.repository.AnnouncementRepository
import com.example.data.repository.AnnouncementRepositoryImpl
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.AttendanceRepositoryImpl
import com.example.data.repository.CommunityRepository
import com.example.data.repository.CommunityRepositoryImpl
import com.example.data.repository.DepartmentRepository
import com.example.data.repository.DepartmentRepositoryImpl
import com.example.data.repository.EventRepository
import com.example.data.repository.EventRepositoryImpl
import com.example.data.repository.FinanceRepository
import com.example.data.repository.FinanceRepositoryImpl
import com.example.data.repository.GamesRepository
import com.example.data.repository.GamesRepositoryImpl
import com.example.data.repository.MultiplayerGameRepository
import com.example.data.repository.MultiplayerGameRepositoryImpl
import com.example.data.repository.SpiritualResourceRepository
import com.example.data.repository.SpiritualResourceRepositoryImpl
import com.example.data.repository.UserRepository
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WeeklyReportRepository
import com.example.data.repository.WeeklyReportRepositoryImpl

interface AppContainer {
    val database: ChurchYouthDatabase
    val gideonitesCloudRepository: com.example.data.repository.GideonitesCloudRepository
    val userRepository: UserRepository
    val eventRepository: EventRepository
    val announcementRepository: AnnouncementRepository
    val financeRepository: FinanceRepository
    val gamesRepository: GamesRepository
    val multiplayerGameRepository: MultiplayerGameRepository
    val weeklyReportRepository: WeeklyReportRepository
    val departmentRepository: DepartmentRepository
    val attendanceRepository: AttendanceRepository
    val communityRepository: CommunityRepository
    val spiritualResourceRepository: SpiritualResourceRepository
    val bibleRepository: com.example.data.repository.BibleRepository
    val notificationService: com.example.core.notification.FellowshipNotificationService
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: ChurchYouthDatabase by lazy {
        ChurchYouthDatabase.getInstance(context)
    }

    override val gideonitesCloudRepository: com.example.data.repository.GideonitesCloudRepository by lazy {
        com.example.data.repository.GideonitesCloudRepository(context.applicationContext)
    }

    override val userRepository: UserRepository by lazy {
        UserRepositoryImpl(
            userDao = database.userDao(),
            auditLogDao = database.auditLogDao(),
            database = database,
            cloudRepository = gideonitesCloudRepository
        )
    }

    override val eventRepository: EventRepository by lazy {
        EventRepositoryImpl(
            eventDao = database.eventDao(),
            auditLogDao = database.auditLogDao(),
            cloudRepository = gideonitesCloudRepository,
            notificationService = notificationService,
            appContext = context.applicationContext
        )
    }

    override val announcementRepository: AnnouncementRepository by lazy {
        AnnouncementRepositoryImpl(
            announcementDao = database.announcementDao(),
            auditLogDao = database.auditLogDao(),
            cloudRepository = gideonitesCloudRepository,
            notificationService = notificationService
        )
    }

    override val financeRepository: FinanceRepository by lazy {
        FinanceRepositoryImpl(
            financialRecordDao = database.financialRecordDao(),
            auditLogDao = database.auditLogDao()
        )
    }

    override val gamesRepository: GamesRepository by lazy {
        GamesRepositoryImpl(
            gameDao = database.gameDao(),
            auditLogDao = database.auditLogDao(),
            cloudRepository = gideonitesCloudRepository
        )
    }

    override val multiplayerGameRepository: MultiplayerGameRepository by lazy {
        MultiplayerGameRepositoryImpl(
            multiplayerGameDao = database.multiplayerGameDao(),
            auditLogDao = database.auditLogDao(),
            cloudRepository = gideonitesCloudRepository,
            notificationService = notificationService
        )
    }

    override val weeklyReportRepository: WeeklyReportRepository by lazy {
        WeeklyReportRepositoryImpl(
            weeklyReportDao = database.weeklyReportDao(),
            auditLogDao = database.auditLogDao()
        )
    }

    override val departmentRepository: DepartmentRepository by lazy {
        DepartmentRepositoryImpl(
            departmentDao = database.departmentDao(),
            userDao = database.userDao(),
            auditLogDao = database.auditLogDao()
        )
    }

    override val attendanceRepository: AttendanceRepository by lazy {
        AttendanceRepositoryImpl(
            attendanceDao = database.attendanceDao(),
            auditLogDao = database.auditLogDao()
        )
    }

    override val communityRepository: CommunityRepository by lazy {
        CommunityRepositoryImpl(
            communityDao = database.communityDao(),
            auditLogDao = database.auditLogDao(),
            userDao = database.userDao(),
            cloudRepository = gideonitesCloudRepository,
            notificationService = notificationService
        )
    }

    override val spiritualResourceRepository: SpiritualResourceRepository by lazy {
        SpiritualResourceRepositoryImpl(
            spiritualResourceDao = database.spiritualResourceDao(),
            cloudRepository = gideonitesCloudRepository,
            notificationService = notificationService,
            appContext = context.applicationContext
        )
    }

    override val bibleRepository: com.example.data.repository.BibleRepository by lazy {
        com.example.data.repository.BibleRepositoryImpl(
            bibleDao = database.bibleDao(),
            cloudRepository = gideonitesCloudRepository,
            notificationService = notificationService,
            appContext = context.applicationContext
        )
    }

    override val notificationService: com.example.core.notification.FellowshipNotificationService by lazy {
        com.example.core.notification.FellowshipNotificationService(
            context = context,
            notificationDao = database.notificationDao(),
            cloudRepository = gideonitesCloudRepository
        )
    }
}
