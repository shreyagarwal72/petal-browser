package com.petal.browser.compose.settings.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.petal.browser.PetalApplication
import com.petal.browser.data.repository.SettingsRepository
import com.petal.browser.data.repository.SettingsRepositoryImpl

object SettingsViewModelFactory : ViewModelProvider.Factory {

    val repository: SettingsRepository by lazy {
        val app = PetalApplication.instance ?: throw IllegalStateException("PetalApplication not initialized")
        SettingsRepositoryImpl(app)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AddressBarSettingsViewModel::class.java) ->
                AddressBarSettingsViewModel(repository) as T
            modelClass.isAssignableFrom(AppearanceSettingsViewModel::class.java) ->
                AppearanceSettingsViewModel(repository) as T
            modelClass.isAssignableFrom(DisplaySettingsViewModel::class.java) ->
                DisplaySettingsViewModel(repository) as T
            modelClass.isAssignableFrom(DownloadSettingsViewModel::class.java) ->
                DownloadSettingsViewModel(repository) as T
            modelClass.isAssignableFrom(MiscSettingsViewModel::class.java) ->
                MiscSettingsViewModel(repository) as T
            modelClass.isAssignableFrom(PrivacySettingsViewModel::class.java) ->
                PrivacySettingsViewModel(repository) as T
            modelClass.isAssignableFrom(SearchHomeSettingsViewModel::class.java) ->
                SearchHomeSettingsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
        }
    }
}
