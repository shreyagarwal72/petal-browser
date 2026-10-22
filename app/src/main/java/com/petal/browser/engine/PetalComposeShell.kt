package com.petal.browser.engine

import android.content.Context
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.petal.browser.ui.theme.PetalTheme

/**
 * PetalComposeShell
 *
 * Singleton factory that creates the root Petal Compose UI overlay injected into
 * BraveActivity by patch 0003. This is the single entry point from Java/Brave into
 * Petal's Kotlin/Compose world.
 *
 * The ComposeView returned by create() hosts the entire Petal UI:
 *   - PetalAddressBar  (top, collapsing)
 *   - ExpressiveFloatingDockBar  (bottom navigation pill)
 *   - PetalTabSwitcherSheet  (tab grid overlay)
 *   - PetalOverflowMenuSheet  (3-dot Liquid Glass menu)
 *   - PetalPrivacyShieldSheet  (Brave Shields control panel)
 *   - PetalExtensionsSheet  (Chrome extension manager)
 *   - PetalFlagsSettingsScreen  (chrome://flags surface)
 *
 * All data flows down from PetalChromiumActivityBridge (ViewModel) which reads from
 * Chromium's TabModelSelector and PetalShieldEngine via JNI.
 */
object PetalComposeShell {

    @JvmStatic
    fun create(context: Context): View {
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        // Lifecycle owners must be wired when embedding ComposeView into a non-Compose Activity
        val activity = context as? androidx.activity.ComponentActivity
        activity?.let {
            composeView.setViewTreeLifecycleOwner(it)
            composeView.setViewTreeViewModelStoreOwner(it)
            composeView.setViewTreeSavedStateRegistryOwner(it)
        }

        composeView.setContent {
            PetalTheme {
                PetalChromiumUIRoot()
            }
        }

        return composeView
    }
}

/**
 * PetalChromiumUIRoot
 *
 * Root composable for the Petal-on-Chromium UI. All Petal screens and overlays
 * are composed here. This replaces the old BrowserActivity.java inflate path.
 */
@Composable
private fun PetalChromiumUIRoot() {
    val bridge = remember { PetalChromiumActivityBridge.getInstance() }

    // Tab state observed from Chromium TabModelSelector
    val currentUrl by bridge.currentUrl.collectAsState()
    val currentTitle by bridge.currentTitle.collectAsState()
    val currentProgress by bridge.loadProgress.collectAsState()
    val tabCount by bridge.tabCount.collectAsState()
    val isIncognito by bridge.isIncognito.collectAsState()

    // Sheet/overlay visibility state
    var showTabSwitcher by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showShields by remember { mutableStateOf(false) }
    var showExtensions by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        // ---- Collapsing top address bar ----
        com.petal.browser.ui.components.PetalAddressBar(
            url = currentUrl,
            title = currentTitle,
            progress = currentProgress,
            isIncognito = isIncognito,
            onSearch = { query -> bridge.navigate(query) },
            onMenuClick = { showOverflowMenu = true },
            onShieldsClick = { showShields = true },
            modifier = androidx.compose.ui.Modifier.align(
                androidx.compose.ui.Alignment.TopCenter)
        )

        // ---- Bottom dock bar ----
        com.petal.browser.ui.components.ExpressiveFloatingDockBar(
            canGoBack = bridge.canGoBack.collectAsState().value,
            canGoForward = bridge.canGoForward.collectAsState().value,
            tabCount = tabCount,
            onBack = { bridge.goBack() },
            onForward = { bridge.goForward() },
            onTabSwitcher = { showTabSwitcher = true },
            onHome = { bridge.navigate("about:blank") },
            modifier = androidx.compose.ui.Modifier.align(
                androidx.compose.ui.Alignment.BottomCenter)
        )

        // ---- Sheets ----
        if (showTabSwitcher) {
            com.petal.browser.ui.components.PetalTabSwitcherSheet(
                tabs = bridge.allTabs.collectAsState().value,
                onTabSelected = { tab -> bridge.selectTab(tab); showTabSwitcher = false },
                onTabClosed = { tab -> bridge.closeTab(tab) },
                onNewTab = { bridge.openNewTab(incognito = false) },
                onNewIncognitoTab = { bridge.openNewTab(incognito = true) },
                onDismiss = { showTabSwitcher = false }
            )
        }

        if (showOverflowMenu) {
            com.petal.browser.ui.components.PetalOverflowMenuSheet(
                url = currentUrl,
                onDismiss = { showOverflowMenu = false },
                onExtensions = { showExtensions = true; showOverflowMenu = false },
                onShields = { showShields = true; showOverflowMenu = false },
                onFlags = { bridge.navigate("chrome://flags"); showOverflowMenu = false },
                onDevTools = { bridge.navigate("chrome://inspect"); showOverflowMenu = false },
                onSettings = { bridge.openSettings(); showOverflowMenu = false },
                onSharePage = { bridge.sharePage() },
                onDesktopSite = { bridge.toggleDesktopSite() },
                onReaderMode = { bridge.toggleReaderMode() },
                onBookmark = { bridge.bookmarkCurrentPage() },
                onDownload = { bridge.downloadCurrentPage() }
            )
        }

        if (showShields) {
            com.petal.browser.ui.components.PetalPrivacyShieldSheet(
                shieldsObserver = bridge.shieldsObserver,
                currentTabId = bridge.currentTabId.collectAsState().value,
                onDismiss = { showShields = false }
            )
        }

        if (showExtensions) {
            com.petal.browser.compose.extensions.PetalExtensionsScreen(
                onDismiss = { showExtensions = false }
            )
        }
    }
}
