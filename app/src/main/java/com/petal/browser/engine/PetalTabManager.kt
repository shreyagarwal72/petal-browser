package com.petal.browser.engine

import android.content.Context
import android.util.Log

/**
 * PetalTabManager
 * ─────────────────────────────────────────────────────────────────────────────
 * Delegates tab management directly to Chromium's native [PetalChromiumTabModelBridge].
 * Does NOT synthesize any custom tab lists or custom caching.
 */
object PetalTabManager {

    private const val TAG = "PetalTabManager"

    @JvmStatic
    fun init(context: Context, tabModelSelector: Any) {
        PetalChromiumTabModelBridge.init(tabModelSelector)
        Log.i(TAG, "PetalTabManager wired directly to Chromium TabModelSelector")
    }

    @JvmStatic
    fun selectTab(tabId: Int) {
        PetalChromiumTabModelBridge.selectTab(tabId)
    }

    @JvmStatic
    fun closeTab(tabId: Int) {
        PetalChromiumTabModelBridge.closeTab(tabId)
    }

    @JvmStatic
    fun closeAllIncognitoTabs() {
        PetalChromiumTabModelBridge.closeAllTabs(incognitoOnly = true)
    }

    @JvmStatic
    fun selectModel(incognito: Boolean) {
        PetalChromiumTabModelBridge.selectModel(incognito)
    }
}
