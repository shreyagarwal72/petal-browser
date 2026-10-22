package com.petal.browser.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

/**
 * PetalTabThumbnailCache
 * ─────────────────────────────────────────────────────────────────────────────
 * High-performance, two-level (Memory LRU + Disk Cache) thumbnail cache for
 * tab switcher previews.
 *
 * Provides immediate instant preview bitmaps for tab cards in [PetalTabSwitcherSheet]
 * across session restores without requiring Chromium renderer texture recapture.
 */
object PetalTabThumbnailCache {

    private const val TAG = "PetalTabThumbnailCache"
    private const val MEM_CACHE_SIZE = 32 * 1024 * 1024 // 32 MB memory cache

    private val memoryCache = object : LruCache<String, Bitmap>(MEM_CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    private var diskCacheDir: File? = null

    @JvmStatic
    fun init(context: Context) {
        if (diskCacheDir == null) {
            diskCacheDir = File(context.cacheDir, "tab_thumbnails").apply {
                if (!exists()) mkdirs()
            }
            Log.i(TAG, "Initialized tab thumbnail cache at ${diskCacheDir?.absolutePath}")
        }
    }

    @JvmStatic
    fun getThumbnail(tabId: String): Bitmap? {
        // 1. Check in-memory LRU cache
        memoryCache.get(tabId)?.let { return it }

        // 2. Fallback to disk cache
        val diskFile = File(diskCacheDir ?: return null, "$tabId.png")
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(tabId, bitmap)
                    return bitmap
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to decode disk thumbnail for tab $tabId: ${e.message}")
            }
        }
        return null
    }

    @JvmStatic
    fun putThumbnail(tabId: String, bitmap: Bitmap) {
        memoryCache.put(tabId, bitmap)

        // Save to disk asynchronously
        val dir = diskCacheDir ?: return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launchSilently {
            try {
                val diskFile = File(dir, "$tabId.png")
                FileOutputStream(diskFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to write disk thumbnail for tab $tabId: ${e.message}")
            }
        }
    }

    @JvmStatic
    fun removeThumbnail(tabId: String) {
        memoryCache.remove(tabId)
        val dir = diskCacheDir ?: return
        val diskFile = File(dir, "$tabId.png")
        if (diskFile.exists()) {
            diskFile.delete()
        }
    }

    @JvmStatic
    fun clearAll() {
        memoryCache.evictAll()
        diskCacheDir?.listFiles()?.forEach { it.delete() }
    }

    private fun kotlinx.coroutines.CoroutineScope.launchSilently(block: suspend () -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try { block() } catch (ignored: Throwable) {}
        }
    }
}
