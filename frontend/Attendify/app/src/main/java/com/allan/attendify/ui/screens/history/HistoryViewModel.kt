package com.allan.attendify.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allan.attendify.domain.model.Attendance
import com.allan.attendify.domain.repository.AttendanceRepository
import com.allan.attendify.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Attendance>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            // Hardcoded pagination for MVP
            val result = attendanceRepository.getAttendanceHistory(1, 20, null, null)
            
            result.onSuccess { list ->
                _uiState.value = UiState.Success(list)
            }.onFailure {
                _uiState.value = UiState.Error(it.message ?: "Failed to load history")
            }
        }
    }
}
