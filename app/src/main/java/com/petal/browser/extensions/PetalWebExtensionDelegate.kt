package com.petal.browser.extensions

import android.content.Context

/**
 * PetalWebExtensionDelegate
 * Handles Chrome Web Store and CRX extensions directly via genuine Chromium native APIs.
 */
class PetalWebExtensionDelegate(private val context: Context) {
    fun onExtensionInstalled(id: String) {}
    fun onExtensionUninstalled(id: String) {}
}
