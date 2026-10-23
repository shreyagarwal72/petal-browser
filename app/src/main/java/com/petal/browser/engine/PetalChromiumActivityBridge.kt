package com.petal.browser.engine

import android.content.Context

object PetalChromiumActivityBridge {

    fun navigate(url: String) {
        try {
            val mgrClass = Class.forName("com.petal.browser.tab.PetalTabManager")
            val instance = mgrClass.getMethod("getInstance").invoke(null)
            mgrClass.getMethod("loadUrl", String::class.java).invoke(instance, url)
        } catch (e: Exception) {
            android.util.Log.w("PetalBridge", "navigate: $e")
        }
    }

    fun goBack() {
        try {
            val mgrClass = Class.forName("com.petal.browser.tab.PetalTabManager")
            val instance = mgrClass.getMethod("getInstance").invoke(null)
            val tab = mgrClass.getMethod("getActiveTab").invoke(instance)
            tab?.javaClass?.getMethod("goBack")?.invoke(tab)
        } catch (e: Exception) {
            android.util.Log.w("PetalBridge", "goBack: $e")
        }
    }

    fun goForward() {
        try {
            val mgrClass = Class.forName("com.petal.browser.tab.PetalTabManager")
            val instance = mgrClass.getMethod("getInstance").invoke(null)
            val tab = mgrClass.getMethod("getActiveTab").invoke(instance)
            tab?.javaClass?.getMethod("goForward")?.invoke(tab)
        } catch (e: Exception) {
            android.util.Log.w("PetalBridge", "goForward: $e")
        }
    }

    fun reload() {
        try {
            val mgrClass = Class.forName("com.petal.browser.tab.PetalTabManager")
            val instance = mgrClass.getMethod("getInstance").invoke(null)
            val tab = mgrClass.getMethod("getActiveTab").invoke(instance)
            tab?.javaClass?.getMethod("reload")?.invoke(tab)
        } catch (e: Exception) {
            android.util.Log.w("PetalBridge", "reload: $e")
        }
    }

    fun stop() {
        try {
            val mgrClass = Class.forName("com.petal.browser.tab.PetalTabManager")
            val instance = mgrClass.getMethod("getInstance").invoke(null)
            val tab = mgrClass.getMethod("getActiveTab").invoke(instance)
            tab?.javaClass?.getMethod("stop")?.invoke(tab)
        } catch (e: Exception) {
            android.util.Log.w("PetalBridge", "stop: $e")
        }
    }
}
