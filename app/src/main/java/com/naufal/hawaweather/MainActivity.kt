package com.naufal.hawaweather

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : ComponentActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set matching dark status bar color
        window.statusBarColor = Color.parseColor("#070A14")

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true

            // Seamless Google Sign-In: Remove "; wv" marker so Google OAuth does not block or degrade WebView
            val defaultUa = settings.userAgentString
            settings.userAgentString = defaultUa.replace("; wv", "")

            // Accept cookies (required for Google OAuth session & Turso API cookies)
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    android.util.Log.i("HawaWeb", "PAGE_FINISHED $url")
                }
                override fun onReceivedError(
                    view: WebView, req: WebResourceRequest,
                    err: WebResourceError,
                ) {
                    android.util.Log.e("HawaWeb", "PAGE_ERROR ${req.url} ${err.description}")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: android.webkit.ConsoleMessage): Boolean {
                    android.util.Log.i("HawaWeb", "CONSOLE ${m.messageLevel()} ${m.message()}")
                    return true
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean {
                    val popupWeb = WebView(this@MainActivity).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = settings.userAgentString.replace("; wv", "")
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val urlStr = request.url.toString()
                                if (urlStr.startsWith("https://accounts.google.com") || urlStr.contains("hawa")) {
                                    return false
                                }
                                return try {
                                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                            }
                        }
                    }
                    val transport = resultMsg?.obj as? WebView.WebViewTransport
                    transport?.webView = popupWeb
                    resultMsg?.sendToTarget()
                    return true
                }
            }

            loadUrl(getString(R.string.app_url))
        }

        // Apply system bar insets so content never collides with status/navigation bars
        ViewCompat.setOnApplyWindowInsetsListener(web) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(web)
        ViewCompat.requestApplyInsets(web)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Refresh home screen widget automatically when app is active
        HawaWidget.refreshAll(this)
    }
}
