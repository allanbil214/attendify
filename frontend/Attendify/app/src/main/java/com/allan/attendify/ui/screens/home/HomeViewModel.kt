package com.allan.attendify.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allan.attendify.data.remote.dto.AttendanceDto
import com.allan.attendify.domain.model.User
import com.allan.attendify.domain.repository.AttendanceRepository
import com.allan.attendify.domain.repository.AuthRepository
import com.allan.attendify.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _currentUser = authRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _attendanceState = MutableStateFlow<UiState<AttendanceDto?>>(UiState.Loading)
    val attendanceState = _attendanceState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            fetchTodayAttendance()
        }
    }

    private suspend fun fetchTodayAttendance() {
        _attendanceState.value = UiState.Loading
        val result = attendanceRepository.getTodayAttendance()
        
        result.onSuccess { response ->
            // If data is null, it means not checked in yet, which is a valid state (Success with null)
            _attendanceState.value = UiState.Success(response.data)
        }.onFailure {
            // For now, if we fail to get today's attendance, we might assume not checked in 
            // or show error. If 404 is returned for "no attendance today", handle it.
            // Assuming API returns null data or specific response if empty.
            // If it's a network error, we should show it.
            _attendanceState.value = UiState.Error(it.message ?: "Failed to load status")
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
