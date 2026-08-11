package com.example.touchgrassirl.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.touchgrassirl.TouchGrassApp
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.ui.achievements.AchievementsScreen
import com.example.touchgrassirl.ui.achievements.AchievementsViewModel
import com.example.touchgrassirl.ui.activity.ActivityFeedScreen
import com.example.touchgrassirl.ui.challenges.ChallengesScreen
import com.example.touchgrassirl.ui.history.SessionHistoryScreen
import com.example.touchgrassirl.ui.leaderboard.LeaderboardScreen
import com.example.touchgrassirl.ui.location.LocationHistoryScreen
import com.example.touchgrassirl.ui.main.MainScreen
import com.example.touchgrassirl.ui.onboarding.OnboardingScreen
import com.example.touchgrassirl.ui.profile.ProfileScreen
import com.example.touchgrassirl.ui.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TouchGrassNavHost(
    repository: TouchGrassRepository,
    socialRepository: SocialRepository,
    onDarkThemeChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as TouchGrassApp
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(repository, socialRepository),
    )

    val prefs = context.getSharedPreferences("touch_grass_prefs", Context.MODE_PRIVATE)
    val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)

    LaunchedEffect(Unit) {
        val profileId = withContext(Dispatchers.IO) {
            socialRepository.ensureProfileCreated()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (onboardingCompleted) Routes.MAIN else Routes.ONBOARDING,
        modifier = modifier,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) {
            val profileId = profileViewModel.profileId.collectAsStateWithLifecycle("").value
            MainScreen(
                repository = repository,
                socialRepository = socialRepository,
                myProfileId = profileId,
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenActivityFeed = { navController.navigate(Routes.ACTIVITY_FEED) },
                onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
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

        composable(Routes.CHALLENGES) {
            ChallengesScreen(
                socialRepository = socialRepository,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.LOCATION_HISTORY) {
            LocationHistoryScreen(
                socialRepository = socialRepository,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ACHIEVEMENTS) {
            val achievementsViewModel: AchievementsViewModel = viewModel(
                factory = AchievementsViewModel.Factory(repository),
            )
            AchievementsScreen(
                viewModel = achievementsViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
