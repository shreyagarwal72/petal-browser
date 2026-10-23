package com.petal.browser.browser

import androidx.fragment.app.FragmentActivity

/**
 * Browser features stub - Chromium engine handles permissions, fullscreen,
 * Picture-in-Picture, and prompts natively.
 */
class PetalBrowserFeatures(private val activity: FragmentActivity) {
    fun start() {}
    fun stop() {}
    fun onPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {}
    fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?): Boolean = false
    fun onPictureInPictureModeChanged(enabled: Boolean) {}
}
