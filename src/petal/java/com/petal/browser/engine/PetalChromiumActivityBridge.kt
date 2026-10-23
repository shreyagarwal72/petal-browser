package com.petal.browser.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.petal.browser.browser.AlbumController
import com.petal.browser.shields.PetalShieldsObserver

/**
 * PetalChromiumActivityBridge
 *
 * Singleton ViewModel-style bridge between Petal's Chromium native layer and
 * Petal's Compose UI (PetalUIRoot → PetalComposeShell).
 *
 * All state is exposed as StateFlow so Compose collects it reactively.
 * Updates are pushed from PetalTabManager (tab events) and PetalShieldsObserver
 * (shields events) which themselves receive callbacks from Chromium JNI.
 *
 * Navigation commands (goBack, navigate, etc.) delegate down to Chromium's
 * Tab / LoadUrlParams JNI via PetalTabManager.
 */
class PetalChromiumActivityBridge private constructor() {

    companion object {
        @Volatile private var instance: PetalChromiumActivityBridge? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: PetalChromiumActivityBridge().also { instance = it }
        }
    }

    // --- Tab state -----------------------------------------------------------
    val currentUrl     = MutableStateFlow("about:blank")
    val currentTitle   = MutableStateFlow("")
    val loadProgress   = MutableStateFlow(0)
    val canGoBack      = MutableStateFlow(false)
    val canGoForward   = MutableStateFlow(false)
    val tabCount       = MutableStateFlow(1)
    val isIncognito    = MutableStateFlow(false)
    val currentTabId   = MutableStateFlow("")
    val allTabs        = MutableStateFlow<List<AlbumController>>(emptyList())

    // --- Shields state (observed from PetalShieldsObserver) ------------------
    lateinit var shieldsObserver: PetalShieldsObserver

    // --- Navigation commands -------------------------------------------------

    fun navigate(url: String) {
        try {
            val mgr = Class.forName("com.petal.browser.engine.PetalTabManager")
                .getMethod("getInstance").invoke(null)
            mgr.javaClass.getMethod("loadUrl", String::class.java).invoke(mgr, url)
        } catch (e: Exception) {
            android.util.Log.w("PetalBridge", "navigate: $e")
        }
    }

    fun goBack() {
        try {
            val mgr = PetalTabManager::class.java.getDeclaredField("instance")
                .get(null) as? PetalTabManager
            mgr?.getActiveTab()?.let { tab ->
                tab.javaClass.getMethod("goBack").invoke(tab)
            }
        } catch (e: Exception) { android.util.Log.w("PetalBridge", "goBack: $e") }
    }

    fun goForward() {
        try {
            val mgr = PetalTabManager::class.java.getDeclaredField("instance")
                .get(null) as? PetalTabManager
            mgr?.getActiveTab()?.let { tab ->
                tab.javaClass.getMethod("goForward").invoke(tab)
            }
        } catch (e: Exception) { android.util.Log.w("PetalBridge", "goForward: $e") }
    }

    fun selectTab(tab: AlbumController) { /* delegate to TabModelSelector */ }
    fun closeTab(tab: AlbumController) { /* delegate to TabModelSelector */ }
    fun openNewTab(incognito: Boolean) {
        try {
            val mgr = Class.forName("com.petal.browser.engine.PetalTabManager")
                .getMethod("getInstance").invoke(null)
            mgr?.javaClass?.getMethod("openNewTab", Boolean::class.javaPrimitiveType)
                ?.invoke(mgr, incognito)
            if (incognito) {
                this.isIncognito.value = true
                android.util.Log.i("PetalBridge", "Opened new OTR incognito tab")
            }
        } catch (e: Exception) {
            android.util.Log.w("PetalBridge", "openNewTab: $e")
        }
    }
    fun openSettings() { /* start PetalSettingsActivity */ }
    fun sharePage() { /* Android share intent */ }
    fun toggleDesktopSite() {
        try {
            val mgr = PetalTabManager::class.java.getDeclaredField("instance").get(null) as? PetalTabManager
            val activeTab = mgr?.getActiveTab() ?: return
            val webContents = activeTab.javaClass.getMethod("getWebContents").invoke(activeTab)
            val currentUa = webContents?.javaClass?.getMethod("getUserAgentOverride")?.invoke(webContents) as? String ?: ""
            val isCurrentlyDesktop = currentUa.isNotEmpty() && !currentUa.contains("Android")
            val newDesktopEnabled = !isCurrentlyDesktop

            if (newDesktopEnabled) {
                val defaultUa = webContents?.javaClass?.getMethod("getUserAgent")?.invoke(webContents) as? String ?: ""
                val desktopUa = com.petal.browser.engine.petal.DesktopSiteRules.desktopUserAgent(defaultUa)
                webContents?.javaClass?.getMethod("setUserAgentOverride", String::class.java, Boolean::class.javaPrimitiveType)
                    ?.invoke(webContents, desktopUa, false)
            } else {
                webContents?.javaClass?.getMethod("setUserAgentOverride", String::class.java, Boolean::class.javaPrimitiveType)
                    ?.invoke(webContents, "", false)
            }

            runCatching {
                com.petal.browser.bridge.NativePrefBridge.setRequestDesktopSite(newDesktopEnabled)
            }

            activeTab.javaClass.getMethod("reload").invoke(activeTab)
            android.util.Log.i("PetalBridge", "toggleDesktopSite: desktopEnabled=$newDesktopEnabled")
        } catch (e: Exception) {
            android.util.Log.w("PetalBridge", "toggleDesktopSite: $e")
        }
    }
    fun toggleReaderMode() { /* Chromium DomDistillerService */ }
    fun bookmarkCurrentPage() { /* Chromium BookmarkModel */ }
    fun downloadCurrentPage() { /* Chromium DownloadManagerService */ }

    // --- State push from PetalTabManager/JNI callbacks -----------------------

    fun onTabUpdated(url: String, title: String, progress: Int, backEnabled: Boolean,
                     forwardEnabled: Boolean, tabId: String, incognito: Boolean) {
        currentUrl.value     = url
        currentTitle.value   = title
        loadProgress.value   = progress
        canGoBack.value      = backEnabled
        canGoForward.value   = forwardEnabled
        currentTabId.value   = tabId
        isIncognito.value    = incognito
        shieldsObserver.refreshForTab(tabId, extractHost(url))
    }

    fun onTabListChanged(tabs: List<AlbumController>) {
        allTabs.value = tabs
        tabCount.value = tabs.size
    }

    private fun extractHost(url: String): String {
        return try { java.net.URL(url).host } catch (e: Exception) { url }
    }
}
