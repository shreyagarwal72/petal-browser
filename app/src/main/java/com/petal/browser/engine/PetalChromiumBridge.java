package com.petal.browser.engine;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * PetalChromiumBridge
 *
 * Bridge interface connecting Petal Browser's Material 3 Expressive UI to
 * Brave's underlying Chromium engine (WebContents, TabModelSelector, Extensions,
 * Flags, and Shields).
 */
public interface PetalChromiumBridge {

    /**
     * Initializes Chromium engine command line, flags, and native libraries.
     */
    void initializeEngine(@NonNull Context context);

    /**
     * Returns the active Chromium web rendering view.
     */
    @Nullable
    View getActiveContentView();

    /**
     * Navigates the current tab to the target URL.
     */
    void loadUrl(@NonNull String url);

    /**
     * Extension management interface: opens extension popup or manager.
     */
    void openExtensionPopup(@NonNull String extensionId);

    /**
     * Direct bridge to Chromium flags (chrome://flags).
     */
    void openFlagsManager();

    /**
     * Connects Petal's privacy shield UI to Brave Shields C++ filter.
     */
    void setShieldsEnabled(boolean enabled);
}
