package com.dangersense.app.domain

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppUsageTracker(context: Context) {

    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _foreground = MutableStateFlow("")
    val foreground: StateFlow<String> = _foreground

    private var lastEventTime = 0L

    fun start() {
        if (job?.isActive == true) return
        lastEventTime = System.currentTimeMillis() - 5_000
        job = scope.launch {
            while (isActive) {
                poll()
                delay(1_500)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun poll() {
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(lastEventTime, now)
        var pkg = _foreground.value
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                e.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                pkg = e.packageName ?: pkg
            }
        }
        lastEventTime = now
        if (pkg != _foreground.value) _foreground.value = pkg
    }
}
