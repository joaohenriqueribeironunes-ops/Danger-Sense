package com.dangersense.app.domain

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dangersense.app.ai.DiagnosticoAdaptativo
import com.dangersense.app.ai.GenerativeModelService
import com.dangersense.app.data.CrisisDatabase
import com.dangersense.app.data.CrisisEvent
import com.dangersense.app.data.NeuroHabitsLog
import com.dangersense.app.data.RotinaEntity
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
    val bpm = SensorTelemetryService.bpm
    val screenMinutes = SensorTelemetryService.screenMinutes
    val localPrediction = SensorTelemetryService.localPrediction

    private val ingestaoDao = db.ingestaoDao()
    val ingestoes = ingestaoDao.recent()

    private val rotinaDao = db.rotinaDao()
    val rotina = rotinaDao.recente()

    private val gemini = GenerativeModelService()
    private val _analiseStatus = MutableStateFlow("")
    val analiseStatus: StateFlow<String> = _analiseStatus.asStateFlow()

    private val _diagnostico = MutableStateFlow<DiagnosticoAdaptativo?>(null)
    val diagnostico: StateFlow<DiagnosticoAdaptativo?> = _diagnostico.asStateFlow()

    private val _diagStatus = MutableStateFlow("")
    val diagStatus: StateFlow<String> = _diagStatus.asStateFlow()

    private var ultimaCriseRotinaId: Long? = null

    fun analisarIngestao(nome: String, categoria: String) {
        viewModelScope.launch {
            _analiseStatus.value = "analisando"
            gemini.analisar(nome, categoria)
                .onSuccess {
                    ingestaoDao.insert(it)
                    rotinaDao.insert(
                        RotinaEntity(
                            timestamp = it.timestamp,
                            categoria = RotinaEntity.ALIMENTACAO,
                            rotulo = it.nome,
                            metrica = it.risco_gatilho,
                            impactoTea = it.impacto_tea,
                            impactoTdah = it.impacto_tdah,
                            riscoGatilho = it.risco_gatilho,
                            detalhe = it.categoria,
                            validada = true
                        )
                    )
                    _analiseStatus.value = "ok"
                }
                .onFailure { e ->
                    _analiseStatus.value = "Falha: ${e.message}"
                }
        }
    }

    /** Busca 14 dias no Room, injeta no prompt do Gemini e abre o overlay. */
    fun gerarDiagnostico() {
        viewModelScope.launch {
            val desde = System.currentTimeMillis() - 14L * 86_400_000L
            val historico = rotinaDao.desde(desde)
            if (historico.size < 3) {
                _diagStatus.value =
                    "Histórico insuficiente (${historico.size} registros). Responda o diagnóstico diário por alguns dias."
                return@launch
            }
            _diagStatus.value = "analisando"
            val ingestoes = ingestaoDao.recentSince(desde)
            gemini.gerarDiagnosticoAdaptativo(historico, ingestoes)
                .onSuccess {
                    _diagnostico.value = it
                    _diagStatus.value = "ok"
                }
                .onFailure { e ->
                    _diagnostico.value = gemini.diagnosticoLocal(historico)
                    _diagStatus.value = "offline · ${e.message?.take(60)}"
                }
        }
    }

    fun fecharDiagnostico() {
        _diagnostico.value = null
    }

    fun requestPause() = SensorTelemetryService.requestPause()
    fun dismissAlert() {
        _alert.value = false
        SensorTelemetryService.dismissLatest()
        ultimaCriseRotinaId?.let { id ->
            viewModelScope.launch { rotinaDao.invalidar(id) }
        }
        ultimaCriseRotinaId = null
    }

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

            val agora = System.currentTimeMillis()
            rotinaDao.insert(
                RotinaEntity(
                    timestamp = agora,
                    categoria = RotinaEntity.SONO,
                    rotulo = "Sono (questionário)",
                    metrica = sleepHours,
                    duracaoMinutos = sleepMin,
                    detalhe = "diagnóstico diário",
                    validada = true
                )
            )
            rotinaDao.insert(
                RotinaEntity(
                    timestamp = agora,
                    categoria = RotinaEntity.ALIMENTACAO,
                    rotulo = diet.ifBlank { "Sem refeição registrada" }.take(60),
                    metrica = dietScore,
                    impactoTea = dietScore,
                    impactoTdah = dietScore,
                    riscoGatilho = (-dietScore).coerceIn(0f, 1f),
                    detalhe = "diagnóstico diário",
                    validada = true
                )
            )
            rotinaDao.insert(
                RotinaEntity(
                    timestamp = agora,
                    categoria = RotinaEntity.ATIVIDADE,
                    rotulo = activities.ifBlank { "Sem atividades registradas" }.take(60),
                    metrica = DietAudioAnalysisEngine.activityRisk(activities) * 100f,
                    interacoesSociais = contarInteracoes(activities),
                    riscoGatilho = DietAudioAnalysisEngine.activityRisk(activities),
                    detalhe = "diagnóstico diário",
                    validada = true
                )
            )

            _diagnosticResult.value = prob
        }
    }

    private fun contarInteracoes(texto: String): Int {
        val t = texto.lowercase()
        return listOf(
            "reunião", "reuniao", "festa", "multidão", "multidao", "trabalho",
            "escola", "visita", "amigo", "social", "conversa", "grupo"
        ).count { t.contains(it) }
    }

    /** Crise detectada pelos sensores vira registro de rotina (validado até "Falso Alarme"). */
    private fun registrarCrise(e: CrisisEvent) {
        viewModelScope.launch {
            ultimaCriseRotinaId = rotinaDao.insert(
                RotinaEntity(
                    timestamp = e.timestamp,
                    categoria = RotinaEntity.CRISE,
                    rotulo = e.crisisType ?: "Quase-crise",
                    metrica = e.decibels,
                    riscoGatilho = (e.motionPeak / 3f).coerceIn(0f, 1f),
                    detalhe = "%.1fg · %.0f dB · %s".format(
                        e.motionPeak, e.decibels, e.foregroundPackage
                    ),
                    validada = true
                )
            )
        }
    }

    init {
        tracker.start()
        viewModelScope.launch {
            SensorTelemetryService.latest.collect { e ->
                if (e != null) {
                    SensorTelemetryService.latest.value = null
                    registrarCrise(e)
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
