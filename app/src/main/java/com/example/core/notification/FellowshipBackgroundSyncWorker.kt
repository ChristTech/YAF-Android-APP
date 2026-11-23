package com.example.core.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ChurchYouthApp
import com.example.core.di.AppContainer
import com.example.core.di.DefaultAppContainer
import com.example.data.local.entity.CommunityPostEntity
import com.example.data.local.entity.NotificationType
import com.example.data.repository.EventRepositoryImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Background Jetpack WorkManager worker that periodically syncs Cloud Firestore updates
 * (notifications, programmes, community posts, live games, daily scriptures), purges expired
 * programmes/posts, evaluates due 5-day/1-day programme reminders, and surfaces real Android
 * OS status-bar notifications even when the app is closed or backgrounded.
 */
class FellowshipBackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container: AppContainer = (applicationContext as? ChurchYouthApp)?.container
            ?: DefaultAppContainer(applicationContext)

        val nowMillis = System.currentTimeMillis()
        val activeSession = try {
            container.database.userDao().getActiveSessionSync()
        } catch (_: Exception) {
            null
        }
        val activeUserId = activeSession?.userId?.takeIf { it.isNotBlank() }
        if (activeUserId != null) {
            container.gideonitesCloudRepository.activeSessionUserId = activeUserId
        }

        // 1. Always purge expired past-date events & expired forum posts locally/remotely
        try {
            container.eventRepository.purgeExpiredEvents(nowMillis)
        } catch (_: Exception) {
        }
        try {
            container.communityRepository.purgeExpiredPosts(nowMillis)
        } catch (_: Exception) {
        }

        // Only pull & surface remote notifications when a fellowship session is signed in on device
        if (activeUserId == null) {
            return@withContext Result.success()
        }

        val cloudRepo = container.gideonitesCloudRepository
        val notificationService = container.notificationService

        // 2. Sync explicit cross-device notifications (/fellowship_notifications)
        try {
            val cloudNotifs = cloudRepo.fetchFellowshipNotificationsOnce()
            for (cn in cloudNotifs) {
                if (cn.id.isBlank() || cn.title.isBlank()) continue
                val resolvedType = runCatching {
                    NotificationType.valueOf(cn.type)
                }.getOrDefault(NotificationType.ANNOUNCEMENT)
                val tsMillis = cn.timestampMillis.takeIf { it > 0L }
                    ?: cn.createdAt?.toDate()?.time
                    ?: nowMillis
                notificationService.syncRemoteNotificationIfNew(
                    id = cn.id,
                    title = cn.title,
                    message = cn.message,
                    type = resolvedType,
                    authorName = cn.authorName.ifBlank { "Youth Leadership" },
                    authorRole = cn.authorRole.ifBlank { "Executive" },
                    isUrgent = cn.isUrgent,
                    timestamp = tsMillis,
                    senderDeviceId = cn.senderDeviceId,
                    recipientUserId = cn.recipientUserId,
                    activeUserId = activeUserId
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        // 3. Sync Youth Programmes & filter out expired dates before notifying
        try {
            val programmes = cloudRepo.fetchYouthProgrammesOnce()
            if (programmes.isNotEmpty()) {
                container.eventRepository.syncCloudProgrammesToLocalDb(programmes)
                for (prog in programmes) {
                    if (prog.id.isBlank()) continue
                    val resolvedDate = prog.startDateMillis.takeIf { it > 0L } ?: nowMillis
                    if (EventRepositoryImpl.isEventDatePassed(resolvedDate, nowMillis)) {
                        continue
                    }
                    val rawId = prog.id.removePrefix("prog_")
                    val tsMillis = prog.createdAt?.toDate()?.time ?: nowMillis
                    notificationService.syncRemoteNotificationIfNew(
                        id = "notif_event_$rawId",
                        title = "📅 Upcoming Youth Programme: ${prog.titleEn}",
                        message = "${prog.venue} • Minister: ${prog.speaker?.takeIf { it.isNotBlank() } ?: "Youth Alive Ministry"}. ${prog.descriptionEn}",
                        type = NotificationType.EVENT,
                        authorName = prog.contactPerson?.takeIf { it.isNotBlank() } ?: "Youth Leadership",
                        authorRole = "Executive",
                        timestamp = tsMillis,
                        activeUserId = activeUserId
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        // 4. Sync Community Posts & Announcements
        try {
            val posts = cloudRepo.fetchCommunityPostsOnce()
            if (posts.isNotEmpty()) {
                container.communityRepository.syncCloudPostsToLocalDb(
                    cloudPosts = posts,
                    announcementDao = container.database.announcementDao(),
                    nowMillis = nowMillis
                )
                for (post in posts.filter { !it.isHidden }) {
                    val createdAtMillis = post.createdAt?.toDate()?.time ?: nowMillis
                    val isAnnouncement = post.category.startsWith("ANNOUNCEMENT", ignoreCase = true)
                    val isUrgent = post.category.equals("ANNOUNCEMENT_URGENT", ignoreCase = true)
                    val retentionMillis = CommunityPostEntity.retentionDurationForCategory(post.category)
                    if (isAnnouncement || (nowMillis - createdAtMillis < retentionMillis)) {
                        notificationService.syncRemoteNotificationIfNew(
                            id = if (post.id.startsWith("ann_")) "notif_${post.id}" else "notif_post_${post.id}",
                            title = when {
                                isUrgent -> "🚨 URGENT: ${post.title}"
                                isAnnouncement -> "📢 ${post.title}"
                                else -> "💬 New Forum Post (${post.category}): ${post.title}"
                            },
                            message = post.content,
                            type = if (isAnnouncement) {
                                NotificationType.ANNOUNCEMENT
                            } else {
                                NotificationType.COMMUNITY_POST
                            },
                            authorName = post.authorName,
                            authorRole = post.authorRole,
                            isUrgent = isUrgent,
                            timestamp = createdAtMillis,
                            activeUserId = activeUserId
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        // 5. Sync Live Games & Quiz Sessions (skipping DRAFT, CANCELLED, and COMPLETED games)
        try {
            val quizSessions = cloudRepo.fetchCloudQuizSessionsOnce()
            if (quizSessions.isNotEmpty()) {
                container.multiplayerGameRepository.syncCloudQuizSessionsToLocalDb(
                    cloudSessions = quizSessions,
                    gameDao = container.database.gameDao()
                )
                for (cs in quizSessions) {
                    if (!cs.status.equals("DRAFT", ignoreCase = true) &&
                        !cs.status.equals("CANCELLED", ignoreCase = true) &&
                        !cs.status.equals("COMPLETED", ignoreCase = true)
                    ) {
                        val rawQuizId = cs.id.removePrefix("quiz_")
                        val tsMillis = cs.createdAt?.toDate()?.time ?: nowMillis
                        notificationService.syncRemoteNotificationIfNew(
                            id = "notif_game_$rawQuizId",
                            title = "🎮 Live Fellowship Game: ${cs.titleEn}",
                            message = "Hosted by ${cs.hostName} • ${cs.totalQuestions} rounds.",
                            type = NotificationType.GAME_TIME,
                            authorName = cs.hostName.ifBlank { "Youth Leadership" },
                            authorRole = "Executive",
                            isUrgent = true,
                            timestamp = tsMillis,
                            activeUserId = activeUserId
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        // 6. Sync Daily Scriptures & Spiritual Resources
        try {
            val scriptures = cloudRepo.fetchDailyScripturesOnce()
            if (scriptures.isNotEmpty()) {
                container.spiritualResourceRepository.syncCloudScripturesToLocalDb(scriptures)
                container.bibleRepository.syncCloudBibleRecordsToLocalDb(scriptures)
                for (s in scriptures) {
                    if (s.id.isBlank()) continue
                    val rawId = s.id.removePrefix("scripture_")
                    val tsMillis = s.createdAt?.toDate()?.time ?: nowMillis
                    val prompt = s.prayerPromptEn.trim()
                    val isGeneralResource = prompt.startsWith("{") && !prompt.contains("\"SCRIPTURE_OF_THE_DAY\"")
                    if (!isGeneralResource) {
                        notificationService.syncRemoteNotificationIfNew(
                            id = "notif_scripture_$rawId",
                            title = "📖 Scripture for the Day: ${s.referenceEn}",
                            message = "\"${s.verseTextEn}\" — Posted by ${s.authorName.ifBlank { "Youth Leadership" }} for The Gideonites - The Youth Alive Fellowship.",
                            type = NotificationType.ANNOUNCEMENT,
                            authorName = s.authorName.ifBlank { "Youth Leadership" },
                            authorRole = "Youth Leadership",
                            timestamp = tsMillis,
                            activeUserId = activeUserId
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        // 7. Evaluate any due 5-day or 1-day registered programme reminders in the background
        try {
            dispatchDueReminders(container, activeUserId, nowMillis)
        } catch (_: Exception) {
        }

        // 8. Surface any remaining unread notifications to the OS status bar
        try {
            notificationService.deliverUnreadNotificationsToDeviceForUser(activeUserId)
        } catch (_: Exception) {
        }

        Result.success()
    }

    companion object {
        private const val PERIODIC_SYNC_WORK_NAME = "fellowship_periodic_background_sync"
        private const val IMMEDIATE_SYNC_WORK_NAME = "fellowship_immediate_background_sync"

        internal suspend fun dispatchDueReminders(
            container: AppContainer,
            userId: String,
            nowMillis: Long = System.currentTimeMillis()
        ) {
            val dueReminders = container.eventRepository.checkAndMarkDueEmailReminders(userId, nowMillis)
            if (dueReminders.isEmpty()) return
            val dateFmt = SimpleDateFormat("EEE, MMM dd • h:mm a", Locale.getDefault())
            for ((event, reg, daysBefore) in dueReminders) {
                val targetEmail = reg.userEmail.ifBlank { "your registered email" }
                val label = if (daysBefore == 5) "5 Days Before" else "1 Day Before (Tomorrow)"
                val subject = "Reminder ($label): ${event.title} • The Gideonites"
                val body = "Grace & Peace ${reg.userName},\n\nThis is your $label reminder that you are registered for '${event.title}' happening on ${dateFmt.format(Date(event.dateTimeMillis))} at ${event.venue}.\n\nIn His Service,\nThe Gideonites — The Youth Alive Fellowship"
                val sentViaApi = FellowshipEmailService.trySendBackgroundEmail(
                    recipientEmail = targetEmail,
                    subject = subject,
                    textBody = body
                )
                container.notificationService.postNotification(
                    id = "notif_rem_${daysBefore}d_${event.id}_$userId",
                    title = "⏰ Programme Reminder ($label): ${event.title}",
                    message = if (sentViaApi) {
                        "Emailed to $targetEmail — '${event.title}' is happening in $daysBefore ${if (daysBefore == 1) "day" else "days"} on ${dateFmt.format(Date(event.dateTimeMillis))} at ${event.venue}."
                    } else {
                        "Reminder for ${reg.userName} ($targetEmail): '${event.title}' is happening in $daysBefore ${if (daysBefore == 1) "day" else "days"} on ${dateFmt.format(Date(event.dateTimeMillis))} at ${event.venue}."
                    },
                    type = NotificationType.EVENT,
                    authorName = "The Gideonites Programme Desk",
                    authorRole = "Scheduled Reminder",
                    isUrgent = daysBefore == 1,
                    publishToCloud = false,
                    recipientUserId = userId
                )
            }
        }

        fun schedulePeriodicSync(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val periodicRequest = PeriodicWorkRequestBuilder<FellowshipBackgroundSyncWorker>(
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    PERIODIC_SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
            }
        }

        fun triggerImmediateSync(context: Context) {
            runCatching {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val immediateRequest = OneTimeWorkRequestBuilder<FellowshipBackgroundSyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    IMMEDIATE_SYNC_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    immediateRequest
                )
            }
        }

        fun scheduleProgrammeReminders(
            context: Context,
            eventId: String,
            eventTitle: String,
            eventDateMillis: Long,
            userId: String
        ) {
            runCatching {
                val now = System.currentTimeMillis()
                val wm = WorkManager.getInstance(context.applicationContext)
                val tag = "prog_reminder_$eventId"

                val delay5Days = (eventDateMillis - EventRepositoryImpl.FIVE_DAYS_MILLIS) - now
                if (delay5Days > 0L) {
                    val data5 = Data.Builder()
                        .putString(ProgrammeReminderWorker.KEY_EVENT_ID, eventId)
                        .putString(ProgrammeReminderWorker.KEY_EVENT_TITLE, eventTitle)
                        .putString(ProgrammeReminderWorker.KEY_USER_ID, userId)
                        .putInt(ProgrammeReminderWorker.KEY_DAYS_BEFORE, 5)
                        .build()
                    val req5 = OneTimeWorkRequestBuilder<ProgrammeReminderWorker>()
                        .setInitialDelay(delay5Days, TimeUnit.MILLISECONDS)
                        .setInputData(data5)
                        .addTag(tag)
                        .build()
                    wm.enqueueUniqueWork(
                        "prog_rem_5d_${eventId}_$userId",
                        ExistingWorkPolicy.REPLACE,
                        req5
                    )
                }

                val delay1Day = (eventDateMillis - EventRepositoryImpl.ONE_DAY_MILLIS) - now
                if (delay1Day > 0L) {
                    val data1 = Data.Builder()
                        .putString(ProgrammeReminderWorker.KEY_EVENT_ID, eventId)
                        .putString(ProgrammeReminderWorker.KEY_EVENT_TITLE, eventTitle)
                        .putString(ProgrammeReminderWorker.KEY_USER_ID, userId)
                        .putInt(ProgrammeReminderWorker.KEY_DAYS_BEFORE, 1)
                        .build()
                    val req1 = OneTimeWorkRequestBuilder<ProgrammeReminderWorker>()
                        .setInitialDelay(delay1Day, TimeUnit.MILLISECONDS)
                        .setInputData(data1)
                        .addTag(tag)
                        .build()
                    wm.enqueueUniqueWork(
                        "prog_rem_1d_${eventId}_$userId",
                        ExistingWorkPolicy.REPLACE,
                        req1
                    )
                }
            }
        }

        fun cancelProgrammeReminders(context: Context, eventId: String) {
            runCatching {
                WorkManager.getInstance(context.applicationContext)
                    .cancelAllWorkByTag("prog_reminder_$eventId")
            }
        }
    }
}

/**
 * Fires scheduled 5-day and 1-day registered programme reminders reliably in the background
 * via WorkManager without requiring Android exact-alarm permissions.
 */
class ProgrammeReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return@withContext Result.success()
        val userId = inputData.getString(KEY_USER_ID) ?: return@withContext Result.success()

        val container: AppContainer = (applicationContext as? ChurchYouthApp)?.container
            ?: DefaultAppContainer(applicationContext)

        val now = System.currentTimeMillis()
        try {
            container.eventRepository.purgeExpiredEvents(now)
            val event = container.eventRepository.getEventByIdSync(eventId) ?: return@withContext Result.success()
            if (EventRepositoryImpl.isEventDatePassed(event.dateTimeMillis, now)) {
                return@withContext Result.success()
            }
            FellowshipBackgroundSyncWorker.dispatchDueReminders(container, userId, now)
            container.notificationService.deliverUnreadNotificationsToDeviceForUser(userId)
        } catch (_: Exception) {
        }
        Result.success()
    }

    companion object {
        const val KEY_EVENT_ID = "event_id"
        const val KEY_EVENT_TITLE = "event_title"
        const val KEY_USER_ID = "user_id"
        const val KEY_DAYS_BEFORE = "days_before"
    }
}
