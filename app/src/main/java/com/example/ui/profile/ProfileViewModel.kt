package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.AccountStatus
import com.example.data.model.DepartmentType
import com.example.data.model.Gender
import com.example.data.model.UserRole
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isEditProfileOpen: Boolean = false,
    val message: String? = null
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val session: SessionEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = userRepository.getUserById(session.userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val pendingUsers: StateFlow<List<UserEntity>> = userRepository.getUsersByStatus(AccountStatus.PENDING_APPROVAL)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allMembers: StateFlow<List<UserEntity>> = userRepository.getAllUsers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setEditProfileOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isEditProfileOpen = open)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun updateProfile(
        fullName: String,
        phone: String,
        address: String,
        department: DepartmentType,
        profilePhotoUri: String? = currentUser.value?.profilePhotoUri
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(
                fullName = fullName.trim(),
                phone = phone.trim(),
                address = address.trim(),
                department = department,
                profilePhotoUri = profilePhotoUri?.trim()?.takeIf { it.isNotBlank() }
            )
            userRepository.updateUserProfile(updated)
            _uiState.value = _uiState.value.copy(
                isEditProfileOpen = false,
                message = "Profile updated successfully."
            )
        }
    }

    fun approveUser(userId: String) {
        viewModelScope.launch {
            userRepository.updateUserStatus(userId, AccountStatus.APPROVED, session.userId)
            _uiState.value = _uiState.value.copy(message = "Executive registration approved by Admin.")
        }
    }

    fun declineUser(userId: String) {
        viewModelScope.launch {
            userRepository.updateUserStatus(userId, AccountStatus.SUSPENDED, session.userId)
            _uiState.value = _uiState.value.copy(message = "Executive registration declined.")
        }
    }

    fun switchActiveRole(newRole: UserRole) {
        viewModelScope.launch {
            userRepository.updateUserRole(session.userId, newRole, session.userId)
            _uiState.value = _uiState.value.copy(message = "Switched persona to ${newRole.displayName}")
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    companion object {
        fun provideFactory(
            userRepository: UserRepository,
            session: SessionEntity
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(userRepository, session) as T
                }
            }
    }
}
