package com.dangersense.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.app.usage.UsageStatsManager
import androidx.core.app.NotificationCompat
import com.dangersense.app.ai.LocalPrediction
import com.dangersense.app.ai.LocalPredictionEngine
import com.dangersense.app.data.CrisisDatabase
import com.dangersense.app.data.CrisisEvent
import com.dangersense.app.domain.AppUsageTracker
import com.dangersense.app.domain.LocalCrisisEngine
import com.dangersense.app.domain.Prediction
import com.dangersense.app.domain.SoundMapManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

class SensorTelemetryService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var tracker: AppUsageTracker
    private lateinit var engine: LocalCrisisEngine
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioJob: Job? = null
    private var loopJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var baseline = 0.9f
    private var lastTrigger = 0L
    @Volatile private var accelDev = 0f
    @Volatile private var gyroMag = 0f
    @Volatile private var micPaused = false
    private var voice: SensoryVoiceTriggerReceiver? = null
    private var voiceJob: Job? = null
    private var currentCrisisId = 0L
    private lateinit var localEngine: LocalPredictionEngine
    private var lastProactiveNotify = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (audioJob == null || audioJob?.isActive != true) startMic()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        tracker = AppUsageTracker(this).also { it.start() }
        engine = LocalCrisisEngine(CrisisDatabase.get(this).crisisDao())

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            ?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
            ?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        @Suppress("MissingPermission")
        runCatching {
            sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
                ?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }

        localEngine = LocalPredictionEngine(this).also { it.init() }
        refreshScreenTime()

        voice = SensoryVoiceTriggerReceiver(
            context = this,
            onPhrase = { handleVoicePhrase(it) },
            onSessionEnd = { resumeMic() }
        )

        startMic()
        startLoop()
        startVoiceCycle()
    }

    private fun pauseMic() { micPaused = true }

    private fun resumeMic() {
        voice?.endSession()
        micPaused = false
    }

    private fun startVoiceCycle() {
        voiceJob = scope.launch {
            while (isActive) {
                delay(9_000)
                if (voice?.busy == true) continue
                pauseMic()
                delay(250)
                voice?.beginTriggerListen()
            }
        }
    }

    private fun handleVoicePhrase(text: String) {
        val v = voice ?: return
        when (v.mode) {
            SensoryVoiceTriggerReceiver.Mode.TRIGGER -> {
                if (v.isTrigger(text)) beginCrisisProtocol()
                else resumeMic()
            }
            SensoryVoiceTriggerReceiver.Mode.TRIAGE -> {
                val type = v.classify(text)
                val id = currentCrisisId
                scope.launch {
                    if (id > 0) CrisisDatabase.get(applicationContext).crisisDao().assignType(id, type)
                }
                if (type == "Shutdown") _reducedStimuli.value = true
                v.deliverAdvice(type) { resumeMic() }
            }
            else -> resumeMic()
        }
    }

    private fun beginCrisisProtocol() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val e = CrisisEvent(
            timestamp = now,
            dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
            hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
            motionPeak = max(accelDev, gyroMag),
            decibels = max(_dbSlow.value, _db.value),
            foregroundPackage = tracker.foreground.value
        )
        scope.launch {
            currentCrisisId = engine.record(e)
            latest.value = e.copy(id = currentCrisisId)
            voice?.speakQuestionThenListen()
        }
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Monitoramento Sensorial", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Danger Sense ativo")
            .setContentText("Telemetria de sensores e microfone ativos")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, n,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun startMic() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val unprocessed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
                ?.toBoolean() == true
        } else false
        val source = when {
            unprocessed -> MediaRecorder.AudioSource.UNPROCESSED
            else -> MediaRecorder.AudioSource.VOICE_RECOGNITION
        }

        val rates = listOfNotNull(
            am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull(),
            48000, 44100, 32000, 16000
        ).distinct()

        var rec: AudioRecord? = null
        var bufSize = 0
        var rateUsed = 16000
        for (rate in rates) {
            val minBuf = AudioRecord.getMinBufferSize(
                rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) continue
            val size = minBuf * 2
            val candidate = try {
                AudioRecord(
                    source, rate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, size
                )
            } catch (e: SecurityException) {
                return
            } catch (e: IllegalArgumentException) {
                continue
            }
            if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                rec = candidate; bufSize = size; rateUsed = rate; break
            }
            candidate.release()
        }
        val r = rec ?: return
        audioRecord = r
        _sampleRate.value = rateUsed

        audioJob = scope.launch {
            val data = ShortArray(bufSize)
            var recording = false
            var fastSum = 0.0
            var fastCount = 0L
            var fastStart = SystemClock.elapsedRealtime()
            var slowSum = 0.0
            var slowCount = 0L
            var slowStart = fastStart
            var peakDb = 0f
            var peakAt = 0L

            while (isActive) {
                if (micPaused) {
                    if (recording) {
                        try { r.stop() } catch (_: Exception) {}
                        recording = false
                    }
                    delay(80)
                    continue
                }
                if (!recording) {
                    r.startRecording()
                    recording = true
                }
                val read = r.read(data, 0, data.size)
                if (read <= 0) continue

                var sum = 0.0
                for (i in 0 until read) {
                    val s = data[i].toInt()
                    if (s <= -32768 || s >= 32767) _clipped.value = _clipped.value + 1
                    val v = s / 32768.0
                    sum += v * v
                }
                fastSum += sum
                fastCount += read
                slowSum += sum
                slowCount += read

                val nowMs = SystemClock.elapsedRealtime()

                if (nowMs - fastStart >= FAST_WINDOW_MS) {
                    val rms = sqrt(fastSum / fastCount)
                    val db = toDbSpl(rms)
                    _db.value = db
                    SoundMapManager.sample(db, System.currentTimeMillis())
                    if (db >= peakDb || nowMs - peakAt > PEAK_HOLD_MS) {
                        peakDb = if (db >= peakDb) db else peakDb * 0.7f
                        peakAt = nowMs
                    }
                    _dbPeak.value = peakDb
                    fastSum = 0.0
                    fastCount = 0
                    fastStart = nowMs
                }

                if (nowMs - slowStart >= SLOW_WINDOW_MS) {
                    val rms = sqrt(slowSum / slowCount)
                    _dbSlow.value = toDbSpl(rms)
                    slowSum = 0.0
                    slowCount = 0
                    slowStart = nowMs
                }
            }
        }
    }

    private fun toDbSpl(rms: Double): Float =
        if (rms > 1e-7) (20.0 * log10(rms) + CAL_OFFSET_DB).toFloat() else 0f

    private fun startLoop() {
        loopJob = scope.launch {
            var tick = 0
            while (isActive) {
                delay(400)
                evaluate()
                if (++tick % 150 == 0) refreshScreenTime()
            }
        }
    }

    private fun refreshScreenTime() {
        runCatching {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val dayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
            val total = usm
                .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, System.currentTimeMillis())
                ?.sumOf { it.totalTimeInForeground } ?: 0L
            _screenMinutes.value = (total / 60_000L).toInt()
        }
    }

    private fun evaluate() {
        val motion = max(accelDev, gyroMag)
        _motion.value = motion
        val db = _db.value
        val dbAmbient = _dbSlow.value
        val now = System.currentTimeMillis()
        baseline = baseline * 0.9f + motion * 0.1f

        val moveSpike = motion > baseline * 2.4f && motion > 4.5f
        val noiseSpike = dbAmbient > 72f || db > 88f
        if (!moveSpike && !noiseSpike) return
        if (now - lastTrigger < 20_000) return
        lastTrigger = now

        val dbRecorded = max(dbAmbient, db)

        scope.launch {
            val p = engine.predict(motion, dbRecorded, now)
            _prediction.value = p

            val dao = CrisisDatabase.get(applicationContext).ingestaoDao()
            val last4h = dao.recentSince(now - 4 * 3_600_000L)
            val local = localEngine.prever(dbRecorded, _bpm.value, _screenMinutes.value, last4h)
            _localPrediction.value = local

            if (p.risk >= 0.7f && now - lastProactiveNotify > 5 * 60_000L) {
                lastProactiveNotify = now
                HyperActionReceiver.notifyProactive(
                    applicationContext, p.risk,
                    "${local.tipoCriseProvavel} · ${local.acaoPreventiva}"
                )
            }

            if (p.detected) {
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                val e = CrisisEvent(
                    timestamp = now,
                    dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                    hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
                    motionPeak = motion,
                    decibels = dbRecorded,
                    foregroundPackage = tracker.foreground.value
                )
                engine.record(e)
                latest.value = e
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        if (e.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            _temperature.value = e.values.getOrElse(0) { Float.NaN }
            return
        }
        if (e.sensor.type == Sensor.TYPE_HEART_RATE) {
            _bpm.value = e.values.getOrElse(0) { 0f }.toInt()
            return
        }
        val ax = e.values.getOrElse(0) { 0f }
        val ay = e.values.getOrElse(1) { 0f }
        val az = e.values.getOrElse(2) { 0f }
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelDev = kotlin.math.abs(magnitude - 9.81f)
        } else {
            gyroMag = magnitude
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        tracker.stop()
        loopJob?.cancel()
        voiceJob?.cancel()
        audioJob?.cancel()
        voice?.shutdown()
        localEngine.close()
        audioRecord?.run {
            try { stop() } catch (_: Exception) {}
            release()
        }
        audioRecord = null
        scope.cancel()
        _motion.value = 0f
        _db.value = 0f
        _dbSlow.value = 0f
        _dbPeak.value = 0f
        _sampleRate.value = 0
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "sensor_telemetry"
        private const val NOTIF_ID = 101
        private const val FAST_WINDOW_MS = 125
        private const val SLOW_WINDOW_MS = 1000
        private const val PEAK_HOLD_MS = 1500
        private const val CAL_OFFSET_DB = 90.0

        private val _motion = MutableStateFlow(0f)
        private val _db = MutableStateFlow(0f)
        private val _dbSlow = MutableStateFlow(0f)
        private val _dbPeak = MutableStateFlow(0f)
        private val _sampleRate = MutableStateFlow(0)
        private val _clipped = MutableStateFlow(0L)
        private val _temperature = MutableStateFlow(Float.NaN)
        private val _reducedStimuli = MutableStateFlow(false)
        private val _bpm = MutableStateFlow(0)
        private val _screenMinutes = MutableStateFlow(0)
        private val _localPrediction = MutableStateFlow<LocalPrediction?>(null)
        private val _prediction = MutableStateFlow<Prediction?>(null)
        val latest = MutableStateFlow<CrisisEvent?>(null)

        val motion: StateFlow<Float> = _motion
        val db: StateFlow<Float> = _db
        val dbSlow: StateFlow<Float> = _dbSlow
        val dbPeak: StateFlow<Float> = _dbPeak
        val sampleRate: StateFlow<Int> = _sampleRate
        val clipped: StateFlow<Long> = _clipped
        val temperature: StateFlow<Float> = _temperature
        val reducedStimuli: StateFlow<Boolean> = _reducedStimuli
        val bpm: StateFlow<Int> = _bpm
        val screenMinutes: StateFlow<Int> = _screenMinutes
        val localPrediction: StateFlow<LocalPrediction?> = _localPrediction
        val prediction: StateFlow<Prediction?> = _prediction

        fun start(context: Context) {
            val i = Intent(context, SensorTelemetryService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SensorTelemetryService::class.java))
        }

        fun restoreVision() {
            _reducedStimuli.value = false
        }

        fun requestPause() {
            _reducedStimuli.value = true
        }

        fun dismissLatest() {
            latest.value = null
        }
    }
}
