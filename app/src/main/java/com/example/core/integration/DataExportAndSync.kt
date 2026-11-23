package com.example.core.integration

import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.FinancialRecordEntity
import com.example.data.local.entity.UserEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportFinancesToCsv(records: List<FinancialRecordEntity>): String {
        val sb = StringBuilder()
        sb.append("Reference,User,Category,Amount,PaymentMethod,Status,Date,Notes\n")
        for (r in records) {
            val dateStr = dateFormat.format(Date(r.dateMillis))
            sb.append("\"${r.referenceCode}\",\"${r.userName}\",\"${r.category.displayName}\",${r.amount},\"${r.paymentMethod.displayName}\",\"${r.status.name}\",\"$dateStr\",\"${r.purposeOrNotes.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    fun exportAttendanceToCsv(records: List<AttendanceRecordEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,MeetingTitle,MeetingType,Department,Date,Total,Males,Females,FirstTimers,Method,Recorder\n")
        for (r in records) {
            val dateStr = dateFormat.format(Date(r.dateMillis))
            sb.append("\"${r.id}\",\"${r.meetingTitle}\",\"${r.meetingType}\",\"${r.departmentOrGroup}\",\"$dateStr\",${r.totalAttendees},${r.maleCount},${r.femaleCount},${r.firstTimersCount},\"${r.checkInMethod}\",\"${r.recordedByName}\"\n")
        }
        return sb.toString()
    }

    fun exportMembersToCsv(members: List<UserEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,FullName,Email,Phone,Role,Status,Department,Fellowship,RegisteredAt\n")
        for (m in members) {
            val dateStr = dateFormat.format(Date(m.createdAt))
            sb.append("\"${m.id}\",\"${m.fullName}\",\"${m.email}\",\"${m.phone}\",\"${m.role.displayName}\",\"${m.status.name}\",\"${m.department.displayName}\",\"${m.fellowshipGroup}\",\"$dateStr\"\n")
        }
        return sb.toString()
    }
}

// ==========================================
// External Integration Adapters (FR-028)
// ==========================================

data class SmsBroadcastResult(
    val isSuccess: Boolean,
    val recipientCount: Int,
    val messageId: String,
    val statusMessage: String
)

interface SmsGatewayAdapter {
    suspend fun sendSmsBroadcast(recipients: List<String>, message: String): SmsBroadcastResult
}

class MockSmsGatewayAdapter : SmsGatewayAdapter {
    override suspend fun sendSmsBroadcast(recipients: List<String>, message: String): SmsBroadcastResult {
        if (recipients.isEmpty()) {
            return SmsBroadcastResult(false, 0, "", "No recipient phone numbers specified")
        }
        if (message.isBlank()) {
            return SmsBroadcastResult(false, 0, "", "SMS body cannot be blank")
        }
        val msgId = "SMS-" + System.currentTimeMillis()
        return SmsBroadcastResult(
            isSuccess = true,
            recipientCount = recipients.size,
            messageId = msgId,
            statusMessage = "Successfully dispatched $msgId to ${recipients.size} church members via SMS Gateway."
        )
    }
}

data class ChmsSyncResult(
    val isSuccess: Boolean,
    val syncedMembersCount: Int,
    val syncedEventsCount: Int,
    val syncTimestamp: Long = System.currentTimeMillis(),
    val message: String
)

interface ChmsSyncAdapter {
    suspend fun synchronizeWithCentralChurchSystem(): ChmsSyncResult
}

class MockChmsSyncAdapter : ChmsSyncAdapter {
    override suspend fun synchronizeWithCentralChurchSystem(): ChmsSyncResult {
        return ChmsSyncResult(
            isSuccess = true,
            syncedMembersCount = 128,
            syncedEventsCount = 14,
            message = "Bi-directional synchronization with Central Church Management System completed successfully."
        )
    }
}
