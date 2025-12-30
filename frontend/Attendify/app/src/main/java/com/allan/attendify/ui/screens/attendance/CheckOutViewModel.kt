package com.allan.attendify.ui.screens.attendance

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allan.attendify.data.remote.dto.CheckOutRequest
import com.allan.attendify.domain.repository.AttendanceRepository
import com.allan.attendify.ui.common.UiState
import com.allan.attendify.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CheckOutViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _currentAttendanceId = MutableStateFlow<String?>(null)
    private var checkInTimeMillis: Long = 0
    
    private val _note = MutableStateFlow("")
    val note = _note.asStateFlow()

    private val _workDuration = MutableStateFlow("")
    val workDuration = _workDuration.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadCurrentAttendance()
    }

    fun onNoteChange(value: String) {
        _note.value = value
    }

    private fun loadCurrentAttendance() {
        viewModelScope.launch {
            val result = attendanceRepository.getTodayAttendance()
            result.onSuccess { response ->
                val attendance = response.data
                if (attendance != null && attendance.checkOutTime == null) {
                    _currentAttendanceId.value = attendance.id
                    try {
                        // Parse the UTC string from server correctly
                        val timeStr = attendance.checkInTime
                        val formatStr = if (timeStr.length > 19) {
                             "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                        } else {
                             "yyyy-MM-dd'T'HH:mm:ss"
                        }
                        
                        val sdf = SimpleDateFormat(formatStr, Locale.getDefault())
                        sdf.timeZone = TimeZone.getTimeZone("UTC") // Input is UTC
                        
                        val date = sdf.parse(timeStr)
                        if (date != null) {
                            checkInTimeMillis = date.time
                            startDurationTimer()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    _uiState.value = UiState.Error("No active check-in found or already checked out.")
                }
            }
        }
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                // Calculate difference based on UTC if possible, or just local time diff 
                // Since Date.time is epoch millis (UTC), System.currentTimeMillis() is also UTC based epoch
                // So the difference should be correct regardless of timezone as long as both are epoch millis
                
                val now = System.currentTimeMillis()
                val diff = now - checkInTimeMillis
                
                if (diff >= 0) {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                    _workDuration.value = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                }
                delay(1000)
            }
        }
    }

    fun checkOut() {
        val attendanceId = _currentAttendanceId.value
        if (attendanceId == null) {
            _uiState.value = UiState.Error("No active check-in found")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            val location = locationHelper.getCurrentLocation()
            if (location == null) {
                _uiState.value = UiState.Error("Could not get location")
                return@launch
            }

            val request = CheckOutRequest(
                attendanceId = attendanceId,
                latitude = location.latitude,
                longitude = location.longitude,
                note = _note.value
            )

            val result = attendanceRepository.checkOut(request)
            
            result.onSuccess {
                timerJob?.cancel()
                _uiState.value = UiState.Success(Unit)
            }.onFailure {
                _uiState.value = UiState.Error(it.message ?: "Check-out failed")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
