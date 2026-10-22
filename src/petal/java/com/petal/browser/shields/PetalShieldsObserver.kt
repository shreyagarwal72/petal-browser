package com.petal.browser.shields

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * PetalShieldsObserver
 *
 * Bridges Brave's C++ Shields engine to Petal's Compose UI.
 * Replaces the old GeckoView ETP / Total Cookie Protection stat tracking.
 *
 * Data flow:
 *   Brave C++ (BraveShieldsTabObserver JNI) → PetalShieldsObserver → PetalPrivacyShieldSheet
 *
 * The actual Brave Shields JNI class is:
 *   org.chromium.chrome.browser.shields.BraveShieldsHandler  (in brave-core)
 *
 * Patch 0004-petal-shields-observer-jni.patch exposes additional JNI methods
 * needed for per-tab blocked-count tracking.
 */
class PetalShieldsObserver(private val context: Context) {

    private val tag = "PetalShieldsObserver"

    // Compose state — observed by PetalPrivacyShieldSheet
    var isShieldsUp: Boolean by mutableStateOf(true)
        private set
    var blockedAdsCount: Int by mutableIntStateOf(0)
        private set
    var blockedTrackersCount: Int by mutableIntStateOf(0)
        private set
    var blockedFingerprintsCount: Int by mutableIntStateOf(0)
        private set
    var currentHost: String by mutableStateOf("")
        private set

    // --- Shields control (called from PetalPrivacyShieldSheet toggle) ---------

    fun setShieldsEnabled(tabId: String, enabled: Boolean) {
        try {
            // BraveShieldsNativeHelper.setShieldsEnabled(tabId, enabled) — JNI
            val clazz = Class.forName(
                "org.chromium.chrome.browser.shields.BraveShieldsNativeHelper")
            clazz.getMethod("setShieldsEnabled", String::class.java, Boolean::class.java)
                .invoke(null, tabId, enabled)
            isShieldsUp = enabled
        } catch (e: Exception) {
            Log.w(tag, "setShieldsEnabled: $e")
            isShieldsUp = enabled  // optimistic update
        }
    }

    fun setAdBlockEnabled(tabId: String, enabled: Boolean) {
        try {
            val clazz = Class.forName(
                "org.chromium.chrome.browser.shields.BraveShieldsNativeHelper")
            clazz.getMethod("setAdBlockEnabled", String::class.java, Boolean::class.java)
                .invoke(null, tabId, enabled)
        } catch (e: Exception) {
            Log.w(tag, "setAdBlockEnabled: $e")
        }
    }

    fun setFingerprintProtectionEnabled(tabId: String, enabled: Boolean) {
        try {
            val clazz = Class.forName(
                "org.chromium.chrome.browser.shields.BraveShieldsNativeHelper")
            clazz.getMethod("setFingerprintingEnabled", String::class.java, Boolean::class.java)
                .invoke(null, tabId, enabled)
        } catch (e: Exception) {
            Log.w(tag, "setFingerprintProtectionEnabled: $e")
        }
    }

    // --- State refresh (called by PetalBraveActivity on tab selection change) --

    fun refreshForTab(tabId: String, host: String) {
        currentHost = host
        try {
            val clazz = Class.forName(
                "org.chromium.chrome.browser.shields.BraveShieldsNativeHelper")
            isShieldsUp = clazz.getMethod("isShieldsEnabled", String::class.java)
                .invoke(null, tabId) as? Boolean ?: true
            blockedAdsCount = clazz.getMethod("getAdsBlocked", String::class.java)
                .invoke(null, tabId) as? Int ?: 0
            blockedTrackersCount = clazz.getMethod("getTrackersBlocked", String::class.java)
                .invoke(null, tabId) as? Int ?: 0
            blockedFingerprintsCount = clazz.getMethod("getFingerprintsBlocked", String::class.java)
                .invoke(null, tabId) as? Int ?: 0
        } catch (e: Exception) {
            Log.w(tag, "refreshForTab: $e")
        }
    }
}
