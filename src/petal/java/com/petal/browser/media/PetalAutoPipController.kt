package com.petal.browser.media

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Rational

/**
 * PetalAutoPipController
 * ─────────────────────────────────────────────────────────────────────────────
 * Android 12+ Native Auto-Picture-in-Picture (Auto-PiP) Controller for Petal Browser.
 *
 * Hooks into Chromium's native HTML5 video and MediaSession state.
 * When an HTML5 video is playing, this controller automatically registers
 * `setAutoEnterEnabled(true)` so the system seamlessly morphs the web video
 * into a floating window whenever the user swipes up or presses Home.
 */
object PetalAutoPipController {

    private const val TAG = "PetalAutoPip"

    private var isPipSupported: Boolean = false
    private var isVideoPlaying: Boolean = false
    private var currentVideoAspectRatio: Rational? = null

    fun initialize(context: Context) {
        isPipSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        Log.d(TAG, "Petal Auto-PiP initialized (Supported: $isPipSupported)")
    }

    /**
     * Called by Chromium's WebContents / MediaSession observer when playback status changes.
     */
    fun onPlaybackStateChanged(activity: Activity?, isPlaying: Boolean, width: Int = 16, height: Int = 9) {
        if (!isPipSupported || activity == null) return
        isVideoPlaying = isPlaying

        val aspect = if (width > 0 && height > 0) {
            val clampedWidth = width.coerceIn(1, 10000)
            val clampedHeight = height.coerceIn(1, 10000)
            Rational(clampedWidth, clampedHeight)
        } else {
            Rational(16, 9)
        }
        currentVideoAspectRatio = aspect

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val paramsBuilder = PictureInPictureParams.Builder()
                    .setAspectRatio(aspect)
                    .setAutoEnterEnabled(isPlaying)

                activity.setPictureInPictureParams(paramsBuilder.build())
                Log.d(TAG, "Updated AutoEnterEnabled: $isPlaying with aspect ratio: $aspect")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to set auto PiP parameters: ${t.message}")
            }
        }
    }

    /**
     * Manually triggers Picture-in-Picture mode on demand.
     */
    fun enterPip(activity: Activity): Boolean {
        if (!isPipSupported) return false
        return try {
            val aspect = currentVideoAspectRatio ?: Rational(16, 9)
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspect)
                .build()
            activity.enterPictureInPictureMode(params)
        } catch (t: Throwable) {
            Log.e(TAG, "Error entering manual PiP: ${t.message}")
            false
        }
    }
}
