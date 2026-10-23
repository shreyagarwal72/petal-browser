package com.petal.browser.extensions

import android.content.Context

object PetalBuiltInExtensionManager {

    data class BuiltInSpec(
        val assetPath: String = "",
        val extensionId: String = "",
        val label: String = "",
        val description: String = "",
        val prefKey: String = "",
        val defaultOn: Boolean = true
    )

    val builtIns: List<BuiltInSpec> = emptyList()

    fun isEnabled(context: Context, spec: BuiltInSpec): Boolean = false
    fun setEnabled(context: Context, spec: BuiltInSpec, enabled: Boolean) {}
    fun ensureBuiltIn(context: Context) {}
}
