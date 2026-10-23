package com.petal.browser.bridge

import android.util.Log

/**
 * NativePrefBridge
 * ─────────────────────────────────────────────────────────────────────────────
 * Thin reflection-based adapter over Chromium/Brave JNI preference bridges.
 * All calls are fire-and-forget with graceful fallback — the local SharedPrefs
 * optimistic update in SettingsRepositoryImpl ensures UI reacts instantly.
 *
 * Native APIs called:
 *   - org.chromium.chrome.browser.brave_shields.BraveShieldsNativeHelper
 *   - org.chromium.chrome.browser.settings.website.ContentSettingsManagerJni
 *   - org.chromium.chrome.browser.BravePrefServiceBridge
 *   - org.chromium.chrome.browser.profiles.ProfileManager
 */
object NativePrefBridge {

    private const val TAG = "NativePrefBridge"

    // Lazy-resolved Chromium class references
    private val bravePrefBridge by lazy {
        runCatching { Class.forName("org.chromium.chrome.browser.BravePrefServiceBridge") }.getOrNull()
    }
    private val braveShieldsHelper by lazy {
        runCatching { Class.forName("org.chromium.chrome.browser.brave_shields.BraveShieldsNativeHelper") }.getOrNull()
    }
    private val profileManager by lazy {
        runCatching { Class.forName("org.chromium.chrome.browser.profiles.ProfileManager") }.getOrNull()
    }

    private fun getLastUsedProfile(): Any? = runCatching {
        profileManager?.getMethod("getLastUsedRegularProfile")?.invoke(null)
    }.getOrNull()

    // ── Ad Blocking ───────────────────────────────────────────────────────────

    fun setAdBlockEnabled(enabled: Boolean) {
        runCatching {
            val helper = braveShieldsHelper ?: return
            val profile = getLastUsedProfile() ?: return
            val mode = if (enabled) 1 else 0
            helper.getMethod("setAdsBlockingMode", Any::class.java, Int::class.javaPrimitiveType)
                .invoke(null, profile, mode)
        }.onFailure { Log.w(TAG, "setAdBlockEnabled($enabled): ${it.message}") }
    }

    // ── Fingerprint Protection ────────────────────────────────────────────────

    fun setFingerprintProtection(enabled: Boolean) {
        runCatching {
            val helper = braveShieldsHelper ?: return
            val profile = getLastUsedProfile() ?: return
            val mode = if (enabled) 1 else 0
            helper.getMethod("setFingerprintsBlockingMode", Any::class.java, Int::class.javaPrimitiveType)
                .invoke(null, profile, mode)
        }.onFailure { Log.w(TAG, "setFingerprintProtection($enabled): ${it.message}") }
    }

    // ── HTTPS Only Mode ───────────────────────────────────────────────────────

    fun setHttpsOnlyModeEnabled(enabled: Boolean) {
        runCatching {
            val bridge = bravePrefBridge ?: return
            val instance = bridge.getMethod("getInstance").invoke(null)
            instance?.javaClass?.getMethod("setHttpsOnlyModeEnabled", Boolean::class.javaPrimitiveType)
                ?.invoke(instance, enabled)
        }.onFailure { Log.w(TAG, "setHttpsOnlyModeEnabled($enabled): ${it.message}") }
    }

    // ── JavaScript ────────────────────────────────────────────────────────────

    fun setJavaScriptEnabled(enabled: Boolean) {
        runCatching {
            val contentSettingsMgr = runCatching {
                Class.forName("org.chromium.chrome.browser.settings.website.ContentSettingsManagerJni")
            }.getOrNull()
            if (contentSettingsMgr != null) {
                val get = contentSettingsMgr.getMethod("get")
                val instance = get.invoke(null)
                // setDefaultContentSetting(type=26, setting=1/2) — 1=allow, 2=block
                instance?.javaClass?.getMethod(
                    "setDefaultContentSetting",
                    Long::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )?.invoke(instance, 0L, 26, if (enabled) 1 else 2)
            }
        }.onFailure { Log.w(TAG, "setJavaScriptEnabled($enabled): ${it.message}") }
    }

    // ── Block Popups ──────────────────────────────────────────────────────────

    fun setBlockPopups(enabled: Boolean) {
        runCatching {
            val contentSettingsMgr = runCatching {
                Class.forName("org.chromium.chrome.browser.settings.website.ContentSettingsManagerJni")
            }.getOrNull() ?: return
            val instance = contentSettingsMgr.getMethod("get").invoke(null)
            // ContentSettingsType.POPUPS = 25
            instance?.javaClass?.getMethod(
                "setDefaultContentSetting",
                Long::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )?.invoke(instance, 0L, 25, if (enabled) 2 else 1) // block=2, allow=1
        }.onFailure { Log.w(TAG, "setBlockPopups($enabled): ${it.message}") }
    }

    // ── Do Not Track / GPC ───────────────────────────────────────────────────

    fun setDoNotTrack(enabled: Boolean) {
        runCatching {
            val bridge = bravePrefBridge ?: return
            val instance = bridge.getMethod("getInstance").invoke(null)
            instance?.javaClass?.getMethod("setDoNotTrackEnabled", Boolean::class.javaPrimitiveType)
                ?.invoke(instance, enabled)
        }.onFailure { Log.w(TAG, "setDoNotTrack($enabled): ${it.message}") }
    }

    // ── Desktop Site ─────────────────────────────────────────────────────────

    fun setRequestDesktopSite(enabled: Boolean) {
        runCatching {
            val bridge = bravePrefBridge ?: return
            val instance = bridge.getMethod("getInstance").invoke(null)
            instance?.javaClass?.getMethod("setDesktopModeEnabled", Boolean::class.javaPrimitiveType)
                ?.invoke(instance, enabled)
        }.onFailure { Log.w(TAG, "setRequestDesktopSite($enabled): ${it.message}") }
    }

    // ── WebRTC ────────────────────────────────────────────────────────────────

    fun setWebRtcProtection(enabled: Boolean) {
        runCatching {
            val bridge = bravePrefBridge ?: return
            val instance = bridge.getMethod("getInstance").invoke(null)
            val policy = if (enabled) 1 else 0
            instance?.javaClass?.getMethod("setWebRTCPolicy", Int::class.javaPrimitiveType)
                ?.invoke(instance, policy)
        }.onFailure { Log.w(TAG, "setWebRtcProtection($enabled): ${it.message}") }
    }

    // ── Third-Party Cookies ───────────────────────────────────────────────────

    fun setBlockThirdPartyCookies(enabled: Boolean) {
        runCatching {
            val bridge = bravePrefBridge ?: return
            val instance = bridge.getMethod("getInstance").invoke(null)
            instance?.javaClass?.getMethod("setBlockThirdPartyCookies", Boolean::class.javaPrimitiveType)
                ?.invoke(instance, enabled)
        }.onFailure { Log.w(TAG, "setBlockThirdPartyCookies($enabled): ${it.message}") }
    }

    @JvmStatic
    fun syncPreferences(sp: android.content.SharedPreferences) {
        setAdBlockEnabled(sp.getBoolean("sp_ad_block", true))
        setHttpsOnlyModeEnabled(sp.getBoolean("sp_https_only", false))
        setJavaScriptEnabled(sp.getBoolean("sp_javascript", true))
        setBlockPopups(sp.getBoolean("sp_block_popups", true))
        setDoNotTrack(sp.getBoolean("sp_do_not_track", true))
        setRequestDesktopSite(sp.getBoolean("sp_desktop", false))
        setBlockThirdPartyCookies(sp.getBoolean("sp_block_third_party_cookies", true))
    }
}
