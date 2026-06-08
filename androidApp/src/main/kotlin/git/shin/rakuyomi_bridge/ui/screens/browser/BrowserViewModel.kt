package git.shin.rakuyomi_bridge.ui.screens.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcel
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import git.shin.rakuyomi_bridge.R
import git.shin.rakuyomi_bridge.data.model.Bookmark
import git.shin.rakuyomi_bridge.data.model.HistoryEntry
import git.shin.rakuyomi_bridge.data.repository.BrowserRepository
import git.shin.rakuyomi_bridge.util.Downloads
import git.shin.rakuyomi_bridge.util.UrlUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
  @param:ApplicationContext private val appContext: Context,
  private val repository: BrowserRepository
) : ViewModel() {

  private val cookieManager = CookieManager.getInstance().also { it.setAcceptCookie(true) }

  private val _addressBarText = MutableStateFlow(UrlUtils.DEFAULT_HOME)
  val addressBarText: StateFlow<String> = _addressBarText.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _canGoBack = MutableStateFlow(false)
  val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

  private val _canGoForward = MutableStateFlow(false)
  val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

  private val _progress = MutableStateFlow(0)
  val progress: StateFlow<Int> = _progress.asStateFlow()

  private val _currentUrl = MutableStateFlow<String?>(null)
  val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

  private val _currentTitle = MutableStateFlow<String?>(null)
//  val currentTitle: StateFlow<String?> = _currentTitle.asStateFlow()

  val bookmarks: StateFlow<List<Bookmark>> = repository.storage
    .map { it.bookmarks }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val history: StateFlow<List<HistoryEntry>> = repository.storage
    .map { it.history }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val webView: WebView by lazy { buildWebView() }

  @SuppressLint("SetJavaScriptEnabled")
  private fun buildWebView(): WebView {
    val wv = WebView(appContext).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
      settings.useWideViewPort = true
      settings.loadWithOverviewMode = true
      settings.setSupportMultipleWindows(false)
      settings.mediaPlaybackRequiresUserGesture = false
    }
    cookieManager.setAcceptThirdPartyCookies(wv, true)

    wv.webViewClient = object : WebViewClient() {
      override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        _isLoading.value = true
        _currentUrl.value = url
        _canGoBack.value = view?.canGoBack() ?: false
        _canGoForward.value = view?.canGoForward() ?: false
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        _isLoading.value = false
        val resolved = view?.url ?: url
        resolved?.let { resolvedUrl ->
          _currentUrl.value = resolvedUrl
          _canGoBack.value = view?.canGoBack() ?: false
          _canGoForward.value = view?.canGoForward() ?: false
          val title = view?.title
          if (title != null) _currentTitle.value = title
          _addressBarText.value = resolvedUrl
          viewModelScope.launch {
            repository.addHistory(
              HistoryEntry(
                url = resolvedUrl,
                title = title.orEmpty().ifBlank { resolvedUrl },
                visitedAt = System.currentTimeMillis()
              )
            )
          }
        }
      }
    }

    wv.webChromeClient = object : WebChromeClient() {
      override fun onProgressChanged(view: WebView?, newProgress: Int) {
        _progress.value = newProgress
      }

      override fun onReceivedTitle(view: WebView?, title: String?) {
        title?.let { _currentTitle.value = it }
      }
    }

    wv.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
      val started = Downloads.start(
        context = appContext,
        url = url,
        userAgent = userAgent,
        contentDisposition = contentDisposition,
        mimeType = mimetype
      )
      val msg = if (started != null) {
        appContext.getString(R.string.download_started)
      } else {
        appContext.getString(R.string.download_failed)
      }
      Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
    }

    return wv
  }

  init {
    viewModelScope.launch {
      val saved = repository.loadWebViewState()
      val bundle = saved?.let { bytesToBundle(it) }
      val restored = bundle?.let { webView.restoreState(it) } != null
      val currentWebViewUrl = webView.url
      if (restored && currentWebViewUrl != null) {
        _currentUrl.value = currentWebViewUrl
        _addressBarText.value = currentWebViewUrl
      } else {
        webView.loadUrl(UrlUtils.DEFAULT_HOME)
        _addressBarText.value = UrlUtils.DEFAULT_HOME
      }
    }
  }

  fun setAddressBarText(text: String) {
    _addressBarText.value = text
  }

  fun loadUrl(input: String) {
    var target = UrlUtils.normalize(input)
    if (target.startsWith("http") == false && input.isNotBlank()) {
      target = "https://www.google.com/search?q=${java.net.URLEncoder.encode(input, "UTF-8")}"
    }
    _addressBarText.value = target
    webView.loadUrl(target)
  }

  fun goBack() {
    if (webView.canGoBack()) webView.goBack()
  }

  fun goForward() {
    if (webView.canGoForward()) webView.goForward()
  }

  fun reload() {
    webView.reload()
  }

  fun stopLoading() {
    webView.stopLoading()
  }

  fun toggleBookmark() {
    val url = _currentUrl.value ?: return
    val title = _currentTitle.value.orEmpty()
    viewModelScope.launch {
      if (bookmarks.value.any { it.url == url }) {
        repository.removeBookmark(url)
      } else {
        repository.addBookmark(
          Bookmark(
            url = url,
            title = title.ifBlank { url },
            addedAt = System.currentTimeMillis()
          )
        )
      }
    }
  }

  fun removeBookmark(url: String) {
    viewModelScope.launch { repository.removeBookmark(url) }
  }

  fun clearHistory() {
    viewModelScope.launch { repository.clearHistory() }
  }

  override fun onCleared() {
    val bundle = Bundle()
    webView.saveState(bundle)
    val parcel = Parcel.obtain()
    try {
      bundle.writeToParcel(parcel, 0)
      val bytes = parcel.marshall()
      runBlocking { repository.saveWebViewState(bytes) }
    } finally {
      parcel.recycle()
    }
    webView.stopLoading()
    webView.destroy()
    super.onCleared()
  }

  private fun bytesToBundle(bytes: ByteArray): Bundle? {
    val parcel = Parcel.obtain()
    return try {
      parcel.unmarshall(bytes, 0, bytes.size)
      parcel.setDataPosition(0)
      Bundle.CREATOR.createFromParcel(parcel)
    } catch (e: Exception) {
      print(e)
      null
    } finally {
      parcel.recycle()
    }
  }
}
