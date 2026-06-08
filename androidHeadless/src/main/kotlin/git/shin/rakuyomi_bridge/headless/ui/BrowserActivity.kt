package git.shin.rakuyomi_bridge.headless.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import git.shin.rakuyomi_bridge.util.UrlUtils

class BrowserActivity : Activity() {

  private lateinit var addressBar: EditText
  private lateinit var webView: WebView
  private lateinit var progressBar: ProgressBar
  private lateinit var backButton: Button
  private lateinit var forwardButton: Button

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val root = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    }

    val topBar = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    backButton = Button(this).apply {
      text = "<"
      layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
      setOnClickListener { if (webView.canGoBack()) webView.goBack() }
    }
    topBar.addView(backButton)

    forwardButton = Button(this).apply {
      text = ">"
      layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
      setOnClickListener { if (webView.canGoForward()) webView.goForward() }
    }
    topBar.addView(forwardButton)

    addressBar = EditText(this).apply {
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
      imeOptions = EditorInfo.IME_ACTION_GO
      setSingleLine()
      setOnEditorActionListener { _, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_GO ||
          (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)
        ) {
          loadUrl(text.toString())
          true
        } else {
          false
        }
      }
    }
    topBar.addView(addressBar)

    val reloadButton = Button(this).apply {
      text = "R"
      layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
      setOnClickListener { webView.reload() }
    }
    topBar.addView(reloadButton)

    val closeButton = Button(this).apply {
      text = "X"
      layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
      setOnClickListener { finish() }
    }
    topBar.addView(closeButton)

    root.addView(topBar)

    progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(4)
      )
      max = 100
      visibility = android.view.View.GONE
    }
    root.addView(progressBar)

    webView = WebView(this).apply {
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        0,
        1f
      )
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
      settings.useWideViewPort = true
      settings.loadWithOverviewMode = true

      val cookieManager = CookieManager.getInstance()
      cookieManager.setAcceptCookie(true)
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
        cookieManager.setAcceptThirdPartyCookies(this, true)
      }

      webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
          progressBar.visibility = android.view.View.VISIBLE
          addressBar.setText(url)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          progressBar.visibility = android.view.View.GONE
          updateNavButtons()
        }
      }

      webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
          progressBar.progress = newProgress
        }
      }
    }
    root.addView(webView)

    setContentView(root)

    loadUrl(UrlUtils.DEFAULT_HOME)
  }

  private fun loadUrl(input: String) {
    var url = UrlUtils.normalize(input)
    if (!url.startsWith("http") && input.isNotBlank()) {
      url = "https://www.google.com/search?q=${java.net.URLEncoder.encode(input, "UTF-8")}"
    }
    webView.loadUrl(url)
    addressBar.clearFocus()
    val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
    imm.hideSoftInputFromWindow(addressBar.windowToken, 0)
  }

  private fun updateNavButtons() {
    backButton.isEnabled = webView.canGoBack()
    forwardButton.isEnabled = webView.canGoForward()
  }

  private fun dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

  override fun onBackPressed() {
    if (webView.canGoBack()) {
      webView.goBack()
    } else {
      super.onBackPressed()
    }
  }
}
