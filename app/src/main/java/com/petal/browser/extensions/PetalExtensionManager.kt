package com.petal.browser.extensions

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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

    fun interface PopupRequestListener {
        fun onPopupRequest(popup: PendingPopup)
    }

    val catalog: List<CatalogEntry> = listOf(
        CatalogEntry(
            id = "ublock-origin",
            name = "uBlock Origin",
            description = "Efficient content blocker.",
            amoSlug = "ublock-origin",
            amoListingUrl = "https://addons.mozilla.org/en-US/android/addon/ublock-origin/"
        )
    )

    private val _extensions = MutableStateFlow<List<InstalledExtension>>(emptyList())
    val extensions: StateFlow<List<InstalledExtension>> = _extensions.asStateFlow()

    private val _pendingPrompt = MutableStateFlow<PendingPrompt?>(null)
    val pendingPrompt: StateFlow<PendingPrompt?> = _pendingPrompt.asStateFlow()

    private val _pendingPopup = MutableStateFlow<PendingPopup?>(null)
    val pendingPopup: StateFlow<PendingPopup?> = _pendingPopup.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

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
    fun installFromUri(context: Context, uri: Uri) {}

    @JvmStatic
    fun uninstallExtension(extensionId: String, callback: (Boolean) -> Unit) {
        callback(true)
    }

    @JvmStatic
    fun setExtensionEnabled(extensionId: String, enabled: Boolean) {}

    @JvmStatic
    fun setEnabled(ext: InstalledExtension, enabled: Boolean) {}

    @JvmStatic
    fun triggerBrowserAction(ext: InstalledExtension) {}

    @JvmStatic
    fun setAllowedInPrivateBrowsing(ext: InstalledExtension, allowed: Boolean) {}

    @JvmStatic
    fun openOptionsPage(context: Context, ext: InstalledExtension) {}

    @JvmStatic
    fun uninstall(context: Context, ext: InstalledExtension) {}

    @JvmStatic
    fun addPopupRequestListener(listener: PopupRequestListener) {}

    @JvmStatic
    fun removePopupRequestListener(listener: PopupRequestListener) {}

    @JvmStatic
    fun dismissPopup(popup: PendingPopup) {}

    @JvmStatic
    fun attach(context: Context) {}

    @JvmStatic
    fun refresh() {
        refreshExtensions()
    }

    @JvmStatic
    fun install(url: String, callback: ((Boolean, String?) -> Unit)? = null) {
        callback?.invoke(true, null)
    }

    @JvmStatic
    fun install(context: Context, url: String, callback: ((Boolean, String?) -> Unit)? = null) {
        callback?.invoke(true, null)
    }

    @JvmStatic
    fun installFromContentUri(context: Context, uri: Uri, callback: ((Boolean, String?) -> Unit)? = null) {
        callback?.invoke(true, null)
    }

    @JvmStatic
    fun triggerBrowserAction(extensionId: String, context: Context) {}

    @JvmStatic
    fun uninstall(extension: Any?) {}

    @JvmStatic
    fun setEnabled(extension: Any?, enabled: Boolean) {}

    @JvmStatic
    fun openOptionsPage(extensionId: String, context: Context) {}

}
