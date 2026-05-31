package com.tvanime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvanime.app.data.settings.PlaylistSettingsStore
import com.tvanime.app.data.settings.PlaylistSource
import com.tvanime.app.data.settings.PlaylistSyncConfig
import com.tvanime.app.data.settings.PlaylistSyncScheduler
import com.tvanime.app.data.settings.RecurringSitesStore
import com.tvanime.app.data.settings.RecurringSitesSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedSource: PlaylistSource = PlaylistSource.DEMO,
    val remoteUrl: String = "",
    val recurringSitesText: String = "",
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: PlaylistSettingsStore,
    private val syncScheduler: PlaylistSyncScheduler,
    private val recurringSitesStore: RecurringSitesStore,
    private val recurringSitesSyncScheduler: RecurringSitesSyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val config = settingsStore.getConfig()
        _uiState.value = _uiState.value.copy(
            selectedSource = config.source,
            remoteUrl = config.remoteUrl,
            recurringSitesText = recurringSitesStore.getRawText()
        )
    }

    fun selectSource(source: PlaylistSource) {
        _uiState.value = _uiState.value.copy(selectedSource = source, error = null, message = null)
    }

    fun updateRemoteUrl(value: String) {
        _uiState.value = _uiState.value.copy(remoteUrl = value, error = null, message = null)
    }

    fun updateRecurringSites(value: String) {
        _uiState.value = _uiState.value.copy(recurringSitesText = value, error = null, message = null)
    }

    fun save() {
        val state = _uiState.value
        if (state.selectedSource == PlaylistSource.REMOTE_URL && state.remoteUrl.isBlank()) {
            _uiState.value = state.copy(error = "Ingresa una URL M3U valida.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null, message = null)

            val config = PlaylistSyncConfig(
                source = state.selectedSource,
                remoteUrl = state.remoteUrl.trim()
            )

            settingsStore.saveConfig(config)
            recurringSitesStore.saveRawText(state.recurringSitesText)
            syncScheduler.schedulePeriodicSync(config)
            syncScheduler.requestImmediateSync(config)
            recurringSitesSyncScheduler.schedulePeriodicSync()
            recurringSitesSyncScheduler.requestImmediateSync()

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                message = "Configuracion guardada. Sincronizacion M3U y sitios recurrentes lanzada."
            )
        }
    }
}
