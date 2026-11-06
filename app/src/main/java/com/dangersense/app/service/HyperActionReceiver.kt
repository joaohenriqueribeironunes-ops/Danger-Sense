package com.dangersense.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class HyperActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        when (intent?.action) {
            ACTION_PAUSE -> SensorTelemetryService.requestPause()
            ACTION_FALSE_ALARM -> SensorTelemetryService.dismissLatest()
        }
        nm.cancel(NotificationTag.PROACTIVE_ID)
    }

    companion object {
        const val ACTION_PAUSE = "com.dangersense.app.ACTION_PAUSE"
        const val ACTION_FALSE_ALARM = "com.dangersense.app.ACTION_FALSE_ALARM"

        fun notifyProactive(context: Context, risk: Float, tipo: String) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        NotificationTag.CHANNEL, "Alertas proativos",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                )
            }
            fun action(act: String, code: Int) = PendingIntent.getBroadcast(
                context, code,
                Intent(context, HyperActionReceiver::class.java).setAction(act),
                PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(context, NotificationTag.CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Risco de crise: ${(risk * 100).toInt()}%")
                .setContentText(tipo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(0, "Preciso de pausa", action(ACTION_PAUSE, 1))
                .addAction(0, "Falso Alarme", action(ACTION_FALSE_ALARM, 2))
                .build()
            nm.notify(NotificationTag.PROACTIVE_ID, n)
        }
    }
}

object NotificationTag {
    const val CHANNEL = "hyper_proactive"
    const val PROACTIVE_ID = 301
}
