package com.example.data.repository

import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.FinancialRecordDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.FinancialRecordEntity
import com.example.data.model.FinancialCategory
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun getAllFinancialRecords(): Flow<List<FinancialRecordEntity>>
    fun getRecordsForUser(userId: String): Flow<List<FinancialRecordEntity>>
    fun getRecordsByCategory(category: FinancialCategory): Flow<List<FinancialRecordEntity>>
    fun getRecordById(id: String): Flow<FinancialRecordEntity?>
    fun getTotalCompletedAmount(): Flow<Double?>
    fun getTotalCompletedAmountForUser(userId: String): Flow<Double?>
    fun getTotalPledgedAmount(): Flow<Double?>
    fun getTotalPledgedAmountForUser(userId: String): Flow<Double?>
    suspend fun recordFinancialEntry(record: FinancialRecordEntity, actorRole: UserRole): Boolean
    suspend fun verifyRecord(id: String, verifierId: String, verifierRole: UserRole, status: PaymentStatus): Boolean
}

class FinanceRepositoryImpl(
    private val financialRecordDao: FinancialRecordDao,
    private val auditLogDao: AuditLogDao
) : FinanceRepository {

    override fun getAllFinancialRecords(): Flow<List<FinancialRecordEntity>> =
        financialRecordDao.getAllFinancialRecords()

    override fun getRecordsForUser(userId: String): Flow<List<FinancialRecordEntity>> =
        financialRecordDao.getRecordsForUser(userId)

    override fun getRecordsByCategory(category: FinancialCategory): Flow<List<FinancialRecordEntity>> =
        financialRecordDao.getRecordsByCategory(category)

    override fun getRecordById(id: String): Flow<FinancialRecordEntity?> =
        financialRecordDao.getRecordById(id)

    override fun getTotalCompletedAmount(): Flow<Double?> =
        financialRecordDao.getTotalCompletedAmount()

    override fun getTotalCompletedAmountForUser(userId: String): Flow<Double?> =
        financialRecordDao.getTotalCompletedAmountForUser(userId)

    override fun getTotalPledgedAmount(): Flow<Double?> =
        financialRecordDao.getTotalPledgedAmount()

    override fun getTotalPledgedAmountForUser(userId: String): Flow<Double?> =
        financialRecordDao.getTotalPledgedAmountForUser(userId)

    override suspend fun recordFinancialEntry(record: FinancialRecordEntity, actorRole: UserRole): Boolean {
        financialRecordDao.insertRecord(record)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = record.recordedById,
                userRole = actorRole,
                actionType = "FINANCE_ENTRY_RECORDED",
                targetEntity = "FinancialRecord",
                targetId = record.id,
                details = "Recorded ${record.category.displayName} of ${record.currency} ${record.amount} (Ref: ${record.referenceCode}, Status: ${record.status.name})"
            )
        )
        return true
    }

    override suspend fun verifyRecord(
        id: String,
        verifierId: String,
        verifierRole: UserRole,
        status: PaymentStatus
    ): Boolean {
        financialRecordDao.updateStatus(id, status, verifierId)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = verifierId,
                userRole = verifierRole,
                actionType = "FINANCE_VERIFICATION",
                targetEntity = "FinancialRecord",
                targetId = id,
                details = "Updated financial record status to ${status.name}"
            )
        )
        return true
    }
}
