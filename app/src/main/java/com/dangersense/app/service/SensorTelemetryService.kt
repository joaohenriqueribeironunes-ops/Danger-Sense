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
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dangersense.app.data.CrisisDatabase
import com.dangersense.app.data.CrisisEvent
import com.dangersense.app.domain.AppUsageTracker
import com.dangersense.app.domain.LocalCrisisEngine
import com.dangersense.app.domain.Prediction
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

    override fun onBind(intent: Intent?): IBinder? = null

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

        startMic()
        startLoop()
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
            .setContentTitle("SenseCalm ativo")
            .setContentText("Monitorando sensores em tempo real")
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
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) return
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val buf = minBuf * 2
        audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buf
            )
        } catch (e: SecurityException) {
            return
        }
        val rec = audioRecord ?: return
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            audioRecord = null
            return
        }
        rec.startRecording()
        audioJob = scope.launch {
            val data = ShortArray(buf)
            while (isActive) {
                val read = rec.read(data, 0, data.size)
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        val v = data[i] / 32768.0
                        sum += v * v
                    }
                    val rms = sqrt(sum / read)
                    _db.value = if (rms > 1e-7) (20.0 * log10(rms) + 90.0).toFloat() else 0f
                }
            }
        }
    }

    private fun startLoop() {
        loopJob = scope.launch {
            while (isActive) {
                delay(400)
                evaluate()
            }
        }
    }

    private fun evaluate() {
        val motion = max(accelDev, gyroMag)
        _motion.value = motion
        val db = _db.value
        val now = System.currentTimeMillis()
        baseline = baseline * 0.9f + motion * 0.1f

        val moveSpike = motion > baseline * 2.4f && motion > 4.5f
        val noiseSpike = db > 75f
        if (!moveSpike && !noiseSpike) return
        if (now - lastTrigger < 20_000) return
        lastTrigger = now

        scope.launch {
            val p = engine.predict(motion, db, now)
            _prediction.value = p
            if (p.detected) {
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                val e = CrisisEvent(
                    timestamp = now,
                    dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                    hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
                    motionPeak = motion,
                    decibels = db,
                    foregroundPackage = tracker.foreground.value
                )
                engine.record(e)
                latest.value = e
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
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
        audioJob?.cancel()
        audioRecord?.run {
            try { stop() } catch (_: Exception) {}
            release()
        }
        audioRecord = null
        scope.cancel()
        _motion.value = 0f
        _db.value = 0f
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "sensor_telemetry"
        private const val NOTIF_ID = 101
        private const val SAMPLE_RATE = 16000

        private val _motion = MutableStateFlow(0f)
        private val _db = MutableStateFlow(0f)
        private val _prediction = MutableStateFlow<Prediction?>(null)
        val latest = MutableStateFlow<CrisisEvent?>(null)

        val motion: StateFlow<Float> = _motion
        val db: StateFlow<Float> = _db
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
    }
}
