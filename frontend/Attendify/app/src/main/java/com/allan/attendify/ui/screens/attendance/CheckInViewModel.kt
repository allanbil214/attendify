package com.allan.attendify.ui.screens.attendance

import android.content.Context
import android.location.Location
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allan.attendify.data.remote.dto.CheckInRequest
import com.allan.attendify.data.remote.dto.DeviceInfo
import com.allan.attendify.domain.model.Location as OfficeLocation
import com.allan.attendify.domain.repository.AttendanceRepository
import com.allan.attendify.domain.repository.LocationRepository
import com.allan.attendify.domain.repository.PhotoRepository
import com.allan.attendify.ui.common.UiState
import com.allan.attendify.utils.ImageUtils
import com.allan.attendify.utils.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val attendanceRepository: AttendanceRepository,
    private val photoRepository: PhotoRepository,
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
    
    private val cameraExecutor = Executors.newSingleThreadExecutor()

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

    fun validateAndCheckIn(imageCapture: ImageCapture) {
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
            
            // 1. Take picture
            val imageProxy = ImageUtils.takePicture(imageCapture, cameraExecutor)
            if (imageProxy == null) {
                _uiState.value = UiState.Error("Failed to capture image.")
                return@launch
            }
            val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
            imageProxy.close()

            // 2. Upload photo
            val file = ImageUtils.bitmapToFile(context, bitmap)
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)
            
            val uploadResult = photoRepository.uploadPhoto(body)
            
            uploadResult.onSuccess { uploadResponse ->
                val photoUrl = uploadResponse.data?.url
                if (photoUrl == null) {
                     _uiState.value = UiState.Error("Failed to get photo URL from server.")
                     return@onSuccess
                }
                
                // 3. Check in with photo URL
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
                    photoUrl = photoUrl,
                    deviceInfo = deviceInfo
                )

                val checkInResult = attendanceRepository.checkIn(request)
                
                checkInResult.onSuccess {
                    _uiState.value = UiState.Success(Unit)
                }.onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Check-in failed")
                }
                
            }.onFailure {
                _uiState.value = UiState.Error(it.message ?: "Photo upload failed.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }
}
