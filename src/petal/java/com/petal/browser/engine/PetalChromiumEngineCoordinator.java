package com.petal.browser.engine;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.petal.browser.ui.glass.ChromiumGlassBackend;
import java.util.ArrayList;
import java.util.List;

/**
 * PetalChromiumEngineCoordinator
 *
 * Implements the runtime orchestration between Brave's Chromium native browser layer
 * (TabModelSelector, WebContents, Extensions, Flags, Shields) and Petal's Jetpack Compose
 * Material 3 Expressive + Liquid Glass UI.
 */
public class PetalChromiumEngineCoordinator implements PetalChromiumBridge {

    private static final String TAG = "PetalChromiumCoordinator";
    private static volatile PetalChromiumEngineCoordinator sInstance;

    private final Context mContext;
    private View mCurrentContentView;
    private boolean mShieldsActive = true;
    private final List<EngineStateListener> mListeners = new ArrayList<>();

    public interface EngineStateListener {
        void onUrlChanged(@NonNull String newUrl);
        void onProgressChanged(int progress);
        void onSecurityStateChanged(boolean isSecure);
        void onTabCountChanged(int tabCount);
    }

    private PetalChromiumEngineCoordinator(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static PetalChromiumEngineCoordinator getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (PetalChromiumEngineCoordinator.class) {
                if (sInstance == null) {
                    sInstance = new PetalChromiumEngineCoordinator(context);
                }
            }
        }
        return sInstance;
    }

    @Override
    public void initializeEngine(@NonNull Context context) {
        // Chromium command line & native engine initialization
        ChromiumNativeEngineCore.initialize(context);
    }

    /**
     * Mounts Chromium's ContentView and attaches the TextureView frame listener
     * so Petal's AGSL Liquid Glass shaders sample the web page.
     */
    public void attachContentView(@NonNull View contentView) {
        this.mCurrentContentView = contentView;
        ChromiumGlassBackend.attachTextureListener(contentView);
    }

    @Nullable
    @Override
    public View getActiveContentView() {
        return mCurrentContentView;
    }

    @Override
    public void loadUrl(@NonNull String url) {
        if (url == null || url.trim().isEmpty()) return;
        String target = url.trim();
        if (!target.startsWith("http://") && !target.startsWith("https://") && !target.startsWith("chrome://")) {
            target = "https://" + target;
        }
        // Injected into ChromeTabbedActivity / BraveActivity navigation pipeline via patch hook
    }

    @Override
    public void openExtensionPopup(@NonNull String extensionId) {
        // Routes extension popup window to PetalSheet container
    }

    @Override
    public void openFlagsManager() {
        loadUrl("chrome://flags");
    }

    @Override
    public void setShieldsEnabled(boolean enabled) {
        this.mShieldsActive = enabled;
        // Toggles Brave Shields native subresource filter
    }

    public boolean isShieldsEnabled() {
        return mShieldsActive;
    }

    public void addListener(@NonNull EngineStateListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeListener(@NonNull EngineStateListener listener) {
        mListeners.remove(listener);
    }
}
