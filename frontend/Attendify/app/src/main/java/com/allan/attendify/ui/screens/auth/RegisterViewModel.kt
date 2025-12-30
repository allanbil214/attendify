package com.allan.attendify.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allan.attendify.data.remote.dto.RegisterRequest
import com.allan.attendify.domain.repository.AuthRepository
import com.allan.attendify.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _fullName = MutableStateFlow("")
    val fullName = _fullName.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    fun onEmailChange(newValue: String) { _email.value = newValue }
    fun onPasswordChange(newValue: String) { _password.value = newValue }
    fun onFullNameChange(newValue: String) { _fullName.value = newValue }
    fun onPhoneChange(newValue: String) { _phone.value = newValue }

    fun register() {
        if (_email.value.isBlank() || _password.value.isBlank() || _fullName.value.isBlank() || _phone.value.isBlank()) {
            _uiState.value = UiState.Error("Please fill in all fields")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = authRepository.register(
                RegisterRequest(
                    email = _email.value,
                    password = _password.value,
                    fullName = _fullName.value,
                    phone = _phone.value
                )
            )

            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
            }.onFailure {
                _uiState.value = UiState.Error(it.message ?: "Registration failed")
            }
        }
    }
}
