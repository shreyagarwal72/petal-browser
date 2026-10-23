package com.petal.browser.bridge

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LocationBarBridge
 * ─────────────────────────────────────────────────────────────────────────────
 * Singleton bridge to Chromium's AutocompleteController / LocationBarCoordinator.
 *
 * Data flow:
 *   PetalOmniboxPage (Compose) → LocationBarBridge.startInput()
 *   → Chromium AutocompleteController.start() (JNI, reflection)
 *   → suggestions callback → _suggestions StateFlow → PetalOmniboxPage recomposes
 */
object LocationBarBridge {

    private const val TAG = "LocationBarBridge"

    data class AutocompleteSuggestion(
        val text: String,
        val description: String = "",
        val url: String = "",
        val type: SuggestionType = SuggestionType.SEARCH,
        val isAnswer: Boolean = false
    )

    enum class SuggestionType {
        SEARCH,       // magnifying glass icon
        HISTORY_URL,  // history clock icon
        BOOKMARK,     // bookmark icon
        CALCULATOR,   // calculate icon
        URL,          // language/globe icon
        UNKNOWN
    }

    enum class SecurityLevel { SECURE, WARNING, DANGEROUS, NONE }

    private val _suggestions = MutableStateFlow<List<AutocompleteSuggestion>>(emptyList())
    val suggestions: StateFlow<List<AutocompleteSuggestion>> = _suggestions.asStateFlow()

    private val _securityLevel = MutableStateFlow(SecurityLevel.NONE)
    val securityLevel: StateFlow<SecurityLevel> = _securityLevel.asStateFlow()

    private val _displayUrl = MutableStateFlow("")
    val displayUrl: StateFlow<String> = _displayUrl.asStateFlow()

    // Called by PetalOmniboxPage when user types
    fun startInput(text: String) {
        if (text.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        runCatching {
            pushToComposeShellAutocomplete(text)
        }.onFailure {
            Log.d(TAG, "startInput native not available: ${it.message}")
        }
    }

    fun stopInput() {
        _suggestions.value = emptyList()
    }

    // Called back from PetalComposeShell or TabManager when Chromium provides suggestions
    @JvmStatic
    fun onSuggestionsReceived(rawSuggestions: List<Map<String, String>>) {
        _suggestions.value = rawSuggestions.map { map ->
            AutocompleteSuggestion(
                text = map["text"] ?: "",
                description = map["description"] ?: "",
                url = map["url"] ?: "",
                type = when (map["type"]) {
                    "HISTORY_URL", "HISTORY_TITLE" -> SuggestionType.HISTORY_URL
                    "BOOKMARK" -> SuggestionType.BOOKMARK
                    "CALCULATOR" -> SuggestionType.CALCULATOR
                    "URL_WHAT_YOU_TYPED", "NAVSUGGEST" -> SuggestionType.URL
                    else -> SuggestionType.SEARCH
                }
            )
        }
    }

    @JvmStatic
    fun onSecurityLevelChanged(level: String) {
        _securityLevel.value = when (level) {
            "SECURE" -> SecurityLevel.SECURE
            "WARNING" -> SecurityLevel.WARNING  
            "DANGEROUS" -> SecurityLevel.DANGEROUS
            else -> SecurityLevel.NONE
        }
    }

    @JvmStatic
    fun onDisplayUrlChanged(url: String) {
        _displayUrl.value = url
    }

    private fun pushToComposeShellAutocomplete(text: String) {
        runCatching {
            val shellClass = Class.forName("com.petal.browser.engine.PetalComposeShell")
            shellClass.getMethod("requestAutocompleteSuggestions", String::class.java)
                .invoke(null, text)
        }.onFailure { /* graceful no-op */ }
    }
}
