package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.FinancialCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus

@Entity(
    tableName = "financial_records",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["dateMillis"]),
        Index(value = ["category"]),
        Index(value = ["status"]),
        Index(value = ["referenceCode"])
    ]
)
data class FinancialRecordEntity(
    @PrimaryKey
    val id: String, // UUID
    val userId: String,
    val userName: String,
    val category: FinancialCategory,
    val amount: Double,
    val currency: String = "NGN",
    val dateMillis: Long = System.currentTimeMillis(),
    val status: PaymentStatus = PaymentStatus.COMPLETED,
    val paymentMethod: PaymentMethod = PaymentMethod.BANK_TRANSFER,
    val referenceCode: String = "",
    val purposeOrNotes: String = "",
    val recordedById: String,
    val verifiedById: String? = null,
    val verifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
