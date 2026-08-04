package com.aman.debts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.webkit.WebViewAssetLoader
import com.aman.debts.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var webView: WebView
        private set

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val homeUrl = "https://appassets.androidplatform.net/assets/index.html"

    /** منتقي الملفات (لرفع شعار/QR/ختم/توقيع من معرض الصور) */
    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val results: Array<Uri>? = when {
                result.resultCode != RESULT_OK || data == null -> null
                data.clipData != null -> {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                }
                data.data != null -> arrayOf(data.data!!)
                else -> null
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }

    /** طلب صلاحية التخزين (أندرويد 6 - 9 فقط، الإصدارات الأحدث لا تحتاجها) */
    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        webView = binding.webView

        requestLegacyStoragePermissionIfNeeded()
        setupWebView()
        binding.retryButton.setOnClickListener { loadHomeIfConnected() }

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) webView.goBack() else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        loadHomeIfConnected()
    }

    private fun requestLegacyStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun setupWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = false
        }

        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                val scheme = url.scheme ?: ""
                val host = url.host ?: ""

                // روابط داخل التطبيق نفسه (الصفحات المحلية) تبقى داخل WebView
                if (host == "appassets.androidplatform.net") return false

                // روابط تفتح تطبيقات خارجية (واتساب/تيليجرام/بريد/هاتف) أو أي رابط خارجي آخر
                if (scheme == "mailto" || scheme == "tel" || host == "wa.me" || host == "t.me") {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                    } catch (_: Exception) { /* لا يوجد تطبيق يدعمه */ }
                    return true
                }
                if (scheme == "http" || scheme == "https") {
                    // مصادر (CDN) تُحمَّل ضمن نفس الصفحة تلقائياً عبر شبكة الإنترنت،
                    // لكن أي تنقّل كامل (فتح رابط جديد بالكامل) يُفتح في المتصفح الخارجي.
                    if (!request.isForMainFrame) return false
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                        return true
                    } catch (_: Exception) { return false }
                }
                return false
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = android.view.View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.progressBar.visibility =
                    if (newProgress in 1..99) android.view.View.VISIBLE else android.view.View.GONE
                binding.progressBar.progress = newProgress
            }

            override fun onShowFileChooser(
                webView: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                return try {
                    fileChooserLauncher.launch(params.createIntent())
                    true
                } catch (_: Exception) {
                    filePathCallback = null
                    false
                }
            }

            // لا حاجة لأذونات كاميرا/ميكروفون داخل هذا التطبيق
            override fun onPermissionRequest(request: PermissionRequest) {
                request.deny()
            }
        }
    }

    private fun loadHomeIfConnected() {
        if (isOnline()) {
            binding.noConnectionView.visibility = android.view.View.GONE
            webView.visibility = android.view.View.VISIBLE
            if (webView.url == null) webView.loadUrl(homeUrl)
        } else {
            binding.noConnectionView.visibility = android.view.View.VISIBLE
            webView.visibility = android.view.View.GONE
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network: Network = cm.activeNetwork ?: return false
        val caps: NetworkCapabilities = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
