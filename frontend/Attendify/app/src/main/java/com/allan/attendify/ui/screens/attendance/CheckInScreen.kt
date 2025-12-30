package com.allan.attendify.ui.screens.attendance

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allan.attendify.ui.common.UiState
import com.allan.attendify.ui.components.CameraPreview
import com.allan.attendify.ui.components.OsmMapView
import com.allan.attendify.ui.theme.AttendifyTheme
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
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

    val imageCapture = remember { ImageCapture.Builder().build() }
    
    // Using BottomSheetScaffold allows interaction with the main content (Map) 
    // and the TopBar while the sheet is visible/peeking.
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
                    IconButton(onClick = { viewModel.getCurrentLocationAndFetchNearby() }) {
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
                                text = selectedLocation!!.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = selectedLocation!!.address,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isWithinRadius = (distance ?: Double.MAX_VALUE) <= selectedLocation!!.radius
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isWithinRadius) Color.Green else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Distance: ${distance?.toInt() ?: 0}m (Max: ${selectedLocation!!.radius.toInt()}m)",
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
                        // CameraPreview cannot be previewed in IDE easily, so we can mock or just show box in preview
                        // For runtime it works.
                        if (!androidx.compose.ui.platform.LocalInspectionMode.current) {
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
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = viewModel::validateAndCheckIn,
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
                
                // Add some bottom padding for the sheet
                Spacer(modifier = Modifier.height(32.dp))
            }
        },
        sheetPeekHeight = 150.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetTonalElevation = 8.dp
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Map View
            if (userLocation != null) {
                if (!androidx.compose.ui.platform.LocalInspectionMode.current) {
                    OsmMapView(
                        modifier = Modifier.fillMaxSize(),
                        userLocation = userLocation?.let { GeoPoint(it.latitude, it.longitude) },
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
         // Previewing a complex screen like this with ViewModel dependency is hard without mocking.
         // We'll show a placeholder or basic structure if we extracted content.
         // Since we didn't extract the scaffold content to a stateless composable, 
         // this preview will fail if rendered because of ViewModel injection.
         // To fix, we would need to refactor CheckInScreen into CheckInScreen(Stateless) and CheckInScreen(Stateful).
         
         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             Text("CheckIn Screen Preview requires refactoring for stateless content")
         }
    }
}
