package git.shin.rakuyomi_bridge.ui.screens.browser

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import git.shin.rakuyomi_bridge.R

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
  viewModel: BrowserViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val isLoading by viewModel.isLoading.collectAsState()
  val canGoBack by viewModel.canGoBack.collectAsState()
  val canGoForward by viewModel.canGoForward.collectAsState()
  val progress by viewModel.progress.collectAsState()
  val currentUrl by viewModel.currentUrl.collectAsState()
  val addressBarText by viewModel.addressBarText.collectAsState()
  val bookmarks by viewModel.bookmarks.collectAsState()
  val history by viewModel.history.collectAsState()

  val isBookmarked by remember(currentUrl, bookmarks) {
    derivedStateOf {
      val url = currentUrl ?: return@derivedStateOf false
      bookmarks.any { it.url == url }
    }
  }

  var showBookmarks by remember { mutableStateOf(false) }
  var showHistory by remember { mutableStateOf(false) }
  var moreMenuExpanded by remember { mutableStateOf(false) }
  var addressFocused by remember { mutableStateOf(false) }
  val focusRequester = remember { FocusRequester() }

  val webView = viewModel.webView

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> webView.onResume()
        Lifecycle.Event.ON_PAUSE -> webView.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Scaffold { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      AndroidView(
        factory = { webView },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      )
      if (isLoading) {
        LinearProgressIndicator(
          progress = { (progress.coerceIn(0, 100)) / 100f },
          modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
        )
      }
      BrowserBottomBar(
        text = addressBarText,
        onTextChange = viewModel::setAddressBarText,
        onSubmit = { viewModel.loadUrl(addressBarText) },
        canGoBack = canGoBack,
        canGoForward = canGoForward,
        isLoading = isLoading,
        isBookmarked = isBookmarked,
        moreMenuExpanded = moreMenuExpanded,
        onMoreMenuExpandedChange = { moreMenuExpanded = it },
        onBack = viewModel::goBack,
        onForward = viewModel::goForward,
        onReloadOrStop = {
          if (isLoading) viewModel.stopLoading() else viewModel.reload()
        },
        onClear = { viewModel.setAddressBarText("") },
        onFocusChange = { addressFocused = it },
        onToggleBookmark = viewModel::toggleBookmark,
        onShowBookmarks = { showBookmarks = true },
        onShowHistory = { showHistory = true },
        onClearCookies = {
          CookieManager.getInstance().removeAllCookies(null)
          CookieManager.getInstance().removeSessionCookies(null)
          Toast.makeText(
            context,
            context.getString(R.string.cookies_cleared),
            Toast.LENGTH_SHORT
          ).show()
        },
        focusRequester = focusRequester
      )
    }
  }

  if (showBookmarks) {
    BookmarksSheet(
      bookmarks = bookmarks,
      onDismiss = { showBookmarks = false },
      onOpen = { url -> viewModel.loadUrl(url) },
      onRemove = { viewModel.removeBookmark(it) }
    )
  }

  if (showHistory) {
    HistorySheet(
      history = history,
      onDismiss = { showHistory = false },
      onOpen = { url -> viewModel.loadUrl(url) },
      onClearAll = { viewModel.clearHistory() }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserBottomBar(
  text: String,
  onTextChange: (String) -> Unit,
  onSubmit: () -> Unit,
  canGoBack: Boolean,
  canGoForward: Boolean,
  isLoading: Boolean,
  isBookmarked: Boolean,
  moreMenuExpanded: Boolean,
  onMoreMenuExpandedChange: (Boolean) -> Unit,
  onBack: () -> Unit,
  onForward: () -> Unit,
  onReloadOrStop: () -> Unit,
  onClear: () -> Unit,
  onFocusChange: (Boolean) -> Unit,
  onToggleBookmark: () -> Unit,
  onShowBookmarks: () -> Unit,
  onShowHistory: () -> Unit,
  onClearCookies: () -> Unit,
  focusRequester: FocusRequester
) {
  val view = LocalView.current
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack, enabled = canGoBack) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_back)
        )
      }
      IconButton(onClick = onForward, enabled = canGoForward) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = stringResource(R.string.cd_forward)
        )
      }
      IconButton(onClick = onReloadOrStop) {
        Icon(
          imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
          contentDescription = stringResource(
            if (isLoading) R.string.cd_stop else R.string.cd_refresh
          )
        )
      }
      OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .focusRequester(focusRequester)
          .onFocusChanged { focusState -> onFocusChange(focusState.isFocused) },
        colors = TextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedIndicatorColor = MaterialTheme.colorScheme.primary,
          unfocusedIndicatorColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.extraLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(
          onGo = {
            onSubmit()
            hideKeyboard(view)
          }
        ),
        trailingIcon = {
          if (text.isNotEmpty()) {
            IconButton(onClick = onClear) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_clear_url)
              )
            }
          }
        }
      )
      Box {
        IconButton(onClick = { onMoreMenuExpandedChange(true) }) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.cd_more)
          )
        }
        DropdownMenu(
          expanded = moreMenuExpanded,
          onDismissRequest = { onMoreMenuExpandedChange(false) }
        ) {
          DropdownMenuItem(
            text = {
              Text(
                text = stringResource(
                  if (isBookmarked) R.string.bookmark_remove else R.string.bookmark_add
                )
              )
            },
            leadingIcon = {
              Icon(
                imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Filled.FavoriteBorder,
                contentDescription = null
              )
            },
            onClick = {
              onMoreMenuExpandedChange(false)
              onToggleBookmark()
            }
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.bookmarks_title)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null
              )
            },
            onClick = {
              onMoreMenuExpandedChange(false)
              onShowBookmarks()
            }
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.history_title)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null
              )
            },
            onClick = {
              onMoreMenuExpandedChange(false)
              onShowHistory()
            }
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.action_clear_cookies)) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null
              )
            },
            onClick = {
              onMoreMenuExpandedChange(false)
              onClearCookies()
            }
          )
        }
      }
    }
  }
}

private fun hideKeyboard(view: android.view.View) {
  view.clearFocus()
  val imm = view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
    as? android.view.inputmethod.InputMethodManager
  imm?.hideSoftInputFromWindow(view.windowToken, 0)
}
