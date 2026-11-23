package com.example.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FinancialRecordEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.model.FinancialCategory
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.UserRole
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

data class FinanceUiState(
    val isGiveModalOpen: Boolean = false,
    val showExecutiveView: Boolean = false,
    val message: String? = null
)

class FinanceViewModel(
    private val financeRepository: FinanceRepository,
    private val session: SessionEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    val myRecords: StateFlow<List<FinancialRecordEntity>> = financeRepository
        .getRecordsForUser(session.userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRecords: StateFlow<List<FinancialRecordEntity>> = financeRepository
        .getAllFinancialRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val myTotalCompleted: StateFlow<Double?> = financeRepository
        .getTotalCompletedAmountForUser(session.userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val globalTotalCompleted: StateFlow<Double?> = financeRepository
        .getTotalCompletedAmount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun setGiveModalOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isGiveModalOpen = open)
    }

    fun setExecutiveView(isExec: Boolean) {
        _uiState.value = _uiState.value.copy(showExecutiveView = isExec)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun recordGiving(
        amount: Double,
        category: FinancialCategory,
        method: PaymentMethod,
        isPledge: Boolean,
        notes: String
    ) {
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(message = "Please enter a valid amount.")
            return
        }

        viewModelScope.launch {
            val randomRef = "CYP-${System.currentTimeMillis().toString().takeLast(6)}-${Random.nextInt(100, 999)}"
            val status = if (isPledge) PaymentStatus.PLEDGED else PaymentStatus.COMPLETED

            val record = FinancialRecordEntity(
                id = UUID.randomUUID().toString(),
                userId = session.userId,
                userName = session.userName,
                category = category,
                amount = amount,
                currency = "NGN",
                status = status,
                paymentMethod = method,
                referenceCode = randomRef,
                purposeOrNotes = notes.trim(),
                recordedById = session.userId
            )

            financeRepository.recordFinancialEntry(record, session.userRole)
            _uiState.value = _uiState.value.copy(
                isGiveModalOpen = false,
                message = "Financial entry recorded (Ref: $randomRef)"
            )
        }
    }

    companion object {
        fun provideFactory(
            financeRepository: FinanceRepository,
            session: SessionEntity
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FinanceViewModel(financeRepository, session) as T
                }
            }
    }
}
