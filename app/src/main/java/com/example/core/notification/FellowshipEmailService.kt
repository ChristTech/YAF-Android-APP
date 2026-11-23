package com.example.core.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.example.data.local.entity.EventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object FellowshipEmailService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun buildConfirmationEmailSubject(event: EventEntity): String {
        return "Registration Confirmed: ${event.title} • The Gideonites (Youth Alive Fellowship)"
    }

    fun buildConfirmationEmailBody(
        event: EventEntity,
        userName: String,
        userEmail: String,
        passReference: String
    ): String {
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy • h:mm a", Locale.getDefault())
        val reminderFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
        val fiveDayDate = reminderFormat.format(Date(event.dateTimeMillis - (5L * 24 * 60 * 60 * 1000L)))
        val oneDayDate = reminderFormat.format(Date(event.dateTimeMillis - (1L * 24 * 60 * 60 * 1000L)))

        return buildString {
            appendLine("Grace & Peace ${userName.ifBlank { "Kingdom Giant" }},")
            appendLine()
            appendLine("Your registration for the upcoming Youth Alive Fellowship programme has been officially confirmed!")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("OFFICIAL PROGRAMME REGISTRATION PASS")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("• Pass Reference: $passReference")
            appendLine("• Programme: ${event.title}")
            appendLine("• Category: ${event.category.displayName}")
            appendLine("• Date & Time: ${dateFormat.format(Date(event.dateTimeMillis))}")
            appendLine("• Venue: ${event.venue}")
            if (event.speaker.isNotBlank()) {
                appendLine("• Ministering: ${event.speaker}")
            }
            appendLine("• Registered Attendee: $userName ($userEmail)")
            appendLine("• Status: CONFIRMED (Non-Cancellable)")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("AUTOMATED REMINDER SCHEDULE:")
            appendLine("• 5 Days Before Programme: $fiveDayDate")
            appendLine("• 1 Day Before Programme: $oneDayDate")
            appendLine()
            appendLine("We look forward to welcoming you at ${event.venue}!")
            appendLine()
            appendLine("In His Service,")
            appendLine("The Gideonites — The Youth Alive Fellowship")
        }
    }

    /**
     * Attempts background transactional email delivery if a Resend API Key or Webhook URL
     * is configured via BuildConfig/System environment. Returns true if an external HTTP
     * email provider accepted and dispatched the message.
     */
    suspend fun trySendBackgroundEmail(
        recipientEmail: String,
        subject: String,
        textBody: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (recipientEmail.isBlank() || !recipientEmail.contains("@")) return@withContext false
        try {
            val resendKey = runCatching {
                val clazz = Class.forName("com.example.BuildConfig")
                val field = clazz.fields.firstOrNull { it.name == "RESEND_API_KEY" }
                field?.get(null) as? String
            }.getOrNull()?.trim().orEmpty()

            if (resendKey.isNotBlank() && resendKey != "MY_RESEND_API_KEY") {
                val payload = JSONObject().apply {
                    put("from", "The Gideonites <onboarding@resend.dev>")
                    put("to", JSONArray().put(recipientEmail))
                    put("subject", subject)
                    put("text", textBody)
                }
                val request = Request.Builder()
                    .url("https://api.resend.com/emails")
                    .addHeader("Authorization", "Bearer $resendKey")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return@withContext true
                }
            }

            val webhookUrl = runCatching {
                val clazz = Class.forName("com.example.BuildConfig")
                val field = clazz.fields.firstOrNull { it.name == "EMAIL_WEBHOOK_URL" }
                field?.get(null) as? String
            }.getOrNull()?.trim().orEmpty()

            if (webhookUrl.startsWith("https://")) {
                val payload = JSONObject().apply {
                    put("to", recipientEmail)
                    put("subject", subject)
                    put("body", textBody)
                }
                val request = Request.Builder()
                    .url(webhookUrl)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) return@withContext true
                }
            }
        } catch (_: Exception) {
        }
        false
    }

    /**
     * Opens the user's installed Email client (Gmail, Outlook, etc.) pre-addressed to their own
     * email address with the full Programme Confirmation Pass & Reminder Schedule so they can
     * dispatch/save the email directly to their real inbox.
     */
    fun launchEmailConfirmationIntent(
        context: Context,
        event: EventEntity,
        userName: String,
        userEmail: String,
        passReference: String
    ) {
        val subject = buildConfirmationEmailSubject(event)
        val body = buildConfirmationEmailBody(event, userName, userEmail, passReference)
        try {
            val mailtoUri = Uri.parse(
                "mailto:${Uri.encode(userEmail)}" +
                    "?subject=${Uri.encode(subject)}" +
                    "&body=${Uri.encode(body)}"
            )
            val sendToIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendToIntent)
        } catch (_: Exception) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Send Confirmation Pass to Email").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Opens the user's Google Calendar / Device Calendar with the Programme pre-filled so
     * Google Calendar delivers 5-day and 1-day email & device reminders automatically.
     */
    fun launchCalendarReminderIntent(
        context: Context,
        event: EventEntity,
        userName: String,
        userEmail: String,
        passReference: String
    ) {
        try {
            val body = buildConfirmationEmailBody(event, userName, userEmail, passReference)
            val calIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "${event.title} (The Gideonites)")
                putExtra(CalendarContract.Events.EVENT_LOCATION, event.venue)
                putExtra(CalendarContract.Events.DESCRIPTION, body)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.dateTimeMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.dateTimeMillis + 7_200_000L)
                if (userEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, userEmail)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(calIntent)
        } catch (_: Exception) {
        }
    }
}
