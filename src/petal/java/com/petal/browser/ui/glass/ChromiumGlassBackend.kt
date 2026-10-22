package com.petal.browser.ui.glass

import android.graphics.SurfaceTexture
import android.view.TextureView
import android.view.View
import android.view.ViewGroup

/**
 * ChromiumGlassBackend
 *
 * Configures Chromium's ContentViewRenderView / WebContents to render onto an Android
 * TextureView instead of a SurfaceView.
 *
 * This allows the live webpage pixels to pass through the Android view hierarchy so that
 * Petal's Liquid Glass shaders (RenderEffect / AGSL) can sample, frost, and refract the
 * webpage content in real time underneath floating bars, sheets, and popups.
 */
object ChromiumGlassBackend {

    @JvmStatic
    fun attachTextureListener(rootView: View) {
        val textureView = findTextureView(rootView) ?: return
        val currentListener = textureView.surfaceTextureListener ?: return
        if (currentListener is FrameQuietListener) return
        textureView.surfaceTextureListener = FrameQuietListener(currentListener)
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

    private class FrameQuietListener(private val delegate: TextureView.SurfaceTextureListener) :
        TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) =
            delegate.onSurfaceTextureAvailable(surface, width, height)

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) =
            delegate.onSurfaceTextureSizeChanged(surface, width, height)

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
            delegate.onSurfaceTextureDestroyed(surface)

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Drop duplicate self-referential redraw loops on still frames
        }
    }
}
