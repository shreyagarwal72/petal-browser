package com.petal.browser.compose.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.petal.browser.ui.containment.PetalGroup
import com.petal.browser.ui.containment.PetalGroupListRow
import com.petal.browser.ui.components.ExpressiveHeader
import com.petal.browser.ui.components.IconSwitch

/**
 * PetalFlagsSettingsScreen
 *
 * Exposes Chromium's chrome://flags engine (AboutFlagsAndroid) in a searchable,
 * filterable Compose screen inside Petal Settings → Developer → Experimental Flags.
 *
 * Provides the same power-user flags UI as chrome://flags but styled with Petal's
 * Liquid Glass containment surfaces and M3 Expressive motion.
 *
 * Features:
 *  - Full-text search across all flag names and descriptions
 *  - Boolean flags shown as IconSwitch toggles
 *  - Multi-value flags shown as PetalSelectableOptionCard rows
 *  - Reset all to defaults with animated confirmation
 *  - Restart required banner for active flag changes
 */
@Composable
fun PetalFlagsSettingsScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var showRestartBanner by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val allFlags by remember { mutableStateOf(loadAllFlags()) }
    val filteredFlags = remember(query, allFlags) {
        if (query.isBlank()) allFlags
        else allFlags.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        ExpressiveHeader(
            title = "Experimental Flags",
            subtitle = "⚠️ These features may be unstable",
            leadingIcon = Icons.Rounded.Science,
            onBack = onBack,
            actions = {
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset all flags")
                }
            }
        )

        // Restart required banner
        AnimatedVisibility(visible = showRestartBanner) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Restart required to apply flag changes",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        // Trigger Chromium process restart
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }) { Text("Restart") }
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search flags...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (filteredFlags.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No flags match \"$query\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(filteredFlags, key = { it.internalName }) { flag ->
                    FlagRow(
                        flag = flag,
                        onChanged = { showRestartBanner = true }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    // Reset all flags dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all flags?") },
            text = { Text("All experimental flags will be reset to their default values.") },
            confirmButton = {
                TextButton(onClick = {
                    resetAllFlags()
                    showResetDialog = false
                    showRestartBanner = true
                }) { Text("Reset All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FlagRow(flag: PetalFlagEntry, onChanged: () -> Unit) {
    PetalGroup {
        if (flag.options.size <= 2) {
            // Boolean flag → IconSwitch
            val currentlyEnabled = flag.currentValue != "disabled" &&
                flag.currentValue != "Default" || flag.defaultValue == "enabled"
            var enabled by remember { mutableStateOf(currentlyEnabled) }

            PetalGroupListRow(
                headlineText = flag.name,
                supportingText = flag.description,
                trailingContent = {
                    IconSwitch(
                        checked = enabled,
                        icon = Icons.Rounded.Science,
                        onCheckedChange = { on ->
                            enabled = on
                            setFlag(flag.internalName, if (on) "enabled" else "disabled")
                            onChanged()
                        }
                    )
                }
            )
        } else {
            // Multi-value flag → dropdown
            var expanded by remember { mutableStateOf(false) }
            var selected by remember { mutableStateOf(flag.currentValue) }

            PetalGroupListRow(
                headlineText = flag.name,
                supportingText = "${flag.description}\n→ $selected",
                trailingContent = {
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Rounded.ExpandMore, contentDescription = "Choose value")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            flag.options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selected = option
                                        setFlag(flag.internalName, option)
                                        expanded = false
                                        onChanged()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Data model + JNI bridge calls
// ---------------------------------------------------------------------------

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
        val clazz = Class.forName(
            "org.chromium.chrome.browser.flags.AboutFlagsAndroid")
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
                @Suppress("UNCHECKED_CAST")
                options      = cls.getMethod("getOptions").invoke(item) as? List<String> ?: listOf("enabled","disabled")
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
            .getMethod("petalResetAllFlags").invoke(null)
    } catch (e: Exception) {
        android.util.Log.w("PetalFlags", "resetAllFlags: $e")
    }
}
