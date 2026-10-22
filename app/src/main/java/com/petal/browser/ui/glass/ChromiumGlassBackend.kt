package com.petal.browser.ui.glass

import android.graphics.SurfaceTexture
import android.view.TextureView
import android.view.View
import android.view.ViewGroup

/**
 * ChromiumGlassBackend
 *
 * Forces Chromium's ContentViewRenderView to use a TextureView backend instead of
 * SurfaceView, so the rendered webpage pixel stream passes through the Android view
 * hierarchy. This is the prerequisite for Petal's AGSL Liquid Glass shaders — they
 * sample those live pixels to perform blur, tint, lens-refraction, and rim-light effects
 * on any glass panel (PetalSheet, ExpressiveFloatingDockBar, PetalPopupMenu, etc.)
 * that floats above the web page.
 *
 * Technique mirrors the old GeckoGlassBackend:
 * 1. Walk the view tree to find the TextureView Chromium inserts.
 * 2. Wrap its SurfaceTextureListener in FrameQuietListener, which forwards everything
 *    except onSurfaceTextureUpdated, preventing a runaway redraw loop on still pages.
 *
 * Platform: TextureView is available on all supported API levels (26+).
 * The Chromium-side patch (0002-force-texture-view-for-glass-sampling.patch) flips
 * ContentViewRenderView from SURFACE_VIEW to TEXTURE_VIEW before this listener attaches.
 */
object ChromiumGlassBackend {

    /**
     * Call this once after Chromium's ContentView is mounted.
     * Safe to call multiple times — no-op if already wrapped.
     */
    @JvmStatic
    fun attachTextureListener(contentRoot: View) {
        val texture = findTextureView(contentRoot) ?: return
        val existingListener = texture.surfaceTextureListener ?: return
        if (existingListener is FrameQuietListener) return          // already patched
        texture.surfaceTextureListener = FrameQuietListener(existingListener)
    }

    private fun findTextureView(root: View): TextureView? {
        if (root is TextureView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findTextureView(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    /**
     * Forwards every SurfaceTexture event except onSurfaceTextureUpdated.
     * Chromium composites through its own pipeline; echoing back each frame
     * update would trigger a recomposite loop at full vsync rate on still pages.
     */
    private class FrameQuietListener(
        private val delegate: TextureView.SurfaceTextureListener
    ) : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) =
            delegate.onSurfaceTextureAvailable(surface, width, height)

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) =
            delegate.onSurfaceTextureSizeChanged(surface, width, height)

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
            delegate.onSurfaceTextureDestroyed(surface)

        // Drop — prevents runaway recomposite loop on still pages.
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
    }
}
