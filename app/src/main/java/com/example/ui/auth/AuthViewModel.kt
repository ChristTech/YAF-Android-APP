package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.DepartmentType
import com.example.data.model.UserRole
import com.example.data.repository.AuthResult
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isRegisterMode: Boolean = false
)

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentSession: StateFlow<SessionEntity?> = userRepository.getActiveSession()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleAuthMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            errorMessage = null,
            infoMessage = null
        )
    }

    fun login(emailOrPhone: String, plainPassword: String) {
        if (emailOrPhone.isBlank() || plainPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter both credentials.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            when (val result = userRepository.login(emailOrPhone, plainPassword)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is AuthResult.PendingApproval -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = result.message
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        plainPassword: String,
        role: UserRole,
        department: DepartmentType,
        preferredLanguage: com.example.core.localization.AppLanguage = com.example.core.localization.AppLanguage.ENGLISH,
        profilePhotoUri: String? = null
    ) {
        if (fullName.isBlank() || email.isBlank() || phone.isBlank() || plainPassword.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please fill in required fields (password must be at least 6 characters)."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            when (val result = userRepository.registerUser(
                fullName = fullName,
                email = email,
                phone = phone,
                plainPassword = plainPassword,
                role = role,
                department = department,
                preferredLanguage = preferredLanguage,
                profilePhotoUri = profilePhotoUri
            )) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is AuthResult.PendingApproval -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = result.message,
                        isRegisterMode = false
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun quickLoginDemo(email: String, pass: String) {
        login(email, pass)
    }

    fun onGoogleAuthSuccess(
        uid: String,
        fullName: String,
        email: String,
        preferredLanguage: com.example.core.localization.AppLanguage = com.example.core.localization.LanguageManager.preferredLanguage.value
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            when (val result = userRepository.loginWithGoogle(uid, fullName, email, preferredLanguage)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is AuthResult.PendingApproval -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = result.message
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun onGoogleAuthError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = errorMessage
        )
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    companion object {
        fun provideFactory(userRepository: UserRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(userRepository) as T
                }
            }
    }
}
