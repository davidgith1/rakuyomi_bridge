package git.shin.rakuyomi_bridge.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import git.shin.rakuyomi_bridge.BuildConfig
import git.shin.rakuyomi_bridge.R
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
  val context = LocalContext.current
  val currentYear = Calendar.getInstance()[Calendar.YEAR]
  val copyrightText = if (currentYear > 2026) {
    "(c) 2026-$currentYear Tachibana Shin"
  } else {
    "(c) 2026 Tachibana Shin"
  }

  fun openUrl(url: String) {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse(if (url.startsWith("http")) url else "https://$url"))
    context.startActivity(intent)
  }

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
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(32.dp))

      // App Icon
      Box(
        modifier = Modifier
          .size(100.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(Color.White),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_launcher_foreground),
          contentDescription = null,
          modifier = Modifier.fillMaxSize()
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(32.dp))

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

      ListItem(
        headlineContent = { Text(stringResource(R.string.support_dev)) },
        supportingContent = { Text(stringResource(R.string.ko_fi_link)) },
        leadingContent = {
          Icon(
            Icons.Default.Favorite,
            contentDescription = null,
            tint = Color(0xFFEF5350)
          )
        },
        modifier = Modifier.clickable { openUrl(context.getString(R.string.ko_fi_link)) }
      )

      HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

      ListItem(
        headlineContent = { Text(stringResource(R.string.source_code)) },
        supportingContent = { Text(stringResource(R.string.github_link)) },
        leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
        modifier = Modifier.clickable { openUrl("https://github.com/tachibana-shin/rakuyomi_bridge") }
      )

      HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

      ListItem(
        headlineContent = { Text(stringResource(R.string.license_title)) },
        supportingContent = { Text("GNU AGPL 3.0-1") },
        leadingContent = { Icon(Icons.Default.Gavel, contentDescription = null) },
        modifier = Modifier.clickable { openUrl("https://github.com/tachibana-shin/rakuyomi_bridge?tab=AGPL-3.0-1-ov-file") }
      )

      HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

      Spacer(modifier = Modifier.height(48.dp))

      Text(
        text = copyrightText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
