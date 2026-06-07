package git.shin.rakuyomi_bridge.ui.screens.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import git.shin.rakuyomi_bridge.R
import git.shin.rakuyomi_bridge.data.model.Bookmark
import git.shin.rakuyomi_bridge.data.model.HistoryEntry
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksSheet(
  bookmarks: List<Bookmark>,
  onDismiss: () -> Unit,
  onOpen: (String) -> Unit,
  onRemove: (String) -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState
  ) {
    SheetHeader(
      title = stringResource(R.string.bookmarks_title),
      onClose = onDismiss
    )
    if (bookmarks.isEmpty()) {
      EmptyState(stringResource(R.string.bookmarks_empty))
    } else {
      LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(bookmarks, key = { it.url }) { item ->
          UrlRow(
            title = item.title,
            subtitle = item.url,
            onClick = {
              onOpen(item.url)
              onDismiss()
            },
            onCopy = { copyToClipboard(context, item.url) },
            onShare = { shareUrl(context, item.url) },
            onOpenExternal = { openInExternalBrowser(context, item.url) },
            onRemove = { onRemove(item.url) },
            removeLabel = stringResource(R.string.action_remove)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
  history: List<HistoryEntry>,
  onDismiss: () -> Unit,
  onOpen: (String) -> Unit,
  onClearAll: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState
  ) {
    SheetHeader(
      title = stringResource(R.string.history_title),
      onClose = onDismiss,
      trailing = {
        if (history.isNotEmpty()) {
          TextButton(onClick = onClearAll) {
            Text(stringResource(R.string.action_clear_history))
          }
        }
      }
    )
    if (history.isEmpty()) {
      EmptyState(stringResource(R.string.history_empty))
    } else {
      LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        items(history, key = { it.url + it.visitedAt }) { item ->
          UrlRow(
            title = item.title,
            subtitle = dateFormat.format(Date(item.visitedAt)),
            onClick = {
              onOpen(item.url)
              onDismiss()
            },
            onCopy = { copyToClipboard(context, item.url) },
            onShare = { shareUrl(context, item.url) },
            onOpenExternal = { openInExternalBrowser(context, item.url) },
            onRemove = null,
            removeLabel = null
          )
        }
      }
    }
  }
}

@Composable
private fun SheetHeader(
  title: String,
  onClose: () -> Unit,
  trailing: @Composable (() -> Unit)? = null
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 0.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.weight(1f)
      )
      trailing?.invoke()
      IconButton(onClick = onClose) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = stringResource(R.string.cd_close_sheet)
        )
      }
    }
  }
}

@Composable
private fun EmptyState(message: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun UrlRow(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  onCopy: () -> Unit,
  onShare: () -> Unit,
  onOpenExternal: () -> Unit,
  onRemove: (() -> Unit)?,
  removeLabel: String?
) {
  var menuExpanded by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 24.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Default.Search,
      contentDescription = null,
      modifier = Modifier.size(20.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.size(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    Box {
      IconButton(onClick = { menuExpanded = true }) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = null
        )
      }
      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false }
      ) {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.action_copy_url)) },
          onClick = {
            menuExpanded = false
            onCopy()
          }
        )
        DropdownMenuItem(
          text = { Text(stringResource(R.string.action_share_url)) },
          onClick = {
            menuExpanded = false
            onShare()
          }
        )
        DropdownMenuItem(
          text = { Text(stringResource(R.string.action_open_external)) },
          onClick = {
            menuExpanded = false
            onOpenExternal()
          }
        )
        if (onRemove != null && removeLabel != null) {
          DropdownMenuItem(
            text = { Text(removeLabel) },
            onClick = {
              menuExpanded = false
              onRemove()
            }
          )
        }
      }
    }
  }
}

private fun copyToClipboard(context: Context, url: String) {
  val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
  manager.setPrimaryClip(ClipData.newPlainText("url", url))
  Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
}

private fun shareUrl(context: Context, url: String) {
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, url)
  }
  val chooser = Intent.createChooser(intent, context.getString(R.string.action_share_url))
  chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  context.startActivity(chooser)
}

private fun openInExternalBrowser(context: Context, url: String) {
  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  runCatching { context.startActivity(intent) }
}
