package com.example.ui.departments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.DepartmentEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.DepartmentType
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.DepartmentRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DepartmentUiState(
    val departments: List<DepartmentEntity> = emptyList(),
    val selectedDepartment: DepartmentEntity? = null,
    val selectedDepartmentMembers: List<UserEntity> = emptyList(),
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val totalAttendeesAcrossServices: Int = 0,
    val activeTab: Int = 0, // 0: Departments Directory, 1: Attendance Tracker, 2: Self Check-In
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val generatedCheckInCode: String = "CYP-2026",
    val checkInSuccessRecord: AttendanceRecordEntity? = null
)

class DepartmentViewModel(
    private val departmentRepository: DepartmentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedDeptType = MutableStateFlow<DepartmentType?>(null)
    private val _activeTab = MutableStateFlow(0)
    private val _successMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _generatedCode = MutableStateFlow(attendanceRepository.generateCheckInCode())
    private val _lastCheckIn = MutableStateFlow<AttendanceRecordEntity?>(null)

    val allUsers: StateFlow<List<UserEntity>> = userRepository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<DepartmentUiState> = combine(
        departmentRepository.getAllDepartments(),
        attendanceRepository.getAllAttendance(),
        attendanceRepository.getTotalAttendeesAcrossServices(),
        _selectedDeptType
    ) { deps, atts, totalAtts, selType ->
        val selectedDept = deps.find { it.type == selType } ?: deps.firstOrNull()
        DepartmentUiState(
            departments = deps,
            selectedDepartment = selectedDept,
            attendanceRecords = atts,
            totalAttendeesAcrossServices = totalAtts ?: 0,
            activeTab = _activeTab.value,
            successMessage = _successMessage.value,
            errorMessage = _errorMessage.value,
            generatedCheckInCode = _generatedCode.value,
            checkInSuccessRecord = _lastCheckIn.value
        )
    }.combine(_activeTab) { state, tab ->
        state.copy(activeTab = tab)
    }.combine(_successMessage) { state, succ ->
        state.copy(successMessage = succ)
    }.combine(_errorMessage) { state, err ->
        state.copy(errorMessage = err)
    }.combine(_generatedCode) { state, code ->
        state.copy(generatedCheckInCode = code)
    }.combine(_lastCheckIn) { state, checkIn ->
        state.copy(checkInSuccessRecord = checkIn)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DepartmentUiState(isLoading = true))

    private val _deptMembers = MutableStateFlow<List<UserEntity>>(emptyList())
    val deptMembers = _deptMembers.asStateFlow()

    fun selectDepartment(dept: DepartmentEntity) {
        _selectedDeptType.value = dept.type
        viewModelScope.launch {
            departmentRepository.getDepartmentMembers(dept.type).collect { members ->
                _deptMembers.value = members
            }
        }
    }

    fun setTab(tabIndex: Int) {
        _activeTab.value = tabIndex
        clearMessages()
    }

    fun recordManualAttendance(
        title: String,
        type: String,
        department: String,
        males: Int,
        females: Int,
        firstTimers: Int,
        notes: String,
        recorderId: String,
        recorderName: String
    ) {
        viewModelScope.launch {
            val total = males + females + firstTimers
            val record = AttendanceRecordEntity(
                id = UUID.randomUUID().toString(),
                meetingTitle = title,
                meetingType = type,
                departmentOrGroup = department,
                dateMillis = System.currentTimeMillis(),
                totalAttendees = total,
                maleCount = males,
                femaleCount = females,
                firstTimersCount = firstTimers,
                checkInMethod = "MANUAL",
                recordedById = recorderId,
                recordedByName = recorderName,
                notes = notes
            )
            val result = attendanceRepository.recordAttendance(record)
            if (result.isSuccess) {
                _successMessage.value = "Attendance recorded successfully! Total: $total"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to record attendance"
            }
        }
    }

    fun refreshCheckInCode() {
        _generatedCode.value = attendanceRepository.generateCheckInCode()
    }

    fun performSelfCheckIn(
        code: String,
        userId: String,
        userName: String,
        serviceTitle: String
    ) {
        viewModelScope.launch {
            if (code.isBlank()) {
                _errorMessage.value = "Please enter a check-in verification code"
                return@launch
            }
            val result = attendanceRepository.verifyAndCheckIn(code, userId, userName, serviceTitle)
            if (result.isSuccess) {
                _lastCheckIn.value = result.getOrNull()
                _successMessage.value = "Check-in successful! Welcome to $serviceTitle."
            } else {
                _errorMessage.value = "Check-in failed. Please verify code."
            }
        }
    }

    fun assignMember(userId: String, department: DepartmentType, adminUserId: String) {
        viewModelScope.launch {
            val result = departmentRepository.assignMemberToDepartment(userId, department, adminUserId)
            if (result.isSuccess) {
                _successMessage.value = "Member assigned to ${department.displayName} successfully!"
                departmentRepository.getDepartmentMembers(department).collect {
                    _deptMembers.value = it
                }
            } else {
                _errorMessage.value = "Failed to assign member: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }

    companion object {
        fun provideFactory(
            departmentRepository: DepartmentRepository,
            attendanceRepository: AttendanceRepository,
            userRepository: UserRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DepartmentViewModel(departmentRepository, attendanceRepository, userRepository) as T
            }
        }
    }
}
