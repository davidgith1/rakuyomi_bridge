package git.shin.rakuyomi_bridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import git.shin.rakuyomi_bridge.ui.screens.MainApp
import git.shin.rakuyomi_bridge.ui.theme.RakuyomiTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakuyomiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                  MainApp()
                }
            }
        }
    }
}
