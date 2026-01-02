package com.allan.attendify.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allan.attendify.data.remote.AuthEvent
import com.allan.attendify.data.remote.AuthEventBus
import com.allan.attendify.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authEventBus: AuthEventBus
) : ViewModel() {

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            authEventBus.events.collectLatest {
                if (it is AuthEvent.SessionExpired) {
                    logout()
                }
            }
        }
    }

    private suspend fun logout() {
        authRepository.logout()
        _logoutEvent.emit(Unit)
    }
}
