package com.petal.browser.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.ValueCallback;
import com.petal.browser.browser.AlbumController;
import com.petal.browser.browser.PetalTabViewController;
import com.petal.browser.media.PetalMediaBridge;

/**
 * Backward-compatibility wrapper forwarding legacy GeckoView calls
 * directly to Petal's Chromium / Compose architecture.
 */
public class PetalGeckoView extends PetalTabViewController {

    public interface OnScrollChangeListener {
        default void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {}
        default void onScrollDown() {}
        default void onScrollUp() {}
    }

    private static com.petal.browser.browser.BrowserController globalBrowserController = null;
    public static com.petal.browser.browser.BrowserController getBrowserController() {
        return globalBrowserController;
    }
    public static void setGlobalBrowserController(com.petal.browser.browser.BrowserController controller) {
        globalBrowserController = controller;
    }

    public void setBrowserController(Object controller) {
        if (controller instanceof com.petal.browser.browser.BrowserController) {
            globalBrowserController = (com.petal.browser.browser.BrowserController) controller;
        }
    }
    public void resumeTimers() {}
    public void pauseTimers() {}
    public boolean canClearSelection() { return false; }
    public void clearWebSelection() {}
    public boolean isForeground() { return true; }
    public void initPreferences(String url) {}
    public void clearMatches() {}
    public void findAllAsync(String query) {}
    public void findNext(boolean forward) {}
    public void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
        if (resultCallback != null) {
            resultCallback.onReceiveValue("");
        }
    }

    public PetalGeckoView(Context context) {
        super(context);
    }

    public PetalGeckoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PetalGeckoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static String getProfile(Context context) {
        return "profileStandard";
    }

    public void setOnScrollChangeListener(OnScrollChangeListener l) {}
    public void resetGestureExclusionRects() {}
    public void onResume() {}
    public void onPause() {}
    public boolean hasBackHistory() { return canGoBack(); }
    public void resetToHome() { loadUrl("about:blank"); }
    public String getAlbumUrl() { return getUrl(); }
    public String getAlbumTitle() { return getTitle(); }
    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        loadUrl(baseUrl != null ? baseUrl : "about:blank");
    }

    public void setProfileChanged() {}
    public void captureForSwitcher(java.util.function.Consumer<android.graphics.Bitmap> callback) {
        if (callback != null) callback.accept(null);
    }
    public void captureForSwitcher(kotlin.jvm.functions.Function1<? super android.graphics.Bitmap, kotlin.Unit> callback) {
        if (callback != null) callback.invoke(null);
    }
    public com.petal.browser.pwa.PetalPwaManager getPwaManager() { return null; }
    public void setPwaManager(com.petal.browser.pwa.PetalPwaManager manager) {}
    public void printToPdf(java.io.FileOutputStream fos, java.util.function.Consumer<Boolean> callback) {
        if (callback != null) callback.accept(false);
    }
    public void printToPdf(java.io.FileOutputStream fos, kotlin.jvm.functions.Function1<? super Boolean, kotlin.Unit> callback) {
        if (callback != null) callback.invoke(false);
    }

}
