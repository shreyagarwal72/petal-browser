package com.petal.browser.engine

import android.view.View
import com.petal.browser.browser.AlbumController
import com.petal.browser.ui.glass.ChromiumGlassBackend

/**
 * PetalChromiumTabView
 *
 * Implements the AlbumController contract (Petal's tab surface abstraction) backed
 * by a Chromium Tab / WebContents instead of GeckoView.
 *
 * This replaces PetalGeckoView.kt entirely. All Petal UI code that previously cast
 * AlbumController to PetalGeckoView must be updated to use this class or the abstract
 * AlbumController interface methods.
 *
 * Integration path:
 *   BraveActivity creates a Chromium Tab via TabCreator.
 *   PetalBraveActivity wraps each Tab in a PetalChromiumTabView.
 *   PetalBraveActivity registers each view with BrowserController.showAlbum().
 *
 * Lifecycle:
 *   activate()   → Tab.setHidden(false)  — resumes renderer work
 *   deactivate() → Tab.setHidden(true)   — suspends renderer work
 *   destroy()    → Tab.destroy()         — releases native WebContents
 *
 * Liquid Glass:
 *   After ContentView is mounted, ChromiumGlassBackend.attachTextureListener() is called
 *   so the AGSL shaders can sample webpage pixels through the TextureView.
 *
 * NOTE: Chromium Tab / TabModelSelector / ContentView are accessed via JNI through
 *       Brave's existing Java bridge classes. Real class names depend on the exact
 *       Chromium version synced by brave-browser's package.json.
 */
class PetalChromiumTabView(
    private val chromiumTab: Any,         // org.chromium.chrome.browser.tab.Tab
    private val contentView: View,        // org.chromium.content.browser.ContentView
    private val incognitoMode: Boolean,
    private val tabId: String
) : View(contentView.context), AlbumController {

    private val tag = "PetalChromiumTabView"

    // --- AlbumController contract -------------------------------------------

    override fun getAlbumView(): View = contentView

    override fun activate() {
        try {
            // Tab.setHidden(false) — resumes Chromium renderer work for this tab
            chromiumTab.javaClass
                .getMethod("setHidden", Boolean::class.java)
                .invoke(chromiumTab, false)
        } catch (e: Exception) {
            android.util.Log.w(tag, "activate: $e")
        }
    }

    override fun deactivate() {
        try {
            // Tab.setHidden(true) — suspends renderer, saves memory
            chromiumTab.javaClass
                .getMethod("setHidden", Boolean::class.java)
                .invoke(chromiumTab, true)
        } catch (e: Exception) {
            android.util.Log.w(tag, "deactivate: $e")
        }
    }

    override fun getTitle(): String {
        return try {
            chromiumTab.javaClass.getMethod("getTitle").invoke(chromiumTab) as? String ?: ""
        } catch (e: Exception) { "" }
    }

    override fun getUrl(): String {
        return try {
            val gurl = chromiumTab.javaClass.getMethod("getUrl").invoke(chromiumTab)
            gurl?.javaClass?.getMethod("getSpec")?.invoke(gurl) as? String ?: ""
        } catch (e: Exception) { "" }
    }

    override fun isIncognito(): Boolean = incognitoMode

    override fun destroy() {
        try {
            chromiumTab.javaClass.getMethod("destroy").invoke(chromiumTab)
        } catch (e: Exception) {
            android.util.Log.w(tag, "destroy: $e")
        }
    }

    // --- Liquid Glass setup -------------------------------------------------

    /**
     * Call once after contentView is added to the window.
     * Attaches ChromiumGlassBackend's FrameQuietListener so AGSL shaders
     * can sample live webpage pixels through the TextureView.
     */
    fun attachGlassBackend() {
        ChromiumGlassBackend.attachTextureListener(contentView)
    }

    fun getTabId(): String = tabId
}
