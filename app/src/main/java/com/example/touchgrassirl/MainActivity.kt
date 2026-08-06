package com.example.touchgrassirl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.touchgrassirl.data.service.OutdoorDetectionService
import com.example.touchgrassirl.ui.navigation.TouchGrassNavHost
import com.example.touchgrassirl.ui.theme.TouchGrassTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] ?: false
        } else true

        if (fineLocation) {
            startOutdoorDetection()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestOutdoorPermissions()
        val app = application as TouchGrassApp
        val repository = app.repository
        val socialRepository = app.socialRepository
        val prefs = getSharedPreferences("touch_grass_prefs", Context.MODE_PRIVATE)

        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
            TouchGrassTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TouchGrassNavHost(
                        repository = repository,
                        socialRepository = socialRepository,
                        onDarkThemeChange = { enabled ->
                            isDarkTheme = enabled
                        },
                    )
                }
            }
        }
    }

    private fun requestOutdoorPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needsRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsRequest) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            startOutdoorDetection()
        }
    }

    private fun startOutdoorDetection() {
        val prefs = getSharedPreferences("touch_grass_prefs", Context.MODE_PRIVATE)
        val hasHomeSet = prefs.contains("home_lat") && prefs.contains("home_lng")
        if (hasHomeSet) {
            OutdoorDetectionService.start(this)
        }
    }
}
