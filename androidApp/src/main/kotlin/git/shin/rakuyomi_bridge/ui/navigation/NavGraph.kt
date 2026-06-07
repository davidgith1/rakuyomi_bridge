package git.shin.rakuyomi_bridge.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import git.shin.rakuyomi_bridge.ui.screens.about.AboutScreen
import git.shin.rakuyomi_bridge.ui.screens.browser.BrowserScreen
import git.shin.rakuyomi_bridge.ui.screens.home.HomeScreen
import git.shin.rakuyomi_bridge.ui.screens.log.LogScreen
import git.shin.rakuyomi_bridge.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(
  navController: NavHostController,
  paddingValues: PaddingValues
) {
  NavHost(
    navController = navController,
    startDestination = Screen.Home.route,
    modifier = Modifier.padding(paddingValues)
  ) {
    composable(Screen.Home.route) {
      HomeScreen()
    }
    composable(Screen.Logs.route) {
      LogScreen()
    }
    composable(Screen.Browser.route) {
      BrowserScreen()
    }
    composable(Screen.Settings.route) {
      SettingsScreen()
    }
    composable(Screen.About.route) {
      AboutScreen()
    }
  }
}
