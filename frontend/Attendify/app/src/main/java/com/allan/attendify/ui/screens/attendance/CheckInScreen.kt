package com.allan.attendify.ui.screens.attendance

import android.location.Location
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allan.attendify.domain.model.Location as OfficeLocation
import com.allan.attendify.ui.common.UiState
import com.allan.attendify.ui.components.CameraPreview
import com.allan.attendify.ui.components.OsmMapView
import com.allan.attendify.ui.theme.AttendifyTheme
import org.osmdroid.util.GeoPoint

@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onCheckInSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val nearbyLocations by viewModel.nearbyLocations.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val distance by viewModel.distanceToLocation.collectAsState()
    val note by viewModel.note.collectAsState()

    CheckInScreenContent(
        uiState = uiState,
        userLocation = userLocation,
        nearbyLocations = nearbyLocations,
        selectedLocation = selectedLocation,
        distance = distance,
        note = note,
        onNoteChange = viewModel::onNoteChange,
        onRefreshLocation = viewModel::getCurrentLocationAndFetchNearby,
        onValidateAndCheckIn = viewModel::validateAndCheckIn,
        onNavigateBack = onNavigateBack,
        onCheckInSuccess = onCheckInSuccess
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreenContent(
    uiState: UiState<Unit>,
    userLocation: Location?,
    nearbyLocations: List<OfficeLocation>,
    selectedLocation: OfficeLocation?,
    distance: Double?,
    note: String,
    onNoteChange: (String) -> Unit,
    onRefreshLocation: () -> Unit,
    onValidateAndCheckIn: () -> Unit,
    onNavigateBack: () -> Unit,
    onCheckInSuccess: () -> Unit
) {
    val imageCapture = remember { ImageCapture.Builder().build() }
    val scaffoldState = rememberBottomSheetScaffoldState()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            onCheckInSuccess()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Check In") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshLocation) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Location")
                    }
                }
            )
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Confirm Location",
                    style = MaterialTheme.typography.titleLarge
                )

                if (selectedLocation != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = selectedLocation.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = selectedLocation.address,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isWithinRadius = (distance ?: Double.MAX_VALUE) <= selectedLocation.radius
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isWithinRadius) Color.Green else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Distance: ${distance?.toInt() ?: 0}m (Max: ${selectedLocation.radius.toInt()}m)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                } else {
                    Text("No office location selected.")
                }

                // Camera Preview Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!LocalInspectionMode.current) {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                imageCapture = imageCapture
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Camera Preview")
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onValidateAndCheckIn,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is UiState.Loading &&
                            selectedLocation != null &&
                            (distance ?: Double.MAX_VALUE) <= (selectedLocation?.radius ?: 0.0)
                ) {
                    if (uiState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Check In Now")
                    }
                }

                if (uiState is UiState.Error) {
                    Text(
                        text = (uiState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        },
        sheetPeekHeight = 150.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetTonalElevation = 8.dp
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (userLocation != null) {
                if (!LocalInspectionMode.current) {
                    OsmMapView(
                        modifier = Modifier.fillMaxSize(),
                        userLocation = GeoPoint(userLocation.latitude, userLocation.longitude),
                        officeLocations = nearbyLocations
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Map View")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CheckInScreenPreview() {
    AttendifyTheme {
        val mockLocation = Location("provider").apply {
            latitude = 0.0
            longitude = 0.0
        }
        val mockOfficeLocation = OfficeLocation(
            id = "1",
            name = "Head Office",
            address = "123 Business Rd",
            latitude = 0.001,
            longitude = 0.001,
            radius = 100.0
        )
        
        CheckInScreenContent(
            uiState = UiState.Idle,
            userLocation = mockLocation,
            nearbyLocations = listOf(mockOfficeLocation),
            selectedLocation = mockOfficeLocation,
            distance = 50.0,
            note = "",
            onNoteChange = {},
            onRefreshLocation = {},
            onValidateAndCheckIn = {},
            onNavigateBack = {},
            onCheckInSuccess = {}
        )
    }
}
