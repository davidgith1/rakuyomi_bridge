package git.shin.rakuyomi_bridge.ui.screens

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import git.shin.rakuyomi_bridge.ui.navigation.NavGraph
import git.shin.rakuyomi_bridge.ui.navigation.bottomNavItems

@Composable
fun MainApp() {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  Scaffold(
    bottomBar = {
      NavigationBar {
        bottomNavItems.forEach { screen ->
          val title = stringResource(screen.titleRes)
          NavigationBarItem(
            icon = { Icon(screen.icon, contentDescription = title) },
            label = { Text(title) },
            selected = currentRoute == screen.route,
            onClick = {
              if (currentRoute != screen.route) {
                navController.navigate(screen.route) {
                  popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              }
            }
          )
        }
      }
    }
  ) { paddingValues ->
    NavGraph(navController = navController, paddingValues = paddingValues)
  }
}
