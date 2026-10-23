package com.petal.browser.browser;

import android.view.View;
import android.webkit.ValueCallback;

/**
 * A browser tab controller wrapping the active engine view.
 */
public interface AlbumController {
    View getAlbumView();
    void activate();
    void deactivate();
    String getTitle();
    String getUrl();
    default void destroy() {}
    default boolean isIncognito() { return false; }

    default boolean canGoBack() { return false; }
    default boolean canGoForward() { return false; }
    default void goBack() {}
    default void goForward() {}
    default void reload() {}
    default void loadUrl(String url) {}
    default void evaluateJavascript(String script, ValueCallback<String> callback) {}
}
