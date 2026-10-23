package com.petal.browser.browser

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.petal.browser.bridge.NativePrefBridge
import com.petal.browser.browser.adblock.PetalBraveShieldEngine

/**
 * PetalPrivacyController
 * ─────────────────────────────────────────────────────────────────────────────
 * Manages privacy protection, ad blocking, and anti-fingerprinting policies
 * for Petal Browser directly via Chromium & Brave Shields ([PetalShieldEngine]).
 *
 * Replaces old GeckoView ContentBlocking / ETP with zero Mozilla dependencies.
 */
object PetalPrivacyController {

    private const val TAG = "PetalPrivacyController"

    enum class EtpMode {
        STANDARD,
        STRICT,
        CUSTOM
    }

    /**
     * Applies the designated tracking protection level.
     */
    fun applyEtpMode(context: Context, mode: EtpMode) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val strictMode = mode == EtpMode.STRICT
        sp.edit().putString("petal_etp_mode", mode.name).apply()

        try {
            PetalBraveShieldEngine.initialize(context)
            NativePrefBridge.setAdBlockEnabled(true)
            NativePrefBridge.setFingerprintProtection(strictMode)
            Log.i(TAG, "Applied tracking protection mode: $mode (strict=$strictMode)")
        } catch (e: Throwable) {
            Log.w(TAG, "Error applying tracking protection mode: ${e.message}")
        }
    }

    fun isStrict(context: Context): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val mode = sp.getString("petal_etp_mode", EtpMode.STANDARD.name) ?: EtpMode.STANDARD.name
        return mode == EtpMode.STRICT.name
    }
}
