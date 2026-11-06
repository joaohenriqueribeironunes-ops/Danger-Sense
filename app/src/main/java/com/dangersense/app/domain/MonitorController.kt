package com.dangersense.app.domain

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangersense.app.data.CrisisDatabase
import com.dangersense.app.service.SensorTelemetryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.settingsStore by preferencesDataStore("settings")

class MonitorController private constructor(private val context: Context) {

    private val key = booleanPreferencesKey("monitoring")
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    init {
        kotlinx.coroutines.GlobalScope.launch {
            context.settingsStore.data.map { it[key] ?: false }.collect { _enabled.value = it }
        }
    }

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        kotlinx.coroutines.GlobalScope.launch {
            context.settingsStore.edit { it[key] = value }
        }
        if (value) SensorTelemetryService.start(context)
        else SensorTelemetryService.stop(context)
    }

    fun toggle() = setEnabled(!_enabled.value)

    companion object {
        @Volatile private var instance: MonitorController? = null
        fun get(context: Context): MonitorController =
            instance ?: synchronized(this) {
                instance ?: MonitorController(context.applicationContext).also { instance = it }
            }
    }
}

class MainViewModel(context: Context) : ViewModel() {

    private val dao = CrisisDatabase.get(context).crisisDao()
    val engine = LocalCrisisEngine(dao)
    val tracker = AppUsageTracker(context)
    val controller = MonitorController.get(context)

    val events = dao.recent(30)
    val motion = SensorTelemetryService.motion
    val db = SensorTelemetryService.db
    val dbPeak = SensorTelemetryService.dbPeak
    val dbAmbient = SensorTelemetryService.dbSlow
    val sampleRate = SensorTelemetryService.sampleRate
    val prediction = SensorTelemetryService.prediction

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    private val _alert = MutableStateFlow(false)
    val alert: StateFlow<Boolean> = _alert.asStateFlow()

    fun onMessage(text: String) {
        if (text.isBlank()) return
        _messages.value = _messages.value + text
    }

    fun triggerAlert() {
        _alert.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(12_000)
            _alert.value = false
        }
    }

    init {
        tracker.start()
        viewModelScope.launch {
            SensorTelemetryService.latest.collect { e ->
                if (e != null) {
                    SensorTelemetryService.latest.value = null
                    triggerAlert()
                }
            }
        }
    }

    override fun onCleared() {
        tracker.stop()
        super.onCleared()
    }
}
