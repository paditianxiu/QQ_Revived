package me.padi.qqlite.revived.shared.viewmodel.module

import androidx.lifecycle.ViewModel
import io.github.libxposed.service.XposedService
import me.padi.qqlite.revived.shared.model.module.MainUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    fun updateService(service: XposedService?) {
        _uiState.value = if (service == null) {
            MainUiState(binderText = "Binder is null")
        } else {
            service.toUiState()
        }
    }

    fun updateScope(scope: List<String>?) {
        _uiState.value = _uiState.value.copy(scopeText = "Scope: $scope")
    }

    private fun XposedService.toUiState(): MainUiState {
        val properties = frameworkProperties.toPropertyNames()
        val runningTargets = if (apiVersion >= 102) {
            runningTargets.map { it.processName }
        } else {
            emptyList()
        }

        return MainUiState(
            binderText = "Binder acquired",
            apiText = "API $apiVersion",
            frameworkText = "Framework $frameworkName",
            frameworkVersionText = "Framework version $frameworkVersion",
            frameworkVersionCodeText = "Framework version code $frameworkVersionCode",
            frameworkPropertiesText = "Framework properties: $properties",
            scopeText = "Scope: $scope",
            processText = if (apiVersion >= 102) "Processes: $runningTargets" else "",
            reloadEnabled = apiVersion >= 102
        )
    }

    private fun Long.toPropertyNames(): List<String> {
        val names = mutableListOf<String>()
        if (and(XposedService.PROP_CAP_SYSTEM) != 0L) names.add("PROP_CAP_SYSTEM")
        if (and(XposedService.PROP_CAP_REMOTE) != 0L) names.add("PROP_CAP_REMOTE")
        if (and(XposedService.PROP_RT_API_PROTECTION) != 0L) names.add("PROP_RT_API_PROTECTION")
        return names
    }
}
