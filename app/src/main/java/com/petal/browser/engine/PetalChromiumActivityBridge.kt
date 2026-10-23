package com.petal.browser.engine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PetalChromiumActivityBridge {

    private val _currentUrl = MutableStateFlow("about:blank")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _currentTitle = MutableStateFlow("New Tab")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _loadProgress = MutableStateFlow(100)
    val loadProgress: StateFlow<Int> = _loadProgress.asStateFlow()

    private val _isIncognito = MutableStateFlow(false)
    val isIncognito: StateFlow<Boolean> = _isIncognito.asStateFlow()

    @JvmStatic
    fun getInstance(): PetalChromiumActivityBridge = this

    fun sharePage() {}

    fun navigate(url: String) {
        _currentUrl.value = url
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
