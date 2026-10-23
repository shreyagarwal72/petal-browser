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
        void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY);
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
}
