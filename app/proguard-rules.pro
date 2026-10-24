# Petal release shrinking (R8).
#
# Libraries with their own consumer rules (GeckoView, Gson, OkHttp, PDFBox, Hilt, AndroidX,
# Compose, Play services) need nothing here. What follows covers the code that is reached by
# name at runtime: Gson models, JavaScript bridges, reflection into Mozilla Components and
# GeckoView, and yt-dlp's JSON mapping.

# Keep line numbers so release crash traces stay readable (with build/outputs/mapping).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Generic signatures, inner classes and annotations: Gson's TypeToken and Kotlin metadata.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# ---- Petal ----------------------------------------------------------------------------------
# Gson (menu items, tab groups, collections, credentials, inactive tabs, quick tools...)
# reads and writes fields by name, @JavascriptInterface methods are called by name from page
# scripts, and enums are restored from stored names. Classes nothing uses can still go.
-keep,allowshrinking class com.petal.browser.** { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---- Mozilla Android Components -------------------------------------------------------------
# Ships no consumer rules. Petal reads GeckoEngineSession's geckoSession by reflection
# (PetalGeckoView.extractGeckoSession), so nothing here may be renamed.
-keep,allowshrinking class mozilla.components.** { *; }

# ---- GeckoView ------------------------------------------------------------------------------
# Native code calls into these by name; ContextElement.textContent is read by reflection.
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }

# ---- Brave & Chromium Core Engine -----------------------------------------------------------
-keep class org.chromium.** { *; }
-keep interface org.chromium.** { *; }
-dontwarn org.chromium.**

-keep class org.brave.** { *; }
-keep interface org.brave.** { *; }
-dontwarn org.brave.**

-keepclasseswithmembernames class * {
    native <methods>;
}

# ---- yt-dlp (youtubedl-android) -------------------------------------------------------------
# Maps yt-dlp's JSON output onto its model classes with Jackson.
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

# ---- Media3 ---------------------------------------------------------------------------------
# The animated wallpaper switches PlayerView's surface type by reflection.
-keep class androidx.media3.ui.PlayerView {
    *** setVideoSurfaceViewType(int);
    *** surfaceType;
}

# ---- Fetch2 (downloads) ---------------------------------------------------------------------
-keep class com.tonyodev.fetch2.** { *; }
-keep class com.tonyodev.fetch2core.** { *; }
-keep class com.tonyodev.fetch2okhttp.** { *; }

# ---- PDFBox ---------------------------------------------------------------------------------
# JPEG 2000 support is an optional dependency that is not bundled.
-dontwarn com.gemalto.jp2.**

# Keep Android Translation & ContentCapture callbacks from interface method stripping
-keep interface android.view.translation.ViewTranslationCallback { *; }
-keep class * implements android.view.translation.ViewTranslationCallback { *; }
-dontwarn android.view.translation.**

# Keep Android OnBackInvokedCallback from interface method stripping
-keep interface android.window.OnBackInvokedCallback { *; }
-keep class * implements android.window.OnBackInvokedCallback { *; }
-dontwarn android.window.**

# Keep AndroidX Back / PredictiveBack from interface stripping
-keep class androidx.activity.** { *; }
-keep interface androidx.activity.** { *; }
-keep class * implements android.window.OnBackInvokedCallback { *; }
-keep interface android.window.OnBackInvokedCallback { *; }
-keep class * implements android.window.OnBackAnimationCallback { *; }
-keep interface android.window.OnBackAnimationCallback { *; }
-dontwarn android.window.**
