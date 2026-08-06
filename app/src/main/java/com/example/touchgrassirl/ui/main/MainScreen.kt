package com.example.touchgrassirl.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
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
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.ui.friends.FriendsScreen
import com.example.touchgrassirl.ui.friends.FriendsViewModel
import com.example.touchgrassirl.ui.home.HomeScreen
import com.example.touchgrassirl.ui.home.HomeViewModel
import com.example.touchgrassirl.ui.navigation.MainTab
import com.example.touchgrassirl.ui.profile.ProfileScreen
import com.example.touchgrassirl.ui.profile.ProfileViewModel
import com.example.touchgrassirl.ui.settings.SettingsScreen
import com.example.touchgrassirl.ui.theme.ForestGreen
import com.example.touchgrassirl.ui.theme.SoftSage

@Composable
fun MainScreen(
    repository: TouchGrassRepository,
    socialRepository: SocialRepository,
    myProfileId: String,
    onOpenHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            repository = repository,
            onBack = { showSettings = false },
        )
        return
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
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
                            indicatorColor = SoftSage.copy(alpha = 0.5f),
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
                    onOpenSettings = { showSettings = true },
                    modifier = Modifier.padding(innerPadding),
                )
            }
            MainTab.FRIENDS -> {
                val friendsViewModel: FriendsViewModel = viewModel(
                    factory = FriendsViewModel.Factory(socialRepository),
                )
                FriendsScreen(
                    socialRepository = socialRepository,
                    myProfileId = myProfileId,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            MainTab.PROFILE -> {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.Factory(repository),
                )
                ProfileScreen(
                    viewModel = profileViewModel,
                    onOpenSettings = { showSettings = true },
                    onOpenHistory = onOpenHistory,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
