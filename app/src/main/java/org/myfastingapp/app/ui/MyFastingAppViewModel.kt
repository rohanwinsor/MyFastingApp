package org.myfastingapp.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.myfastingapp.app.MyFastingAppApplication
import org.myfastingapp.app.data.FastRepository
import org.myfastingapp.app.data.SettingsStore
import org.myfastingapp.app.domain.FastPlan
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.FastStats
import org.myfastingapp.app.domain.StatsCalculator
import org.myfastingapp.app.domain.UserSettings
import org.myfastingapp.app.domain.WeightEntry
import org.myfastingapp.app.domain.WeightTrend
import org.myfastingapp.app.domain.WeightTrendCalculator
import org.myfastingapp.app.domain.WeightUnit
import org.myfastingapp.app.notify.FastReminderScheduler
import org.myfastingapp.app.widget.MyFastingAppWidgetProvider

data class MyFastingAppUiState(
    val sessions: List<FastSession> = emptyList(),
    val activeSession: FastSession? = null,
    val weights: List<WeightEntry> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val stats: FastStats = FastStats(),
    val weightTrend: WeightTrend = WeightTrend(),
) {
    val selectedPlan: FastPlan = settings.defaultPlan
}

class MyFastingAppViewModel(
    private val application: MyFastingAppApplication,
    private val repository: FastRepository,
    private val settingsStore: SettingsStore,
    private val reminderScheduler: FastReminderScheduler,
) : ViewModel() {
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    val uiState: StateFlow<MyFastingAppUiState> = combine(
        repository.sessions,
        repository.weights,
        settingsStore.settings,
    ) { sessions, weights, settings ->
        MyFastingAppUiState(
            sessions = sessions,
            activeSession = sessions.firstOrNull { it.isActive },
            weights = weights,
            settings = settings,
            stats = StatsCalculator.calculate(sessions),
            weightTrend = WeightTrendCalculator.calculate(weights, settings.targetWeightKg),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyFastingAppUiState())

    init {
        refreshExternalSurfaces()
    }

    fun refreshExternalSurfaces() {
        viewModelScope.launch {
            refreshReminderAndWidget()
        }
    }

    fun startFast(plan: FastPlan = uiState.value.selectedPlan) {
        viewModelScope.launch {
            runCatching {
                repository.startFast(plan)
                refreshReminderAndWidget()
            }.onFailure { showMessage(it.message ?: "Could not start fast.") }
        }
    }

    fun endFast() {
        viewModelScope.launch {
            runCatching {
                repository.endActiveFast()
                refreshReminderAndWidget()
            }.onFailure { showMessage(it.message ?: "Could not end fast.") }
        }
    }

    fun deleteFast(id: Long) {
        viewModelScope.launch {
            repository.deleteFast(id)
            refreshReminderAndWidget()
        }
    }

    fun editFast(id: Long, startEpochMillis: Long, endEpochMillis: Long) {
        viewModelScope.launch {
            runCatching {
                repository.editFast(id, startEpochMillis, endEpochMillis)
                refreshReminderAndWidget()
            }.onFailure { showMessage(it.message ?: "Could not edit fast.") }
        }
    }

    fun addCompletedFast(planName: String, targetSeconds: Long, startEpochMillis: Long, endEpochMillis: Long) {
        viewModelScope.launch {
            runCatching {
                repository.addCompletedFast(planName, targetSeconds, startEpochMillis, endEpochMillis)
                refreshReminderAndWidget()
            }.onSuccess {
                showMessage("Fast logged.")
            }.onFailure { showMessage(it.message ?: "Could not log fast.") }
        }
    }

    fun addWeight(weightKg: Double, recordedEpochMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            runCatching {
                repository.addWeight(weightKg, recordedEpochMillis)
            }.onFailure { showMessage(it.message ?: "Could not save weight.") }
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            repository.deleteWeight(id)
        }
    }

    fun selectPlan(planId: String) {
        viewModelScope.launch {
            settingsStore.setDefaultPlan(planId)
        }
    }

    fun setCustomPlan(minutes: Int) {
        viewModelScope.launch {
            settingsStore.setCustomFastingMinutes(minutes)
        }
    }

    fun setReminders(enabled: Boolean, leadMinutes: Int) {
        viewModelScope.launch {
            settingsStore.setReminders(enabled, leadMinutes)
            refreshReminderAndWidget()
        }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            settingsStore.setWeightUnit(unit)
        }
    }

    fun setTargetWeightKg(weightKg: Double?) {
        viewModelScope.launch {
            runCatching {
                settingsStore.setTargetWeightKg(weightKg)
            }.onSuccess {
                showMessage("Target weight saved.")
            }.onFailure { showMessage(it.message ?: "Could not save target weight.") }
        }
    }

    suspend fun exportJson(): String {
        return repository.exportBackupJson(settingsStore.settings.first())
    }

    suspend fun exportCsv(): String {
        return repository.exportSessionsCsv()
    }

    fun importJson(rawJson: String) {
        viewModelScope.launch {
            runCatching {
                repository.importBackupJson(rawJson)
                refreshReminderAndWidget()
                showMessage("Backup imported.")
            }.onFailure { showMessage(it.message ?: "Could not import backup.") }
        }
    }

    fun deleteAllLocalData() {
        viewModelScope.launch {
            runCatching {
                repository.deleteAllLocalData()
                refreshReminderAndWidget()
            }.onSuccess {
                showMessage("Local data deleted from this device.")
            }.onFailure { showMessage(it.message ?: "Could not delete local data.") }
        }
    }

    private fun showMessage(message: String) {
        viewModelScope.launch { _messages.emit(message) }
    }

    private suspend fun refreshReminderAndWidget() {
        repository.repairActivePlanTarget()
        val active = repository.activeSession.first()
        val settings = settingsStore.settings.first()
        reminderScheduler.schedule(active, settings)
        MyFastingAppWidgetProvider.updateWidgets(application)
    }

    companion object {
        fun factory(
            application: MyFastingAppApplication,
            repository: FastRepository,
            settingsStore: SettingsStore,
            reminderScheduler: FastReminderScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MyFastingAppViewModel(application, repository, settingsStore, reminderScheduler) as T
            }
        }
    }
}
