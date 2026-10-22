package com.petal.browser.unit;

import android.content.Context;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/**
 * CacheManager: Chromium website cache & storage manager.
 * ─────────────────────────────────────────────────────────────────────────────
 * Delegates to [PetalCacheManager] which clears data via Chromium's native
 * [BrowsingDataBridge] and Android WebStorage. Zero Mozilla/Gecko dependencies.
 */
public class CacheManager {

    public static void clearAllCache(@NonNull Context context, @Nullable WebView activeWebView) {
        PetalCacheManager.clearAllCache(context, activeWebView);
    }

    public static void clearAllWebsiteData(@NonNull Context context) {
        PetalCacheManager.clearAllWebsiteData(context);
    }

    public static void clearSiteCache(@NonNull Context context, @NonNull String host) {
        PetalCacheManager.clearSiteCache(context, host);
    }

    public static void clearSiteData(@NonNull Context context, @NonNull String host) {
        PetalCacheManager.clearSiteData(context, host, true, false);
    }

    public static void clearSiteDataAndPermissions(@NonNull Context context, @NonNull String host) {
        PetalCacheManager.clearSiteData(context, host, true, true);
    }

    public static boolean deleteDir(File dir) {
        return PetalCacheManager.deleteDir(dir);
    }

    public static void deleteDirContents(File dir) {
        PetalCacheManager.deleteDirContents(dir);
    }

    public static void deleteDirContentsExcluding(File dir, String... excludedNames) {
        PetalCacheManager.deleteDirContentsExcluding(dir, excludedNames);
    }
}
