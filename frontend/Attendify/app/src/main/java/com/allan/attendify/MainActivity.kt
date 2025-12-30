package com.allan.attendify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.allan.attendify.ui.AttendifyAppContent
import com.allan.attendify.ui.theme.AttendifyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ensure content is not drawn behind system bars unless intended.
        // We set decorFitsSystemWindows to false, but Scaffold inside AppContent usually handles padding.
        // To fix keyboard overlap, we might need to adjust how edge-to-edge works or rely on imePadding.
        
        // WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            AttendifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AttendifyAppContent()
                }
            }
        }
    }
}
