package com.example.touchgrassirl.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.ui.achievements.AchievementsScreen
import com.example.touchgrassirl.ui.achievements.AchievementsViewModel
import com.example.touchgrassirl.ui.home.HomeScreen
import com.example.touchgrassirl.ui.home.HomeViewModel
import com.example.touchgrassirl.ui.map.MapScreen
import com.example.touchgrassirl.ui.map.MapViewModel
import com.example.touchgrassirl.ui.navigation.MainTab
import com.example.touchgrassirl.ui.profile.ProfileScreen
import com.example.touchgrassirl.ui.profile.ProfileViewModel
import com.example.touchgrassirl.ui.theme.CreamBackground
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.MeadowGreen

@Composable
fun MainScreen(
    repository: TouchGrassRepository,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    Scaffold(
        modifier = modifier,
        containerColor = CreamBackground,
        bottomBar = {
            NavigationBar(containerColor = CreamBackground) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.labelRes),
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            indicatorColor = MeadowGreen.copy(alpha = 0.25f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.HOME -> {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(repository),
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onStartSession = onStartSession,
                    onViewMap = { selectedTab = MainTab.MAP },
                    onViewAchievements = { selectedTab = MainTab.ACHIEVEMENTS },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            MainTab.MAP -> {
                val mapViewModel: MapViewModel = viewModel(
                    factory = MapViewModel.Factory(repository),
                )
                MapScreen(
                    viewModel = mapViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            MainTab.ACHIEVEMENTS -> {
                val achievementsViewModel: AchievementsViewModel = viewModel(
                    factory = AchievementsViewModel.Factory(repository),
                )
                AchievementsScreen(
                    viewModel = achievementsViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            MainTab.PROFILE -> {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.Factory(repository),
                )
                ProfileScreen(
                    viewModel = profileViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
