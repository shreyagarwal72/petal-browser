package com.petal.browser.engine;

import android.content.Context;
import android.util.Log;

/**
 * ChromiumNativeEngineCore
 * Manages native Chromium library loading and command line initialization.
 */
public class ChromiumNativeEngineCore {
    private static final String TAG = "ChromiumNativeCore";
    private static boolean isNativeLibraryLoaded = false;

    public static synchronized void initialize(Context context) {
        if (isNativeLibraryLoaded) return;
        try {
            System.loadLibrary("chrome");
            isNativeLibraryLoaded = true;
            Log.i(TAG, "Chromium libchrome.so successfully initialized");
        } catch (UnsatisfiedLinkError e) {
            Log.i(TAG, "Chromium native binary will load via ChromeActivity loader: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Chromium native core", e);
        }
    }

    public static boolean isNativeLoaded() {
        return isNativeLibraryLoaded;
    }
}
