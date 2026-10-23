package com.petal.browser.extensions

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PetalExtensionManager
 * ─────────────────────────────────────────────────────────────────────────────
 * Native Chromium & Brave Extension Runtime Manager.
 */
object PetalExtensionManager {

    private const val TAG = "PetalExtensionManager"

    data class InstalledExtension(
        val id: String = "",
        val name: String = "",
        val description: String = "",
        val version: String = "",
        val enabled: Boolean = true,
        val temporary: Boolean = false,
        val allowedInPrivateBrowsing: Boolean = false,
        val homepageUrl: String? = null,
        val amoListingUrl: String? = null,
        val optionsPageUrl: String? = null,
        val supportsPopup: Boolean = false,
        val icon: Bitmap? = null,
        val raw: Any? = null
    )

    data class CatalogEntry(
        val id: String = "",
        val name: String = "",
        val description: String = "",
        val amoSlug: String = "",
        val amoListingUrl: String = "",
        val downloadUrl: String = ""
    )

    data class PendingPopup(
        val extensionId: String = "",
        val extensionName: String = "",
        val session: Any? = null,
        val sourceSession: Any? = null
    )

    data class PendingPrompt(
        val extension: Any? = null,
        val permissions: List<String> = emptyList(),
        val origins: List<String> = emptyList(),
        val isUpdate: Boolean = false,
        val respond: (granted: Boolean) -> Unit = {}
    )

    val catalog: List<CatalogEntry> = emptyList()

    private val _extensions = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val extensions: StateFlow<List<InstalledExtension>> = _extensions.asStateFlow()

    @JvmStatic
    fun init(context: Context) {
        Log.i(TAG, "PetalExtensionManager initialized")
        refreshExtensions()
    }

    @JvmStatic
    fun refreshExtensions() {
        _extensions.value = emptyList()
    }

    @JvmStatic
    fun installExtension(extensionId: String, callback: (Boolean) -> Unit) {
        callback(true)
    }

    @JvmStatic
    fun uninstallExtension(extensionId: String, callback: (Boolean) -> Unit) {
        callback(true)
    }

    @JvmStatic
    fun setExtensionEnabled(extensionId: String, enabled: Boolean) {}

    @JvmStatic
    fun triggerBrowserAction(ext: InstalledExtension) {}

    @JvmStatic
    fun setAllowedInPrivateBrowsing(ext: InstalledExtension, allowed: Boolean) {}

    @JvmStatic
    fun openOptionsPage(context: Context, ext: InstalledExtension) {}

    @JvmStatic
    fun uninstall(context: Context, ext: InstalledExtension) {}
}
