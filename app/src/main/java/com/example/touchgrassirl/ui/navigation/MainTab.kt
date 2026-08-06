package com.example.touchgrassirl.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.touchgrassirl.R

enum class MainTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, Icons.Default.Home),
    FRIENDS(R.string.nav_friends, Icons.Default.People),
    PROFILE(R.string.nav_profile, Icons.Default.Person),
}
