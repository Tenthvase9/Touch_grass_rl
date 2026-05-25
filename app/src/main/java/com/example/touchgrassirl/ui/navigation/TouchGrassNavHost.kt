package com.example.touchgrassirl.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.touchgrassirl.TouchGrassApp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.touchgrassirl.data.repository.SessionResult
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.ui.celebration.CelebrationScreen
import com.example.touchgrassirl.ui.main.MainScreen
import com.example.touchgrassirl.ui.session.SessionScreen
import com.example.touchgrassirl.ui.session.SessionViewModel

@Composable
fun TouchGrassNavHost(
    repository: TouchGrassRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    var celebrationResult by remember { mutableStateOf<SessionResult?>(null) }

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        modifier = modifier,
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                repository = repository,
                onStartSession = { navController.navigate(Routes.SESSION) },
            )
        }

        composable(Routes.SESSION) {
            val app = LocalContext.current.applicationContext as TouchGrassApp
            val sessionViewModel: SessionViewModel = viewModel(
                factory = SessionViewModel.Factory(
                    repository = repository,
                    motionTracker = app.sessionMotionTracker,
                ),
            )
            SessionScreen(
                viewModel = sessionViewModel,
                onSessionEnded = { result ->
                    celebrationResult = result
                    navController.navigate(Routes.CELEBRATION) {
                        popUpTo(Routes.SESSION) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onCancel = {
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.CELEBRATION) {
            val result = celebrationResult
            if (result != null) {
                CelebrationScreen(
                    result = result,
                    onDone = {
                        celebrationResult = null
                        navController.popBackStack(Routes.MAIN, inclusive = false)
                    },
                )
            }
        }
    }
}
