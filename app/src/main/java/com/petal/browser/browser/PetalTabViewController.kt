package com.petal.browser.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.petal.browser.media.PetalMediaBridge

@SuppressLint("SetJavaScriptEnabled")
open class PetalTabViewController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), AlbumController {

    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var tabId: String = ""
    private var tabGroupId: String? = null
    private var tabGroupTitle: String? = null
    private val mediaBridge = PetalMediaBridge(context)

    data class BrowserState(
        val url: String = "",
        val title: String = "",
        val progress: Int = 100,
        val isSecure: Boolean = false,
        val loading: Boolean = false
    )

    fun interface OnBrowserStateChangeListener {
        fun onStateChanged(state: BrowserState)
    }

    private var stateChangeListener: OnBrowserStateChangeListener? = null

    fun getOnBrowserStateChanged(): OnBrowserStateChangeListener? = stateChangeListener

    fun setOnBrowserStateChanged(listener: OnBrowserStateChangeListener?) {
        this.stateChangeListener = listener
    }

    fun setOnBrowserStateChanged(listener: (BrowserState) -> Unit) {
        this.stateChangeListener = OnBrowserStateChangeListener { listener(it) }
    }

    protected lateinit var webView: WebView

    init {
        initWebView()
    }

    private fun initWebView() {
        webView = WebView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
                allowFileAccess = true
                allowContentAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    this@PetalTabViewController.progress = newProgress
                    val isLoading = newProgress < 100
                    stateChangeListener?.onStateChanged(
                        BrowserState(
                            url = currentUrl ?: "",
                            title = currentTitle ?: "",
                            progress = newProgress,
                            isSecure = isSecure,
                            loading = isLoading
                        )
                    )
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrEmpty()) {
                        currentTitle = title
                        notifyState()
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    currentUrl = url
                    isSecure = url?.startsWith("https://") == true
                    stateChangeListener?.onStateChanged(
                        BrowserState(
                            url = currentUrl ?: "",
                            title = currentTitle ?: "",
                            progress = this@PetalTabViewController.progress,
                            isSecure = isSecure,
                            loading = true
                        )
                    )
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    currentUrl = url
                    currentTitle = view?.title ?: currentTitle
                    isSecure = url?.startsWith("https://") == true
                    stateChangeListener?.onStateChanged(
                        BrowserState(
                            url = currentUrl ?: "",
                            title = currentTitle ?: "",
                            progress = 100,
                            isSecure = isSecure,
                            loading = false
                        )
                    )
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val reqUrl = request?.url?.toString()
                    if (reqUrl != null && (reqUrl.startsWith("http://") || reqUrl.startsWith("https://") || reqUrl.startsWith("about:"))) {
                        return false
                    }
                    return false
                }
            }
        }
        addView(webView)
    }

    fun getWebView(): WebView = webView

    override fun getAlbumView(): View = this
    override fun getUrl(): String? = currentUrl ?: webView.url
    fun setUrl(url: String?) {
        currentUrl = url
        notifyState()
    }

    override fun getTitle(): String? = currentTitle ?: webView.title
    fun setTitle(title: String?) {
        currentTitle = title
        notifyState()
    }

    override fun getPersistentUrl(): String? = currentUrl ?: webView.url
    override fun getTabId(): String = tabId
    fun setTabId(id: String) { tabId = id }
    override fun getTabGroupId(): String? = tabGroupId
    override fun setTabGroupId(id: String?) { tabGroupId = id }
    override fun getTabGroupTitle(): String? = tabGroupTitle
    override fun setTabGroupTitle(title: String?) { tabGroupTitle = title }

    override fun getFavicon(): Bitmap? = webView.favicon
    fun getCachedPreviewBitmap(): Bitmap? = null
    override fun getMediaBridge(): PetalMediaBridge = mediaBridge
    fun isAdoptedPopup(): Boolean = false
    fun captureFullPageBitmap(callback: (Bitmap?) -> Unit) { callback(null) }
    override fun setDesktopMode(enabled: Boolean) {
        val ua = if (enabled) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        } else {
            ""
        }
        setUserAgent(ua)
    }
    fun isPageAtTop(): Boolean = webView.scrollY == 0
    fun bindTab(pair: Any?) {}
    fun setTrackingProtection(enabled: Boolean) {}
    val currentState: BrowserState get() = BrowserState(getUrl() ?: "", getTitle() ?: "", progress, isSecure)
    fun capturePreviewBitmapAsync(callback: (Bitmap?) -> Unit) { callback(null) }
    fun setUserAgent(ua: String) {
        if (ua.isEmpty()) {
            webView.settings.userAgentString = null
        } else {
            webView.settings.userAgentString = ua
        }
    }

    var progress: Int = 0
        set(value) {
            field = value
            notifyState()
        }
    var isSecure: Boolean = false
        set(value) {
            field = value
            notifyState()
        }

    fun attachLifecycle(owner: Any?) {}

    fun processBackPressed(onFallback: (() -> Unit)? = null): Boolean {
        if (canGoBack()) {
            goBack()
            return true
        }
        onFallback?.invoke()
        return false
    }

    fun processBackPressed(fallback: Boolean): Boolean {
        if (canGoBack()) {
            goBack()
            return true
        }
        return fallback
    }

    private var predecessor: AlbumController? = null
    fun getPredecessor(): AlbumController? = predecessor
    fun setPredecessor(controller: AlbumController?) { predecessor = controller }

    open fun clearFindMatches() { webView.clearMatches() }
    open fun findAll(query: String) { webView.findAllAsync(query) }
    open fun findNext(forward: Boolean) { webView.findNext(forward) }

    fun reloadBypassCache() {
        webView.clearCache(true)
        reload()
    }

    override fun canGoBack(): Boolean = webView.canGoBack()
    override fun goBack() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }
    override fun reload() { webView.reload() }
    override fun loadUrl(url: String) {
        currentUrl = url
        isSecure = url.startsWith("https://")
        notifyState()
        if (url.isNotEmpty() && !url.equals("about:blank", ignoreCase = true)) {
            webView.loadUrl(url)
        }
    }
    override fun activate() {
        webView.onResume()
    }
    override fun deactivate() {
        webView.onPause()
    }
    override fun destroy() {
        try {
            webView.stopLoading()
            removeView(webView)
            webView.destroy()
        } catch (ignored: Throwable) {}
    }

    private fun notifyState() {
        stateChangeListener?.onStateChanged(currentState)
    }

    open fun captureForSwitcher(callback: java.util.function.Consumer<Bitmap?>) {
        callback.accept(null)
    }
    fun currentState(): BrowserState = currentState
    open fun loadData(data: String?, mimeType: String?, encoding: String?) {
        webView.loadData(data ?: "", mimeType ?: "text/html", encoding ?: "UTF-8")
    }
    private var pwaManager: com.petal.browser.pwa.PetalPwaManager? = null
    open fun getPwaManager(): com.petal.browser.pwa.PetalPwaManager? = pwaManager
    open fun setPwaManager(manager: com.petal.browser.pwa.PetalPwaManager?) { pwaManager = manager }
    override fun printToPdf(fos: java.io.FileOutputStream, callback: java.util.function.Consumer<Boolean>) {
        callback.accept(false)
    }

    open fun updatePreviewCache(force: Boolean = true) {}
}
