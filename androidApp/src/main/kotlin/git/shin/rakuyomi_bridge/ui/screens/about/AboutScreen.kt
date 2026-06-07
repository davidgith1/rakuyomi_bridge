package git.shin.rakuyomi_bridge.ui.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import git.shin.rakuyomi_bridge.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.about_title)) }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(24.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = stringResource(R.string.app_name),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.version_format, "0.1.0"),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(32.dp))

      Card(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = stringResource(R.string.about_description),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      Text(
        text = stringResource(R.string.credits_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.align(Alignment.Start)
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.credits_text),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.align(Alignment.Start)
      )

      Spacer(modifier = Modifier.weight(1f))

      Text(
        text = stringResource(R.string.copyright),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
