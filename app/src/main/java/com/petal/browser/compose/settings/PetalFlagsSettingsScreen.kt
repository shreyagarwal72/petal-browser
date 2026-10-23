package com.petal.browser.compose.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petal.browser.ui.components.ExpressiveHeader
import com.petal.browser.ui.containment.PetalGroup
import com.petal.browser.ui.containment.PetalGroupListRow
import com.petal.browser.ui.containment.PetalGroupPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetalFlagsSettingsScreen(
    onBack: () -> Unit,
    onRestartRequired: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var flags by remember { mutableStateOf<List<PetalFlagEntry>>(emptyList()) }
    var showRestartBanner by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        flags = loadAllFlags()
    }

    val filteredFlags = remember(searchQuery, flags) {
        if (searchQuery.isBlank()) flags
        else flags.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.internalName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ExpressiveHeader(
            title = "Experimental Flags",
            subtitle = "⚠️ These features may be unstable",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset all flags")
                }
            }
        )

        AnimatedVisibility(visible = showRestartBanner) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Restart required to apply changes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRestartRequired) {
                        Text("Restart")
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search flags…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredFlags, key = { it.internalName }) { flag ->
                FlagRow(flag = flag, onChanged = { showRestartBanner = true })
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all flags?") },
            text = { Text("This will restore all experimental flags to their default state.") },
            confirmButton = {
                TextButton(onClick = {
                    resetAllFlags()
                    flags = loadAllFlags()
                    showRestartBanner = true
                    showResetDialog = false
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FlagRow(flag: PetalFlagEntry, onChanged: () -> Unit) {
    PetalGroup(rowCount = 1) { _, position ->
        var expanded by remember { mutableStateOf(false) }
        var selected by remember { mutableStateOf(flag.currentValue) }

        PetalGroupListRow(
            position = position,
            onClick = { expanded = true },
            leading = {
                Icon(
                    Icons.Rounded.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            content = {
                Text(
                    text = flag.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${flag.description}\nValue: $selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailing = {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Rounded.ExpandMore, contentDescription = "Select Option")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        flag.options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selected = option
                                    setFlag(flag.internalName, option)
                                    onChanged()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        )
    }
}

data class PetalFlagEntry(
    val internalName: String,
    val name: String,
    val description: String,
    val currentValue: String,
    val defaultValue: String,
    val options: List<String>
)

private fun loadAllFlags(): List<PetalFlagEntry> {
    return try {
        val clazz = Class.forName("org.chromium.chrome.browser.flags.AboutFlagsAndroid")
        @Suppress("UNCHECKED_CAST")
        val list = clazz.getMethod("petalGetAllFlags").invoke(null) as? List<Any>
        list?.map { item ->
            val cls = item.javaClass
            PetalFlagEntry(
                internalName = cls.getMethod("getInternalName").invoke(item) as? String ?: "",
                name         = cls.getMethod("getName").invoke(item) as? String ?: "",
                description  = cls.getMethod("getDescription").invoke(item) as? String ?: "",
                currentValue = cls.getMethod("getCurrentValue").invoke(item) as? String ?: "Default",
                defaultValue = cls.getMethod("getDefaultValue").invoke(item) as? String ?: "Default",
                options      = (cls.getMethod("getOptions").invoke(item) as? List<*>)?.filterIsInstance<String>() ?: listOf("enabled", "disabled")
            )
        } ?: emptyList()
    } catch (e: Exception) {
        android.util.Log.w("PetalFlags", "loadAllFlags: $e")
        emptyList()
    }
}

private fun setFlag(internalName: String, value: String) {
    try {
        Class.forName("org.chromium.chrome.browser.flags.AboutFlagsAndroid")
            .getMethod("petalSetFlagValue", String::class.java, String::class.java)
            .invoke(null, internalName, value)
    } catch (e: Exception) {
        android.util.Log.w("PetalFlags", "setFlag: $e")
    }
}

private fun resetAllFlags() {
    try {
        Class.forName("org.chromium.chrome.browser.flags.AboutFlagsAndroid")
            .getMethod("petalResetAllFlags")
            .invoke(null)
    } catch (e: Exception) {
        android.util.Log.w("PetalFlags", "resetAllFlags: $e")
    }
}
