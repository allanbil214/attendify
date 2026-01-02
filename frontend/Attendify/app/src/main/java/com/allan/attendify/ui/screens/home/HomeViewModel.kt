package com.allan.attendify.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allan.attendify.data.remote.dto.AttendanceDto
import com.allan.attendify.data.remote.dto.ScheduleDto
import com.allan.attendify.domain.model.User
import com.allan.attendify.domain.repository.AttendanceRepository
import com.allan.attendify.domain.repository.AuthRepository
import com.allan.attendify.domain.repository.ScheduleRepository
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
    private val attendanceRepository: AttendanceRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _currentUser = authRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _attendanceState = MutableStateFlow<UiState<AttendanceDto?>>(UiState.Loading)
    val attendanceState = _attendanceState.asStateFlow()

    private val _scheduleState = MutableStateFlow<UiState<ScheduleDto?>>(UiState.Loading)
    val scheduleState = _scheduleState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            fetchTodayAttendance()
            fetchTodaySchedule()
        }
    }

    private suspend fun fetchTodayAttendance() {
        _attendanceState.value = UiState.Loading
        val result = attendanceRepository.getTodayAttendance()
        
        result.onSuccess { response ->
            _attendanceState.value = UiState.Success(response.data)
        }.onFailure { 
            _attendanceState.value = UiState.Error(it.message ?: "Failed to load status")
        }
    }

    private suspend fun fetchTodaySchedule() {
        _scheduleState.value = UiState.Loading
        val result = scheduleRepository.getTodaySchedule()

        result.onSuccess { response ->
            _scheduleState.value = UiState.Success(response.data)
        }.onFailure {
            _scheduleState.value = UiState.Error(it.message ?: "Failed to load schedule")
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
