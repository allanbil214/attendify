package com.allan.attendify.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.allan.attendify.data.remote.dto.AttendanceDto
import com.allan.attendify.data.remote.dto.ScheduleDto
import com.allan.attendify.ui.common.UiState
import com.allan.attendify.ui.components.BottomNavBar
import com.allan.attendify.ui.theme.AttendifyTheme
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToCheckIn: () -> Unit,
    onNavigateToCheckOut: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val attendanceState by viewModel.attendanceState.collectAsState()
    val scheduleState by viewModel.scheduleState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

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
            // ...
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
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(navController = navController, currentRoute = "home")
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
                    val schedule = (scheduleState as? UiState.Success)?.data
                    
                    DashboardContent(
                        attendance = attendance,
                        schedule = schedule,
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
    schedule: ScheduleDto?,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (schedule != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (schedule.isWorkingDay) {
                        Text("Work Hours: ${schedule.startTime} - ${schedule.endTime}")
                        Text(schedule.message, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Today is not a working day.")
                    }
                }
            }
        }

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
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onCheckOutClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check Out")
                    }
                } else {
                    StatusRow(
                        label = "Check In",
                        time = attendance.checkInTime,
                        location = attendance.locationName ?: "Unknown"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    StatusRow(
                        label = "Check Out",
                        time = attendance.checkOutTime,
                        location = attendance.locationName ?: "Unknown" 
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
    }
}

@Composable
fun StatusRow(label: String, time: String, location: String) {
    val displayTime = try {
        val inputFormat = if (time.length > 19) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        }
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        
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
                style = MaterialTheme.typography.labelMedium
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
                modifier = Modifier.size(16.dp)
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
            attendance = null,
            schedule = ScheduleDto(
                employeeType = "fixed",
                dayOfWeek = 1,
                startTime = "08:00:00",
                endTime = "17:00:00",
                isWorkingDay = true,
                message = "Your work hours today: 08:00:00 - 17:00:00"
            ),
            onCheckInClick = {},
            onCheckOutClick = {},
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}
