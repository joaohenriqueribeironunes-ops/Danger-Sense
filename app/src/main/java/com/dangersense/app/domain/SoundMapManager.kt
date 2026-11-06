package com.dangersense.app.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

object SoundMapManager {

    private val _peakDb = MutableStateFlow(0f)
    private val _peakAt = MutableStateFlow(0L)
    private val _log = MutableStateFlow<List<Pair<Long, Float>>>(emptyList())

    val peakDb: StateFlow<Float> = _peakDb.asStateFlow()
    val peakAt: StateFlow<Long> = _peakAt.asStateFlow()
    val log: StateFlow<List<Pair<Long, Float>>> = _log.asStateFlow()

    private var day = -1L

    @Synchronized
    fun sample(db: Float, time: Long) {
        val d = time / 86_400_000L
        if (d != day) {
            day = d
            _peakDb.value = 0f
            _peakAt.value = 0L
            _log.value = emptyList()
        }
        if (db > _peakDb.value) {
            _peakDb.value = db
            _peakAt.value = time
        }
        if (db >= 65f) {
            _log.value = (_log.value + (time to db)).takeLast(400)
        }
    }

    fun peakHour(): Int =
        if (_peakAt.value == 0L) -1
        else Calendar.getInstance().apply { timeInMillis = _peakAt.value }
            .get(Calendar.HOUR_OF_DAY)
}
