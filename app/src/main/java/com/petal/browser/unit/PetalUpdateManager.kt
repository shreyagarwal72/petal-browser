package com.petal.browser.unit

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.preference.PreferenceManager
import com.petal.browser.download.PetalDownloadEngine
import com.petal.browser.view.PetalToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Decoupled, persistent singleton manager for Petal Browser updates.
 * Powered natively by Petal's Chromium Download Engine.
 */
object PetalUpdateManager {

    private const val TAG = "PetalUpdateManager"

    private const val PREF_KEY_UPDATE_DOWNLOAD_ID = "sp_active_update_download_id"
    private const val PREF_KEY_UPDATE_FILE_PATH = "sp_active_update_file_path"
    private const val PREF_KEY_UPDATE_VERSION = "sp_active_update_version"
    private const val PREF_KEY_UPDATE_URL = "sp_active_update_download_url"

    sealed class UpdateDownloadState {
        object Idle : UpdateDownloadState()
        data class Downloading(val progress: Int, val version: String, val downloadId: Long) : UpdateDownloadState()
        data class Paused(val progress: Int, val version: String, val downloadId: Long) : UpdateDownloadState()
        data class Completed(val version: String, val apkFile: File) : UpdateDownloadState()
        data class Failed(val error: String, val version: String) : UpdateDownloadState()
    }

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    @Synchronized
    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val sp = PreferenceManager.getDefaultSharedPreferences(appContext)
        val activeDownloadId = sp.getLong(PREF_KEY_UPDATE_DOWNLOAD_ID, -1L)
        val activeVersion = sp.getString(PREF_KEY_UPDATE_VERSION, null)
        val activePath = sp.getString(PREF_KEY_UPDATE_FILE_PATH, null)

        if (activeDownloadId != -1L && activeVersion != null && activePath != null) {
            val file = File(activePath)
            if (file.exists() && file.length() > 0) {
                _downloadState.value = UpdateDownloadState.Completed(activeVersion, file)
            }
        }
    }

    @Synchronized
    fun startUpdateDownload(context: Context, downloadUrl: String, version: String): Long {
        val appContext = context.applicationContext
        val fileName = "Petal-Browser-$version.apk"
        val downloadDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: appContext.filesDir
        val apkFile = File(downloadDir, fileName)

        val id = PetalDownloadEngine.getInstance(appContext).enqueueDownload(
            downloadUrl,
            fileName,
            null,
            "application/vnd.android.package-archive",
            null,
            null
        )

        val sp = PreferenceManager.getDefaultSharedPreferences(appContext)
        sp.edit()
            .putLong(PREF_KEY_UPDATE_DOWNLOAD_ID, id)
            .putString(PREF_KEY_UPDATE_FILE_PATH, apkFile.absolutePath)
            .putString(PREF_KEY_UPDATE_VERSION, version)
            .putString(PREF_KEY_UPDATE_URL, downloadUrl)
            .apply()

        _downloadState.value = UpdateDownloadState.Downloading(100, version, id)
        return id
    }

    fun pauseUpdate(context: Context) {}
    fun resumeUpdate(context: Context) {}
    fun cancelUpdate(context: Context) {
        _downloadState.value = UpdateDownloadState.Idle
    }
}
