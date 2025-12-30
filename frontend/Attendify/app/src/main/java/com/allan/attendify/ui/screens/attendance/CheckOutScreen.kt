package com.allan.attendify.ui.screens.attendance

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
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
import com.allan.attendify.ui.common.UiState
import com.allan.attendify.ui.components.CameraPreview
import com.allan.attendify.ui.theme.AttendifyTheme

@Composable
fun CheckOutScreen(
    viewModel: CheckOutViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onCheckOutSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val note by viewModel.note.collectAsState()
    val workDuration by viewModel.workDuration.collectAsState()

    CheckOutScreenContent(
        uiState = uiState,
        note = note,
        workDuration = workDuration,
        onNoteChange = viewModel::onNoteChange,
        onCheckOut = viewModel::checkOut,
        onNavigateBack = onNavigateBack,
        onCheckOutSuccess = onCheckOutSuccess
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckOutScreenContent(
    uiState: UiState<Unit>,
    note: String,
    workDuration: String,
    onNoteChange: (String) -> Unit,
    onCheckOut: () -> Unit,
    onNavigateBack: () -> Unit,
    onCheckOutSuccess: () -> Unit
) {
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            onCheckOutSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check Out") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ending Your Work Day",
                style = MaterialTheme.typography.titleLarge
            )
            
            Text(
                text = "You have worked for:",
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Placeholder for duration
            Text(
                text = if (workDuration.isNotEmpty()) workDuration else "Calculating...",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Camera Preview Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onCheckOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                enabled = uiState !is UiState.Loading
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text("Check Out Now")
                }
            }
            
            if (uiState is UiState.Error) {
                Text(
                    text = (uiState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CheckOutScreenPreview() {
    AttendifyTheme {
        CheckOutScreenContent(
            uiState = UiState.Idle,
            note = "",
            workDuration = "08:30:15",
            onNoteChange = {},
            onCheckOut = {},
            onNavigateBack = {},
            onCheckOutSuccess = {}
        )
    }
}
