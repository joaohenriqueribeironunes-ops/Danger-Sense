package com.dangersense.app.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import kotlin.math.sin

class SensoryVoiceTriggerReceiver(
    private val context: Context,
    private val onPhrase: (String) -> Unit,
    private val onSessionEnd: () -> Unit
) {

    enum class Mode { IDLE, TRIGGER, TRIAGE }

    var mode: Mode = Mode.IDLE
        private set
    val busy: Boolean get() = mode != Mode.IDLE

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val pt = Locale("pt", "BR")

    fun beginTriggerListen() {
        mode = Mode.TRIGGER
        listen()
    }

    fun beginTriageListen() {
        mode = Mode.TRIAGE
        listen()
    }

    fun endSession() {
        mode = Mode.IDLE
        main.post { recognizer?.destroy(); recognizer = null }
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            endSession()
            onSessionEnd()
            return
        }
        main.post {
            recognizer?.destroy()
            val r = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = r
            r.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    main.post { recognizer?.destroy(); recognizer = null }
                    onPhrase(text)
                }
                override fun onError(error: Int) {
                    main.post { recognizer?.destroy(); recognizer = null }
                    onPhrase("")
                }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            })
        }
    }

    fun isTrigger(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("estou em crise") || t.contains("to em crise") ||
                t.contains("tô em crise") || t.contains("estou numa crise")
    }

    fun classify(text: String): String {
        val t = text.lowercase()
        return when {
            listOf("sobrecarga", "sensorial", "barulho", "luz", "som", "estímulo", "estimulo")
                .any { t.contains(it) } -> "Sobrecarga Sensorial"
            listOf("meltdown", "explos", "grito", "choro", "colapso", "perdi o controle", "explode")
                .any { t.contains(it) } -> "Meltdown"
            listOf("shutdown", "deslig", "fechar", "paralis", "mudo", "vazio", "trancar")
                .any { t.contains(it) } -> "Shutdown"
            else -> "Sobrecarga Sensorial"
        }
    }

    fun askCrisisType(onAnswer: (String) -> Unit) {
        speak(
            "Sentidos sobrecarregados. Eu estou aqui com você. " +
            "Qual é o tipo de crise que você está enfrentando agora? " +
            "Sobrecarga sensorial, meltdown ou shutdown?"
        ) { onAnswer("LISTEN") }
    }

    fun speakQuestionThenListen() {
        speak(
            "Sentidos sobrecarregados. Eu estou aqui com você. " +
            "Qual é o tipo de crise que você está enfrentando agora? " +
            "Sobrecarga sensorial, meltdown ou shutdown?"
        ) { beginTriageListen() }
    }

    fun deliverAdvice(type: String, onDone: () -> Unit) {
        when (type) {
            "Meltdown" -> speak(
                "Você está seguro. Ninguém aqui para julgar você. " +
                "Se puder, saia do ambiente que está te sobrecarregando. " +
                "Agora, balance as mãos, aperte uma esfera ou balance o corpo: " +
                "um estímulo motor seguro vai ajudar o seu sistema a desescalonar."
            ) { onDone() }

            "Shutdown" -> speak("Sem estímulos agora. Você pode descansar.") {
                BinauralTone.play(seconds = 30)
                onDone()
            }

            else -> speak(
                "Vamos reduzir o estímulo. Feche os olhos ou se afaste da fonte de som. " +
                "Respiração quadrada comigo: inspire por quatro segundos, " +
                "segure por quatro, expire por quatro, segure por quatro. " +
                "Quatro... quatro... quatro... quatro. Mais uma vez, no seu ritmo."
            ) { onDone() }
        }
    }

    private fun ensureTts(onReady: () -> Unit) {
        tts?.let { onReady(); return }
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = pt
                    voices?.firstOrNull {
                        it.locale.language == "pt" && it.quality >= Voice.QUALITY_VERY_HIGH
                    }?.let { v -> voice = v }
                    setSpeechRate(0.92f)
                    setPitch(1.05f)
                }
            }
            onReady()
        }
    }

    fun speak(text: String, onDone: () -> Unit) {
        main.post {
            ensureTts {
                val t = tts
                if (t == null) { onDone(); return@ensureTts }
                t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { main.post { onDone() } }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { main.post { onDone() } }
                })
                t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "triage")
            }
        }
    }

    fun shutdown() {
        endSession()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

object BinauralTone {

    fun play(seconds: Int = 30, left: Double = 58.0, right: Double = 62.0) {
        Thread {
            val sr = 48000
            val total = sr * seconds
            val minBuf = AudioTrack.getMinBufferSize(
                sr, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return@Thread
            val track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sr)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
                minBuf * 2,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.play()
            var pL = 0.0
            var pR = 0.0
            val buf = ShortArray(4096)
            var written = 0
            while (written < total) {
                val n = minOf(buf.size / 2, total - written)
                for (i in 0 until n) {
                    buf[i * 2] = (sin(pL) * 9000).toInt().toShort()
                    buf[i * 2 + 1] = (sin(pR) * 9000).toInt().toShort()
                    pL += 2 * Math.PI * left / sr
                    pR += 2 * Math.PI * right / sr
                }
                track.write(buf, 0, n * 2)
                written += n
            }
            try { track.stop() } catch (_: Exception) {}
            track.release()
        }.start()
    }
}
