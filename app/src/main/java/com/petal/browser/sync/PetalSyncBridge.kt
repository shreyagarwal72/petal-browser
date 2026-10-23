package com.petal.browser.sync

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import java.lang.reflect.Method
import java.util.Date

/**
 * PetalSyncBridge
 * ─────────────────────────────────────────────────────────────────────────────
 * Seamless Petal Sync & Recovery Architecture.
 *
 * Replaces external account requirements with Petal's zero-knowledge 24-word
 * recovery seed & QR-code device pairing chain.
 *
 * Communicates natively with the underlying Chromium/Petal-Core sync engine:
 *   - Device list discovery & remote device deletion.
 *   - 24-word seed generation, verification, and import.
 *   - Local preferences caching for offline capability.
 */
object PetalSyncBridge {

    private const val TAG = "PetalSyncBridge"
    private const val PREF_SYNC_CHAIN_SEED = "sp_petal_sync_chain_seed"
    private const val PREF_SYNC_DEVICE_NAME = "sp_petal_sync_device_name"

    data class PairedDevice(
        val guid: String,
        val name: String,
        val isCurrentDevice: Boolean,
        val deviceType: String,
        val lastUpdated: Date?
    )

    // JNI reflection handles for native device & sync controller
    private var nativeSyncDevicesClass: Class<*>? = null
    private var getDevicesInstanceMethod: Method? = null
    private var getDeviceListMethod: Method? = null
    private var deleteDeviceMethod: Method? = null

    fun init(context: Context) {
        resolveNativeHandles()
    }

    init {
        resolveNativeHandles()
    }

    private fun resolveNativeHandles() {
        try {
            val cls = Class.forName("org.chromium.chrome.browser.sync.BraveSyncDevices")
            nativeSyncDevicesClass = cls
            getDevicesInstanceMethod = cls.getMethod("get")
            getDeviceListMethod = cls.getMethod("getSyncDeviceList")
            deleteDeviceMethod = cls.getMethod("deleteDevice", String::class.java)
            Log.d(TAG, "Successfully resolved native Petal Sync engine JNI handles")
        } catch (t: Throwable) {
            Log.w(TAG, "Native sync engine JNI unavailable (standalone mode): ${t.message}")
        }
    }

    /**
     * Checks if this browser is currently enrolled in a Petal Sync Chain.
     */
    @JvmStatic
    fun isSyncActive(context: Context): Boolean {
        val sp = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return !sp.getString(PREF_SYNC_CHAIN_SEED, null).isNullOrBlank()
    }

    /**
     * Returns the 24-word recovery seed for the current sync chain.
     */
    @JvmStatic
    fun getRecoverySeed(context: Context): String? {
        val sp = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return sp.getString(PREF_SYNC_CHAIN_SEED, null)
    }

    /**
     * Initializes a brand new Petal Sync Chain and creates the 24-word seed.
     */
    @JvmStatic
    fun createSyncChain(context: Context, deviceName: String): String {
        val seed = PetalSyncChainManager.createSyncChain(context, deviceName)
        val sp = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        sp.edit()
            .putString(PREF_SYNC_CHAIN_SEED, seed)
            .putString(PREF_SYNC_DEVICE_NAME, deviceName)
            .apply()
        Log.i(TAG, "Initialized new Petal Sync Chain for device: $deviceName")
        return seed
    }

    /**
     * Joins an existing Petal Sync Chain using a 24-word recovery seed or QR code string.
     */
    @JvmStatic
    fun joinSyncChain(context: Context, seed: String, deviceName: String): Boolean {
        val trimmed = seed.trim()
        val words = trimmed.split("\\s+".toRegex())
        if (words.size < 12) {
            Log.w(TAG, "Invalid sync recovery seed: insufficient word count (${words.size})")
            return false
        }

        val success = PetalSyncChainManager.joinSyncChain(context, trimmed, deviceName)
        if (success) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            sp.edit()
                .putString(PREF_SYNC_CHAIN_SEED, trimmed)
                .putString(PREF_SYNC_DEVICE_NAME, deviceName)
                .apply()
            Log.i(TAG, "Joined Petal Sync Chain with device name: $deviceName")
        }
        return success
    }

    /**
     * Disconnects this device and leaves the current Petal Sync Chain.
     */
    @JvmStatic
    fun leaveSyncChain(context: Context) {
        PetalSyncChainManager.leaveSyncChain(context)
        val sp = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        sp.edit()
            .remove(PREF_SYNC_CHAIN_SEED)
            .remove(PREF_SYNC_DEVICE_NAME)
            .apply()
        Log.i(TAG, "Device left Petal Sync Chain")
    }

    /**
     * Retrieves the real-time list of devices connected to this Petal Sync Chain.
     */
    @JvmStatic
    fun getPairedDevices(context: Context): List<PairedDevice> {
        val devices = mutableListOf<PairedDevice>()
        try {
            if (getDevicesInstanceMethod != null && getDeviceListMethod != null) {
                val instance = getDevicesInstanceMethod!!.invoke(null)
                val rawList = getDeviceListMethod!!.invoke(instance) as? List<*>
                if (rawList != null) {
                    for (item in rawList) {
                        if (item == null) continue
                        val itemCls = item.javaClass
                        val name = itemCls.getField("mName").get(item) as? String ?: "Petal Device"
                        val guid = itemCls.getField("mGuid").get(item) as? String ?: ""
                        val isCurrent = itemCls.getField("mIsCurrentDevice").getBoolean(item)
                        val type = itemCls.getField("mType").get(item) as? String ?: "phone"
                        val timestamp = itemCls.getField("mLastUpdatedTimestamp").get(item) as? Date
                        devices.add(PairedDevice(guid, name, isCurrent, type, timestamp))
                    }
                }
            }
        } catch (t: Throwable) {
            Log.d(TAG, "Could not fetch native devices list: ${t.message}")
        }

        if (devices.isEmpty() && isSyncActive(context)) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            val currentName = sp.getString(PREF_SYNC_DEVICE_NAME, "This Phone") ?: "This Phone"
            devices.add(
                PairedDevice(
                    guid = "local_device",
                    name = currentName,
                    isCurrentDevice = true,
                    deviceType = "phone",
                    lastUpdated = Date()
                )
            )
        }
        return devices
    }

    /**
     * Removes a remote device from this sync chain by GUID.
     */
    @JvmStatic
    fun removeDevice(guid: String): Boolean {
        return try {
            if (getDevicesInstanceMethod != null && deleteDeviceMethod != null) {
                val instance = getDevicesInstanceMethod!!.invoke(null)
                deleteDeviceMethod!!.invoke(instance, guid)
                Log.i(TAG, "Removed device $guid from sync chain")
                true
            } else false
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to remove device $guid: ${t.message}")
            false
        }
    }
}
