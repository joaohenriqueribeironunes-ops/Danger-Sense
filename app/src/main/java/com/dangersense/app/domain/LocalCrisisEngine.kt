package com.dangersense.app.domain

import com.dangersense.app.data.CrisisDao
import com.dangersense.app.data.CrisisEvent
import java.util.Calendar
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class Prediction(
    val risk: Float,
    val windowMinutes: Int,
    val topPackage: String,
    val confidence: Float,
    val detected: Boolean
)

class LocalCrisisEngine(private val dao: CrisisDao) {

    suspend fun record(e: CrisisEvent) = dao.insert(e)

    suspend fun predict(
        motion: Float,
        db: Float,
        now: Long = System.currentTimeMillis()
    ): Prediction {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dow = cal.get(Calendar.DAY_OF_WEEK)

        val history = dao.recentOnce(300)
        val n = history.size

        var temporal = 0f
        var topPkg = ""
        var topScore = 0f
        val pkgScores = HashMap<String, Float>()

        if (n > 0) {
            val nowH = now.toDouble()
            var hourHits = 0f
            var dayHits = 0f
            var hourTotal = 0f

            for (e in history) {
                val ageDays = (nowH - e.timestamp) / 86_400_000.0
                val w = exp(-ageDays / 14.0).toFloat()
                hourTotal += w
                if (e.hourOfDay == hour) hourHits += w
                if (e.dayOfWeek == dow) dayHits += w
                val s = pkgScores.getOrDefault(e.foregroundPackage, 0f) + w
                pkgScores[e.foregroundPackage] = s
                if (s > topScore) { topScore = s; topPkg = e.foregroundPackage }
            }

            temporal = 0.65f * (hourHits / max(hourTotal, 0.001f)) +
                    0.35f * (dayHits / max(hourTotal, 0.001f))
            temporal *= min(1f, n / 8f)
        }

        val motionRisk = min(1f, motion / 18f)
        val dbRisk = min(1f, max(0f, (db - 55f) / 35f))
        val live = 0.5f * motionRisk + 0.5f * dbRisk

        val risk = min(1f, 0.55f * temporal + 0.45f * live)
        val confidence = min(1f, n / 20f)

        return Prediction(
            risk = risk,
            windowMinutes = 30,
            topPackage = topPkg,
            confidence = confidence,
            detected = live >= 0.7f || risk >= 0.85f
        )
    }
}
