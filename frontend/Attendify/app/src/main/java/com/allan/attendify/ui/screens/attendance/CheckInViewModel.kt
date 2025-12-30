package com.allan.attendify.ui.screens.attendance

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allan.attendify.data.remote.dto.CheckInRequest
import com.allan.attendify.data.remote.dto.DeviceInfo
import com.allan.attendify.domain.model.Location as OfficeLocation
import com.allan.attendify.domain.repository.AttendanceRepository
import com.allan.attendify.domain.repository.LocationRepository
import com.allan.attendify.ui.common.UiState
import com.allan.attendify.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val attendanceRepository: AttendanceRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val _nearbyLocations = MutableStateFlow<List<OfficeLocation>>(emptyList())
    val nearbyLocations = _nearbyLocations.asStateFlow()

    private val _selectedLocation = MutableStateFlow<OfficeLocation?>(null)
    val selectedLocation = _selectedLocation.asStateFlow()

    private val _distanceToLocation = MutableStateFlow<Double?>(null)
    val distanceToLocation = _distanceToLocation.asStateFlow()

    private val _note = MutableStateFlow("")
    val note = _note.asStateFlow()

    init {
        getCurrentLocationAndFetchNearby()
    }

    fun onNoteChange(value: String) {
        _note.value = value
    }

    fun getCurrentLocationAndFetchNearby() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                _userLocation.value = location
                fetchNearbyLocations(location.latitude, location.longitude)
            } else {
                _uiState.value = UiState.Error("Could not get current location. Please enable GPS.")
            }
        }
    }

    private suspend fun fetchNearbyLocations(lat: Double, lng: Double) {
        val result = locationRepository.getNearbyLocations(lat, lng, 1000.0) // 1km radius search
        result.onSuccess { locations ->
            _nearbyLocations.value = locations
            // Auto-select the nearest one if available
            if (locations.isNotEmpty()) {
                selectLocation(locations.first())
            } else {
                 _uiState.value = UiState.Error("No office locations found nearby.")
            }
             if (_uiState.value is UiState.Loading) _uiState.value = UiState.Idle
        }.onFailure {
            _uiState.value = UiState.Error(it.message ?: "Failed to load locations")
        }
    }

    fun selectLocation(location: OfficeLocation) {
        _selectedLocation.value = location
        _userLocation.value?.let { userLoc ->
            val dist = locationHelper.calculateDistance(
                userLoc.latitude, userLoc.longitude,
                location.latitude, location.longitude
            )
            _distanceToLocation.value = dist
        }
    }

    fun validateAndCheckIn() {
        val selected = _selectedLocation.value
        val userLoc = _userLocation.value
        val dist = _distanceToLocation.value

        if (selected == null || userLoc == null || dist == null) {
            _uiState.value = UiState.Error("Location data invalid")
            return
        }

        if (dist > selected.radius) {
            _uiState.value = UiState.Error("You are too far from the office. Distance: ${dist.toInt()}m, Allowed: ${selected.radius.toInt()}m")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            // In a real app, we would upload the photo here and get a URL, 
            // or send base64. For MVP, we skip photo upload logic but the camera is there.
            
            val deviceInfo = DeviceInfo(
                model = android.os.Build.MODEL,
                os = "Android ${android.os.Build.VERSION.RELEASE}",
                appVersion = "1.0.0"
            )

            val request = CheckInRequest(
                locationId = selected.id,
                latitude = userLoc.latitude,
                longitude = userLoc.longitude,
                note = _note.value,
                deviceInfo = deviceInfo
            )

            val result = attendanceRepository.checkIn(request)
            
            result.onSuccess {
                _uiState.value = UiState.Success(Unit)
            }.onFailure {
                _uiState.value = UiState.Error(it.message ?: "Check-in failed")
            }
        }
    }
}
