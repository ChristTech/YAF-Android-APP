package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FinancialRecordEntity
import com.example.data.model.FinancialCategory
import com.example.data.model.PaymentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialRecordDao {

    @Query("SELECT * FROM financial_records ORDER BY dateMillis DESC")
    fun getAllFinancialRecords(): Flow<List<FinancialRecordEntity>>

    @Query("SELECT * FROM financial_records WHERE userId = :userId ORDER BY dateMillis DESC")
    fun getRecordsForUser(userId: String): Flow<List<FinancialRecordEntity>>

    @Query("SELECT * FROM financial_records WHERE category = :category ORDER BY dateMillis DESC")
    fun getRecordsByCategory(category: FinancialCategory): Flow<List<FinancialRecordEntity>>

    @Query("SELECT * FROM financial_records WHERE status = :status ORDER BY dateMillis DESC")
    fun getRecordsByStatus(status: PaymentStatus): Flow<List<FinancialRecordEntity>>

    @Query("SELECT * FROM financial_records WHERE id = :id LIMIT 1")
    fun getRecordById(id: String): Flow<FinancialRecordEntity?>

    @Query("SELECT SUM(amount) FROM financial_records WHERE status = 'COMPLETED'")
    fun getTotalCompletedAmount(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM financial_records WHERE userId = :userId AND status = 'COMPLETED'")
    fun getTotalCompletedAmountForUser(userId: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM financial_records WHERE status = 'PLEDGED'")
    fun getTotalPledgedAmount(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM financial_records WHERE userId = :userId AND status = 'PLEDGED'")
    fun getTotalPledgedAmountForUser(userId: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FinancialRecordEntity)

    @Update
    suspend fun updateRecord(record: FinancialRecordEntity)

    @Query("UPDATE financial_records SET status = :status, verifiedById = :verifiedById, verifiedAt = :verifiedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: PaymentStatus, verifiedById: String, verifiedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM financial_records WHERE id = :id")
    suspend fun deleteRecord(id: String)
}
