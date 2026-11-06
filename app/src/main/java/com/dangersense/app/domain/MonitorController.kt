package com.dangersense.app.domain

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangersense.app.data.CrisisDatabase
import com.dangersense.app.data.NeuroHabitsLog
import com.dangersense.app.service.DailyDiagnosticReceiver
import com.dangersense.app.service.SensorTelemetryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val db = CrisisDatabase.get(context)
    private val dao = db.crisisDao()
    private val habitsDao = db.neuroHabitsDao()
    val engine = LocalCrisisEngine(dao)
    val tracker = AppUsageTracker(context)
    val controller = MonitorController.get(context)
    private val appContext = context.applicationContext

    val events = dao.recent(30)
    val motion = SensorTelemetryService.motion
    val dbLevel = SensorTelemetryService.db
    val dbPeak = SensorTelemetryService.dbPeak
    val dbAmbient = SensorTelemetryService.dbSlow
    val sampleRate = SensorTelemetryService.sampleRate
    val temperature = SensorTelemetryService.temperature
    val reducedStimuli = SensorTelemetryService.reducedStimuli
    val prediction = SensorTelemetryService.prediction
    val soundPeak = SoundMapManager.peakDb
    val soundPeakAt = SoundMapManager.peakAt

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    private val _alert = MutableStateFlow(false)
    val alert: StateFlow<Boolean> = _alert.asStateFlow()

    private val _diagnosticResult = MutableStateFlow<Float?>(null)
    val diagnosticResult: StateFlow<Float?> = _diagnosticResult.asStateFlow()

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

    fun restoreVision() = SensorTelemetryService.restoreVision()

    fun runDiagnostic(sleepHoursText: String, diet: String, activities: String, scheduleHour: Int?) {
        val sleepHours = sleepHoursText.replace(',', '.').toFloatOrNull() ?: 7f
        val sleepMin = (sleepHours * 60f).toInt().coerceIn(0, 1440)
        val peak = SoundMapManager.peakDb.value
        val peakAt = SoundMapManager.peakAt.value
        val temp = temperature.value.let { if (it.isNaN()) 0f else it }
        val dietScore = DietAudioAnalysisEngine.neuroImpact(diet)
        scheduleHour?.let { DailyDiagnosticReceiver.schedule(appContext, it) }

        viewModelScope.launch {
            val crises = dao.countSince(System.currentTimeMillis() - 86_400_000L)
            val historicalRate = (crises / 3f).coerceIn(0f, 1f)
            val prob = CrisisPredictionEngine.probability(
                DiagnosticInput(
                    sleepMinutes = sleepMin,
                    dietText = diet,
                    activitiesText = activities,
                    peakDb = peak,
                    temperature = temp,
                    historicalCrisisRate = historicalRate
                )
            )
            habitsDao.insert(
                NeuroHabitsLog(
                    timestamp = System.currentTimeMillis(),
                    dateString = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(Date()),
                    sleepDurationMinutes = sleepMin,
                    dietSummary = diet,
                    dietNeuroImpactScore = dietScore,
                    activitiesSummary = activities,
                    maximalDetectedDb = peak,
                    maximalDbTimestamp = peakAt,
                    registeredTemperature = temp,
                    predictedCrisisProbability = prob,
                    actualCrisisOccurred = crises > 0,
                    assignedCrisisType = dao.lastCrisisType()
                )
            )
            _diagnosticResult.value = prob
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
