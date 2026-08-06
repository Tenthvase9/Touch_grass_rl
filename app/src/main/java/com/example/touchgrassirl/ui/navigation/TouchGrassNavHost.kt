package com.example.touchgrassirl.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.touchgrassirl.TouchGrassApp
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.ui.activity.ActivityFeedScreen
import com.example.touchgrassirl.ui.activity.ActivityFeedViewModel
import com.example.touchgrassirl.ui.history.SessionHistoryScreen
import com.example.touchgrassirl.ui.leaderboard.LeaderboardScreen
import com.example.touchgrassirl.ui.leaderboard.LeaderboardViewModel
import com.example.touchgrassirl.ui.main.MainScreen
import com.example.touchgrassirl.ui.profile.ProfileScreen
import com.example.touchgrassirl.ui.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withContext

@Composable
fun TouchGrassNavHost(
    repository: TouchGrassRepository,
    socialRepository: SocialRepository,
    onDarkThemeChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val app = LocalContext.current.applicationContext as TouchGrassApp
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(repository, socialRepository),
    )

    LaunchedEffect(Unit) {
        val profile = withContext(Dispatchers.IO) {
            socialRepository.ensureProfileCreated()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        modifier = modifier,
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                repository = repository,
                socialRepository = socialRepository,
                myProfileId = "",
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenActivityFeed = { navController.navigate(Routes.ACTIVITY_FEED) },
                onDarkThemeChange = onDarkThemeChange,
            )
        }

composable(Routes.HISTORY) {
            SessionHistoryScreen(
                database = app.database,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ACTIVITY_FEED) {
            ActivityFeedScreen(
                socialRepository = socialRepository,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(
                socialRepository = socialRepository,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
