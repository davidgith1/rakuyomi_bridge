package git.shin.rakuyomi_bridge.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
  object Home : Screen("home", "Home", Icons.Default.Home)
  object Logs : Screen("logs", "Logs", Icons.AutoMirrored.Filled.List)
  object Browser : Screen("browser", "Browser", Icons.Default.Language)
  object Settings : Screen("settings", "Settings", Icons.Default.Settings)
  object About : Screen("about", "About", Icons.Default.Info)
}

val bottomNavItems = listOf(
  Screen.Home,
  Screen.Logs,
  Screen.Browser,
  Screen.Settings,
  Screen.About
)
