package com.example.myapplication.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.database.AppEntity
import com.example.myapplication.database.GuardianHubDatabase
import com.example.myapplication.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GuardianViewModel(
    private val appRepository: AppRepository
) : ViewModel() {
    private val _importedApps = MutableStateFlow<List<AppEntity>>(emptyList())
    val importedApps: StateFlow<List<AppEntity>> = _importedApps.asStateFlow()

    private val _allApps = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val allApps: StateFlow<List<Pair<String, String>>> = _allApps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showVault = MutableStateFlow(false)
    val showVault: StateFlow<Boolean> = _showVault.asStateFlow()

    init {
        loadImportedApps()
        loadAllApps()
    }

    private fun loadImportedApps() {
        viewModelScope.launch {
            appRepository.getImportedApps().collect { apps ->
                _importedApps.value = apps
            }
        }
    }

    private fun loadAllApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _allApps.value = appRepository.getAllInstalledApps()
            _isLoading.value = false
        }
    }

    fun importApp(packageName: String) {
        viewModelScope.launch {
            try {
                appRepository.importApp(packageName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            appRepository.removeApp(packageName)
        }
    }

    fun toggleVault() {
        _showVault.value = !_showVault.value
    }

    class Factory(
        private val appRepository: AppRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GuardianViewModel(appRepository) as T
        }
    }
}

fun createGuardianViewModel(context: Context): GuardianViewModel {
    val database = GuardianHubDatabase.getInstance(context)
    val appDao = database.appDao()
    val appRepository = AppRepository(appDao, context)
    return GuardianViewModel(appRepository)
}
