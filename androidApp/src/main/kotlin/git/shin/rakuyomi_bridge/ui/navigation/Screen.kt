package git.shin.rakuyomi_bridge.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import git.shin.rakuyomi_bridge.R

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
  object Home : Screen("home", R.string.home_title, Icons.Default.Home)
  object Logs : Screen("logs", R.string.logs_title, Icons.AutoMirrored.Filled.List)
  object Browser : Screen("browser", R.string.browser_title, Icons.Default.Language)
  object Settings : Screen("settings", R.string.settings_title, Icons.Default.Settings)
  object About : Screen("about", R.string.about_title, Icons.Default.Info)
}

val bottomNavItems = listOf(
  Screen.Home,
  Screen.Logs,
  Screen.Browser,
  Screen.Settings,
  Screen.About
)
