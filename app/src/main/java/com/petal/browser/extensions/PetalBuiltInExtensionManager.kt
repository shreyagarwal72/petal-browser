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

    val builtIns: List<BuiltInSpec> = listOf(
        BuiltInSpec(
            assetPath = "extensions/webcompat/",
            extensionId = "webcompat@mozilla.org",
            label = "WebCompat",
            description = "Site compatibility engine.",
            prefKey = "petal_builtin_webcompat"
        )
    )

    @JvmStatic
    fun isBuiltIn(extensionId: String?): Boolean {
        if (extensionId.isNullOrBlank()) return false
        val clean = extensionId.trim()
        return builtIns.any { it.extensionId.equals(clean, ignoreCase = true) }
    }

    @JvmStatic
    fun isEnabled(context: Context, spec: BuiltInSpec): Boolean = false

    @JvmStatic
    fun setEnabled(context: Context, spec: BuiltInSpec, enabled: Boolean) {}

    @JvmStatic
    fun ensureBuiltIn(context: Context) {}
}
