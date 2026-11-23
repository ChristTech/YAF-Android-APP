package com.example.core.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.dao.NotificationDao
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.NotificationType
import com.example.data.repository.GideonitesCloudRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FellowshipNotificationService(
    private val context: Context,
    private val notificationDao: NotificationDao,
    private val cloudRepository: GideonitesCloudRepository? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val syncMutex = Mutex()
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        const val CHANNEL_ID = "gideonites_live_alerts_v2"
        const val CHANNEL_NAME = "The Gideonites Live Fellowship Alerts"
        const val CHANNEL_DESC = "Real-time cross-device alerts for announcements, programmes, registrations, forum discussions, scriptures, and live games."
        private const val DELIVERY_PREFS_NAME = "fellowship_notif_delivery_prefs"
        private const val KEY_LOCAL_ORIGIN_IDS = "local_origin_ids"
        private const val KEY_DELIVERED_PREFIX = "delivered_status_bar_"
        private const val MAX_PERSISTED_IDS = 300
    }

    private val deliveryPrefs by lazy {
        context.getSharedPreferences(DELIVERY_PREFS_NAME, Context.MODE_PRIVATE)
    }
    private val deliveryPrefsLock = Any()

    /**
     * Unique per-device installation identifier so two devices logged into the same or different
     * accounts always distinguish local actions from cross-device remote notifications.
     */
    val deviceInstanceId: String by lazy {
        val prefs = context.getSharedPreferences("fellowship_device_prefs", Context.MODE_PRIVATE)
        var existing = prefs.getString("device_instance_id", null)
        if (existing.isNullOrBlank()) {
            existing = "dev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            prefs.edit().putString("device_instance_id", existing).apply()
        }
        existing
    }

    // Tracks which notification IDs originated on this physical device (persisted across restarts)
    private val locallyOriginatingIds = ConcurrentHashMap.newKeySet<String>()

    // Tracks which notification IDs have been surfaced to the device OS status bar per user session
    private val deliveredToStatusBarByUser = mutableMapOf<String, MutableSet<String>>()

    init {
        createNotificationChannel()
        synchronized(deliveryPrefsLock) {
            deliveryPrefs.getStringSet(KEY_LOCAL_ORIGIN_IDS, null)?.let { saved ->
                locallyOriginatingIds.addAll(saved)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    private val _liveBannerNotification = MutableStateFlow<NotificationEntity?>(null)
    val liveBannerNotification: StateFlow<NotificationEntity?> = _liveBannerNotification.asStateFlow()

    fun dismissLiveBanner() {
        _liveBannerNotification.value = null
    }

    private fun recordLocalOriginIds(vararg ids: String) {
        val valid = ids.filter { it.isNotBlank() }
        if (valid.isEmpty()) return
        locallyOriginatingIds.addAll(valid)
        synchronized(deliveryPrefsLock) {
            val current = deliveryPrefs.getStringSet(KEY_LOCAL_ORIGIN_IDS, emptySet())
                ?.toMutableList() ?: mutableListOf()
            for (id in valid) {
                if (!current.contains(id)) current.add(id)
            }
            val trimmed = if (current.size > MAX_PERSISTED_IDS) {
                current.takeLast(MAX_PERSISTED_IDS).toHashSet()
            } else {
                current.toHashSet()
            }
            deliveryPrefs.edit().putStringSet(KEY_LOCAL_ORIGIN_IDS, trimmed).apply()
        }
    }

    private fun isLocalOriginId(rawId: String, cleanId: String): Boolean {
        if (locallyOriginatingIds.contains(rawId) || locallyOriginatingIds.contains(cleanId)) {
            return true
        }
        synchronized(deliveryPrefsLock) {
            val saved = deliveryPrefs.getStringSet(KEY_LOCAL_ORIGIN_IDS, emptySet()).orEmpty()
            return saved.contains(rawId) || saved.contains(cleanId)
        }
    }

    private fun getDeliveredSetForUser(userId: String): MutableSet<String> {
        return deliveredToStatusBarByUser.getOrPut(userId) {
            deliveryPrefs.getStringSet("$KEY_DELIVERED_PREFIX$userId", emptySet())
                ?.toMutableSet() ?: mutableSetOf()
        }
    }

    private fun isDeliveredToStatusBar(userId: String, notifId: String): Boolean {
        synchronized(deliveryPrefsLock) {
            return getDeliveredSetForUser(userId).contains(notifId)
        }
    }

    private fun markDeliveredToStatusBar(userId: String, notifId: String) {
        if (userId.isBlank() || notifId.isBlank()) return
        synchronized(deliveryPrefsLock) {
            val set = getDeliveredSetForUser(userId)
            if (set.add(notifId)) {
                val capped = if (set.size > MAX_PERSISTED_IDS) {
                    set.toList().takeLast(MAX_PERSISTED_IDS).toHashSet()
                } else {
                    HashSet(set)
                }
                if (capped.size != set.size) {
                    set.clear()
                    set.addAll(capped)
                }
                deliveryPrefs.edit().putStringSet("$KEY_DELIVERED_PREFIX$userId", capped).apply()
            }
        }
    }

    private fun sanitizeNotificationId(rawId: String): String {
        return rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(120).ifBlank {
            "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postNotification(
        title: String,
        message: String,
        type: NotificationType,
        authorName: String,
        authorRole: String,
        isUrgent: Boolean = false,
        deepLink: String = "",
        id: String = UUID.randomUUID().toString(),
        publishToCloud: Boolean = true,
        recipientUserId: String? = null
    ) {
        val cleanId = sanitizeNotificationId(id)
        recordLocalOriginIds(id, cleanId)

        val now = System.currentTimeMillis()
        val entity = NotificationEntity(
            id = cleanId,
            title = title,
            message = message,
            type = type,
            authorName = authorName,
            authorRole = authorRole,
            timestamp = now,
            isRead = false,
            actionDeepLink = deepLink,
            isUrgent = isUrgent
        )

        scope.launch {
            try {
                syncMutex.withLock {
                    val existing = notificationDao.getNotificationByIdSync(cleanId)
                        ?: notificationDao.getNotificationByIdSync(id)
                    if (existing == null) {
                        notificationDao.insertNotification(entity)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (publishToCloud) {
                try {
                    cloudRepository?.publishFellowshipNotification(
                        notificationId = cleanId,
                        title = title,
                        message = message,
                        type = type.name,
                        authorName = authorName,
                        authorRole = authorRole,
                        senderDeviceId = deviceInstanceId,
                        recipientUserId = recipientUserId,
                        actionDeepLink = deepLink,
                        isUrgent = isUrgent,
                        timestampMillis = now
                    )
                } catch (e: Exception) {
                    // Local notification already saved
                }
            }
        }

        // Trigger real Android OS status bar notification on the device
        val shown = showSystemNotification(entity)
        if (shown) {
            val activeUserId = recipientUserId?.takeIf { it.isNotBlank() }
                ?: cloudRepository?.activeSessionUserId?.takeIf { it.isNotBlank() }
            if (!activeUserId.isNullOrBlank()) {
                markDeliveredToStatusBar(activeUserId, cleanId)
            }
        }
    }

    /**
     * Broadcasts a notification to all OTHER devices via Cloud Firestore without showing a
     * duplicate notification on the originating device (used e.g. when Device 1 already showed
     * a personal registration pass confirmation and wants all other devices to be alerted that
     * someone registered for the programme).
     */
    fun broadcastCrossDeviceOnly(
        id: String,
        title: String,
        message: String,
        type: NotificationType,
        authorName: String,
        authorRole: String,
        isUrgent: Boolean = false,
        recipientUserId: String? = null
    ) {
        val cleanId = sanitizeNotificationId(id)
        recordLocalOriginIds(id, cleanId)
        val now = System.currentTimeMillis()
        scope.launch {
            try {
                cloudRepository?.publishFellowshipNotification(
                    notificationId = cleanId,
                    title = title,
                    message = message,
                    type = type.name,
                    authorName = authorName,
                    authorRole = authorRole,
                    senderDeviceId = deviceInstanceId,
                    recipientUserId = recipientUserId,
                    isUrgent = isUrgent,
                    timestampMillis = now
                )
            } catch (e: Exception) {
                // Ignore when offline
            }
        }
    }

    suspend fun pushLocalNotificationsToCloud() {
        val cloudRepo = cloudRepository ?: return
        try {
            val recentLocal = notificationDao.getAllNotifications().firstOrNull().orEmpty().take(25)
            for (notif in recentLocal) {
                if (notif.authorRole.equals("Registration Confirmed", ignoreCase = true) ||
                    notif.authorRole.equals("Scheduled Reminder", ignoreCase = true)
                ) {
                    continue
                }
                try {
                    cloudRepo.publishFellowshipNotification(
                        notificationId = sanitizeNotificationId(notif.id),
                        title = notif.title,
                        message = notif.message,
                        type = notif.type.name,
                        authorName = notif.authorName,
                        authorRole = notif.authorRole,
                        senderDeviceId = deviceInstanceId,
                        actionDeepLink = notif.actionDeepLink,
                        isUrgent = notif.isUrgent,
                        timestampMillis = notif.timestamp
                    )
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Delivers a notification originating from Cloud Firestore to this user's device
     * if it hasn't been received on this device yet.
     */
    suspend fun syncRemoteNotificationIfNew(
        id: String,
        title: String,
        message: String,
        type: NotificationType,
        authorName: String,
        authorRole: String,
        timestamp: Long = System.currentTimeMillis(),
        isUrgent: Boolean = false,
        senderDeviceId: String? = null,
        recipientUserId: String? = null,
        activeUserId: String? = null
    ) {
        try {
            val cleanId = sanitizeNotificationId(id)
            // Skip if this notification originated on this physical device
            if (!senderDeviceId.isNullOrBlank() && senderDeviceId == deviceInstanceId) {
                return
            }
            if (isLocalOriginId(id, cleanId)) {
                return
            }
            if (!recipientUserId.isNullOrBlank() && !activeUserId.isNullOrBlank() && recipientUserId != activeUserId) {
                return
            }

            var insertedEntity: NotificationEntity? = null
            syncMutex.withLock {
                val existing = notificationDao.getNotificationByIdSync(cleanId)
                    ?: notificationDao.getNotificationByIdSync(id)
                if (existing == null) {
                    val entity = NotificationEntity(
                        id = cleanId,
                        title = title,
                        message = message,
                        type = type,
                        authorName = authorName,
                        authorRole = authorRole,
                        timestamp = timestamp,
                        isRead = false,
                        isUrgent = isUrgent
                    )
                    notificationDao.insertNotification(entity)
                    insertedEntity = entity
                }
            }

            insertedEntity?.let { entity ->
                _liveBannerNotification.value = entity
                val shown = showSystemNotification(entity)
                if (shown && !activeUserId.isNullOrBlank()) {
                    markDeliveredToStatusBar(activeUserId, cleanId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Ensures that when any user logs in, receives a background sync update, or grants notification permission
     * on the device, any unread notifications not yet shown on the status bar are delivered reliably.
     */
    suspend fun deliverUnreadNotificationsToDeviceForUser(userId: String) {
        if (!hasNotificationPermission()) return
        try {
            val unreadList = notificationDao.getUnreadNotificationsSync()
            for (notif in unreadList.take(15)) {
                if (!isDeliveredToStatusBar(userId, notif.id)) {
                    val shown = showSystemNotification(notif)
                    if (shown) {
                        markDeliveredToStatusBar(userId, notif.id)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSystemNotification(entity: NotificationEntity): Boolean {
        if (!hasNotificationPermission()) return false
        return try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_notifications", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                entity.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationId = entity.id.hashCode()
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_flame)
                .setColor(0xFFD92D20.toInt())
                .setContentTitle(entity.title)
                .setContentText("${entity.authorRole} • ${entity.message}")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${entity.authorRole} (${entity.authorName}):\n${entity.message}"))
                .setWhen(entity.timestamp)
                .setShowWhen(true)
                .setPriority(if (entity.isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager?.notify(notificationId, builder.build())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun markAsRead(id: String) {
        scope.launch {
            notificationDao.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        scope.launch {
            notificationDao.markAllAsRead()
        }
    }

    fun deleteNotification(id: String) {
        scope.launch {
            notificationDao.deleteNotification(id)
        }
    }
}

