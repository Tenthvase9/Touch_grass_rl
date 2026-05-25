package com.example.touchgrassirl

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.touchgrassirl.ui.navigation.TouchGrassNavHost
import com.example.touchgrassirl.ui.theme.TouchGrassIrlTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* grants handled opportunistically for future sensors */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestOutdoorPermissions()
        val repository = (application as TouchGrassApp).repository

        enableEdgeToEdge()
        setContent {
            TouchGrassIrlTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TouchGrassNavHost(repository = repository)
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
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
