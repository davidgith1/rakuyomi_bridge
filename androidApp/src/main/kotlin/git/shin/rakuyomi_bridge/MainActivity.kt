package git.shin.rakuyomi_bridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import git.shin.rakuyomi_bridge.data.repository.UpdateRepository
import git.shin.rakuyomi_bridge.ui.components.dialogs.UpdateDialog
import git.shin.rakuyomi_bridge.ui.screens.MainApp
import git.shin.rakuyomi_bridge.ui.screens.main.MainViewModel
import git.shin.rakuyomi_bridge.ui.theme.RakuyomiTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var updateRepository: UpdateRepository

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val updateInfo by updateRepository.updateInfo.collectAsState()

            LaunchedEffect(Unit) {
                updateRepository.checkForUpdate()
            }

            RakuyomiTheme(appTheme = appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainApp()

                    updateInfo?.let { info ->
                        UpdateDialog(
                            info = info,
                            onDismiss = { updateRepository.dismissUpdate() },
                            onConfirm = { updateRepository.downloadAndInstall(info) }
                        )
                    }
                }
            }
        }
    }
}
