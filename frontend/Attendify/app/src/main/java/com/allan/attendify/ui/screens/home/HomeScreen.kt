package com.allan.attendify.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.allan.attendify.data.remote.dto.AttendanceDto
import com.allan.attendify.ui.common.UiState
import com.allan.attendify.ui.theme.AttendifyTheme
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToCheckIn: () -> Unit,
    onNavigateToCheckOut: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLocations: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val attendanceState by viewModel.attendanceState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh data when screen resumes (e.g. coming back from CheckIn/CheckOut)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            // Wait a bit or check if we should really navigate (might be initial load)
            // Ideally, the MainActivity/AuthRepository flow handles this, but here
            // we can trigger if user becomes null (logout).
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = "Attendify",
                            style = MaterialTheme.typography.titleLarge
                        )
                        currentUser?.let {
                            Text(
                                text = "Hello, ${it.fullName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.logout()
                        onNavigateToLogin()
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { /* Already here */ }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = false,
                    onClick = onNavigateToHistory
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = onNavigateToProfile
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = attendanceState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: ${state.message}")
                        Button(onClick = { viewModel.refreshData() }) {
                            Text("Retry")
                        }
                    }
                }
                is UiState.Success -> {
                    val attendance = state.data
                    DashboardContent(
                        attendance = attendance,
                        onCheckInClick = onNavigateToCheckIn,
                        onCheckOutClick = onNavigateToCheckOut,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
                UiState.Idle -> {}
            }
        }
    }
}

@Composable
fun DashboardContent(
    attendance: AttendanceDto?,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today's Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Today's Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (attendance == null) {
                    Text("You haven't checked in yet.")
                    Button(
                        onClick = onCheckInClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check In")
                    }
                } else if (attendance.checkOutTime == null) {
                    StatusRow(
                        label = "Check In",
                        time = attendance.checkInTime,
                        location = attendance.locationName ?: "Unknown"
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onCheckOutClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check Out")
                    }
                } else {
                    StatusRow(
                        label = "Check In",
                        time = attendance.checkInTime,
                        location = attendance.locationName ?: "Unknown"
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    StatusRow(
                        label = "Check Out",
                        time = attendance.checkOutTime,
                        location = attendance.locationName ?: "Unknown" // Usually same location or null
                    )
                    
                    Text(
                        text = "You have completed your work day!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Quick Stats or other info could go here
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        // Add more cards or buttons if needed
    }
}

@Composable
fun StatusRow(label: String, time: String, location: String) {
    // Basic date parsing for display
    val displayTime = try {
        // Server sends UTC time string like "2025-12-30T07:27:49.864Z"
        // We need to parse it as UTC, then format it to local time
        val inputFormat = if (time.length > 19) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        }
        inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Input is UTC
        
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault() // Output is Local Time
        
        val date = inputFormat.parse(time)
        date?.let { outputFormat.format(it) } ?: time
    } catch (e: Exception) {
        time
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = displayTime,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
             Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = location,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AttendifyTheme {
        DashboardContent(
            attendance = AttendanceDto(
                id = "1",
                userId = "user1",
                locationId = "loc1",
                checkInTime = "2024-01-01T09:00:00Z",
                checkOutTime = null,
                checkInLatitude = 0.0,
                checkInLongitude = 0.0,
                status = "present",
                isLate = false,
                locationName = "Main Office",
                locationAddress = "123 Main St"
            ),
            onCheckInClick = {},
            onCheckOutClick = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}
