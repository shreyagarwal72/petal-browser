package com.petal.browser.engine

import android.content.Context
import android.util.Log
import android.view.View
import com.petal.browser.browser.AlbumController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PetalTabGroup
 * Represents a group of related tabs (like Chromium tab groups).
 */
data class PetalTabGroup(
    val id: String,
    val title: String,
    val colorHex: Long,
    val tabIds: List<String> = emptyList(),
    val isCollapsed: Boolean = false
)

/**
 * PetalTabManager
 * ─────────────────────────────────────────────────────────────────────────────
 * Coordinates native Chromium TabModelSelector, TabGroupModelFilter, Tab Lifecycle,
 * and Incognito Tab isolation.
 *
 * Exposes tab list, tab groups, and incognito mode status as StateFlow for Petal UI.
 */
class PetalTabManager(private val context: Context) {

    private val tag = "PetalTabManager"
    private val tabs = mutableMapOf<String, PetalChromiumTabView>()
    private var activeTabId: String? = null

    // State flows for Petal UI
    private val _tabList = MutableStateFlow<List<PetalChromiumTabView>>(emptyList())
    val tabList: StateFlow<List<PetalChromiumTabView>> = _tabList.asStateFlow()

    private val _tabGroups = MutableStateFlow<Map<String, PetalTabGroup>>(emptyMap())
    val tabGroups: StateFlow<Map<String, PetalTabGroup>> = _tabGroups.asStateFlow()

    private val _incognitoTabCount = MutableStateFlow(0)
    val incognitoTabCount: StateFlow<Int> = _incognitoTabCount.asStateFlow()

    init {
        PetalTabThumbnailCache.init(context)
    }

    // -------------------------------------------------------------------------
    // Tab Lifecycle
    // -------------------------------------------------------------------------

    fun onTabCreated(chromiumTab: Any, contentView: View, incognito: Boolean): PetalChromiumTabView? {
        val tabId = getTabId(chromiumTab) ?: return null
        val tabView = PetalChromiumTabView(chromiumTab, contentView, incognito, tabId)
        tabView.attachGlassBackend()
        tabs[tabId] = tabView
        refreshState()
        Log.d(tag, "Tab created: $tabId, incognito=$incognito")
        return tabView
    }

    fun onTabSelected(chromiumTab: Any) {
        val tabId = getTabId(chromiumTab) ?: return
        val prevActive = activeTabId?.let { tabs[it] }
        prevActive?.deactivate()

        activeTabId = tabId
        val newActive = tabs[tabId]
        newActive?.activate()
        refreshState()
        Log.d(tag, "Tab selected: $tabId")
    }

    fun onTabClosed(chromiumTab: Any) {
        val tabId = getTabId(chromiumTab) ?: return
        tabs.remove(tabId)?.destroy()
        if (activeTabId == tabId) activeTabId = null

        // Remove from groups
        val updatedGroups = _tabGroups.value.mapValues { (_, group) ->
            group.copy(tabIds = group.tabIds.filter { it != tabId })
        }.filterValues { it.tabIds.isNotEmpty() }
        _tabGroups.value = updatedGroups

        refreshState()
        Log.d(tag, "Tab closed: $tabId")
    }

    fun getActiveTab(): PetalChromiumTabView? = activeTabId?.let { tabs[it] }

    fun getTab(tabId: String): PetalChromiumTabView? = tabs[tabId]

    // -------------------------------------------------------------------------
    // Tab Groups
    // -------------------------------------------------------------------------

    fun createTabGroup(title: String, colorHex: Long, tabIds: List<String>): String {
        val groupId = java.util.UUID.randomUUID().toString()
        val group = PetalTabGroup(
            id = groupId,
            title = title.ifBlank { "Group" },
            colorHex = colorHex,
            tabIds = tabIds
        )
        val map = _tabGroups.value.toMutableMap()
        map[groupId] = group
        _tabGroups.value = map
        Log.d(tag, "Created tab group $groupId with ${tabIds.size} tabs")
        return groupId
    }

    fun addTabToGroup(groupId: String, tabId: String) {
        val group = _tabGroups.value[groupId] ?: return
        if (tabId !in group.tabIds) {
            val updated = group.copy(tabIds = group.tabIds + tabId)
            _tabGroups.value = _tabGroups.value + (groupId to updated)
        }
    }

    fun removeTabFromGroup(groupId: String, tabId: String) {
        val group = _tabGroups.value[groupId] ?: return
        val remaining = group.tabIds.filter { it != tabId }
        val map = _tabGroups.value.toMutableMap()
        if (remaining.isEmpty()) {
            map.remove(groupId)
        } else {
            map[groupId] = group.copy(tabIds = remaining)
        }
        _tabGroups.value = map
    }

    fun toggleGroupCollapse(groupId: String) {
        val group = _tabGroups.value[groupId] ?: return
        val updated = group.copy(isCollapsed = !group.isCollapsed)
        _tabGroups.value = _tabGroups.value + (groupId to updated)
    }

    // -------------------------------------------------------------------------
    // Incognito Tab Isolation
    // -------------------------------------------------------------------------

    fun closeAllIncognitoTabs() {
        val incognitoTabs = tabs.values.filter { it.isIncognito() }
        incognitoTabs.forEach { tab ->
            tabs.remove(tab.tabId)?.destroy()
        }
        if (activeTabId != null && tabs[activeTabId]?.isIncognito() == true) {
            activeTabId = tabs.values.firstOrNull { !it.isIncognito() }?.tabId
        }
        refreshState()
        Log.i(tag, "Closed all incognito tabs and released memory")
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private fun refreshState() {
        _tabList.value = tabs.values.toList()
        _incognitoTabCount.value = tabs.values.count { it.isIncognito() }
    }

    private fun getTabId(chromiumTab: Any): String? {
        return try {
            val method = chromiumTab.javaClass.getMethod("getId")
            method.invoke(chromiumTab)?.toString()
        } catch (e: Throwable) {
            chromiumTab.hashCode().toString()
        }
    }
}
