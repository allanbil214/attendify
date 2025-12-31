package com.allan.attendify.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allan.attendify.domain.model.Attendance
import com.allan.attendify.ui.common.UiState
import com.allan.attendify.ui.components.BottomNavBar
import com.allan.attendify.ui.navigation.Screen
import com.allan.attendify.ui.theme.AttendifyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // We need a way to trigger navigation for the bottom bar.
            // Since we don't have the navController here, we construct a fake one or
            // better yet, we can't easily use the reusable BottomNavBar component 
            // without the NavController reference or callbacks.
            // However, the reusable component expects a NavController.
            // Ideally, we refactor BottomNavBar to take callbacks or just copy the UI here.
            // Given the constraint, copying the NavigationBar UI structure is cleaner than passing NavController everywhere.
            
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(com.allan.attendify.ui.components.BottomNavItem.Home.icon, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = onNavigateToHome
                )
                NavigationBarItem(
                    icon = { Icon(com.allan.attendify.ui.components.BottomNavItem.History.icon, contentDescription = "History") },
                    label = { Text("History") },
                    selected = true,
                    onClick = { /* Already here */ }
                )
                NavigationBarItem(
                    icon = { Icon(com.allan.attendify.ui.components.BottomNavItem.Profile.icon, contentDescription = "Profile") },
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
            when (val state = uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No attendance records found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.data) { attendance ->
                                AttendanceItem(attendance = attendance)
                            }
                        }
                    }
                }
                UiState.Idle -> {}
            }
        }
    }
}

@Composable
fun AttendanceItem(attendance: Attendance) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(attendance.checkInTime),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Badge(
                    containerColor = if (attendance.isLate) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (attendance.isLate) "LATE" else "ON TIME",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = if (attendance.isLate) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Check In",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = timeFormat.format(attendance.checkInTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (attendance.checkOutTime != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Check Out",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = timeFormat.format(attendance.checkOutTime!!),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                     Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
            
            if (attendance.locationName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = attendance.locationName!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AttendanceItemPreview() {
    AttendifyTheme {
        AttendanceItem(
            attendance = Attendance(
                id = "1",
                userId = "user1",
                locationId = "loc1",
                checkInTime = Date(),
                checkOutTime = Date(System.currentTimeMillis() + 3600000), // +1 hour
                checkInLatitude = 0.0,
                checkInLongitude = 0.0,
                status = "completed",
                isLate = true,
                locationName = "Main Office",
                locationAddress = "123 Main St"
            )
        )
    }
}
