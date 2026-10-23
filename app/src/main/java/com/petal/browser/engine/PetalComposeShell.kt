package com.petal.browser.engine

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.petal.browser.ui.theme.PetalExpressiveTheme

/**
 * PetalComposeShell
 *
 * Factory creating the root Petal Compose UI overlay.
 */
object PetalComposeShell {

    @JvmStatic
    fun create(context: Context): View {
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        val activity = context as? androidx.activity.ComponentActivity
        activity?.let {
            composeView.setViewTreeLifecycleOwner(it)
            composeView.setViewTreeViewModelStoreOwner(it)
            composeView.setViewTreeSavedStateRegistryOwner(it)
        }

        composeView.setContent {
            PetalExpressiveTheme {
                PetalChromiumUIRoot()
            }
        }

        return composeView
    }
}

@Composable
private fun PetalChromiumUIRoot() {
    val bridge = remember { PetalChromiumActivityBridge.getInstance() }

    val currentUrl by bridge.currentUrl.collectAsState()
    val currentTitle by bridge.currentTitle.collectAsState()
    val currentProgress by bridge.loadProgress.collectAsState()
    val isIncognito by bridge.isIncognito.collectAsState()

    var showTabSwitcher by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        com.petal.browser.ui.components.PetalAddressBar(
            url = currentUrl,
            title = currentTitle,
            progress = currentProgress.toFloat(),
            isIncognito = isIncognito,
            onBackClick = { bridge.goBack() },
            onShareClick = { bridge.sharePage() },
            onAddressClick = { bridge.navigate(currentUrl) },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
