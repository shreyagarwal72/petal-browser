package com.petal.browser.engine

import android.content.Context
import android.util.Log
import com.petal.browser.browser.AlbumController

/**
 * PetalTabManager
 *
 * Manages the mapping between Chromium's TabModelSelector (native C++ tab model)
 * and Petal's AlbumController tab abstraction.
 *
 * Replaces PetalEngineStore.kt (which managed GeckoView sessions).
 *
 * Responsibilities:
 *  - Wraps each Chromium Tab in a PetalChromiumTabView (AlbumController impl).
 *  - Notifies BrowserController when tabs are opened, closed, or switched.
 *  - Provides incognito tab isolation by routing incognito tabs to a
 *    separate off-the-record Profile (Chromium native).
 *
 * The real Chromium TabModelSelector class:
 *   org.chromium.chrome.browser.tabmodel.TabModelSelector
 */
class PetalTabManager(private val context: Context) {

    private val tag = "PetalTabManager"
    private val tabs = mutableMapOf<String, PetalChromiumTabView>()
    private var activeTabId: String? = null

    // -------------------------------------------------------------------------

    fun onTabCreated(chromiumTab: Any, contentView: android.view.View, incognito: Boolean) {
        val tabId = getTabId(chromiumTab) ?: return
        val tabView = PetalChromiumTabView(chromiumTab, contentView, incognito, tabId)
        tabView.attachGlassBackend()
        tabs[tabId] = tabView
        Log.d(tag, "Tab created: $tabId, incognito=$incognito")
    }

    fun onTabSelected(chromiumTab: Any) {
        val tabId = getTabId(chromiumTab) ?: return
        activeTabId = tabId
        Log.d(tag, "Tab selected: $tabId")
    }

    fun onTabClosed(chromiumTab: Any) {
        val tabId = getTabId(chromiumTab) ?: return
        tabs.remove(tabId)?.destroy()
        if (activeTabId == tabId) activeTabId = null
        Log.d(tag, "Tab closed: $tabId")
    }

    fun getActiveTab(): PetalChromiumTabView? = tabs[activeTabId]

    fun getAllTabs(): List<AlbumController> = tabs.values.toList()

    fun getTabCount(): Int = tabs.size

    private fun getTabId(chromiumTab: Any): String? {
        return try {
            chromiumTab.javaClass.getMethod("getId").invoke(chromiumTab)?.toString()
        } catch (e: Exception) {
            Log.w(tag, "getTabId: $e")
            null
        }
    }
}
