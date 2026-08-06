package com.example.touchgrassirl.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.touchgrassirl.data.local.entity.UserProgressEntity
import com.example.touchgrassirl.data.service.OutdoorDetectionService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.touchgrassirl.R
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.data.service.ReminderScheduler
import com.example.touchgrassirl.domain.GameConstants
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.MeadowGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: TouchGrassRepository,
    onBack: () -> Unit,
) {
    val progress by repository.observeProgress().collectAsState(initial = UserProgressEntity())
    var dailyGoal by remember(progress) { mutableIntStateOf(progress.dailyGoalMinutes.coerceIn(5, 120)) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    val prefs = LocalContext.current.getSharedPreferences("touch_grass_prefs", 0)
    var darkThemeEnabled by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
    var homeLocationSet by remember {
        mutableStateOf(prefs.contains("home_lat") && prefs.contains("home_lng"))
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val getCurrentLocation: ((Double, Double) -> Unit) -> Unit = { callback ->
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                fusedClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token,
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        callback(location.latitude, location.longitude)
                    }
                }
            } catch (_: SecurityException) {
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily goal: $dailyGoal minutes",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = dailyGoal.toFloat(),
                            onValueChange = { dailyGoal = it.toInt() },
                            valueRange = 5f..120f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = ForestGreen,
                                activeTrackColor = MeadowGreen,
                            ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("5 min", style = MaterialTheme.typography.labelSmall)
                            Text("120 min", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Home location",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (homeLocationSet) "Home set — we'll detect when you leave and return." else "Set your home to enable automatic outdoor tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (homeLocationSet) {
                            Button(
                                onClick = {
                                    prefs.edit().remove("home_lat").remove("home_lng").apply()
                                    homeLocationSet = false
                                    OutdoorDetectionService.stop(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Reset home location")
                            }
                        } else {
                            Button(
                                onClick = {
                                    getCurrentLocation { lat, lng ->
                                        prefs.edit()
                                            .putFloat("home_lat", lat.toFloat())
                                            .putFloat("home_lng", lng.toFloat())
                                            .apply()
                                        homeLocationSet = true
                                        OutdoorDetectionService.start(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Use current location as home")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsToggle(
                            title = "Daily reminders",
                            subtitle = "Remind you to go outside",
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsToggle(
                            title = "Dark theme",
                            subtitle = "Use dark color scheme",
                            checked = darkThemeEnabled,
                            onCheckedChange = { enabled ->
                                darkThemeEnabled = enabled
                                prefs.edit().putBoolean("dark_theme", enabled).apply()
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            repository.setDailyGoal(dailyGoal)
                            if (notificationsEnabled) {
                                ReminderScheduler.scheduleDaily(context = context, hour = 10, minute = 0)
                            } else {
                                ReminderScheduler.cancel(context = context)
                            }
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Save", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ForestGreen,
            ),
        )
    }
}
