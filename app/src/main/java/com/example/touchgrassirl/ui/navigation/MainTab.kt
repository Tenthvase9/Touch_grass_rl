package com.example.touchgrassirl.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.touchgrassirl.R

enum class MainTab(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, Icons.Default.Home),
    MAP(R.string.nav_map, Icons.Default.Map),
    ACHIEVEMENTS(R.string.nav_achievements, Icons.Default.EmojiEvents),
    PROFILE(R.string.nav_profile, Icons.Default.Person),
}
