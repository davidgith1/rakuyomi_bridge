package git.shin.rakuyomi_bridge.headless.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import git.shin.rakuyomi_bridge.headless.R

class AboutActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(buildUi())
  }

  private fun buildUi(): View {
    val padding = dp(24)
    val gap = dp(16)

    val root = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(Color.WHITE)
      setPadding(padding, padding, padding, padding)
      gravity = Gravity.CENTER_HORIZONTAL
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    }

    // App Icon
    val icon = ImageView(this).apply {
      setImageResource(R.mipmap.ic_launcher)
      layoutParams = LinearLayout.LayoutParams(dp(80), dp(80))
    }
    root.addView(icon)
    root.addView(spacer(gap))

    // App Name
    val name = TextView(this).apply {
      text = getString(R.string.app_name)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
      setTypeface(typeface, android.graphics.Typeface.BOLD)
      gravity = Gravity.CENTER_HORIZONTAL
    }
    root.addView(name)

    // Version
    val version = TextView(this).apply {
      val pkg = packageManager.getPackageInfo(packageName, 0)
      text = getString(R.string.version_format, pkg.versionName)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      setTextColor(Color.GRAY)
      gravity = Gravity.CENTER_HORIZONTAL
    }
    root.addView(version)
    root.addView(spacer(gap * 2))

    // Description
    val desc = TextView(this).apply {
      text = getString(R.string.about_description)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
      setTextColor(Color.DKGRAY)
      gravity = Gravity.CENTER_HORIZONTAL
    }
    root.addView(desc)
    root.addView(spacer(gap * 2))

    // Divider
    root.addView(divider())
    root.addView(spacer(gap))

    // Support
    root.addView(makeListItem(getString(R.string.support_dev), getString(R.string.ko_fi_link)) {
      openUrl("https://" + getString(R.string.ko_fi_link))
    })
    root.addView(divider())

    // Source Code
    root.addView(makeListItem(getString(R.string.source_code), getString(R.string.github_link)) {
      openUrl("https://" + getString(R.string.github_link))
    })
    root.addView(divider())

    // License
    root.addView(makeListItem(getString(R.string.license_title), getString(R.string.license_name)) {
      openUrl("https://github.com/tachibana-shin/rakuyomi/blob/main/LICENSE")
    })
    root.addView(divider())

    root.addView(spacer(gap * 2))

    // Copyright
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val copyrightText = if (currentYear > 2026) {
      "© 2026-$currentYear Tachibana Shin"
    } else {
      "© 2026 Tachibana Shin"
    }

    val copyright = TextView(this).apply {
      text = copyrightText
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      setTextColor(Color.GRAY)
      gravity = Gravity.CENTER_HORIZONTAL
    }
    root.addView(copyright)

    return root
  }

  private fun makeListItem(title: String, sub: String, onClick: () -> Unit): View {
    return LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(0, dp(12), 0, dp(12))
      isClickable = true
      val outValue = TypedValue()
      context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
      setBackgroundResource(outValue.resourceId)
      setOnClickListener { onClick() }

      addView(TextView(context).apply {
        text = title
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTextColor(Color.BLACK)
      })
      addView(TextView(context).apply {
        text = sub
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTextColor(Color.GRAY)
      })
    }
  }

  private fun divider(): View = View(this).apply {
    setBackgroundColor(Color.LTGRAY)
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
  }

  private fun spacer(height: Int): View = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
  }

  private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

  private fun openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
  }
}
