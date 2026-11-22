package com.naufal.hawaweather

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                runOnUiThread {
                    web.evaluateJavascript(
                        "window.onNativeGoogleLoginSuccess && window.onNativeGoogleLoginSuccess('$idToken')",
                        null
                    )
                }
            } else {
                runOnUiThread {
                    web.evaluateJavascript(
                        "window.onNativeGoogleLoginError && window.onNativeGoogleLoginError('Gagal mendapatkan ID token dari Google')",
                        null
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HawaAuth", "Google sign-in exception: ${e.message}")
            val safeMsg = e.message?.replace("'", "\\'")?.replace("\n", " ") ?: "Login dibatalkan"
            runOnUiThread {
                web.evaluateJavascript(
                    "window.onNativeGoogleLoginError && window.onNativeGoogleLoginError('$safeMsg')",
                    null
                )
            }
        }
    }

    fun startNativeGoogleSignIn() {
        // Sign out first so user can choose account or switch account if desired
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    class WebAppInterface(private val activity: MainActivity) {
        @JavascriptInterface
        fun loginWithGoogle() {
            activity.runOnUiThread {
                activity.startNativeGoogleSignIn()
            }
        }

        @JavascriptInterface
        fun isAndroidApp(): Boolean = true
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set matching dark status bar color
        window.statusBarColor = Color.parseColor("#070A14")

        // Initialize Native Google Sign-In Client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            clearCache(true)

            // Register native bridge for seamless 1-tap Google Sign-In
            addJavascriptInterface(WebAppInterface(this@MainActivity), "Android")

            // Remove "; wv" marker so Google OAuth does not block or degrade WebView
            val defaultUa = settings.userAgentString
            settings.userAgentString = defaultUa.replace("; wv", "")

            // Accept cookies (required for Google OAuth session & Turso API cookies)
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
            cookieManager.flush()

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val urlStr = request.url.toString()
                    if (urlStr.startsWith("https://accounts.google.com") ||
                        urlStr.contains("naufalputra.my.id") ||
                        urlStr.contains("hawa") ||
                        urlStr.startsWith("https://lh3.googleusercontent.com")) {
                        return false
                    }
                    return try {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
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
                    val popupDialog = android.app.Dialog(this@MainActivity, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
                    val popupWeb = WebView(this@MainActivity).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = settings.userAgentString.replace("; wv", "")
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val urlStr = request.url.toString()
                                if (urlStr.startsWith("https://accounts.google.com") || urlStr.contains("hawa") || urlStr.contains("naufalputra.my.id")) {
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
                        webChromeClient = object : WebChromeClient() {
                            override fun onCloseWindow(window: WebView?) {
                                popupDialog.dismiss()
                            }
                        }
                    }
                    popupDialog.setContentView(popupWeb)
                    popupDialog.show()
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
        // Refresh home screen widgets automatically when app is active
        HawaWidget.refreshAll(this)
        HawaClockWidget.refreshAll(this)
    }
}
