package com.petal.browser.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petal.browser.ui.components.IconSwitch
import com.petal.browser.ui.components.bouncyClickable
import com.petal.browser.ui.containment.PetalPredictiveBackSurface
import com.petal.browser.ui.glass.liquidGlassEffect

/**
 * PetalSyncScreen
 * ─────────────────────────────────────────────────────────────────────────────
 * Complete Material 3 Expressive + Liquid Glass Petal Sync & Recovery Interface.
 *
 * Replaces old email/account sign-in with Petal's zero-knowledge 24-word
 * recovery phrase and decentralized device pairing chain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetalSyncScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isSyncActive by remember { mutableStateOf(PetalSyncBridge.isSyncActive(context)) }
    var recoverySeed by remember { mutableStateOf(PetalSyncBridge.getRecoverySeed(context)) }
    var pairedDevices by remember { mutableStateOf(PetalSyncBridge.getPairedDevices(context)) }

    var showStartDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showSeedDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmation by remember { mutableStateOf(false) }

    // Sync categories toggle states
    var syncBookmarks by remember { mutableStateOf(true) }
    var syncHistory by remember { mutableStateOf(true) }
    var syncPasswords by remember { mutableStateOf(true) }
    var syncTabs by remember { mutableStateOf(true) }
    var syncSettings by remember { mutableStateOf(true) }

    PetalPredictiveBackSurface(onBack = onBack) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Petal Sync & Recovery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header status card
                item {
                    SyncStatusCard(
                        isSyncActive = isSyncActive,
                        deviceCount = pairedDevices.size,
                        onViewSeed = { showSeedDialog = true },
                        onStartChain = { showStartDialog = true },
                        onJoinChain = { showJoinDialog = true }
                    )
                }

                if (isSyncActive) {
                    // Sync items toggles card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassEffect(cornerRadius = 24.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Synced Data",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                SyncToggleRow(title = "Bookmarks", checked = syncBookmarks, onCheckedChange = { syncBookmarks = it })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                SyncToggleRow(title = "History", checked = syncHistory, onCheckedChange = { syncHistory = it })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                SyncToggleRow(title = "Saved Passwords", checked = syncPasswords, onCheckedChange = { syncPasswords = it })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                SyncToggleRow(title = "Open Tabs", checked = syncTabs, onCheckedChange = { syncTabs = it })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                SyncToggleRow(title = "Settings & Preferences", checked = syncSettings, onCheckedChange = { syncSettings = it })
                            }
                        }
                    }

                    // Connected devices list header
                    item {
                        Text(
                            text = "Connected Devices (${pairedDevices.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Connected devices items
                    items(pairedDevices) { device ->
                        DeviceItemCard(
                            device = device,
                            onRemove = {
                                if (PetalSyncBridge.removeDevice(device.guid)) {
                                    pairedDevices = PetalSyncBridge.getPairedDevices(context)
                                    Toast.makeText(context, "Device removed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    // Add new device / Leave chain actions
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showSeedDialog = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.QrCode, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Device (View Sync Code)", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showLeaveConfirmation = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Leave Sync Chain", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: View 24-Word Recovery Seed
    if (showSeedDialog && recoverySeed != null) {
        RecoverySeedDialog(
            seed = recoverySeed!!,
            onDismiss = { showSeedDialog = false }
        )
    }

    // Dialog: Start New Sync Chain
    if (showStartDialog) {
        StartSyncChainDialog(
            onDismiss = { showStartDialog = false },
            onConfirm = { deviceName ->
                val newSeed = PetalSyncBridge.createSyncChain(context, deviceName)
                recoverySeed = newSeed
                isSyncActive = true
                pairedDevices = PetalSyncBridge.getPairedDevices(context)
                showStartDialog = false
                showSeedDialog = true
            }
        )
    }

    // Dialog: Join Existing Chain
    if (showJoinDialog) {
        JoinSyncChainDialog(
            onDismiss = { showJoinDialog = false },
            onConfirm = { seedInput, deviceName ->
                if (PetalSyncBridge.joinSyncChain(context, seedInput, deviceName)) {
                    recoverySeed = seedInput
                    isSyncActive = true
                    pairedDevices = PetalSyncBridge.getPairedDevices(context)
                    showJoinDialog = false
                    Toast.makeText(context, "Joined Sync Chain successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid 24-word recovery seed", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Dialog: Confirm Leave
    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmation = false },
            title = { Text("Leave Sync Chain?", fontWeight = FontWeight.Bold) },
            text = { Text("This device will stop syncing with your other devices. Your local data will remain saved on this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        PetalSyncBridge.leaveSyncChain(context)
                        isSyncActive = false
                        recoverySeed = null
                        pairedDevices = emptyList()
                        showLeaveConfirmation = false
                    }
                ) {
                    Text("Leave", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SyncStatusCard(
    isSyncActive: Boolean,
    deviceCount: Int,
    onViewSeed: () -> Unit,
    onStartChain: () -> Unit,
    onJoinChain: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassEffect(cornerRadius = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSyncActive) Icons.Rounded.Sync else Icons.Rounded.SyncDisabled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isSyncActive) "Sync Active" else "No Sync Chain",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isSyncActive) "$deviceCount connected device(s) in chain" else "Decentralized & End-to-End Encrypted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSyncActive) {
                Text(
                    text = "Petal uses a zero-knowledge 24-word recovery phrase instead of accounts. No email or password needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartChain,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start Chain", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onJoinChain,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Join Chain", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        IconSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DeviceItemCard(
    device: PetalSyncBridge.PairedDevice,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassEffect(cornerRadius = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (device.deviceType.lowercase()) {
                    "laptop", "computer", "desktop" -> Icons.Rounded.Laptop
                    "tablet" -> Icons.Rounded.Tablet
                    else -> Icons.Rounded.Smartphone
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name + if (device.isCurrentDevice) " (This Device)" else "",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (device.isCurrentDevice) "Active now" else "Last synced recently",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!device.isCurrentDevice) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove device",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun RecoverySeedDialog(
    seed: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val words = remember(seed) { seed.split("\\s+".toRegex()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Your 24-Word Recovery Phrase", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Write down these 24 words in order and store them safely. This phrase is the ONLY key to recover and sync your data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 24 words grid display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until (words.size + 1) / 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val idx1 = i * 2
                            val idx2 = i * 2 + 1
                            if (idx1 < words.size) {
                                Text(
                                    text = "${idx1 + 1}. ${words[idx1]}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (idx2 < words.size) {
                                Text(
                                    text = "${idx2 + 1}. ${words[idx2]}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clip = ClipData.newPlainText("Petal Recovery Phrase", seed)
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(clip)
                    Toast.makeText(context, "Recovery phrase copied to clipboard", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Words")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun StartSyncChainDialog(
    onDismiss: () -> Unit,
    onConfirm: (deviceName: String) -> Unit
) {
    var deviceName by remember { mutableStateOf("Android Phone") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start a New Sync Chain", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Name this device so you can identify it on other paired devices:")
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (deviceName.isNotBlank()) onConfirm(deviceName) }
            ) {
                Text("Generate Phrase")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun JoinSyncChainDialog(
    onDismiss: () -> Unit,
    onConfirm: (seed: String, deviceName: String) -> Unit
) {
    var seedInput by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("Android Phone") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Existing Chain", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the 24-word recovery phrase from your other device:")
                OutlinedTextField(
                    value = seedInput,
                    onValueChange = { seedInput = it },
                    label = { Text("24-word recovery phrase") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 4
                )
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (seedInput.isNotBlank() && deviceName.isNotBlank()) onConfirm(seedInput, deviceName) }
            ) {
                Text("Join Chain")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
