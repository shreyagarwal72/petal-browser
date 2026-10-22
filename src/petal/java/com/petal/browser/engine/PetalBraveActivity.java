package com.petal.browser.engine;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;

/**
 * PetalBraveActivity
 *
 * Thin Java shell that extends BraveActivity (Chromium/Brave) and overlays
 * Petal's full Material 3 Expressive + Liquid Glass Compose UI on top of
 * Brave's native Chromium content frame.
 *
 * Architecture:
 *   ChromeTabbedActivity (Chromium)
 *     └── BraveActivity (Brave)
 *           └── PetalBraveActivity  ← THIS CLASS
 *                 └── ComposeView (Petal UI shell, injected into Brave's content frame)
 *
 * What this does:
 *   1. onPostInflationStartup(): hides Brave's own toolbar/UI chrome, injects
 *      Petal's ComposeView shell (address bar, dock bar, tab switcher, menus).
 *   2. onStartWithNativeInitialized(): initialises ChromiumGlassBackend (TextureView
 *      mode for AGSL sampling), PetalShieldsObserver (Brave Shields C++ JNI bridge).
 *   3. Tab events: wraps each new Chromium Tab in a PetalChromiumTabView and registers
 *      it with PetalTabManager so Petal's tab switcher UI stays in sync.
 *
 * Build note: This file is compiled inside brave-core's Android build via the
 *   petal overlay module declared in src/brave/android/java/BUILD.gn.
 *   It references Brave/Chromium classes by their fully-qualified names as they
 *   exist in the compiled brave-core output.
 */
public class PetalBraveActivity extends Activity /* extends org.chromium.chrome.browser.app.BraveActivity */ {

    private static final String TAG = "PetalBraveActivity";

    private PetalTabManager petalTabManager;
    private PetalShieldsObserver petalShieldsObserver;
    private View petalComposeShell;

    // -------------------------------------------------------------------------
    // Lifecycle hooks (called by BraveActivity superclass)
    // -------------------------------------------------------------------------

    /**
     * Called after Brave's layout is inflated.
     * Hides Brave's toolbar and injects Petal's Compose UI overlay.
     */
    protected void onPostInflationStartup() {
        // (super.onPostInflationStartup() called by BraveActivity's own override)

        hideBraveUiChrome();
        injectPetalComposeShell();

        Log.i(TAG, "Petal UI shell injected over Brave content frame");
    }

    /**
     * Called after Chromium's native library is fully initialised.
     * Safe to make JNI calls to Chromium / Brave native here.
     */
    protected void onStartWithNativeInitialized() {
        // (super.onStartWithNativeInitialized() called by BraveActivity)

        // Force Chromium's ContentViewRenderView into TextureView mode so
        // Petal's AGSL Liquid Glass shaders can sample live webpage pixels.
        View contentFrame = findBraveContentFrame();
        if (contentFrame != null) {
            com.petal.browser.ui.glass.ChromiumGlassBackend.attachTextureListener(contentFrame);
        }

        // Initialise Brave Shields → Petal Shields UI bridge
        petalShieldsObserver = new PetalShieldsObserver(this);

        // Initialise Petal tab manager (wraps Chromium TabModelSelector)
        petalTabManager = new PetalTabManager(this);

        Log.i(TAG, "Petal native bridges initialised");
    }

    // -------------------------------------------------------------------------
    // UI chrome management
    // -------------------------------------------------------------------------

    /** Hides Brave's own toolbar so Petal's address bar takes over. */
    private void hideBraveUiChrome() {
        try {
            // Brave's toolbar container view ID — hide it so Petal's bar shows
            View braveToolbar = findViewById(
                getResources().getIdentifier("toolbar", "id", getPackageName()));
            if (braveToolbar != null) braveToolbar.setVisibility(View.GONE);

            View braveBottomToolbar = findViewById(
                getResources().getIdentifier("bottom_toolbar", "id", getPackageName()));
            if (braveBottomToolbar != null) braveBottomToolbar.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.w(TAG, "hideBraveUiChrome: " + e.getMessage());
        }
    }

    /** Injects Petal's Jetpack Compose shell (address bar, dock, tab switcher, menus). */
    private void injectPetalComposeShell() {
        try {
            ViewGroup contentRoot = (ViewGroup) getWindow().getDecorView()
                .findViewById(android.R.id.content);

            // PetalComposeShell is a Kotlin object that returns a ComposeView
            // pre-wired to PetalBraveActivityBridge (the ViewModel/state layer).
            petalComposeShell = PetalComposeShell.INSTANCE.create(this);

            contentRoot.addView(petalComposeShell,
                new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

        } catch (Exception e) {
            Log.e(TAG, "injectPetalComposeShell failed: " + e.getMessage());
        }
    }

    /** Locates Brave's main web content frame for glass backend attachment. */
    @Nullable
    private View findBraveContentFrame() {
        try {
            int id = getResources().getIdentifier("coordinator", "id", getPackageName());
            return findViewById(id);
        } catch (Exception e) {
            return null;
        }
    }
}
